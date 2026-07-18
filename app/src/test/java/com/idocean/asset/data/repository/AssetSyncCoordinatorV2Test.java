package com.idocean.asset.data.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.idocean.asset.data.repository.LogRepository;
import com.idocean.asset.data.sync.AssetSyncCombinationBuilderV2;
import com.idocean.asset.data.sync.AssetSyncCoordinatorV2;
import com.idocean.asset.data.sync.AssetSyncErrorType;
import com.idocean.asset.data.sync.AssetSyncExecutionClientV2;
import com.idocean.asset.data.sync.AssetSyncExecutorV2;
import com.idocean.asset.data.sync.AssetSyncQueryV2;
import com.idocean.asset.model.Asset;
import com.idocean.asset.utils.AssetLocationUtils;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AssetSyncCoordinatorV2Test {
    @Test
    public void sync_fullSyncExecutesExactlyOneRequest() throws Exception {
        RecordingExecutor executor = new RecordingExecutor();
        executor.enqueue(Collections.singletonList(asset("CODE-01", "TID-01", "Laptop A", "SN-01")));

        AssetSyncCoordinatorV2 coordinator = new AssetSyncCoordinatorV2(
                LogRepository.getInstance(),
                new AssetSyncCombinationBuilderV2(),
                executor
        );

        AssetSyncCoordinatorV2.SyncResult result = coordinator.sync(AssetSyncQueryV2.fullSync());

        assertEquals(1, executor.getExecutedQueries().size());
        assertEquals(1, result.getRequestCount());
        assertEquals(1, result.getAssets().size());
    }

    @Test
    public void sync_filteredMultiValueExecutesAllSubRequestsAndDeduplicatesByTidAndCode() throws Exception {
        RecordingExecutor executor = new RecordingExecutor();
        executor.enqueue(Collections.singletonList(asset("CODE-01", "TID-01", "", "", "IT", "Warehouse")));
        executor.enqueue(Collections.singletonList(asset("CODE-01", "TID-01", "Laptop Dell", "SN-01", "IT", "Storage")));
        executor.enqueue(Collections.singletonList(asset("CODE-02", "TID-02", "Monitor A", "SN-02", "HR", "Warehouse")));
        executor.enqueue(Collections.singletonList(asset("CODE-03", "TID-03", "Printer A", "SN-03", "HR", "Storage")));

        AssetSyncCoordinatorV2 coordinator = new AssetSyncCoordinatorV2(
                LogRepository.getInstance(),
                new AssetSyncCombinationBuilderV2(),
                executor
        );

        AssetSyncQueryV2 query = AssetSyncQueryV2.filtered(
                Arrays.asList("IT", "HR"),
                Arrays.asList("Warehouse", "Storage"),
                Collections.<String>emptyList()
        );
        AssetSyncCoordinatorV2.SyncResult result = coordinator.sync(query);

        assertEquals(4, executor.getExecutedQueries().size());
        assertEquals(4, result.getRequestCount());
        assertEquals(3, result.getAssets().size());
        assertEquals("Laptop Dell", result.getAssets().get(0).getAssetName());
        assertEquals(AssetSyncQueryV2.Mode.FILTERED, executor.getExecutedQueries().get(0).getMode());
    }

    @Test
    public void sync_sessionModeRunsThroughV2SessionQuery() throws Exception {
        RecordingExecutor executor = new RecordingExecutor();
        executor.enqueue(Collections.singletonList(asset("CODE-11", "TID-11", "Dock", "SN-11")));

        AssetSyncCoordinatorV2 coordinator = new AssetSyncCoordinatorV2(
                LogRepository.getInstance(),
                new AssetSyncCombinationBuilderV2(),
                executor
        );

        AssetSyncCoordinatorV2.SyncResult result = coordinator.sync(
                AssetSyncQueryV2.session("IT", Collections.<String>emptyList(), Collections.<String>emptyList())
        );

        assertEquals(1, executor.getExecutedQueries().size());
        assertEquals(AssetSyncQueryV2.Mode.SESSION, executor.getExecutedQueries().get(0).getMode());
        assertEquals(1, result.getAssets().size());
    }

    @Test
    public void sync_subRequestFailureUsesStandardizedMessageWithCombinationIndex() {
        AssetSyncCoordinatorV2 coordinator = new AssetSyncCoordinatorV2(
                LogRepository.getInstance(),
                new AssetSyncCombinationBuilderV2(),
                new FailingExecutor(new IOException("HTTP 500"))
        );

        try {
            coordinator.sync(AssetSyncQueryV2.fullSync());
        } catch (AssetSyncCoordinatorV2.SyncFailureException exception) {
            assertEquals(AssetSyncErrorType.API, exception.getErrorType());
            assertTrue(exception.getMessage().contains("1/1"));
            assertTrue(exception.getMessage().contains("API"));
            return;
        }

        throw new AssertionError("Expected sync failure");
    }

    @Test
    public void sync_locationOnlyUsesLocationKeyAndFiltersByCanonicalGroup() throws Exception {
        AliasAwareExecutor executor = new AliasAwareExecutor();
        executor.put("TT16_F5", Arrays.asList(
                asset("CODE-01", "TID-01", "Laptop A", "SN-01", "Idoplex-5"),
                asset("CODE-02", "TID-02", "Laptop B", "SN-02", "L\u1ea7u 5 - TT16"),
                asset("CODE-03", "TID-03", "Laptop C", "SN-03", "Warehouse")
        ));

        AssetSyncCoordinatorV2 coordinator = new AssetSyncCoordinatorV2(
                LogRepository.getInstance(),
                new AssetSyncCombinationBuilderV2(),
                executor
        );

        AssetSyncCoordinatorV2.SyncResult result = coordinator.sync(
                AssetSyncQueryV2.filtered(
                        Collections.<String>emptyList(),
                        Collections.singletonList("L\u1ea7u 5 - TT16"),
                        Collections.<String>emptyList()
                )
        );

        assertEquals(
                Collections.singletonList(AssetLocationUtils.resolveLocationKey("L\u1ea7u 5 - TT16")),
                executor.getRequestLocationValues()
        );
        assertEquals(2, result.getAssets().size());
        assertEquals("CODE-01", result.getAssets().get(0).getAssetCode());
        assertEquals("CODE-02", result.getAssets().get(1).getAssetCode());
        assertTrue(result.getAssets().stream().noneMatch(asset -> "CODE-03".equals(asset.getAssetCode())));
    }

    @Test
    public void sync_locationOnlyFallsBackToWiderRequestWhenAliasFetchReturnsNoMatch() throws Exception {
        AliasAwareExecutor executor = new AliasAwareExecutor();
        executor.put("TT16_F5", Collections.<Asset>emptyList());
        executor.put("", Arrays.asList(
                asset("CODE-10", "TID-10", "Laptop A", "SN-10", "L\u1ea7u 5 - TT16"),
                asset("CODE-11", "TID-11", "Laptop B", "SN-11", "Warehouse")
        ));

        AssetSyncCoordinatorV2 coordinator = new AssetSyncCoordinatorV2(
                LogRepository.getInstance(),
                new AssetSyncCombinationBuilderV2(),
                executor
        );

        AssetSyncCoordinatorV2.SyncResult result = coordinator.sync(
                AssetSyncQueryV2.filtered(
                        Collections.<String>emptyList(),
                        Collections.singletonList("L\u1ea7u 5 - TT16"),
                        Collections.<String>emptyList()
                )
        );

        List<String> expectedRequests = new ArrayList<>(
                Collections.singletonList(AssetLocationUtils.resolveLocationKey("L\u1ea7u 5 - TT16"))
        );
        expectedRequests.add("");
        assertEquals(expectedRequests, executor.getRequestLocationValues());
        assertEquals(1, result.getAssets().size());
        assertEquals("CODE-10", result.getAssets().get(0).getAssetCode());
    }

    @Test
    public void sync_groupedQueryKeepsSuccessfulCombinationWhenAnotherCombinationIsEmpty() throws Exception {
        RecordingExecutor executor = new RecordingExecutor();
        executor.enqueue(Collections.singletonList(
                asset("CODE-21", "TID-21", "Laptop A", "SN-21", "IT", "L\u1ea7u 5 - TT16")
        ));
        executor.enqueue(Collections.<Asset>emptyList());

        AssetSyncCoordinatorV2 coordinator = new AssetSyncCoordinatorV2(
                LogRepository.getInstance(),
                new AssetSyncCombinationBuilderV2(),
                executor
        );

        AssetSyncCoordinatorV2.SyncResult result = coordinator.sync(
                AssetSyncQueryV2.filtered(
                        Arrays.asList("IT", "HR"),
                        Collections.singletonList("L\u1ea7u 5 - TT16"),
                        Collections.<String>emptyList()
                )
        );

        assertEquals(3, executor.getExecutedQueries().size());
        assertEquals(2, result.getRequestCount());
        assertEquals(1, result.getAssets().size());
        assertEquals("CODE-21", result.getAssets().get(0).getAssetCode());
        assertEquals("IT", executor.getExecutedQueries().get(0).getSingleDepartment());
        assertEquals("HR", executor.getExecutedQueries().get(1).getSingleDepartment());
        assertEquals("", executor.getExecutedQueries().get(2).getSingleLocation());
    }

    private Asset asset(String assetCode, String tid, String assetName, String serialNumber) {
        return asset(assetCode, tid, assetName, serialNumber, "IT", "Lau 5 - TT16");
    }

    private Asset asset(String assetCode, String tid, String assetName, String serialNumber, String location) {
        return asset(assetCode, tid, assetName, serialNumber, "IT", location);
    }

    private Asset asset(
            String assetCode,
            String tid,
            String assetName,
            String serialNumber,
            String department,
            String location
    ) {
        return new Asset(
                1,
                assetCode,
                tid,
                "",
                "",
                assetName,
                "Laptop",
                serialNumber,
                department,
                "",
                location,
                "",
                "",
                "",
                "",
                "",
                "API"
        );
    }

    private static final class RecordingExecutor implements AssetSyncExecutionClientV2 {
        private final List<List<Asset>> queuedResults = new ArrayList<>();
        private final List<AssetSyncQueryV2> executedQueries = new ArrayList<>();

        void enqueue(List<Asset> assets) {
            queuedResults.add(assets == null ? Collections.<Asset>emptyList() : new ArrayList<>(assets));
        }

        List<AssetSyncQueryV2> getExecutedQueries() {
            return executedQueries;
        }

        @Override
        public AssetSyncExecutorV2.ExecutionResult execute(AssetSyncQueryV2 query) {
            executedQueries.add(query);
            int resultIndex = executedQueries.size() - 1;
            List<Asset> assets = resultIndex < queuedResults.size()
                    ? queuedResults.get(resultIndex)
                    : Collections.<Asset>emptyList();
            return new AssetSyncExecutorV2.ExecutionResult(
                    query,
                    "mock://sync-v2/" + resultIndex,
                    assets
            );
        }
    }

    private static final class FailingExecutor implements AssetSyncExecutionClientV2 {
        private final IOException exception;

        FailingExecutor(IOException exception) {
            this.exception = exception;
        }

        @Override
        public AssetSyncExecutorV2.ExecutionResult execute(AssetSyncQueryV2 query) throws IOException {
            throw exception;
        }
    }

    private static final class AliasAwareExecutor implements AssetSyncExecutionClientV2 {
        private final Map<String, List<Asset>> resultsByLocation = new LinkedHashMap<>();
        private final List<String> requestLocationValues = new ArrayList<>();

        void put(String requestLocationValue, List<Asset> assets) {
            resultsByLocation.put(
                    requestLocationValue,
                    assets == null ? Collections.<Asset>emptyList() : new ArrayList<>(assets)
            );
        }

        List<String> getRequestLocationValues() {
            return requestLocationValues;
        }

        @Override
        public AssetSyncExecutorV2.ExecutionResult execute(AssetSyncQueryV2 query) {
            String requestLocationValue = query.getRequestLocationValue();
            requestLocationValues.add(requestLocationValue);
            List<Asset> assets = resultsByLocation.containsKey(requestLocationValue)
                    ? resultsByLocation.get(requestLocationValue)
                    : Collections.<Asset>emptyList();
            return new AssetSyncExecutorV2.ExecutionResult(
                    query,
                    "mock://sync-v2/" + requestLocationValue,
                    assets
            );
        }
    }
}

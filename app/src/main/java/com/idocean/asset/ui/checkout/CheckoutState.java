package com.idocean.asset.ui.checkout;

import com.idocean.asset.model.Asset;
import com.idocean.asset.model.CheckInResultItem;
import com.idocean.asset.model.CheckoutAssetItem;
import com.idocean.asset.model.ImportedCheckoutData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Trang thai runtime cua man checkout/checkin.
 */
public final class CheckoutState {
    private final LinkedHashMap<String, Asset> cachedByTid = new LinkedHashMap<>();
    private final LinkedHashMap<String, Asset> cachedByCode = new LinkedHashMap<>();
    private final LinkedHashMap<String, CheckoutAssetItem> checkoutItems = new LinkedHashMap<>();
    private final LinkedHashMap<String, CheckInResultItem> expectedCheckinItems = new LinkedHashMap<>();
    private final LinkedHashMap<String, String> expectedByTid = new LinkedHashMap<>();
    private final LinkedHashMap<String, String> expectedByCode = new LinkedHashMap<>();

    private ImportedCheckoutData importedCheckoutData;

    public Map<String, Asset> getCachedByTid() {
        return cachedByTid;
    }

    public Map<String, Asset> getCachedByCode() {
        return cachedByCode;
    }

    public Map<String, CheckoutAssetItem> getCheckoutItems() {
        return checkoutItems;
    }

    public Map<String, CheckInResultItem> getExpectedCheckinItems() {
        return expectedCheckinItems;
    }

    public Map<String, String> getExpectedByTid() {
        return expectedByTid;
    }

    public Map<String, String> getExpectedByCode() {
        return expectedByCode;
    }

    public ImportedCheckoutData getImportedCheckoutData() {
        return importedCheckoutData;
    }

    public void setImportedCheckoutData(ImportedCheckoutData importedCheckoutData) {
        this.importedCheckoutData = importedCheckoutData;
    }

    public void replaceCachedAssets(Map<String, Asset> byTid, Map<String, Asset> byCode) {
        cachedByTid.clear();
        if (byTid != null) {
            cachedByTid.putAll(byTid);
        }
        cachedByCode.clear();
        if (byCode != null) {
            cachedByCode.putAll(byCode);
        }
    }

    public void clearCheckoutItems() {
        checkoutItems.clear();
    }

    public void removeCheckoutItem(String identityKey) {
        if (identityKey == null) {
            return;
        }
        checkoutItems.remove(identityKey);
    }

    public void resetCheckinSession() {
        expectedCheckinItems.clear();
        expectedByTid.clear();
        expectedByCode.clear();
        if (importedCheckoutData != null) {
            for (CheckoutAssetItem item : importedCheckoutData.getExpectedItems()) {
                CheckInResultItem resultItem = CheckInResultItem.fromExpected(item);
                expectedCheckinItems.put(resultItem.getIdentityKey(), resultItem);
                String normalizedTid = CheckoutStateUtils.normalizeKey(item.getTid());
                if (!normalizedTid.isEmpty() && !expectedByTid.containsKey(normalizedTid)) {
                    expectedByTid.put(normalizedTid, resultItem.getIdentityKey());
                }
                String normalizedCode = CheckoutStateUtils.normalizeKey(item.getCode());
                if (!normalizedCode.isEmpty() && !expectedByCode.containsKey(normalizedCode)) {
                    expectedByCode.put(normalizedCode, resultItem.getIdentityKey());
                }
            }
        }
    }

    public Snapshot snapshot() {
        return new Snapshot(
                new ArrayList<>(checkoutItems.values()),
                importedCheckoutData,
                new ArrayList<>(expectedCheckinItems.values())
        );
    }

    public void restore(Snapshot snapshot) {
        checkoutItems.clear();
        expectedCheckinItems.clear();
        expectedByTid.clear();
        expectedByCode.clear();
        if (snapshot == null) {
            importedCheckoutData = null;
            return;
        }
        if (snapshot.checkoutItems != null) {
            for (CheckoutAssetItem item : snapshot.checkoutItems) {
                checkoutItems.put(item.getIdentityKey(), item);
            }
        }
        importedCheckoutData = snapshot.importedCheckoutData;
        if (snapshot.expectedItems != null) {
            for (CheckInResultItem item : snapshot.expectedItems) {
                expectedCheckinItems.put(item.getIdentityKey(), item);
                String normalizedTid = CheckoutStateUtils.normalizeKey(item.getTid());
                if (!normalizedTid.isEmpty() && !expectedByTid.containsKey(normalizedTid)) {
                    expectedByTid.put(normalizedTid, item.getIdentityKey());
                }
                String normalizedCode = CheckoutStateUtils.normalizeKey(item.getCode());
                if (!normalizedCode.isEmpty() && !expectedByCode.containsKey(normalizedCode)) {
                    expectedByCode.put(normalizedCode, item.getIdentityKey());
                }
            }
        }
    }

    public static final class Snapshot {
        private final List<CheckoutAssetItem> checkoutItems;
        private final ImportedCheckoutData importedCheckoutData;
        private final List<CheckInResultItem> expectedItems;

        Snapshot(List<CheckoutAssetItem> checkoutItems,
                 ImportedCheckoutData importedCheckoutData,
                 List<CheckInResultItem> expectedItems) {
            this.checkoutItems = checkoutItems == null ? new ArrayList<>() : checkoutItems;
            this.importedCheckoutData = importedCheckoutData;
            this.expectedItems = expectedItems == null ? new ArrayList<>() : expectedItems;
        }

        public List<CheckoutAssetItem> getCheckoutItems() {
            return checkoutItems;
        }

        public ImportedCheckoutData getImportedCheckoutData() {
            return importedCheckoutData;
        }

        public List<CheckInResultItem> getExpectedItems() {
            return expectedItems;
        }
    }
}

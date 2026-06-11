package com.idocean.asset.data.repository;

import com.idocean.asset.model.AssetSyncQuery;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import okhttp3.HttpUrl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AssetSyncRequestBuilderTest {
    private final AssetSyncRequestBuilder builder = new AssetSyncRequestBuilder();

    @Test
    public void buildGetDbUrl_usesPluralCsvKeysForRemoteMultiValueSync() {
        AssetSyncQuery query = AssetSyncQuery.withFilters(
                Arrays.asList("IT", "HR"),
                Arrays.asList("Lầu 5 - TT16", "Lầu 6 - TT16"),
                Arrays.asList(
                        "Lầu 5 - TT16",
                        "Idoplex - 5",
                        "Idoplex-5",
                        "Lầu 5 - TT17",
                        "Lầu 6 - TT16",
                        "Idoplex - 6",
                        "Idoplex-6"
                ),
                Arrays.asList("LAPTOP", "ADAPTER"),
                300
        );

        String url = builder.buildGetDbUrl("https://n8n.idocean.info:8443/", query, null, 300, 0);
        HttpUrl httpUrl = HttpUrl.parse(url);

        assertNotNull(httpUrl);
        assertTrue(httpUrl.queryParameterValues("department").isEmpty());
        assertTrue(httpUrl.queryParameterValues("location").isEmpty());
        assertTrue(httpUrl.queryParameterValues("assetType").isEmpty());
        assertEquals("IT,HR", httpUrl.queryParameter("departments"));
        assertEquals(
                "Lầu 5 - TT16,Idoplex - 5,Idoplex-5,Lầu 5 - TT17,Lầu 6 - TT16,Idoplex - 6,Idoplex-6",
                httpUrl.queryParameter("locations")
        );
        assertEquals("LAPTOP,ADAPTER", httpUrl.queryParameter("assetTypes"));
        assertEquals("300", httpUrl.queryParameter("limit"));
        assertEquals("0", httpUrl.queryParameter("offset"));
    }

    @Test
    public void buildGetDbUrl_usesPluralLocationKeyForSingleCanonicalLocationWithAliases() {
        AssetSyncQuery query = AssetSyncQuery.withFilters(
                Collections.singletonList("IT"),
                Collections.singletonList("Lầu 5 - TT16"),
                Arrays.asList("Lầu 5 - TT16", "Idoplex - 5", "Idoplex-5", "Lầu 5 - TT17"),
                Collections.singletonList("LAPTOP"),
                300
        );

        String url = builder.buildGetDbUrl("https://n8n.idocean.info:8443/", query, null, 300, 0);
        HttpUrl httpUrl = HttpUrl.parse(url);

        assertNotNull(httpUrl);
        assertEquals("IT", httpUrl.queryParameter("department"));
        assertEquals("LAPTOP", httpUrl.queryParameter("assetType"));
        assertTrue(httpUrl.queryParameterValues("location").isEmpty());
        assertEquals("Lầu 5 - TT16,Idoplex - 5,Idoplex-5,Lầu 5 - TT17", httpUrl.queryParameter("locations"));
    }

    @Test
    public void buildGetDbUrl_keepsSingularKeysForSingleDisplayValuesWithoutAliases() {
        AssetSyncQuery query = AssetSyncQuery.withFilters(
                Collections.singletonList("IT"),
                Collections.singletonList("Kho tổng"),
                Collections.singletonList("Kho tổng"),
                Collections.singletonList("LAPTOP"),
                300
        );

        String url = builder.buildGetDbUrl("https://n8n.idocean.info:8443/", query, null, 300, 0);
        HttpUrl httpUrl = HttpUrl.parse(url);

        assertNotNull(httpUrl);
        assertEquals("IT", httpUrl.queryParameter("department"));
        assertEquals("Kho tổng", httpUrl.queryParameter("location"));
        assertEquals("LAPTOP", httpUrl.queryParameter("assetType"));
        assertTrue(httpUrl.queryParameterValues("departments").isEmpty());
        assertTrue(httpUrl.queryParameterValues("locations").isEmpty());
        assertTrue(httpUrl.queryParameterValues("assetTypes").isEmpty());
    }

    @Test
    public void buildGetDbUrl_usesLocationOverrideWhenProvided() {
        AssetSyncQuery query = AssetSyncQuery.withFilters(
                Arrays.asList("IT"),
                Arrays.asList("Lầu 5 - TT16", "Lầu 6 - TT16"),
                Arrays.asList("Lầu 5 - TT16", "Idoplex - 5", "Idoplex-5"),
                Arrays.asList("LAPTOP"),
                300
        );

        String url = builder.buildGetDbUrl("https://n8n.idocean.info:8443/", query, "Idoplex-5", 300, 0);
        HttpUrl httpUrl = HttpUrl.parse(url);

        assertNotNull(httpUrl);
        assertEquals("Idoplex-5", httpUrl.queryParameter("location"));
        assertTrue(httpUrl.queryParameterValues("locations").isEmpty());
    }

    @Test
    public void buildGetDbUrl_omitsEmptyGroupsForFullSync() {
        AssetSyncQuery query = AssetSyncQuery.withFilters(
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                300
        );

        String url = builder.buildGetDbUrl("https://n8n.idocean.info:8443/", query, null, 300, 0);
        HttpUrl httpUrl = HttpUrl.parse(url);

        assertNotNull(httpUrl);
        assertTrue(httpUrl.queryParameterValues("department").isEmpty());
        assertTrue(httpUrl.queryParameterValues("location").isEmpty());
        assertTrue(httpUrl.queryParameterValues("assetType").isEmpty());
        assertTrue(httpUrl.queryParameterValues("departments").isEmpty());
        assertTrue(httpUrl.queryParameterValues("locations").isEmpty());
        assertTrue(httpUrl.queryParameterValues("assetTypes").isEmpty());
        assertEquals("300", httpUrl.queryParameter("limit"));
        assertEquals("0", httpUrl.queryParameter("offset"));
    }
}

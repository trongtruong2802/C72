package com.idocean.asset.data.repository;

import org.junit.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;

import static org.junit.Assert.assertEquals;

public class AssetSyncV2ServiceTest {
    @Test
    public void classifyIOException_detectsHttpAsApi() {
        assertEquals(
                AssetSyncErrorType.API,
                AssetSyncV2Service.classifyIOException(new IOException("HTTP 500"))
        );
    }

    @Test
    public void classifyIOException_detectsCacheWriteAsStorage() {
        assertEquals(
                AssetSyncErrorType.STORAGE,
                AssetSyncV2Service.classifyIOException(new IOException("CACHE_WRITE: disk full"))
        );
    }

    @Test
    public void classifyIOException_defaultsToNetwork() {
        assertEquals(
                AssetSyncErrorType.NETWORK,
                AssetSyncV2Service.classifyIOException(new IOException("socket closed"))
        );
    }

    @Test
    public void classifyIOException_detectsTimeoutAsTimeout() {
        assertEquals(
                AssetSyncErrorType.TIMEOUT,
                AssetSyncV2Service.classifyIOException(new SocketTimeoutException("timeout"))
        );
    }
}

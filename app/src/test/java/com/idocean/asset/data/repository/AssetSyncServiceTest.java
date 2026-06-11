package com.idocean.asset.data.repository;

import org.junit.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;

import static org.junit.Assert.assertEquals;

public class AssetSyncServiceTest {
    @Test
    public void classifyIOException_detectsHttpAsApi() {
        assertEquals(
                AssetSyncErrorType.API,
                AssetSyncService.classifyIOException(new IOException("HTTP 500"))
        );
    }

    @Test
    public void classifyIOException_detectsCacheWriteAsStorage() {
        assertEquals(
                AssetSyncErrorType.STORAGE,
                AssetSyncService.classifyIOException(new IOException("CACHE_WRITE: disk full"))
        );
    }

    @Test
    public void classifyIOException_defaultsToNetwork() {
        assertEquals(
                AssetSyncErrorType.NETWORK,
                AssetSyncService.classifyIOException(new IOException("socket closed"))
        );
    }

    @Test
    public void classifyIOException_detectsTimeoutAsTimeout() {
        assertEquals(
                AssetSyncErrorType.TIMEOUT,
                AssetSyncService.classifyIOException(new SocketTimeoutException("timeout"))
        );
    }
}

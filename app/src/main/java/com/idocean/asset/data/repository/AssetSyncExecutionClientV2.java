package com.idocean.asset.data.repository;

import com.google.gson.JsonParseException;

import java.io.IOException;

interface AssetSyncExecutionClientV2 {
    AssetSyncExecutorV2.ExecutionResult execute(AssetSyncQueryV2 query) throws IOException, JsonParseException;
}

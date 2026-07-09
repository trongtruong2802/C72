package com.idocean.asset.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "pending_mutations")
public class PendingMutation {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    @NonNull
    private final String actionType;
    
    @NonNull
    private final String assetCode;
    
    @NonNull
    private final String payload;
    
    private final long createdAt;

    public PendingMutation(@NonNull String actionType, @NonNull String assetCode, @NonNull String payload, long createdAt) {
        this.actionType = actionType;
        this.assetCode = assetCode;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @NonNull
    public String getActionType() {
        return actionType;
    }

    @NonNull
    public String getAssetCode() {
        return assetCode;
    }

    @NonNull
    public String getPayload() {
        return payload;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}

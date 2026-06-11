package com.idocean.asset.data.dto;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Batch payload gui len webhook check-in.
 */
public final class InventoryCheckinBatchRequestDto {
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    @SerializedName("items")
    private final List<InventoryCheckinRequestItemDto> items;

    public InventoryCheckinBatchRequestDto(List<InventoryCheckinRequestItemDto> items) {
        List<InventoryCheckinRequestItemDto> safeItems = items == null
                ? Collections.<InventoryCheckinRequestItemDto>emptyList()
                : new ArrayList<>(items);
        this.items = Collections.unmodifiableList(safeItems);
    }

    public List<InventoryCheckinRequestItemDto> getItems() {
        return items;
    }

    public int size() {
        return items.size();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public JsonObject toJson() {
        return GSON.toJsonTree(this).getAsJsonObject();
    }
}

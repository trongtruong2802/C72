package com.idocean.asset.data.mapper;

import com.idocean.asset.data.dto.InventoryCheckinBatchRequestDto;
import com.idocean.asset.data.dto.InventoryCheckinRequestItemDto;
import com.idocean.asset.model.InventoryItemStatus;
import com.idocean.asset.model.InventoryScanSource;
import com.idocean.asset.model.InventorySessionItem;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Mapper doc lap tu inventory session sang payload check-in.
 */
public final class InventoryCheckinPayloadMapper {
    private static final DateTimeFormatter API_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.US);
    private static final String WORKFLOW_STATUS_CHECKED = "\u0110\u00e3 ki\u1ec3m k\u00ea";
    private static final String WORKFLOW_STATUS_OUTSIDE = "Ngo\u00e0i danh s\u00e1ch";

    public InventoryCheckinBatchRequestDto buildRequest(List<InventorySessionItem> items) {
        List<InventoryCheckinRequestItemDto> requestItems = new ArrayList<>();
        if (items != null) {
            for (InventorySessionItem item : items) {
                if (item == null) {
                    continue;
                }
                InventoryCheckinRequestItemDto requestItem = mapItem(item);
                if (requestItem != null) {
                    requestItems.add(requestItem);
                }
            }
        }
        return new InventoryCheckinBatchRequestDto(requestItems);
    }

    public InventoryCheckinRequestItemDto mapItem(InventorySessionItem item) {
        InventorySessionItem safeItem = item;
        if (safeItem == null) {
            return null;
        }
        if (!isEligibleForUpload(safeItem)) {
            return null;
        }
        return new InventoryCheckinRequestItemDto(
                nullableValue(safeItem.getDisplayCode()),
                nullableValue(safeItem.getDisplayTid()),
                nullableValue(safeItem.getDisplayEpcHex()),
                resolveScanSource(safeItem.getScanSource()),
                formatApiTimestamp(safeItem.getScannedAt()),
                resolveWorkflowInventoryStatus(safeItem.getStatus()),
                nullableValue(safeItem.getAssetName()),
                nullableValue(safeItem.getAssignedUser()),
                nullableValue(safeItem.getDepartment()),
                nullableValue(safeItem.getLocation()),
                nullableValue(safeItem.getAssetType()),
                nullableValue(safeItem.getSerialNumber()),
                nullableValue(safeItem.getOperatorName()),
                nullableValue(safeItem.getInventoryNote())
        );
    }

    static boolean isEligibleForUpload(InventorySessionItem item) {
        if (item == null || item.getScannedAt() <= 0L) {
            return false;
        }
        InventoryItemStatus status = item.getStatus();
        return status == InventoryItemStatus.CHECKED || status == InventoryItemStatus.OUTSIDE;
    }

    static String formatApiTimestamp(long scannedAt) {
        if (scannedAt <= 0L) {
            return null;
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(scannedAt), ZoneId.systemDefault())
                .format(API_TIMESTAMP_FORMATTER);
    }

    private static String resolveWorkflowInventoryStatus(InventoryItemStatus status) {
        if (status == InventoryItemStatus.CHECKED) {
            return WORKFLOW_STATUS_CHECKED;
        }
        if (status == InventoryItemStatus.OUTSIDE) {
            return WORKFLOW_STATUS_OUTSIDE;
        }
        return null;
    }

    private static String resolveScanSource(InventoryScanSource scanSource) {
        if (scanSource == null || scanSource == InventoryScanSource.NONE) {
            return null;
        }
        return nullableValue(scanSource.getLabel());
    }

    private static String nullableValue(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }
}

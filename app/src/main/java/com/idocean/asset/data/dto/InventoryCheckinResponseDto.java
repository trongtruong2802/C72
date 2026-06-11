package com.idocean.asset.data.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ket qua tra ve tu webhook check-in.
 */
public final class InventoryCheckinResponseDto {
    private final boolean success;
    private final String message;
    private final String sessionId;
    private final int totalReceived;
    private final int totalScannedValid;
    private final int totalSkipped;
    private final int totalInserted;
    private final List<InventoryCheckinInsertedRowDto> insertedRows;

    public InventoryCheckinResponseDto(
            boolean success,
            String message,
            String sessionId,
            int totalReceived,
            int totalScannedValid,
            int totalSkipped,
            int totalInserted,
            List<InventoryCheckinInsertedRowDto> insertedRows
    ) {
        this.success = success;
        this.message = safe(message);
        this.sessionId = safe(sessionId);
        this.totalReceived = Math.max(0, totalReceived);
        this.totalScannedValid = Math.max(0, totalScannedValid);
        this.totalSkipped = Math.max(0, totalSkipped);
        this.totalInserted = Math.max(0, totalInserted);
        this.insertedRows = Collections.unmodifiableList(
                insertedRows == null
                        ? Collections.<InventoryCheckinInsertedRowDto>emptyList()
                        : new ArrayList<>(insertedRows)
        );
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getSessionId() {
        return sessionId;
    }

    public int getTotalReceived() {
        return totalReceived;
    }

    public int getTotalScannedValid() {
        return totalScannedValid;
    }

    public int getTotalSkipped() {
        return totalSkipped;
    }

    public int getTotalInserted() {
        return totalInserted;
    }

    public List<InventoryCheckinInsertedRowDto> getInsertedRows() {
        return insertedRows;
    }

    public boolean isWarningOnly() {
        return success && totalInserted <= 0;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

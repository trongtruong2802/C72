package com.idocean.asset.data.dto;

/**
 * Tom tat dong da duoc insert thanh cong tu webhook check-in.
 */
public final class InventoryCheckinInsertedRowDto {
    private final String code;
    private final String tid;
    private final String checkin;

    public InventoryCheckinInsertedRowDto(String code, String tid, String checkin) {
        this.code = safe(code);
        this.tid = safe(tid);
        this.checkin = safe(checkin);
    }

    public String getCode() {
        return code;
    }

    public String getTid() {
        return tid;
    }

    public String getCheckin() {
        return checkin;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

package com.idocean.asset.model;

/**
 * Dữ liệu log thao tác hiển thị ở màn nhật ký.
 */
public class OperationLogEntry {
    private final long id;
    private final long timestamp;
    private final String action;
    private final String message;
    private final String detail;
    private final boolean error;

    public OperationLogEntry(long timestamp, String message) {
        this(timestamp, timestamp, "SYSTEM", message, "", false);
    }

    public OperationLogEntry(long id, long timestamp, String action, String message, String detail, boolean error) {
        this.id = id;
        this.timestamp = timestamp;
        this.action = action == null ? "SYSTEM" : action;
        this.message = message == null ? "" : message;
        this.detail = detail == null ? "" : detail;
        this.error = error;
    }

    public long getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getAction() {
        return action;
    }

    public String getMessage() {
        return message;
    }

    public String getDetail() {
        return detail;
    }

    public boolean isError() {
        return error;
    }
}

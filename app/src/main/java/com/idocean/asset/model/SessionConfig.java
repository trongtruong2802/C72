package com.idocean.asset.model;

/**
 * Cấu hình phiên làm việc hiện tại.
 */
public class SessionConfig {
    private final String operatorName;
    private final String department;
    private final String sessionNote;
    private final boolean manualEntryEachSession;

    public SessionConfig(String operatorName, String department, String sessionNote, boolean manualEntryEachSession) {
        this.operatorName = operatorName;
        this.department = department;
        this.sessionNote = sessionNote;
        this.manualEntryEachSession = manualEntryEachSession;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public String getDepartment() {
        return department;
    }

    public String getSessionNote() {
        return sessionNote;
    }

    public boolean isManualEntryEachSession() {
        return manualEntryEachSession;
    }
}

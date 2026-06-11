package com.idocean.asset.data.dto;

public class SessionDto {
    public final String operatorName;
    public final String department;
    public final String sessionNote;
    public final boolean manualEntryEachSession;

    public SessionDto(String operatorName, String department, String sessionNote, boolean manualEntryEachSession) {
        this.operatorName = operatorName;
        this.department = department;
        this.sessionNote = sessionNote;
        this.manualEntryEachSession = manualEntryEachSession;
    }
}

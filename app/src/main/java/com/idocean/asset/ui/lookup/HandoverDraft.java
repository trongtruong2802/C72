package com.idocean.asset.ui.lookup;

/**
 * Draft thong tin ban giao cua man tra cuu.
 */
public final class HandoverDraft {
    public final String newUser;
    public final String newDepartment;
    public final String newLocation;
    public final String handoverDate;

    public HandoverDraft(String newUser, String newDepartment, String newLocation, String handoverDate) {
        this.newUser = newUser;
        this.newDepartment = newDepartment;
        this.newLocation = newLocation;
        this.handoverDate = handoverDate;
    }
}

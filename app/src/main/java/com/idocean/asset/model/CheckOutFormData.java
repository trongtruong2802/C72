package com.idocean.asset.model;

import java.io.Serializable;

/**
 * Du lieu form check out duoc lap lai tren moi dong CSV de check in co the import lai.
 */
public class CheckOutFormData implements Serializable {
    private final String ticketId;
    private final String exportedAt;
    private final String carrierName;
    private final String department;
    private final String purpose;
    private final String eventName;
    private final String checkoutAt;
    private final String expectedReturnAt;
    private final String approver;
    private final String note;

    public CheckOutFormData(String ticketId,
                            String exportedAt,
                            String carrierName,
                            String department,
                            String purpose,
                            String eventName,
                            String checkoutAt,
                            String expectedReturnAt,
                            String approver,
                            String note) {
        this.ticketId = safe(ticketId);
        this.exportedAt = safe(exportedAt);
        this.carrierName = safe(carrierName);
        this.department = safe(department);
        this.purpose = safe(purpose);
        this.eventName = safe(eventName);
        this.checkoutAt = safe(checkoutAt);
        this.expectedReturnAt = safe(expectedReturnAt);
        this.approver = safe(approver);
        this.note = safe(note);
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getExportedAt() {
        return exportedAt;
    }

    public String getCarrierName() {
        return carrierName;
    }

    public String getDepartment() {
        return department;
    }

    public String getPurpose() {
        return purpose;
    }

    public String getEventName() {
        return eventName;
    }

    public String getCheckoutAt() {
        return checkoutAt;
    }

    public String getExpectedReturnAt() {
        return expectedReturnAt;
    }

    public String getApprover() {
        return approver;
    }

    public String getNote() {
        return note;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

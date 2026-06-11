package com.idocean.asset.ui.checkout;

/**
 * Draft form cho phieu check out.
 */
public final class CheckoutDraft {
    private final String carrierName;
    private final String department;
    private final String purpose;
    private final String eventName;
    private final String checkoutAt;
    private final String expectedReturnAt;
    private final String approver;
    private final String note;

    public CheckoutDraft(String carrierName,
                         String department,
                         String purpose,
                         String eventName,
                         String checkoutAt,
                         String expectedReturnAt,
                         String approver,
                         String note) {
        this.carrierName = safe(carrierName);
        this.department = safe(department);
        this.purpose = safe(purpose);
        this.eventName = safe(eventName);
        this.checkoutAt = safe(checkoutAt);
        this.expectedReturnAt = safe(expectedReturnAt);
        this.approver = safe(approver);
        this.note = safe(note);
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

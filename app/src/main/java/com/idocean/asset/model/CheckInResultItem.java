package com.idocean.asset.model;

import java.io.Serializable;

/**
 * Item chi tiet ket qua check in sau khi doi chieu voi file check out.
 */
public class CheckInResultItem implements Serializable {
    private final String identityKey;
    private final boolean expectedInTicket;
    private final String tid;
    private final String code;
    private final String assetName;
    private final String assetType;
    private final String serialNumber;
    private final String department;
    private final String assignedUser;
    private final String location;
    private final String checkoutScanSource;
    private final long checkoutScannedAt;

    private CheckInResultStatus status;
    private String matchedBy;
    private String checkinScanSource;
    private long checkinScannedAt;
    private String note;

    public CheckInResultItem(String identityKey,
                             boolean expectedInTicket,
                             String tid,
                             String code,
                             String assetName,
                             String assetType,
                             String serialNumber,
                             String department,
                             String assignedUser,
                             String location,
                             String checkoutScanSource,
                             long checkoutScannedAt,
                             CheckInResultStatus status,
                             String matchedBy,
                             String checkinScanSource,
                             long checkinScannedAt,
                             String note) {
        this.identityKey = safe(identityKey);
        this.expectedInTicket = expectedInTicket;
        this.tid = safe(tid);
        this.code = safe(code);
        this.assetName = safe(assetName);
        this.assetType = safe(assetType);
        this.serialNumber = safe(serialNumber);
        this.department = safe(department);
        this.assignedUser = safe(assignedUser);
        this.location = safe(location);
        this.checkoutScanSource = safe(checkoutScanSource);
        this.checkoutScannedAt = checkoutScannedAt;
        this.status = status == null ? CheckInResultStatus.MISSING : status;
        this.matchedBy = safe(matchedBy);
        this.checkinScanSource = safe(checkinScanSource);
        this.checkinScannedAt = checkinScannedAt;
        this.note = safe(note);
    }

    public static CheckInResultItem fromExpected(CheckoutAssetItem item) {
        return new CheckInResultItem(
                item.getIdentityKey(),
                true,
                item.getTid(),
                item.getCode(),
                item.getAssetName(),
                item.getAssetType(),
                item.getSerialNumber(),
                item.getDepartment(),
                item.getAssignedUser(),
                item.getLocation(),
                item.getScanSource(),
                item.getScannedAt(),
                CheckInResultStatus.MISSING,
                "",
                "",
                0L,
                ""
        );
    }

    public static CheckInResultItem createOutside(String identityKey,
                                                  String tid,
                                                  String code,
                                                  String assetName,
                                                  String assetType,
                                                  String serialNumber,
                                                  String department,
                                                  String assignedUser,
                                                  String location,
                                                  String checkinScanSource,
                                                  long checkinScannedAt,
                                                  String note) {
        return new CheckInResultItem(
                identityKey,
                false,
                tid,
                code,
                assetName,
                assetType,
                serialNumber,
                department,
                assignedUser,
                location,
                "",
                0L,
                CheckInResultStatus.OUTSIDE,
                "",
                checkinScanSource,
                checkinScannedAt,
                note
        );
    }

    public void markReturned(String matchedBy, String checkinScanSource, long checkinScannedAt) {
        this.status = CheckInResultStatus.RETURNED;
        this.matchedBy = safe(matchedBy);
        this.checkinScanSource = safe(checkinScanSource);
        this.checkinScannedAt = checkinScannedAt;
        this.note = "";
    }

    public String getIdentityKey() {
        return identityKey;
    }

    public boolean isExpectedInTicket() {
        return expectedInTicket;
    }

    public String getTid() {
        return tid;
    }

    public String getCode() {
        return code;
    }

    public String getAssetName() {
        return assetName;
    }

    public String getAssetType() {
        return assetType;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getDepartment() {
        return department;
    }

    public String getAssignedUser() {
        return assignedUser;
    }

    public String getLocation() {
        return location;
    }

    public String getCheckoutScanSource() {
        return checkoutScanSource;
    }

    public long getCheckoutScannedAt() {
        return checkoutScannedAt;
    }

    public CheckInResultStatus getStatus() {
        return status;
    }

    public String getMatchedBy() {
        return matchedBy;
    }

    public String getCheckinScanSource() {
        return checkinScanSource;
    }

    public long getCheckinScannedAt() {
        return checkinScannedAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = safe(note);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}

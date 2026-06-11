package com.idocean.asset.ui.checkout;

import com.idocean.asset.data.repository.CheckoutCsvRepository;
import com.idocean.asset.data.repository.LogRepository;
import com.idocean.asset.model.Asset;
import com.idocean.asset.model.CheckInResultItem;
import com.idocean.asset.model.CheckInResultStatus;
import com.idocean.asset.model.CheckOutFormData;
import com.idocean.asset.model.CheckoutAssetItem;
import com.idocean.asset.model.ImportedCheckoutData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CheckoutController {
    public static final class ValidationResult {
        public enum Field {
            NONE,
            CARRIER_NAME,
            DEPARTMENT,
            PURPOSE,
            EVENT,
            CHECKOUT_TIME,
            EXPECTED_RETURN,
            APPROVER
        }

        private final Field field;
        private final String message;

        private ValidationResult(Field field, String message) {
            this.field = field;
            this.message = message == null ? "" : message;
        }

        public static ValidationResult ok() {
            return new ValidationResult(Field.NONE, "");
        }

        public static ValidationResult error(Field field, String message) {
            return new ValidationResult(field, message);
        }

        public Field getField() {
            return field;
        }

        public String getMessage() {
            return message;
        }

        public boolean isValid() {
            return field == Field.NONE;
        }
    }

    public static final class ScanOutcome {
        public enum Type {
            NONE,
            NEED_IDENTIFIER,
            CHECKOUT_ADDED,
            CHECKOUT_DUPLICATE,
            CHECKIN_RETURNED,
            CHECKIN_DUPLICATE_RETURNED,
            CHECKIN_NOT_IN_TICKET
        }

        private final Type type;
        private final String displayValue;
        private final String identityKey;

        private ScanOutcome(Type type, String displayValue, String identityKey) {
            this.type = type == null ? Type.NONE : type;
            this.displayValue = displayValue == null ? "" : displayValue;
            this.identityKey = identityKey == null ? "" : identityKey;
        }

        public static ScanOutcome none() {
            return new ScanOutcome(Type.NONE, "", "");
        }

        public static ScanOutcome needIdentifier() {
            return new ScanOutcome(Type.NEED_IDENTIFIER, "", "");
        }

        public static ScanOutcome checkoutAdded(String displayValue, String identityKey) {
            return new ScanOutcome(Type.CHECKOUT_ADDED, displayValue, identityKey);
        }

        public static ScanOutcome checkoutDuplicate(String displayValue, String identityKey) {
            return new ScanOutcome(Type.CHECKOUT_DUPLICATE, displayValue, identityKey);
        }

        public static ScanOutcome checkinReturned(String displayValue, String identityKey) {
            return new ScanOutcome(Type.CHECKIN_RETURNED, displayValue, identityKey);
        }

        public static ScanOutcome checkinDuplicateReturned(String displayValue, String identityKey) {
            return new ScanOutcome(Type.CHECKIN_DUPLICATE_RETURNED, displayValue, identityKey);
        }

        public static ScanOutcome checkinNotInTicket(String displayValue, String identityKey) {
            return new ScanOutcome(Type.CHECKIN_NOT_IN_TICKET, displayValue, identityKey);
        }

        public Type getType() {
            return type;
        }

        public String getDisplayValue() {
            return displayValue;
        }

        public String getIdentityKey() {
            return identityKey;
        }

        public boolean isHandled() {
            return type != Type.NONE;
        }
    }

    public static final class CheckoutSummary {
        private final int selectedCount;
        private final int cachedCount;
        private final int outsideCacheCount;

        CheckoutSummary(int selectedCount, int cachedCount, int outsideCacheCount) {
            this.selectedCount = Math.max(0, selectedCount);
            this.cachedCount = Math.max(0, cachedCount);
            this.outsideCacheCount = Math.max(0, outsideCacheCount);
        }

        public int getSelectedCount() {
            return selectedCount;
        }

        public int getCachedCount() {
            return cachedCount;
        }

        public int getOutsideCacheCount() {
            return outsideCacheCount;
        }
    }

    public static final class CheckinSummary {
        private final int totalCount;
        private final int returnedCount;
        private final int missingCount;

        CheckinSummary(int totalCount, int returnedCount, int missingCount) {
            this.totalCount = Math.max(0, totalCount);
            this.returnedCount = Math.max(0, returnedCount);
            this.missingCount = Math.max(0, missingCount);
        }

        public int getTotalCount() {
            return totalCount;
        }

        public int getReturnedCount() {
            return returnedCount;
        }

        public int getMissingCount() {
            return missingCount;
        }
    }

    private final CheckoutCsvRepository csvRepository;
    private final LogRepository logRepository;
    private final CheckoutState state = new CheckoutState();

    public CheckoutController(CheckoutCsvRepository csvRepository, LogRepository logRepository) {
        this.csvRepository = csvRepository == null ? new CheckoutCsvRepository() : csvRepository;
        this.logRepository = logRepository;
    }

    public CheckoutState getState() {
        return state;
    }

    public void setCachedAssets(List<Asset> cachedAssets) {
        LinkedHashMap<String, Asset> byTid = new LinkedHashMap<>();
        LinkedHashMap<String, Asset> byCode = new LinkedHashMap<>();
        if (cachedAssets != null) {
            for (Asset asset : cachedAssets) {
                if (asset == null) {
                    continue;
                }
                String normalizedTid = normalizeKey(asset.getTid());
                if (!normalizedTid.isEmpty() && !byTid.containsKey(normalizedTid)) {
                    byTid.put(normalizedTid, asset);
                }
                String normalizedCode = normalizeKey(asset.getAssetCode());
                if (!normalizedCode.isEmpty() && !byCode.containsKey(normalizedCode)) {
                    byCode.put(normalizedCode, asset);
                }
            }
        }
        state.replaceCachedAssets(byTid, byCode);
    }

    public void restore(CheckoutState.Snapshot snapshot) {
        state.restore(snapshot);
    }

    public CheckoutState.Snapshot snapshot() {
        return state.snapshot();
    }

    public boolean hasImportedCheckoutData() {
        return state.getImportedCheckoutData() != null;
    }

    public ImportedCheckoutData getImportedCheckoutData() {
        return state.getImportedCheckoutData();
    }

    public void applyImportedCheckoutData(ImportedCheckoutData importedData) {
        state.setImportedCheckoutData(importedData);
        state.resetCheckinSession();
    }

    public void clearCheckoutItems() {
        state.clearCheckoutItems();
        log("CHECKOUT", "Da xoa danh sach tai san dang chon cho phieu check out", "");
    }

    public boolean removeCheckoutItem(CheckoutAssetItem item) {
        if (item == null) {
            return false;
        }
        boolean existed = state.getCheckoutItems().containsKey(item.getIdentityKey());
        state.removeCheckoutItem(item.getIdentityKey());
        if (existed) {
            log("CHECKOUT", "Da xoa 1 tai san khoi phieu check out", item.getIdentityKey());
        }
        return existed;
    }

    public void clearCheckinSession() {
        state.resetCheckinSession();
        log("CHECKIN", "Da dat lai ket qua check in theo file import", "");
    }

    public List<CheckoutAssetItem> buildOrderedCheckoutItems() {
        List<CheckoutAssetItem> items = new ArrayList<>(state.getCheckoutItems().values());
        Collections.sort(items, new Comparator<CheckoutAssetItem>() {
            @Override
            public int compare(CheckoutAssetItem left, CheckoutAssetItem right) {
                return Long.compare(right.getScannedAt(), left.getScannedAt());
            }
        });
        return items;
    }

    public List<CheckInResultItem> buildOrderedCheckinItems() {
        List<CheckInResultItem> items = new ArrayList<>(state.getExpectedCheckinItems().values());
        Collections.sort(items, new Comparator<CheckInResultItem>() {
            @Override
            public int compare(CheckInResultItem left, CheckInResultItem right) {
                int leftRank = statusRank(left);
                int rightRank = statusRank(right);
                if (leftRank != rightRank) {
                    return Integer.compare(leftRank, rightRank);
                }
                return Long.compare(right.getCheckinScannedAt(), left.getCheckinScannedAt());
            }
        });
        return items;
    }

    public CheckoutSummary buildCheckoutSummary() {
        int selectedCount = state.getCheckoutItems().size();
        int cachedCount = 0;
        for (CheckoutAssetItem item : state.getCheckoutItems().values()) {
            if (item != null && item.isMatchedFromCache()) {
                cachedCount++;
            }
        }
        int outsideCacheCount = Math.max(0, selectedCount - cachedCount);
        return new CheckoutSummary(selectedCount, cachedCount, outsideCacheCount);
    }

    public CheckinSummary buildCheckinSummary() {
        int returnedCount = 0;
        int missingCount = 0;
        for (CheckInResultItem item : state.getExpectedCheckinItems().values()) {
            if (item.getStatus() == CheckInResultStatus.RETURNED) {
                returnedCount++;
            } else {
                missingCount++;
            }
        }
        return new CheckinSummary(state.getExpectedCheckinItems().size(), returnedCount, missingCount);
    }

    public ValidationResult validateCheckoutDraft(CheckoutDraft draft) {
        if (draft == null || isEmpty(draft.getCarrierName())) {
            return ValidationResult.error(ValidationResult.Field.CARRIER_NAME, "checkout_need_carrier");
        }
        if (isEmpty(draft.getDepartment())) {
            return ValidationResult.error(ValidationResult.Field.DEPARTMENT, "checkout_need_department");
        }
        if (isEmpty(draft.getPurpose())) {
            return ValidationResult.error(ValidationResult.Field.PURPOSE, "checkout_need_purpose");
        }
        if (isEmpty(draft.getEventName())) {
            return ValidationResult.error(ValidationResult.Field.EVENT, "checkout_need_event");
        }
        if (isEmpty(draft.getCheckoutAt())) {
            return ValidationResult.error(ValidationResult.Field.CHECKOUT_TIME, "checkout_need_checkout_time");
        }
        if (!csvRepository.isValidDate(draft.getCheckoutAt())) {
            return ValidationResult.error(ValidationResult.Field.CHECKOUT_TIME, "checkout_invalid_date_format");
        }
        if (isEmpty(draft.getExpectedReturnAt())) {
            return ValidationResult.error(ValidationResult.Field.EXPECTED_RETURN, "checkout_need_expected_return");
        }
        if (!csvRepository.isValidDate(draft.getExpectedReturnAt())) {
            return ValidationResult.error(ValidationResult.Field.EXPECTED_RETURN, "checkout_invalid_date_format");
        }
        if (isEmpty(draft.getApprover())) {
            return ValidationResult.error(ValidationResult.Field.APPROVER, "checkout_need_approver");
        }
        return ValidationResult.ok();
    }

    public CheckOutFormData buildCheckoutFormData(CheckoutDraft draft) {
        return new CheckOutFormData(
                csvRepository.generateTicketId(),
                csvRepository.now(),
                draft == null ? "" : draft.getCarrierName(),
                draft == null ? "" : draft.getDepartment(),
                draft == null ? "" : draft.getPurpose(),
                draft == null ? "" : draft.getEventName(),
                draft == null ? "" : draft.getCheckoutAt(),
                draft == null ? "" : draft.getExpectedReturnAt(),
                draft == null ? "" : draft.getApprover(),
                draft == null ? "" : draft.getNote()
        );
    }

    public ScanOutcome processCheckoutQr(String code, long timestamp) {
        String normalizedCode = normalizeKey(code);
        if (normalizedCode.isEmpty()) {
            return ScanOutcome.needIdentifier();
        }
        Asset asset = state.getCachedByCode().get(normalizedCode);
        String identityKey = buildIdentityKey(asset, "", normalizedCode);
        if (identityKey.isEmpty()) {
            return ScanOutcome.needIdentifier();
        }
        if (state.getCheckoutItems().containsKey(identityKey)) {
            return ScanOutcome.checkoutDuplicate(valueOrDash(code), identityKey);
        }
        state.getCheckoutItems().put(identityKey, createCheckoutItem(asset, identityKey, "", normalizedCode, "QR", timestamp));
        log("SCAN_QR", "Da quet QR cho Check Out", valueOrDash(code));
        log("CHECKOUT", "Da them tai san vao phieu check out", identityKey);
        return ScanOutcome.checkoutAdded(valueOrDash(code), identityKey);
    }

    public ScanOutcome processCheckinQr(String code, long timestamp) {
        String normalizedCode = normalizeKey(code);
        if (normalizedCode.isEmpty()) {
            return ScanOutcome.needIdentifier();
        }
        String expectedKey = state.getExpectedByCode().get(normalizedCode);
        if (expectedKey != null) {
            CheckInResultItem item = state.getExpectedCheckinItems().get(expectedKey);
            if (item != null && item.getStatus() == CheckInResultStatus.RETURNED) {
                return ScanOutcome.checkinDuplicateReturned(valueOrDash(code), expectedKey);
            }
            if (item != null) {
                item.markReturned("Code", "QR", timestamp);
                log("SCAN_QR", "Da quet QR cho Check In", valueOrDash(code));
                log("CHECKIN", "Da ghi nhan tai san da mang ve", item.getIdentityKey());
                return ScanOutcome.checkinReturned(valueOrDash(code), item.getIdentityKey());
            }
            return ScanOutcome.needIdentifier();
        }

        Asset asset = state.getCachedByCode().get(normalizedCode);
        String identityKey = buildIdentityKey(asset, "", normalizedCode);
        if (identityKey.isEmpty()) {
            return ScanOutcome.needIdentifier();
        }
        log("OUTSIDE_LIST", "QR khong nam trong phieu check out", valueOrDash(code));
        return ScanOutcome.checkinNotInTicket(valueOrDash(code), identityKey);
    }

    public ScanOutcome processCheckoutRfid(String tid, String code, long scannedAt, boolean suppressDuplicateToast) {
        String normalizedTid = normalizeKey(tid);
        if (normalizedTid.isEmpty()) {
            return ScanOutcome.needIdentifier();
        }
        Asset asset = state.getCachedByTid().get(normalizedTid);
        String identityKey = buildIdentityKey(asset, normalizedTid, code);
        if (identityKey.isEmpty()) {
            return ScanOutcome.needIdentifier();
        }
        if (state.getCheckoutItems().containsKey(identityKey)) {
            return ScanOutcome.checkoutDuplicate(valueOrDash(tid), identityKey);
        }
        state.getCheckoutItems().put(identityKey, createCheckoutItem(asset, identityKey, normalizedTid, code, "RFID", scannedAt));
        log("SCAN_RFID", "Da quet RFID cho Check Out", valueOrDash(tid));
        log("CHECKOUT", "Da them tai san vao phieu check out", identityKey);
        return ScanOutcome.checkoutAdded(valueOrDash(tid), identityKey);
    }

    public ScanOutcome processCheckinRfid(String tid, String code, long scannedAt, boolean suppressDuplicateToast) {
        String normalizedTid = normalizeKey(tid);
        if (normalizedTid.isEmpty()) {
            return ScanOutcome.needIdentifier();
        }
        String expectedKey = state.getExpectedByTid().get(normalizedTid);
        if (expectedKey != null) {
            CheckInResultItem item = state.getExpectedCheckinItems().get(expectedKey);
            if (item != null && item.getStatus() == CheckInResultStatus.RETURNED) {
                return ScanOutcome.checkinDuplicateReturned(valueOrDash(tid), expectedKey);
            }
            if (item != null) {
                item.markReturned("TID", "RFID", scannedAt);
                log("SCAN_RFID", "Da quet RFID cho Check In", valueOrDash(tid));
                log("CHECKIN", "Da ghi nhan tai san da mang ve", item.getIdentityKey());
                return ScanOutcome.checkinReturned(valueOrDash(tid), item.getIdentityKey());
            }
            return ScanOutcome.needIdentifier();
        }

        Asset asset = state.getCachedByTid().get(normalizedTid);
        String identityKey = buildIdentityKey(asset, normalizedTid, code);
        if (identityKey.isEmpty()) {
            return ScanOutcome.needIdentifier();
        }
        log("OUTSIDE_LIST", "RFID khong nam trong phieu check out", valueOrDash(tid));
        return ScanOutcome.checkinNotInTicket(valueOrDash(tid), identityKey);
    }

    private CheckoutAssetItem createCheckoutItem(Asset asset,
                                                 String identityKey,
                                                 String fallbackTid,
                                                 String fallbackCode,
                                                 String scanSource,
                                                 long scannedAt) {
        if (asset == null) {
            return new CheckoutAssetItem(
                    identityKey,
                    fallbackTid,
                    fallbackCode,
                    "[Ngoai cache]",
                    "",
                    "",
                    "",
                    "",
                    "",
                    scanSource,
                    scannedAt,
                    false
            );
        }
        return new CheckoutAssetItem(
                identityKey,
                isEmpty(asset.getTid()) ? fallbackTid : asset.getTid(),
                isEmpty(asset.getAssetCode()) ? fallbackCode : asset.getAssetCode(),
                asset.getAssetName(),
                asset.getAssetType(),
                asset.getSerialNumber(),
                asset.getDepartment(),
                asset.getAssignedUser(),
                asset.getLocation(),
                scanSource,
                scannedAt,
                true
        );
    }

    private int statusRank(CheckInResultItem item) {
        if (item == null || item.getStatus() == null) {
            return 3;
        }
        switch (item.getStatus()) {
            case RETURNED:
                return 0;
            case MISSING:
            default:
                return 1;
        }
    }

    private String buildIdentityKey(Asset asset, String fallbackTid, String fallbackCode) {
        String assetTid = normalizeKey(asset == null ? "" : asset.getTid());
        if (!assetTid.isEmpty()) {
            return "TID:" + assetTid;
        }
        String assetCode = normalizeKey(asset == null ? "" : asset.getAssetCode());
        if (!assetCode.isEmpty()) {
            return "CODE:" + assetCode;
        }
        return csvRepository.buildIdentityKey(fallbackTid, fallbackCode);
    }

    private void log(String action, String message, String detail) {
        if (logRepository != null) {
            logRepository.logInfo(action, message, detail);
        }
    }

    private String normalizeKey(String value) {
        return csvRepository.normalizeKey(value);
    }

    private String valueOrDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}

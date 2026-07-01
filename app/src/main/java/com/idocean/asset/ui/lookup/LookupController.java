package com.idocean.asset.ui.lookup;

import com.idocean.asset.data.repository.AssetRepository;
import com.idocean.asset.data.repository.AssetUpdateCallback;
import com.idocean.asset.data.repository.LogRepository;
import com.idocean.asset.model.Asset;
import com.idocean.asset.utils.AssetFieldNormalizer;
import com.idocean.asset.utils.AssetLocationUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class LookupController {
    public interface LookupUi {
        void renderAsset(Asset asset);

        void showStatus(String message);

        void renderEditMode(boolean editing);

        void renderSaving(boolean saving);

        void showToast(String message);

        String lookupNeedAssetFirst();

        String lookupNeedAssetName();

        String lookupOpenedFromList();

        String lookupStatusNotFound();

        String lookupStatusFound(String assetName);

        String lookupEditCancelled();

        String lookupStatusEditing();

        String lookupStatusUpdateFailed(String message);

        String lookupHandoverNeedUser();

        String lookupHandoverNeedDate();

        String lookupHandoverInvalidDate();

        String lookupHandoverNoChange();

        String lookupHandoverSuccess();

        String lookupHandoverFailed(String message);
    }

    public static final class ValidationResult {
        public enum Field {
            NONE,
            ASSET_CODE,
            ASSET_NAME,
            HANDOVER_USER,
            HANDOVER_DATE
        }

        private final Field field;
        private final String message;

        private ValidationResult(Field field, String message) {
            this.field = field;
            this.message = message;
        }

        public static ValidationResult ok() {
            return new ValidationResult(Field.NONE, "");
        }

        public static ValidationResult error(Field field, String message) {
            return new ValidationResult(field, message == null ? "" : message);
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

    private final AssetRepository assetRepository;
    private final LogRepository logRepository;
    private final LookupState state = new LookupState();
    private final SimpleDateFormat tagDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private List<Asset> cachedAssets = new ArrayList<>();

    public LookupController(AssetRepository assetRepository, LogRepository logRepository) {
        this.assetRepository = assetRepository;
        this.logRepository = logRepository;
    }

    public LookupState getState() {
        return state;
    }

    public void setCachedAssets(List<Asset> assets) {
        cachedAssets = assets == null ? new ArrayList<>() : new ArrayList<>(assets);
    }

    public void invalidateCachedAssets() {
        cachedAssets = new ArrayList<>();
    }

    public void restoreState(String savedCode, String savedTid, boolean editing, LookupUi ui) {
        state.setEditing(editing);
        state.setSaving(false);
        Asset asset = findAsset(savedCode, savedTid);
        if (asset == null) {
            state.setCurrentAsset(null);
            return;
        }
        publishAsset(ui, asset, editing ? ui.lookupStatusEditing() : ui.lookupOpenedFromList(), false);
        state.setEditing(editing);
        ui.renderEditMode(editing);
        ui.renderSaving(false);
    }

    public void openAssetFromIntent(String assetCode, String assetTid, LookupUi ui) {
        if (valueOrEmpty(assetCode).trim().isEmpty() && valueOrEmpty(assetTid).trim().isEmpty()) {
            return;
        }
        Asset asset = findAsset(assetCode, assetTid);
        if (asset == null) {
            if (logRepository != null) {
                logRepository.logInfo("INTENT", "Khong tim thay tai san trong cache khi mo tu Intent. Khoi tao tai san moi.", "code=" + assetCode + ", tid=" + assetTid);
            }
            Asset newAsset = new Asset(
                    null,
                    valueOrEmpty(assetCode),
                    valueOrEmpty(assetTid),
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    todayDateString(),
                    "",
                    "",
                    "NEW"
            );
            state.setCurrentAsset(newAsset);
            state.setEditing(true);
            state.setSaving(false);
            ui.renderAsset(newAsset);
            ui.renderEditMode(true);
            ui.renderSaving(false);
            ui.showStatus("Tài sản chưa có trong hệ thống. Hãy nhập thông tin để thêm.");
            return;
        }
        publishAsset(ui, asset, ui.lookupOpenedFromList(), false);
    }

    public void handleLookupResult(String tid, String code, String matchedBy, String rawValue, LookupUi ui) {
        Asset asset = findAsset(code, tid);
        if (asset == null) {
            if (logRepository != null) {
                logRepository.logInfo("SCAN_" + matchedBy, "Khong tim thay tai san trong cache. Khoi tao tai san moi.", valueOrDash(rawValue));
            }
            Asset newAsset = new Asset(
                    null,
                    valueOrEmpty(code),
                    valueOrEmpty(tid),
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    todayDateString(),
                    "",
                    "",
                    "NEW"
            );
            state.setCurrentAsset(newAsset);
            state.setEditing(true);
            state.setSaving(false);
            ui.renderAsset(newAsset);
            ui.renderEditMode(true);
            ui.renderSaving(false);
            ui.showStatus("Tài sản chưa có trong hệ thống. Hãy nhập thông tin để thêm.");
            ui.showToast("Phát hiện tài sản mới! Đã hiển thị mã quét và TID.");
            return;
        }

        if (logRepository != null) {
            logRepository.logInfo("SCAN_" + matchedBy, "Da tim thay tai san", asset.getAssetCode());
        }
        publishAsset(ui, asset, ui.lookupStatusFound(valueOrDash(asset.getAssetName())), false);
    }

    public void startManualAdd(LookupUi ui) {
        if (logRepository != null) {
            logRepository.logInfo("MANUAL_ADD", "Nguoi dung bat dau dang ky thu cong", "");
        }
        Asset newAsset = new Asset(
                null,
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                todayDateString(),
                "",
                "",
                "NEW"
        );
        state.setCurrentAsset(newAsset);
        state.setEditing(true);
        state.setSaving(false);
        ui.renderAsset(newAsset);
        ui.renderEditMode(true);
        ui.renderSaving(false);
        ui.showStatus("Nhập thông tin tài sản mới.");
    }

    public void startEdit(LookupUi ui) {
        if (!state.hasCurrentAsset()) {
            ui.showToast(ui.lookupNeedAssetFirst());
            return;
        }
        state.setEditing(true);
        state.setSaving(false);
        ui.renderEditMode(true);
        ui.renderSaving(false);
        ui.showStatus(ui.lookupStatusEditing());
    }

    public void cancelEdit(LookupUi ui) {
        if (!state.hasCurrentAsset()) {
            state.setEditing(false);
            ui.renderEditMode(false);
            ui.renderSaving(false);
            return;
        }
        Asset currentAsset = state.getCurrentAsset();
        if (currentAsset.getRowNumber() == null) {
            state.reset();
            ui.renderAsset(null);
            ui.showStatus("Đã hủy đăng ký.");
            ui.renderEditMode(false);
            ui.renderSaving(false);
            return;
        }
        state.setEditing(false);
        state.setSaving(false);
        ui.renderAsset(state.getCurrentAsset());
        ui.showStatus(ui.lookupEditCancelled());
        ui.showToast(ui.lookupEditCancelled());
        ui.renderEditMode(false);
        ui.renderSaving(false);
    }

    public ValidationResult validateEditableDraft(EditableAssetDraft draft, LookupUi ui) {
        if (draft == null || valueOrEmpty(draft.code).trim().isEmpty()) {
            return ValidationResult.error(ValidationResult.Field.ASSET_CODE, "Mã tài sản (Code) là bắt buộc");
        }
        if (draft == null || valueOrEmpty(draft.assetName).isEmpty()) {
            return ValidationResult.error(ValidationResult.Field.ASSET_NAME, ui.lookupNeedAssetName());
        }
        return ValidationResult.ok();
    }

    public ValidationResult validateHandoverDraft(HandoverDraft draft, LookupUi ui) {
        if (draft == null || valueOrEmpty(draft.newUser).isEmpty()) {
            return ValidationResult.error(ValidationResult.Field.HANDOVER_USER, ui.lookupHandoverNeedUser());
        }
        String handoverDate = valueOrEmpty(draft.handoverDate);
        if (handoverDate.isEmpty()) {
            return ValidationResult.error(ValidationResult.Field.HANDOVER_DATE, ui.lookupHandoverNeedDate());
        }
        if (!isValidTagDate(handoverDate)) {
            return ValidationResult.error(ValidationResult.Field.HANDOVER_DATE, ui.lookupHandoverInvalidDate());
        }
        return ValidationResult.ok();
    }

    public void saveEditableAsset(EditableAssetDraft draft, LookupUi ui) {
        if (!state.hasCurrentAsset()) {
            ui.showToast(ui.lookupNeedAssetFirst());
            return;
        }
        if (!state.isEditing() || state.isSaving()) {
            return;
        }

        Asset currentAsset = state.getCurrentAsset();
        String finalTid = valueOrEmpty(currentAsset.getTid()).trim().isEmpty() ? valueOrEmpty(draft.tid) : currentAsset.getTid();
        Asset updatedAsset = new Asset(
                currentAsset.getRowNumber(),
                valueOrEmpty(draft.code),
                finalTid,
                valueOrEmpty(draft.oldCode),
                valueOrEmpty(draft.oldSerial),
                valueOrEmpty(draft.assetName),
                AssetFieldNormalizer.normalizeAssetTypeForDisplay(draft.assetType),
                valueOrEmpty(draft.serialNumber),
                valueOrEmpty(draft.department),
                valueOrEmpty(draft.assignedUser),
                valueOrEmpty(draft.location),
                AssetFieldNormalizer.normalizeInventoryStatusForDisplay(draft.inventoryStatus),
                AssetFieldNormalizer.normalizeConditionForDisplay(draft.inventoryStatus),
                valueOrEmpty(currentAsset.getTagDate()),
                valueOrEmpty(currentAsset.getTagBy()),
                valueOrEmpty(draft.note),
                valueOrEmpty(currentAsset.getSource())
        );

        state.setSaving(true);
        ui.renderSaving(true);
        assetRepository.updateAsset(currentAsset, updatedAsset, new AssetUpdateCallback() {
            @Override
            public void onSuccess(Asset asset, String message) {
                invalidateCachedAssets();
                state.setCurrentAsset(asset);
                state.setEditing(false);
                state.setSaving(false);
                ui.renderAsset(asset);
                ui.showStatus(message);
                ui.renderEditMode(false);
                ui.renderSaving(false);
                ui.showToast(message);
            }

            @Override
            public void onError(String message) {
                state.setEditing(true);
                state.setSaving(false);
                ui.renderEditMode(true);
                ui.renderSaving(false);
                ui.showStatus(ui.lookupStatusUpdateFailed(message));
                ui.showToast(message);
            }
        });
    }

    public void performHandover(HandoverDraft draft, LookupUi ui) {
        if (!state.hasCurrentAsset()) {
            ui.showToast(ui.lookupNeedAssetFirst());
            return;
        }
        if (state.isSaving()) {
            return;
        }

        Asset sourceAsset = state.getCurrentAsset();
        String newDepartment = normalizeDepartmentForHandover(sourceAsset, draft.newDepartment);
        String newLocation = normalizeLocationForHandover(sourceAsset, draft.newLocation);
        if (!hasHandoverChanges(sourceAsset, draft.newUser, newDepartment, newLocation, draft.handoverDate)) {
            ui.showToast(ui.lookupHandoverNoChange());
            return;
        }

        Asset updatedAsset = buildHandoverAsset(sourceAsset, draft.newUser, newDepartment, newLocation);
        state.setSaving(true);
        ui.renderSaving(true);
        logRepository.logInfo(
                "HANDOVER",
                "Bat dau ban giao tai san",
                assetSummaryForLog(sourceAsset) + " -> " + assetSummaryForLog(updatedAsset)
        );
        assetRepository.handoverAsset(sourceAsset, updatedAsset, valueOrEmpty(draft.handoverDate), new AssetUpdateCallback() {
            @Override
            public void onSuccess(Asset asset, String message) {
                invalidateCachedAssets();
                state.setCurrentAsset(asset);
                state.setSaving(false);
                state.setEditing(false);
                logRepository.logInfo(
                        "HANDOVER",
                        "Da ban giao tai san",
                        assetSummaryForLog(asset) + " | " + valueOrDash(draft.handoverDate)
                );
                ui.renderAsset(asset);
                ui.showStatus(ui.lookupHandoverSuccess());
                ui.renderEditMode(false);
                ui.renderSaving(false);
                ui.showToast(message);
            }

            @Override
            public void onError(String message) {
                state.setSaving(false);
                state.setEditing(false);
                logRepository.logError(
                        "HANDOVER",
                        "Ban giao tai san that bai",
                        assetSummaryForLog(sourceAsset) + " | " + message
                );
                ui.renderEditMode(false);
                ui.renderSaving(false);
                ui.showStatus(ui.lookupHandoverFailed(message));
                ui.showToast(message);
            }
        });
    }

    public static String buildHandoverCurrentSummary(Asset asset) {
        if (asset == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Người dùng hiện tại: ").append(valueOrDash(asset.getAssignedUser())).append('\n');
        builder.append("Phòng ban hiện tại: ")
                .append(valueOrDash(AssetFieldNormalizer.normalizeDepartmentForDisplay(asset.getDepartment())))
                .append('\n');
        builder.append("Vị trí hiện tại: ")
                .append(valueOrDash(AssetLocationUtils.normalizeLocationForDisplay(asset.getLocation())))
                .append('\n');
        builder.append("TID: ").append(valueOrDash(asset.getTid()));
        return builder.toString();
    }

    public static String normalizeDepartmentForHandover(Asset asset, String value) {
        String normalized = AssetFieldNormalizer.normalizeDepartmentForDisplay(value);
        if (!normalized.isEmpty()) {
            return normalized;
        }
        return AssetFieldNormalizer.normalizeDepartmentForDisplay(asset == null ? "" : asset.getDepartment());
    }

    public static String normalizeLocationForHandover(Asset asset, String value) {
        String normalized = AssetLocationUtils.normalizeLocationForDisplay(value);
        if (!normalized.isEmpty()) {
            return normalized;
        }
        return AssetLocationUtils.normalizeLocationForDisplay(asset == null ? "" : asset.getLocation());
    }

    public static boolean hasHandoverChanges(
            Asset sourceAsset,
            String newUser,
            String newDepartment,
            String newLocation,
            String handoverDate
    ) {
        String currentUser = normalizeSearch(sourceAsset == null ? "" : sourceAsset.getAssignedUser());
        String currentDepartment = normalizeSearch(AssetFieldNormalizer.normalizeDepartmentForDisplay(
                sourceAsset == null ? "" : sourceAsset.getDepartment()
        ));
        String currentLocation = normalizeSearch(AssetLocationUtils.normalizeLocationForDisplay(
                sourceAsset == null ? "" : sourceAsset.getLocation()
        ));
        String nextUser = normalizeSearch(newUser);
        String nextDepartment = normalizeSearch(newDepartment);
        String nextLocation = normalizeSearch(newLocation);
        return !nextUser.equals(currentUser)
                || !nextDepartment.equals(currentDepartment)
                || !nextLocation.equals(currentLocation)
                || !normalizeSearch(handoverDate).equals(normalizeSearch(todayDateString()));
    }

    public static String sanitizeNoteForMasterAsset(String note) {
        String safeNote = valueOrEmpty(note);
        if (safeNote.isEmpty()) {
            return "";
        }

        String[] lines = safeNote.split("\\r?\\n");
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (looksLikeLegacyHandoverTrail(trimmed)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(trimmed);
        }
        return builder.toString().trim();
    }

    public static String assetSummaryForLog(Asset asset) {
        if (asset == null) {
            return "-";
        }
        return valueOrDash(asset.getAssetCode())
                + " | " + valueOrDash(asset.getAssignedUser())
                + " | " + valueOrDash(AssetFieldNormalizer.normalizeDepartmentForDisplay(asset.getDepartment()))
                + " | " + valueOrDash(AssetLocationUtils.normalizeLocationForDisplay(asset.getLocation()));
    }

    public static long parseDateMillis(String value) {
        if (value == null || value.trim().isEmpty()) {
            return -1L;
        }
        try {
            synchronized (DATE_FORMAT) {
                DATE_FORMAT.setLenient(false);
                Date parsed = DATE_FORMAT.parse(value.trim());
                return parsed == null ? -1L : parsed.getTime();
            }
        } catch (ParseException ignored) {
            return -1L;
        }
    }

    public static String formatDate(long millis) {
        synchronized (DATE_FORMAT) {
            return DATE_FORMAT.format(new Date(millis));
        }
    }

    public static String todayDateString() {
        return formatDate(System.currentTimeMillis());
    }

    public static boolean isValidTagDate(String value) {
        return parseDateMillis(value) > 0L;
    }

    public static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    public static String valueOrDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    public static String normalizeSearch(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private Asset findAsset(String code, String tid) {
        Asset cachedAsset = findAssetInSnapshot(code, tid);
        if (cachedAsset != null) {
            return cachedAsset;
        }
        return assetRepository == null ? null : assetRepository.findAsset(code, tid);
    }

    private Asset findAssetInSnapshot(String code, String tid) {
        if (cachedAssets == null || cachedAssets.isEmpty()) {
            return null;
        }
        String normalizedCode = normalizeAssetKey(code);
        String normalizedTid = normalizeAssetKey(tid);
        for (Asset asset : cachedAssets) {
            if (asset == null) {
                continue;
            }
            if (!normalizedCode.isEmpty() && normalizedCode.equals(normalizeAssetKey(asset.getAssetCode()))) {
                return asset;
            }
            if (!normalizedTid.isEmpty() && normalizedTid.equals(normalizeAssetKey(asset.getTid()))) {
                return asset;
            }
        }
        return null;
    }

    private String normalizeAssetKey(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static Asset buildHandoverAsset(Asset sourceAsset, String newUser, String newDepartment, String newLocation) {
        if (sourceAsset == null) {
            return null;
        }
        return new Asset(
                sourceAsset.getRowNumber(),
                sourceAsset.getAssetCode(),
                sourceAsset.getTid(),
                valueOrEmpty(sourceAsset.getOldCode()),
                valueOrEmpty(sourceAsset.getOldSerial()),
                valueOrEmpty(sourceAsset.getAssetName()),
                AssetFieldNormalizer.normalizeAssetTypeForDisplay(sourceAsset.getAssetType()),
                valueOrEmpty(sourceAsset.getSerialNumber()),
                valueOrEmpty(newDepartment),
                valueOrEmpty(newUser),
                valueOrEmpty(newLocation),
                AssetFieldNormalizer.normalizeInventoryStatusForDisplay(sourceAsset.getInventoryStatus()),
                AssetFieldNormalizer.normalizeConditionForDisplay(sourceAsset.getAssetCondition()),
                valueOrEmpty(sourceAsset.getTagDate()),
                valueOrEmpty(sourceAsset.getTagBy()),
                sanitizeNoteForMasterAsset(sourceAsset.getNote()),
                valueOrEmpty(sourceAsset.getSource())
        );
    }

    private static boolean looksLikeLegacyHandoverTrail(String line) {
        String normalized = valueOrEmpty(line);
        if (normalized.isEmpty()) {
            return false;
        }
        return normalized.startsWith("[Bàn giao ")
                && normalized.contains("Người nhận")
                && normalized.contains("Từ người dùng");
    }

    private void publishAsset(LookupUi ui, Asset asset, String statusMessage, boolean toast) {
        state.setCurrentAsset(asset);
        state.setSaving(false);
        state.setEditing(false);
        ui.renderAsset(asset);
        ui.showStatus(statusMessage);
        ui.renderEditMode(false);
        ui.renderSaving(false);
        if (toast && statusMessage != null && !statusMessage.trim().isEmpty()) {
            ui.showToast(statusMessage);
        }
    }

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
}

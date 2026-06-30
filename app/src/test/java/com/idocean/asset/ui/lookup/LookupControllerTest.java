package com.idocean.asset.ui.lookup;

import com.idocean.asset.model.Asset;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LookupControllerTest {
    private final LookupController controller = new LookupController(null, null);
    private final LookupController.LookupUi ui = new TestLookupUi();

    @Test
    public void validateEditableDraft_requiresAssetName() {
        LookupController.ValidationResult result = controller.validateEditableDraft(
                new EditableAssetDraft(
                        "VALID-CODE",
                        "", // tid
                        "OLD",
                        "SER-OLD",
                        "",
                        "Laptop",
                        "SER-NEW",
                        "IT",
                        "User A",
                        "Lầu 5 - TT16",
                        "Đang sử dụng",
                        "note"
                ),
                ui
        );

        assertFalse(result.isValid());
        assertEquals(LookupController.ValidationResult.Field.ASSET_NAME, result.getField());
        assertEquals("Asset name is required", result.getMessage());
    }

    @Test
    public void validateEditableDraft_requiresAssetCode() {
        LookupController.ValidationResult result = controller.validateEditableDraft(
                new EditableAssetDraft(
                        "",
                        "", // tid
                        "OLD",
                        "SER-OLD",
                        "Laptop",
                        "Laptop",
                        "SER-NEW",
                        "IT",
                        "User A",
                        "Lầu 5 - TT16",
                        "Đang sử dụng",
                        "note"
                ),
                ui
        );

        assertFalse(result.isValid());
        assertEquals(LookupController.ValidationResult.Field.ASSET_CODE, result.getField());
        assertEquals("Mã tài sản (Code) là bắt buộc", result.getMessage());
    }

    @Test
    public void validateHandoverDraft_requiresUserAndDate() {
        LookupController.ValidationResult missingUser = controller.validateHandoverDraft(
                new HandoverDraft("", "IT", "Lầu 5 - TT16", "2026-04-09"),
                ui
        );
        assertFalse(missingUser.isValid());
        assertEquals(LookupController.ValidationResult.Field.HANDOVER_USER, missingUser.getField());

        LookupController.ValidationResult invalidDate = controller.validateHandoverDraft(
                new HandoverDraft("Thang Nguyen", "IT", "Lầu 5 - TT16", "2026-13-09"),
                ui
        );
        assertFalse(invalidDate.isValid());
        assertEquals(LookupController.ValidationResult.Field.HANDOVER_DATE, invalidDate.getField());
        assertEquals("Invalid handover date", invalidDate.getMessage());
    }

    @Test
    public void sanitizeNoteForMasterAsset_removesLegacyHandoverTrail() {
        String sanitized = LookupController.sanitizeNoteForMasterAsset(
                "Ghi chú cũ\n[Bàn giao 2026-04-09] Người nhận: Thang Nguyen | Từ người dùng: Truong Vu | Phòng ban: IT -> BOD | Vị trí: Lầu 5 - TT16 -> Lầu 5 - TT16"
        );

        assertEquals("Ghi chú cũ", sanitized);
    }

    @Test
    public void hasHandoverChanges_detectsNormalizedDifference() {
        Asset source = new Asset(
                1,
                "AREXAT",
                "E2801190200089A73CC203CA",
                "",
                "",
                "Laptop",
                "Laptop",
                "SER-1",
                "IT",
                "Truong Vu",
                "Lầu 5 - TT16",
                "Đang sử dụng",
                "",
                "2026-04-09",
                "Tester",
                "",
                "api"
        );

        assertFalse(LookupController.hasHandoverChanges(
                source,
                "Truong Vu",
                "IT",
                "Lầu 5 - TT16",
                LookupController.todayDateString()
        ));
        assertTrue(LookupController.hasHandoverChanges(
                source,
                "Thang Nguyen",
                "IT",
                "Lầu 5 - TT16",
                LookupController.todayDateString()
        ));
    }

    @Test
    public void normalizeDepartmentForHandover_usesAssetValueWhenInputBlank() {
        Asset source = new Asset(
                1,
                "AREXAT",
                "E2801190200089A73CC203CA",
                "",
                "",
                "Laptop",
                "Laptop",
                "SER-1",
                "Finance and Accountant",
                "Truong Vu",
                "L\u1ea7u 5 - TT16",
                "Dang su dung",
                "",
                "2026-04-09",
                "Tester",
                "",
                "api"
        );

        assertEquals("Finance & Accountant", LookupController.normalizeDepartmentForHandover(source, "   "));
    }

    @Test
    public void normalizeLocationForHandover_prefersNormalizedInputBeforeFallback() {
        Asset source = new Asset(
                1,
                "AREXAT",
                "E2801190200089A73CC203CA",
                "",
                "",
                "Laptop",
                "Laptop",
                "SER-1",
                "IT",
                "Truong Vu",
                "Idoplex-5",
                "Dang su dung",
                "",
                "2026-04-09",
                "Tester",
                "",
                "api"
        );

        assertEquals("L\u1ea7u 2 - TT16", LookupController.normalizeLocationForHandover(source, "Idoplex-2"));
        assertEquals("L\u1ea7u 5 - TT16", LookupController.normalizeLocationForHandover(source, "   "));
    }

    @Test
    public void buildHandoverCurrentSummary_includesCurrentAssetState() {
        Asset source = new Asset(
                1,
                "AREXAT",
                "E2801190200089A73CC203CA",
                "",
                "",
                "Laptop",
                "Laptop",
                "SER-1",
                "IT",
                "Truong Vu",
                "Lầu 5 - TT16",
                "Đang sử dụng",
                "",
                "2026-04-09",
                "Tester",
                "",
                "api"
        );

        String summary = LookupController.buildHandoverCurrentSummary(source);
        assertTrue(summary.contains("Truong Vu"));
        assertTrue(summary.contains("IT"));
        assertTrue(summary.contains("Lầu 5 - TT16"));
        assertTrue(summary.contains("E2801190200089A73CC203CA"));
    }

    @Test
    public void handleLookupResult_whenNotFound_initializesNewAssetAndEntersEditMode() {
        class MockLookupUi extends TestLookupUi {
            Asset renderedAsset;
            boolean editMode;
            String status;
            String toast;

            @Override
            public void renderAsset(Asset asset) {
                this.renderedAsset = asset;
            }

            @Override
            public void renderEditMode(boolean editing) {
                this.editMode = editing;
            }

            @Override
            public void showStatus(String message) {
                this.status = message;
            }

            @Override
            public void showToast(String message) {
                this.toast = message;
            }
        }

        MockLookupUi mockUi = new MockLookupUi();
        controller.handleLookupResult("TID-NEW", "CODE-NEW", "RFID", "TID-NEW", mockUi);

        assertTrue(controller.getState().isEditing());
        assertEquals("CODE-NEW", mockUi.renderedAsset.getAssetCode());
        assertEquals("TID-NEW", mockUi.renderedAsset.getTid());
        assertTrue(mockUi.editMode);
        assertEquals("Tài sản chưa có trong hệ thống. Hãy nhập thông tin để thêm.", mockUi.status);
    }

    @Test
    public void openAssetFromIntent_whenNotFound_initializesNewAssetAndEntersEditMode() {
        class MockLookupUi extends TestLookupUi {
            Asset renderedAsset;
            boolean editMode;
            String status;

            @Override
            public void renderAsset(Asset asset) {
                this.renderedAsset = asset;
            }

            @Override
            public void renderEditMode(boolean editing) {
                this.editMode = editing;
            }

            @Override
            public void showStatus(String message) {
                this.status = message;
            }
        }

        MockLookupUi mockUi = new MockLookupUi();
        controller.openAssetFromIntent("CODE-INTENT", "TID-INTENT", mockUi);

        assertTrue(controller.getState().isEditing());
        assertEquals("CODE-INTENT", mockUi.renderedAsset.getAssetCode());
        assertEquals("TID-INTENT", mockUi.renderedAsset.getTid());
        assertTrue(mockUi.editMode);
        assertEquals("Tài sản chưa có trong hệ thống. Hãy nhập thông tin để thêm.", mockUi.status);
    }

    @Test
    public void startManualAdd_initializesEmptyAssetAndEntersEditMode() {
        class MockLookupUi extends TestLookupUi {
            Asset renderedAsset;
            boolean editMode;
            String status;

            @Override
            public void renderAsset(Asset asset) {
                this.renderedAsset = asset;
            }

            @Override
            public void renderEditMode(boolean editing) {
                this.editMode = editing;
            }

            @Override
            public void showStatus(String message) {
                this.status = message;
            }
        }

        MockLookupUi mockUi = new MockLookupUi();
        controller.startManualAdd(mockUi);

        assertTrue(controller.getState().isEditing());
        assertEquals("", mockUi.renderedAsset.getAssetCode());
        assertEquals("", mockUi.renderedAsset.getTid());
        assertTrue(mockUi.editMode);
        assertEquals("Nhập thông tin tài sản mới.", mockUi.status);
    }

    private static class TestLookupUi implements LookupController.LookupUi {
        @Override
        public void renderAsset(Asset asset) {
        }

        @Override
        public void showStatus(String message) {
        }

        @Override
        public void renderEditMode(boolean editing) {
        }

        @Override
        public void renderSaving(boolean saving) {
        }

        @Override
        public void showToast(String message) {
        }

        @Override
        public String lookupNeedAssetFirst() {
            return "Asset first";
        }

        @Override
        public String lookupNeedAssetName() {
            return "Asset name is required";
        }

        @Override
        public String lookupOpenedFromList() {
            return "Opened from list";
        }

        @Override
        public String lookupStatusNotFound() {
            return "Not found";
        }

        @Override
        public String lookupStatusFound(String assetName) {
            return "Found: " + assetName;
        }

        @Override
        public String lookupEditCancelled() {
            return "Edit cancelled";
        }

        @Override
        public String lookupStatusEditing() {
            return "Editing";
        }

        @Override
        public String lookupStatusUpdateFailed(String message) {
            return "Update failed: " + message;
        }

        @Override
        public String lookupHandoverNeedUser() {
            return "Need user";
        }

        @Override
        public String lookupHandoverNeedDate() {
            return "Need date";
        }

        @Override
        public String lookupHandoverInvalidDate() {
            return "Invalid handover date";
        }

        @Override
        public String lookupHandoverNoChange() {
            return "No change";
        }

        @Override
        public String lookupHandoverSuccess() {
            return "Handover success";
        }

        @Override
        public String lookupHandoverFailed(String message) {
            return "Handover failed: " + message;
        }
    }
}

package com.idocean.asset.ui.checkout;

import com.idocean.asset.model.Asset;
import com.idocean.asset.model.CheckInResultItem;
import com.idocean.asset.model.CheckInResultStatus;
import com.idocean.asset.model.CheckOutFormData;
import com.idocean.asset.model.CheckoutAssetItem;
import com.idocean.asset.model.ImportedCheckoutData;
import com.idocean.asset.scanner.rfid.UhfScanData;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CheckoutControllerTest {
    private CheckoutController controller;

    @Before
    public void setUp() {
        controller = new CheckoutController(null, null);
        controller.setCachedAssets(sampleAssets());
    }

    @Test
    public void validateCheckoutDraft_requiresCarrierName() {
        CheckoutController.ValidationResult result = controller.validateCheckoutDraft(
                new CheckoutDraft(
                        "",
                        "IT",
                        "Bao tri",
                        "SX-01",
                        "2026-04-09",
                        "2026-04-10",
                        "Truong Vu",
                        "note"
                )
        );

        assertFalse(result.isValid());
        assertEquals(CheckoutController.ValidationResult.Field.CARRIER_NAME, result.getField());
        assertEquals("checkout_need_carrier", result.getMessage());
    }

    @Test
    public void buildCheckoutFormData_copiesDraftFields() {
        CheckoutDraft draft = new CheckoutDraft(
                "Vũ Trọng Trường",
                "IT",
                "Cong tac",
                "SX-01",
                "2026-04-09",
                "2026-04-10",
                "Nguyen Van A",
                "Ghi chu"
        );

        CheckOutFormData formData = controller.buildCheckoutFormData(draft);

        assertNotNull(formData.getTicketId());
        assertFalse(formData.getTicketId().trim().isEmpty());
        assertNotNull(formData.getExportedAt());
        assertFalse(formData.getExportedAt().trim().isEmpty());
        assertEquals("Vũ Trọng Trường", formData.getCarrierName());
        assertEquals("IT", formData.getDepartment());
        assertEquals("Cong tac", formData.getPurpose());
        assertEquals("SX-01", formData.getEventName());
        assertEquals("2026-04-09", formData.getCheckoutAt());
        assertEquals("2026-04-10", formData.getExpectedReturnAt());
        assertEquals("Nguyen Van A", formData.getApprover());
        assertEquals("Ghi chu", formData.getNote());
    }

    @Test
    public void processCheckoutQr_addsUniqueAssetAndBlocksDuplicate() {
        CheckoutController.ScanOutcome first = controller.processCheckoutQr("CODE-1", 1000L);
        CheckoutController.ScanOutcome duplicate = controller.processCheckoutQr("CODE-1", 2000L);

        assertEquals(CheckoutController.ScanOutcome.Type.CHECKOUT_ADDED, first.getType());
        assertEquals(CheckoutController.ScanOutcome.Type.CHECKOUT_DUPLICATE, duplicate.getType());
        assertEquals(1, controller.getState().getCheckoutItems().size());

        CheckoutAssetItem item = controller.getState().getCheckoutItems().values().iterator().next();
        assertEquals("TID:TID-1", item.getIdentityKey());
        assertTrue(item.isMatchedFromCache());
        assertEquals("QR", item.getScanSource());
    }

    @Test
    public void processCheckoutRfid_addsUniqueAssetAndBlocksDuplicate() {
        CheckoutController.ScanOutcome first = controller.processCheckoutRfid("TID-1", "CODE-1", 1000L, false);
        CheckoutController.ScanOutcome duplicate = controller.processCheckoutRfid("TID-1", "CODE-1", 2000L, false);

        assertEquals(CheckoutController.ScanOutcome.Type.CHECKOUT_ADDED, first.getType());
        assertEquals(CheckoutController.ScanOutcome.Type.CHECKOUT_DUPLICATE, duplicate.getType());
        assertEquals(1, controller.getState().getCheckoutItems().size());

        CheckoutAssetItem item = controller.getState().getCheckoutItems().values().iterator().next();
        assertEquals("TID:TID-1", item.getIdentityKey());
        assertTrue(item.isMatchedFromCache());
        assertEquals("RFID", item.getScanSource());
    }

    @Test
    public void processCheckinQr_marksReturnedForImportedTicket() {
        controller.applyImportedCheckoutData(importedData());

        CheckoutController.ScanOutcome result = controller.processCheckinQr("CODE-1", 1234L);

        assertEquals(CheckoutController.ScanOutcome.Type.CHECKIN_RETURNED, result.getType());
        CheckInResultItem item = controller.getState().getExpectedCheckinItems().values().iterator().next();
        assertEquals(CheckInResultStatus.RETURNED, item.getStatus());
        assertEquals("Code", item.getMatchedBy());
        assertEquals("QR", item.getCheckinScanSource());
        assertEquals(1234L, item.getCheckinScannedAt());
    }

    @Test
    public void processCheckinRfid_marksReturnedForImportedTicket() {
        controller.applyImportedCheckoutData(importedData());

        CheckoutController.ScanOutcome result = controller.processCheckinRfid("TID-1", "CODE-1", 1234L, false);

        assertEquals(CheckoutController.ScanOutcome.Type.CHECKIN_RETURNED, result.getType());
        CheckInResultItem item = controller.getState().getExpectedCheckinItems().values().iterator().next();
        assertEquals(CheckInResultStatus.RETURNED, item.getStatus());
        assertEquals("TID", item.getMatchedBy());
        assertEquals("RFID", item.getCheckinScanSource());
        assertEquals(1234L, item.getCheckinScannedAt());
    }

    @Test
    public void snapshotRestore_keepsCheckoutAndImportedState() {
        controller.processCheckoutQr("CODE-1", 1000L);
        controller.applyImportedCheckoutData(importedData());
        controller.processCheckinQr("CODE-1", 1234L);

        CheckoutState.Snapshot snapshot = controller.snapshot();
        CheckoutController restored = new CheckoutController(null, null);
        restored.restore(snapshot);

        assertEquals(1, restored.getState().getCheckoutItems().size());
        assertEquals(1, restored.getState().getExpectedCheckinItems().size());
        assertEquals(1, restored.buildCheckoutSummary().getSelectedCount());
        assertEquals(1, restored.buildCheckinSummary().getReturnedCount());
        assertEquals(0, restored.buildCheckinSummary().getMissingCount());
    }

    private ImportedCheckoutData importedData() {
        CheckoutAssetItem expected = new CheckoutAssetItem(
                "TID:TID-1",
                "TID-1",
                "CODE-1",
                "Laptop A",
                "Laptop",
                "SER-1",
                "IT",
                "Truong Vu",
                "Lầu 5 - TT16",
                "QR",
                1000L,
                true
        );
        CheckOutFormData formData = new CheckOutFormData(
                "TICKET-1",
                "2026-04-09 00:00:00.000",
                "Vũ Trọng Trường",
                "IT",
                "Cong tac",
                "SX-01",
                "2026-04-09",
                "2026-04-10",
                "Nguyen Van A",
                "Note"
        );
        List<CheckoutAssetItem> expectedItems = new ArrayList<>();
        expectedItems.add(expected);
        return new ImportedCheckoutData("checkout.csv", formData, expectedItems);
    }

    private List<Asset> sampleAssets() {
        List<Asset> assets = new ArrayList<>();
        assets.add(new Asset(
                1,
                "CODE-1",
                "TID-1",
                "",
                "",
                "Laptop A",
                "Laptop",
                "SER-1",
                "IT",
                "Truong Vu",
                "Lầu 5 - TT16",
                "Đang sử dụng",
                "",
                "",
                "",
                "",
                "api"
        ));
        return assets;
    }
}

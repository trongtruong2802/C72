package com.idocean.asset.data.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonParseException;
import com.idocean.asset.data.dto.InventoryCheckinResponseDto;

import org.junit.Test;

public class InventoryCheckinResponseParserTest {
    private final InventoryCheckinResponseParser parser = new InventoryCheckinResponseParser();

    @Test
    public void parse_readsSuccessfulResponseWithInsertedRows() {
        InventoryCheckinResponseDto response = parser.parse("{"
                + "\"success\":true,"
                + "\"message\":\"Xu ly batch kiem ke thanh cong\","
                + "\"session_id\":\"session-01\","
                + "\"total_received\":12,"
                + "\"total_scanned_valid\":8,"
                + "\"total_skipped\":4,"
                + "\"total_inserted\":8,"
                + "\"inserted_rows\":[{\"code\":\"BAUV7J\",\"tid\":\"TID-01\",\"checkin\":\"2026-04-04 10:54:31\"}]"
                + "}");

        assertTrue(response.isSuccess());
        assertEquals("session-01", response.getSessionId());
        assertEquals(12, response.getTotalReceived());
        assertEquals(8, response.getTotalInserted());
        assertEquals(1, response.getInsertedRows().size());
        assertEquals("BAUV7J", response.getInsertedRows().get(0).getCode());
    }

    @Test
    public void parse_marksZeroInsertedSuccessAsWarningOnly() {
        InventoryCheckinResponseDto response = parser.parse("{"
                + "\"success\":true,"
                + "\"message\":\"Khong co tai san hop le\","
                + "\"total_received\":10,"
                + "\"total_inserted\":0"
                + "}");

        assertTrue(response.isSuccess());
        assertTrue(response.isWarningOnly());
        assertEquals(0, response.getInsertedRows().size());
    }

    @Test
    public void parse_readsNestedSummaryResponseFromCurrentBackendFormat() {
        InventoryCheckinResponseDto response = parser.parse("{"
                + "\"success\":true,"
                + "\"message\":\"Khong co tai san da quet hop le de luu DB\","
                + "\"summary\":{"
                + "\"session_id\":\"session-02\","
                + "\"total_received\":\"12\","
                + "\"total_scanned_valid\":\"0\","
                + "\"total_skipped\":\"12\""
                + "}"
                + "}");

        assertTrue(response.isSuccess());
        assertTrue(response.isWarningOnly());
        assertEquals("session-02", response.getSessionId());
        assertEquals(12, response.getTotalReceived());
        assertEquals(12, response.getTotalSkipped());
        assertEquals(0, response.getTotalInserted());
    }

    @Test
    public void parse_ignoresUnresolvedTemplateValuesInsideSummary() {
        InventoryCheckinResponseDto response = parser.parse("{"
                + "\"success\":true,"
                + "\"message\":\"Khong co tai san da quet hop le de luu DB\","
                + "\"summary\":{"
                + "\"session_id\":\"={{ $json.session_id }}\","
                + "\"total_received\":\"={{ $json.total_received }}\","
                + "\"total_scanned_valid\":\"={{ $json.total_scanned_valid }}\","
                + "\"total_skipped\":\"={{ $json.total_skipped }}\""
                + "}"
                + "}");

        assertTrue(response.isSuccess());
        assertEquals("", response.getSessionId());
        assertEquals(0, response.getTotalReceived());
        assertEquals(0, response.getTotalScannedValid());
        assertEquals(0, response.getTotalSkipped());
    }

    @Test
    public void parse_readsBusinessFailureResponse() {
        InventoryCheckinResponseDto response = parser.parse("{"
                + "\"success\":false,"
                + "\"message\":\"Invalid input\""
                + "}");

        assertFalse(response.isSuccess());
        assertEquals("Invalid input", response.getMessage());
    }

    @Test(expected = JsonParseException.class)
    public void parse_throwsForBlankBody() {
        parser.parse("");
    }
}

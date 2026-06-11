package com.idocean.asset.data.repository;

import org.junit.Test;

import java.net.SocketTimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AssetErrorFormatterTest {
    @Test
    public void isTimeout_detectsSocketTimeoutException() {
        assertTrue(AssetErrorFormatter.isTimeout(new SocketTimeoutException("timeout")));
    }

    @Test
    public void buildConnectivityMessage_formatsTimeoutMessage() {
        assertEquals(
                "Ket noi toi backend update-asset bi qua thoi gian cho khi cap nhat tai san. Thay doi chua duoc luu.",
                AssetErrorFormatter.buildConnectivityMessage(
                        "update-asset",
                        "cap nhat tai san",
                        new SocketTimeoutException("timeout")
                )
        );
    }

    @Test
    public void buildHttpMessage_formatsActivatedWorkflow404Message() {
        assertEquals(
                "Backend checkout-asset chua duoc kich hoat tren n8n. Can activate workflow hoac tao production webhook /checkout-asset.",
                AssetErrorFormatter.buildHttpMessage(
                        404,
                        "Endpoint checkout-asset is not registered",
                        "checkout-asset",
                        "ghi nhan ban giao tai san"
                )
        );
    }
}

package com.idocean.asset.ui.lookup;

import android.content.Context;

import com.idocean.asset.R;
import com.idocean.asset.scanner.rfid.UhfScanData;
import com.idocean.asset.scanner.rfid.RfidTagNormalizer;
import com.idocean.asset.utils.EpcUtils;
import com.rscja.deviceapi.RFIDWithUHFUART;
import com.rscja.deviceapi.entity.InventoryModeEntity;
import com.rscja.deviceapi.entity.UHFTAGInfo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RfidScannerController {
    private final ExecutorService executor;
    private final LookupScannerCallback callback;
    private final Object readerLock = new Object();
    private RFIDWithUHFUART reader;
    private InventoryModeEntity previousInventoryMode;
    private boolean readerReady;
    private volatile boolean started;

    public RfidScannerController(LookupScannerCallback callback) {
        this(callback, Executors.newSingleThreadExecutor());
    }

    RfidScannerController(LookupScannerCallback callback, ExecutorService executor) {
        this.callback = callback;
        this.executor = executor;
    }

    public void onStart() {
        started = true;
    }

    public void onStop() {
        started = false;
        release();
    }

    public void shutdown() {
        onStop();
        executor.shutdownNow();
    }

    public void scanSingle(Context context) {
        if (!started || callback == null || context == null) {
            return;
        }

        callback.onScannerPreparingChanged(true);
        executor.execute(() -> {
            if (!started) {
                return;
            }
            RfidLookupResult result = readSingleRfid(context.getApplicationContext());
            if (!started) {
                return;
            }
            callback.onScannerPreparingChanged(false);
            if (!result.success) {
                callback.onScannerError(result.errorMessage);
                return;
            }
            callback.onScanResult(ScanResult.rfid(result.tid, result.code, result.timestamp));
        });
    }

    public boolean isReady() {
        synchronized (readerLock) {
            return readerReady && reader != null;
        }
    }

    private RfidLookupResult readSingleRfid(Context context) {
        synchronized (readerLock) {
            if (!initReaderIfNeeded(context)) {
                return RfidLookupResult.error("Khong mo duoc dau doc UHF.");
            }
            if (!ensureTidMode()) {
                return RfidLookupResult.error("Khong chuyen duoc reader sang mode EPC + TID.");
            }

            UHFTAGInfo tagInfo = reader == null ? null : reader.inventorySingleTag();
            if (tagInfo == null) {
                return RfidLookupResult.error("Khong doc duoc the RFID.");
            }

            UhfScanData scanData = UhfScanData.from(tagInfo);
            String tid = RfidTagNormalizer.sanitizeTid(scanData.getTid());
            String epcHex = RfidTagNormalizer.normalizeHex(scanData.getEpcHex());
            String code = safe(scanData.getEpcAsciiCode());
            if (tid.isEmpty() && !epcHex.isEmpty()) {
                tid = readTidFromBank(epcHex);
            }
            if (code.isEmpty()) {
                code = EpcUtils.hexToAscii(epcHex);
            }
            if (tid.isEmpty()) {
                return RfidLookupResult.error(context.getString(R.string.lookup_scanner_unknown));
            }
            return RfidLookupResult.success(tid, code);
        }
    }

    private boolean initReaderIfNeeded(Context context) {
        if (readerReady && reader != null) {
            return true;
        }
        try {
            reader = RFIDWithUHFUART.getInstance();
            if (reader == null) {
                readerReady = false;
                return false;
            }
            readerReady = reader.init(context.getApplicationContext());
            if (readerReady) {
                previousInventoryMode = reader.getEPCAndTIDUserMode();
            }
            return readerReady;
        } catch (Exception exception) {
            readerReady = false;
            return false;
        }
    }

    private boolean ensureTidMode() {
        synchronized (readerLock) {
            if (reader == null) {
                return false;
            }
            boolean result = reader.setEPCAndTIDUserMode(
                    new InventoryModeEntity.Builder()
                            .setMode(InventoryModeEntity.MODE_EPC_TID)
                            .build()
            );
            if (!result) {
                result = reader.setEPCAndTIDMode();
            }
            return result;
        }
    }

    private String readTidFromBank(String epcHex) {
        synchronized (readerLock) {
            if (reader == null || epcHex == null || epcHex.trim().isEmpty()) {
                return "";
            }
            try {
                return RfidTagNormalizer.sanitizeTid(
                        reader.readData(
                                "00000000",
                                RFIDWithUHFUART.Bank_EPC,
                                32,
                                epcHex.trim().length() * 4,
                                epcHex.trim(),
                                RFIDWithUHFUART.Bank_TID,
                                0,
                                6
                        )
                );
            } catch (Exception exception) {
                return "";
            }
        }
    }

    public void release() {
        synchronized (readerLock) {
            if (reader == null) {
                readerReady = false;
                return;
            }
            try {
                if (previousInventoryMode != null) {
                    reader.setEPCAndTIDUserMode(previousInventoryMode);
                }
            } catch (Exception ignored) {
            }
            try {
                reader.free();
            } catch (Exception ignored) {
            }
            reader = null;
            previousInventoryMode = null;
            readerReady = false;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class RfidLookupResult {
        private final boolean success;
        private final String tid;
        private final String code;
        private final String errorMessage;
        private final long timestamp;

        private RfidLookupResult(boolean success, String tid, String code, String errorMessage, long timestamp) {
            this.success = success;
            this.tid = tid;
            this.code = code;
            this.errorMessage = errorMessage;
            this.timestamp = timestamp;
        }

        static RfidLookupResult success(String tid, String code) {
            return new RfidLookupResult(true, tid, code, "", System.currentTimeMillis());
        }

        static RfidLookupResult error(String errorMessage) {
            return new RfidLookupResult(false, "", "", errorMessage, System.currentTimeMillis());
        }
    }
}

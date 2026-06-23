package com.idocean.asset.scanner.core;

import android.content.Context;

import com.idocean.asset.scanner.rfid.RfidTagNormalizer;
import com.rscja.deviceapi.RFIDWithUHFUART;
import com.rscja.deviceapi.entity.InventoryModeEntity;
import com.rscja.deviceapi.entity.InventoryParameter;
import com.rscja.deviceapi.entity.UHFTAGInfo;
import com.rscja.deviceapi.interfaces.IUHFInventoryCallback;

public final class RfidReaderSession {
    private final Object readerLock = new Object();

    private RFIDWithUHFUART reader;
    private InventoryModeEntity previousInventoryMode;
    private boolean readerReady;
    private boolean inventoryRunning;

    public boolean initIfNeeded(Context appContext) {
        synchronized (readerLock) {
            if (readerReady && reader != null) {
                return true;
            }
            try {
                reader = RFIDWithUHFUART.getInstance();
                if (reader == null) {
                    readerReady = false;
                    return false;
                }
                Context safeContext = appContext == null ? null : appContext.getApplicationContext();
                readerReady = reader.init(safeContext);
                if (readerReady) {
                    previousInventoryMode = reader.getEPCAndTIDUserMode();
                }
                return readerReady;
            } catch (Exception exception) {
                readerReady = false;
                return false;
            }
        }
    }

    public boolean ensureTidMode() {
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

    public UHFTAGInfo inventorySingleTag() {
        synchronized (readerLock) {
            return reader == null ? null : reader.inventorySingleTag();
        }
    }

    public boolean startInventory(IUHFInventoryCallback inventoryCallback) {
        synchronized (readerLock) {
            if (reader == null) {
                inventoryRunning = false;
                return false;
            }
            try {
                reader.setInventoryCallback(inventoryCallback);
                InventoryParameter parameter = new InventoryParameter();
                parameter.setResultData(new InventoryParameter.ResultData().setNeedPhase(false));
                inventoryRunning = reader.startInventoryTag(parameter);
                if (!inventoryRunning) {
                    reader.setInventoryCallback(null);
                }
                return inventoryRunning;
            } catch (Exception exception) {
                try {
                    reader.setInventoryCallback(null);
                } catch (Exception ignored) {
                }
                inventoryRunning = false;
                return false;
            }
        }
    }

    public void stopInventory() {
        synchronized (readerLock) {
            stopInventoryLocked();
        }
    }

    public String readTidFromBank(String epcHex) {
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
                inventoryRunning = false;
                return;
            }
            stopInventoryLocked();
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
            inventoryRunning = false;
        }
    }

    public boolean isReady() {
        synchronized (readerLock) {
            return readerReady && reader != null;
        }
    }

    public boolean isInventoryRunning() {
        synchronized (readerLock) {
            return inventoryRunning;
        }
    }

    private void stopInventoryLocked() {
        if (reader == null) {
            inventoryRunning = false;
            return;
        }
        try {
            if (inventoryRunning || reader.isInventorying()) {
                reader.stopInventory();
            }
        } catch (Exception ignored) {
        }
        try {
            reader.setInventoryCallback(null);
        } catch (Exception ignored) {
        }
        inventoryRunning = false;
    }
}

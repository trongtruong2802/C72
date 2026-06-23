package com.idocean.asset.scanner.core;

import android.content.Context;

import com.idocean.asset.diagnostics.AppErrorCodes;
import com.rscja.deviceapi.entity.UHFTAGInfo;

public final class RfidSingleScanRunner {
    private RfidSingleScanRunner() {
    }

    public static Result run(Context appContext,
                             RfidReaderSession rfidReaderSession,
                             String initFailedMessage,
                             String modeFailedMessage,
                             String readFailedMessage) {
        if (rfidReaderSession == null) {
            return Result.error(AppErrorCodes.RFID_INIT_FAILED, initFailedMessage);
        }
        if (!rfidReaderSession.initIfNeeded(appContext)) {
            return Result.error(AppErrorCodes.RFID_INIT_FAILED, initFailedMessage);
        }
        if (!rfidReaderSession.ensureTidMode()) {
            return Result.error(AppErrorCodes.RFID_MODE_FAILED, modeFailedMessage);
        }

        UHFTAGInfo tagInfo = rfidReaderSession.inventorySingleTag();
        if (tagInfo == null) {
            return Result.error(AppErrorCodes.RFID_SINGLE_SCAN_FAILED, readFailedMessage);
        }

        return Result.success(RfidTagDecoder.decode(tagInfo, rfidReaderSession::readTidFromBank));
    }

    public static final class Result {
        private final RfidTagDecoder.DecodedTag decodedTag;
        private final String errorCode;
        private final String errorMessage;

        private Result(RfidTagDecoder.DecodedTag decodedTag, String errorCode, String errorMessage) {
            this.decodedTag = decodedTag;
            this.errorCode = errorCode == null ? "" : errorCode;
            this.errorMessage = errorMessage == null ? "" : errorMessage;
        }

        static Result success(RfidTagDecoder.DecodedTag decodedTag) {
            return new Result(decodedTag, "", "");
        }

        static Result error(String errorCode, String errorMessage) {
            return new Result(null, errorCode, errorMessage);
        }

        public boolean isSuccess() {
            return decodedTag != null;
        }

        public RfidTagDecoder.DecodedTag getDecodedTag() {
            return decodedTag;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}

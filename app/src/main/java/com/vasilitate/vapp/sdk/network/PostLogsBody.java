package com.vasilitate.vapp.sdk.network;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;
import com.vasilitate.vapp.sdk.exceptions.VappApiException;

import java.util.List;

/**
 * Models the payload for POST /logs
 */
public final class PostLogsBody {

    public static final String PLUS_SYMBOL = "+";

    private final List<LogEntry> logs;
    private final String cli;

    @SerializedName("cli_detail")
    private final String cliDetail;

    public PostLogsBody(List<LogEntry> logs, String cli, String cliDetail) {
        this.logs = logs;
        this.cliDetail = cliDetail;

        if (cli.startsWith(PLUS_SYMBOL)) {
            this.cli = cli.replace(PLUS_SYMBOL, "");
        }
        else {
            this.cli = cli;
        }
    }

    public List<LogEntry> getLogs() {
        return logs;
    }

    public String getCli() {
        return cli;
    }

    public String getCliDetail() {
        return cliDetail;
    }

    public static class LogEntry {

        private final String message;
        private final String ddi;

        public LogEntry(String message, String ddi) {
            this.message = message;

            validateParameter(message, "message");
            validateParameter(ddi, "ddi");

            if (ddi.startsWith(PLUS_SYMBOL)) {
                this.ddi = ddi.replace(PLUS_SYMBOL, "");
            }
            else {
                this.ddi = ddi;
            }
        }

        private void validateParameter(String value, String paramName) {
            if (TextUtils.isEmpty(value)) {
                throw new VappApiException(String.format("Parameter '%s' cannot be empty.", paramName));
            }
        }

        public String getMessage() {
            return message;
        }

        public String getDdi() {
            return ddi;
        }
    }

}

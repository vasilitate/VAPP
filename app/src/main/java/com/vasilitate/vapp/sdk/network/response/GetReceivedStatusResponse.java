package com.vasilitate.vapp.sdk.network.response;

import android.support.annotation.Nullable;
import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Models the response for GET /receivedstatus
 */
public final class GetReceivedStatusResponse extends BaseResponse {

    @StringDef({RECEIVED_STATUS_YES, RECEIVED_STATUS_NOT_YET, RECEIVED_STATUS_BLACKLISTED})
    @Retention(RetentionPolicy.SOURCE) @interface ReceivedStatus {
    }

    public static final String RECEIVED_STATUS_YES = "yes";
    public static final String RECEIVED_STATUS_NOT_YET = "notYet";
    public static final String RECEIVED_STATUS_BLACKLISTED = "blacklisted";

    @ReceivedStatus private final String received;

    public GetReceivedStatusResponse(@RequestStatus @Nullable String status, @Nullable String error, String received) {
        super(status, error);
        this.received = received;
    }

    public String getReceived() {
        return received;
    }
}

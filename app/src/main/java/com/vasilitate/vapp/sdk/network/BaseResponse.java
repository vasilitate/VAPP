package com.vasilitate.vapp.sdk.network;

import android.support.annotation.Nullable;
import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The base response model which is returned from all API calls, consisting of a status & error
 */
public abstract class BaseResponse {

    @StringDef({HNI_STATUS_UNKNOWN, HNI_STATUS_WHITELISTED, HNI_STATUS_BLACKLISTED})
    @Retention(RetentionPolicy.SOURCE) @interface RequestStatus {
    }

    public static final String HNI_STATUS_UNKNOWN = "unknown";
    public static final String HNI_STATUS_WHITELISTED = "whitelisted";
    public static final String HNI_STATUS_BLACKLISTED = "blacklisted";

    @RequestStatus @Nullable private final String status;

    @Nullable private final String error;

    public BaseResponse(@RequestStatus @Nullable String status, @Nullable String error) {
        this.status = status;
        this.error = error;
    }

    @RequestStatus @Nullable public String getStatus() {
        return status;
    }

    @Nullable public String getError() {
        return error;
    }
}

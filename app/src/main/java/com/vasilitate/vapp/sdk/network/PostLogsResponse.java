package com.vasilitate.vapp.sdk.network;

import android.support.annotation.Nullable;

/**
 * Models the response for POST /logs
 */
public class PostLogsResponse extends BaseResponse {

    public PostLogsResponse(@RequestStatus @Nullable String status, @Nullable String error) {
        super(status, error);
    }
}

package com.vasilitate.vapp.sdk.network;

import android.support.annotation.Nullable;

/**
 * Models the response for GET /hnistatus
 */
public final class GetHniStatusResponse extends BaseResponse {

    public GetHniStatusResponse(@RequestStatus @Nullable String status, @Nullable String error) {
        super(status, error);
    }
}

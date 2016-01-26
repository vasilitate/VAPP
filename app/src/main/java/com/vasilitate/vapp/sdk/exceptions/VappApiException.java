package com.vasilitate.vapp.sdk.exceptions;

public class VappApiException extends VappException {

    public VappApiException(String detailMessage) {
        super(detailMessage);
    }

    public VappApiException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }
}

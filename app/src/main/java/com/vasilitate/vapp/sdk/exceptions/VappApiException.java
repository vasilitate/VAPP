package com.vasilitate.vapp.sdk.exceptions;

/**
 * An expection that is thrown when an error occurs with a call to the VAPP backend
 */
public class VappApiException extends VappException {

    public VappApiException(String detailMessage) {
        super(detailMessage);
    }

    public VappApiException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }
}

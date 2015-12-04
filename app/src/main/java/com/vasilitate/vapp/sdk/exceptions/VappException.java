package com.vasilitate.vapp.sdk.exceptions;

/**
 * A general exception which is thrown by the VAPP! SDK
 */
public class VappException extends RuntimeException {

    public VappException(String detailMessage) {
        super(detailMessage);
    }
    
}

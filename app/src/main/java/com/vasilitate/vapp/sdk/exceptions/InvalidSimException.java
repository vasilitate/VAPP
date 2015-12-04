package com.vasilitate.vapp.sdk.exceptions;

/**
 * An exception that is thrown when the device's does not have a SIM in a ready state.
 */
public class InvalidSimException extends VappException {

    public InvalidSimException(String detailMessage) {
        super(detailMessage);
    }
    
}

package com.vasilitate.vapp.sdk.exceptions;

/**
 * An exception that is thrown when the device is not connected to an originating network
 */
public class NetworkNotSupportedException extends VappException {

    public NetworkNotSupportedException(String detailMessage) {
        super(detailMessage);
    }
}

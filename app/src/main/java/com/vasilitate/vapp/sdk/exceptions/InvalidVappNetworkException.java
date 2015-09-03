package com.vasilitate.vapp.sdk.exceptions;

/**
 * An exception that is thrown when the SMS network is not valid
 */
public class InvalidVappNetworkException extends VappException {

    public InvalidVappNetworkException(String detailMessage) {
        super(detailMessage);
    }
}

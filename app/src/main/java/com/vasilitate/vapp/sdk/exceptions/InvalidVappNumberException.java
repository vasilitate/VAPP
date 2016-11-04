package com.vasilitate.vapp.sdk.exceptions;

/**
 * An exception that is thrown when the Vapp number range is incorrect
 */
public class InvalidVappNumberException extends VappException {

    public InvalidVappNumberException(String detailMessage) {
        super(detailMessage);
    }
}

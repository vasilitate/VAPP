package com.vasilitate.vapp.sdk.exceptions;

/**
 * An exception that is thrown when a product's configuration is invalid
 */
public class InvalidVappProductException extends VappException {

    public InvalidVappProductException(String detailMessage) {
        super(detailMessage);
    }
}

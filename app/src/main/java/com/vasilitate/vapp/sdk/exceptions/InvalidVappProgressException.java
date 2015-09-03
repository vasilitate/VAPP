package com.vasilitate.vapp.sdk.exceptions;

/**
 * An exception that is thrown when an attempt is made to set an invalid progress for a product
 */
public class InvalidVappProgressException extends VappException {

    public InvalidVappProgressException(int progress, int requiredCount) {
        super(String.format("Payment progress %d not within allowed range of 1-%d", progress, requiredCount));
    }

}

package com.vasilitate.vapp.sdk.exceptions;

public class InvalidVappProgressException extends VappException {

    public InvalidVappProgressException(int progress, int requiredCount) {
        super(String.format("Payment progress %d not within allowed range of 1-%d", progress, requiredCount));
    }

}

package com.vasilitate.vapp.sdk.exceptions;

/**
 * An exception that is thrown when an attempt is made to create an invalid Product.
 */
public class InvalidProductIdException extends VappException {

    public InvalidProductIdException(String detailMessage) {
        super(detailMessage);
    }

}

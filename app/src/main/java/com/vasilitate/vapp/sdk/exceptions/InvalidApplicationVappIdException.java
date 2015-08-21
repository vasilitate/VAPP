package com.vasilitate.vapp.sdk.exceptions;

/**
 * An exception that is thrown when an attempt is made to set the SDK's application Vapp Id to an
 * invalid format.
 */
public class InvalidApplicationVappIdException extends VappException {

    public InvalidApplicationVappIdException(String detailMessage) {
        super(detailMessage);
    }

}

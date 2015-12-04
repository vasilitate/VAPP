package com.vasilitate.vapp.sdk;

import com.vasilitate.vapp.sdk.exceptions.InvalidVappNumberException;

/**
 * A range of numbers provided by Vasilitate, which all SMSs should be sent to.
 */
public class VappNumberRange {

    private long startOfRange;
    private long endOfRange;

    public VappNumberRange(String startNumber, String endNumber) {

        startOfRange = parseNumber( startNumber );
        endOfRange = parseNumber( endNumber );

        if( startOfRange > endOfRange ) {
            throw new InvalidVappNumberException("Invalid number range " + startNumber + " > " + endNumber );
        }
    }

    private long parseNumber( String telephoneNumber) {

        if( telephoneNumber == null || !telephoneNumber.startsWith(VappProductManager.INTERNATIONAL_PREFIX) ) {
            throw new InvalidVappNumberException("Telephone number not prefixed with " + VappProductManager.INTERNATIONAL_PREFIX);
        }

        try {
            return Long.parseLong(telephoneNumber.substring(VappProductManager.INTERNATIONAL_PREFIX.length()));
        } catch( Exception e ) {
            throw new InvalidVappNumberException("Invalid Telephone number: " + telephoneNumber );
        }
    }

    public long getRangeSize() {
        return endOfRange - startOfRange + 1;
    }

    public long getStartOfRange() {
        return startOfRange;
    }

    public long getEndOfRange() {
        return endOfRange;
    }

    @Override
    public String toString() {
        return String.format( "%d - %d", startOfRange, endOfRange );
    }
}

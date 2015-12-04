package com.vasilitate.vapp.sdk.exceptions;

import com.vasilitate.vapp.sdk.VappProduct;

/**
 * An exception that is thrown when an SMS count for a product exceeds the allowed bounds by the SDK
 */
public class InvalidSmsCountException extends VappException {

    public InvalidSmsCountException(VappProduct product, int maxSMSLimit ) {
        super(String.format("Product '%s' is not within allowed Required SMS Count range of 1-%d, with count of %d",
                product.getProductId(), maxSMSLimit, product.getRequiredSmsCount()));
    }
}

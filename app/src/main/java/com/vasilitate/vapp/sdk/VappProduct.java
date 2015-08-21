package com.vasilitate.vapp.sdk;

/**
 * A VAPP Product which may be used to unlock functionality once the given number
 * of SMSs have been successfully sent.
 */
public class VappProduct {

    /**
     * A unique id for the product (max 15 characters).
     */
    private final String productId;

    /**
     * The number of SMSs required to unlock the product.
     */
    private final int smsCount;

    /**
     * The maximum number of products of this type allowed to be owned at any one time.
     */
    private int maxProductCount;

    /**
     * Vapp Product Constructor.
     *
     * @param productId Vapp Product Id (1 - 15 alpha-numeric characters (no spaces)).
     * @param smsCount the number of SMSs required to redeem one instance of this product.
     * @param maxProductCount the maximum allowed number of instances of this product e.g. set
     *                        to '1' if this product unlocks a privilege that can only be bought
     *                        once.
     */
    public VappProduct(String productId, int smsCount, int maxProductCount ) {
        this.productId = productId;
        this.smsCount = smsCount;
        this.maxProductCount = maxProductCount;
    }

    /**
     * @return the Vapp Product Id
     */
    public String getProductId() {
        return productId;
    }

    /**
     * @return the number of SMSs required to redeem one instance of this product.
     */
    public int getRequiredSmsCount() {
        return smsCount;
    }

    /**
     * @return the maximum allowed number of instances of this product.
     */
    public int getMaxProductCount() {
        return maxProductCount;
    }
}

package com.vasilitate.vapp.sdk;

import android.content.Context;

import com.vasilitate.vapp.sdk.exceptions.VappException;

import java.util.Calendar;
import java.util.Date;

/**
 * Represents a Virtual Product which can be purchased by sending SMSs.
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
     * Subscriptions can be scheduled every:
     *      'x' days,
     *      'x' weeks or
     *      on a given day of each month.
     */
    private SubscriptionIntervalType intervalType;

    /**
     * If interval type is set to 'DAY' this is the number of days between each subscription
     * request.
     * If interval type is set to 'WEEK' this is the number of weeks between each subscription
     * request.
     * If interval type is 'DAY_OF_MONTH' this is the day of the month on which each the submission
     * is requested (valid range is 1 - 28).
     */
    private int interval;

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
     * Vapp Product Constructor.
     *
     * @param productId Vapp Product Id (1 - 15 alpha-numeric characters (no spaces)).
     * @param smsCount the number of SMSs required to redeem one instance of this product.
     * @param intervalType - days, weeks or day of month.
     * @param interval if interval type is set to 'DAY' this is the number of days between
     *                 each subscription request.
     *                 If interval type is set to 'WEEK' this is the number of weeks between
     *                 each subscription request.
     *                 If interval type is 'DAY_OF_MONTH' this is the day of the month on which
     *                 each the submission is requested (valid range is 1 - 28).
     */
    public VappProduct(String productId, int smsCount,
                       SubscriptionIntervalType intervalType,
                       int interval ) {
        this.productId = productId;
        this.smsCount = smsCount;
        this.intervalType = intervalType;
        this.interval = interval;
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

    /**
     * @return the type of subscription interval (days, weeks or day of month).
     */
    public SubscriptionIntervalType getIntervalType() {
        return intervalType;
    }

    /**
     * @return the subscription interval.
     */
    public int getInterval() {
        return interval;
    }

    /**
     * @return true is a subscription product.
     */
    boolean isSubscriptionProduct() {
        return intervalType != null;
    }

    // Made pubic for testing. - FIXME
    public Date getNextSubscriptionEndDate(final Date startDate ) {

        if( intervalType == null ) {
            throw new VappException("getNextSubscriptionEndDate() called on non-subscription product");
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime( startDate );

        switch( intervalType ) {
            case DAY:
                cal.add( Calendar.DATE, interval );
                break;
            case WEEK:
                cal.add( Calendar.DATE, interval * 7 );
                break;
            case DAY_OF_MONTH:

                // if the 29th, 30th or 31st, move onto the first on the next month...
                if( interval > 28 ) {
                    cal.set( Calendar.DAY_OF_MONTH, 1 );
                    cal.add( Calendar.MONTH, 1);
                } else {
                    cal.set( Calendar.DAY_OF_MONTH, interval );
                }
                // Now add a month to the date.
                cal.add(Calendar.MONTH, 1);
                break;
            default:
                throw new VappException("getNextSubscriptionEndDate() unknown interval type.");
        }

        // Set the end time to a second before midnight...
        cal.set( Calendar.HOUR_OF_DAY, 23 );
        cal.set( Calendar.MINUTE, 59 );
        cal.set( Calendar.SECOND, 59 );

        return cal.getTime();
    }
}

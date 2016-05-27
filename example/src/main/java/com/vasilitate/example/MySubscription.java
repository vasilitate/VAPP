package com.vasilitate.example;

import com.vasilitate.vapp.sdk.SubscriptionIntervalType;
import com.vasilitate.vapp.sdk.VappProduct;

import java.util.ArrayList;
import java.util.List;

/**
 * A convenience enum for setting up the subscriptions.
 */
public enum MySubscription {

    //               productId                  requiredSMSs   intervalType                   Interval
    DAILY_SUBSCRIPTION(  "DailySub",            10,            SubscriptionIntervalType.DAY,  1),
    WEEKLY_SUBSCRIPTION( "WeeklySub",           5,             SubscriptionIntervalType.WEEK, 2),
    DAY_OF_MONTH_SUBSCRIPTION( "DayOfMonthSub", 2,             SubscriptionIntervalType.DAY_OF_MONTH, 28);

    private VappProduct vappProduct;

    MySubscription(String productId,
                   int requiredSMSs,
                   SubscriptionIntervalType intervalType,
                   int interval ) {
        vappProduct = new VappProduct( productId, requiredSMSs, intervalType, interval );
    }

    public VappProduct getVappProduct() {
        return vappProduct;
    }

    public static List<VappProduct> getSubscriptions() {

        List<VappProduct> productList = new ArrayList<>();
        for( MySubscription product : values() ) {
            productList.add(product.vappProduct);
        }
        return productList;
    }
}

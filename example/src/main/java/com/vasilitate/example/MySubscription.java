package com.vasilitate.example;

import com.vasilitate.vapp.sdk.SubscriptionIntervalType;
import com.vasilitate.vapp.sdk.VappProduct;

import java.util.ArrayList;
import java.util.List;

/**
 * A convenience enum for setting up the subscriptions.
 */
public enum MySubscription {

    DAILY_SUBSCRIPTION("DailySub", 8, SubscriptionIntervalType.DAY, 10),
    WEEKLY_SUBSCRIPTION("WeeklySub", 5, SubscriptionIntervalType.WEEK, 2),
    DAY_OF_MONTH_SUBSCRIPTION("DayOfMonthSub", 2, SubscriptionIntervalType.DAY_OF_MONTH, 28);

    private VappProduct vappProduct;

    MySubscription(String productId,
                   int requiredSMSs,
                   SubscriptionIntervalType intervalType,
                   int interval) {

        vappProduct = new VappProduct.Builder(productId, requiredSMSs)
                .setSubscriptionInterval(intervalType, interval)
                .build();
    }

    public VappProduct getVappProduct() {
        return vappProduct;
    }

    public static List<VappProduct> getSubscriptions() {

        List<VappProduct> productList = new ArrayList<>();
        for (MySubscription product : values()) {
            productList.add(product.vappProduct);
        }
        return productList;
    }
}

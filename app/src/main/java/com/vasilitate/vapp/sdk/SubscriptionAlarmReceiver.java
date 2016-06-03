package com.vasilitate.vapp.sdk;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import java.util.Date;


/**
 * Kick off the sending of SMSs will keeping the device awake...
 */
public class SubscriptionAlarmReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {

        VappProduct product = Vapp.getProductBeingPurchased( context );

        // If no products currently being purchased, check if any subscriptions have passed their
        // end dates.  Trigger a subscription update sequence if they have...
        if( product != null ) {
            Intent smsIntent = new Intent(context, VappSmsService.class);
            smsIntent.putExtra(VappActions.EXTRA_PRODUCT_ID, product.getProductId());
            startWakefulService(context, smsIntent);
        }
    }
}

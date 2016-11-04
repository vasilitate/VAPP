package com.vasilitate.vapp.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;


/**
 * On booting check if any subscriptions are due or if future subscriptions are found,
 * reinstate their alarms.
 */
public class BootReceiver extends WakefulBroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {

        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            Log.d( Vapp.TAG, "Processing reboot" );
            VappProduct productToPurchases = Vapp.processReboot( context );

            if( productToPurchases != null ) {
                Log.d( Vapp.TAG, "Processing reboot - product: " + productToPurchases.getProductId()  );

                Intent smsIntent = new Intent(context, VappSmsService.class);
                smsIntent.putExtra(VappActions.EXTRA_PRODUCT_ID, productToPurchases.getProductId());
                startWakefulService(context, smsIntent);
            }
        }
    }
}

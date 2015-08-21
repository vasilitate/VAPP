package com.vasilitate.vapp.sdk;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.telephony.SmsManager;
import android.util.Log;

import com.vasilitate.vapp.R;

import java.util.Stack;

/**
 * Handles the sending of SMSs in the background
 */
public class VappSmsService extends Service {

    private static final String INTENT_SMS_SENT = "com.vasilitate.vapp.sdk.SMS_SENT";
    private static final String INTENT_SMS_DELIVERED = "com.vasilitate.vapp.sdk.INTENT_SMS_DELIVERED";

    private static final int SENT_SMS_REQUEST_CODE = 100;
    private static final int DELIVERED_SMS_REQUEST_CODE = 101;

    private VappProduct currentProduct;
    private int currentSmsIndex;

    private SmsSentReceiver smsSentReceiver;
    private SmsDeliveredReceiver smsDeliveredReceiver;

    private PendingIntent sentPI;
    private PendingIntent deliveredPI;

    private Stack<Integer> sendIntervals;
    private int secondsRemaining;
    private int durationInSeconds;
    private int totalSMSCount;

    private boolean testMode = false;

    private Handler completionHandler = new Handler();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        sentPI = PendingIntent.getBroadcast(this, SENT_SMS_REQUEST_CODE,
                new Intent(INTENT_SMS_DELIVERED), 0);
        deliveredPI = PendingIntent.getBroadcast(this, DELIVERED_SMS_REQUEST_CODE,
                new Intent(INTENT_SMS_SENT), 0);

        smsSentReceiver = new SmsSentReceiver();
        registerReceiver(smsSentReceiver,new IntentFilter(INTENT_SMS_SENT));

        smsDeliveredReceiver = new SmsDeliveredReceiver();

        registerReceiver(smsDeliveredReceiver, new IntentFilter(INTENT_SMS_DELIVERED));


        testMode = VappConfiguration.isTestMode(this);

        String productId = intent.getStringExtra(VappActions.EXTRA_PRODUCT_ID);
        currentProduct = Vapp.getProduct(productId);

        if( currentProduct != null ) {
            currentSmsIndex = Vapp.getSMSPaymentProgress(this, currentProduct);

            initialiseSendIntervals();

            sendNextSMSForCurrentProduct();
        } else {

            // Unrecognised Product!
            terminateService();
        }
        return START_STICKY;
    }

    private void initialiseSendIntervals() {

        totalSMSCount = VappConfiguration.getCurrentDownloadSmsCountForProduct(VappSmsService.this,
                currentProduct);
        int sentCount = VappConfiguration.getSentSmsCountForProduct(VappSmsService.this,
                currentProduct);

        sendIntervals = new Stack<>();
        secondsRemaining = 0;
        for( int i = 0; i < totalSMSCount - sentCount - 1; i++ ) {
            int interval = VappProductManager.generateSMSSendInterval( this );
            sendIntervals.push(interval);
            secondsRemaining += interval;
        }
        durationInSeconds = secondsRemaining;
    }

    @Override
    public void onDestroy() {

        if( smsSentReceiver != null ) {
            unregisterReceiver(smsSentReceiver);
            smsSentReceiver = null;
        }

        if( smsDeliveredReceiver != null ) {
            unregisterReceiver(smsDeliveredReceiver);
            smsDeliveredReceiver = null;
        }
        super.onDestroy();
    }

    private void terminateService() {
        broadcastSMSsCompleted();
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void sendNextSMSForCurrentProduct() {

        // Ensure there are still SMSs to send.  If not (e.g. after a re-start?) mark the
        // current product as bought...
        if (sendIntervals.size() == 0) {
            processSentSMS();
            return;
        }

        int interval = 0;
        if (currentSmsIndex != 0) {
            interval = sendIntervals.peek();
        }

        final int currentInterval = interval;

        int progressPercentage = (durationInSeconds - secondsRemaining) * 100 /durationInSeconds;

        broadcastProgress(currentSmsIndex, progressPercentage );

        if (testMode) {
            Log.d("VAPP!", String.format("Sending Mock SMS %d/%d",
                    currentSmsIndex, currentProduct.getRequiredSmsCount()));
        }

        if( interval == 0 ) {
            sendSMS();
        } else {
            CountDownTimer timer = new CountDownTimer(currentInterval * 1000, 200) {

                private int lastProgressPercentage = -1;

                @Override
                public void onTick(long millisUntilFinished) {

                    int remainingCount = secondsRemaining -
                            (currentInterval - (int) (millisUntilFinished / 1000));

                    int progressPercentage = (durationInSeconds - remainingCount) * 100 / durationInSeconds;

                    if (lastProgressPercentage != progressPercentage) {

                        broadcastProgressPercentage(progressPercentage);
                        lastProgressPercentage = progressPercentage;
                    }
                }

                @Override
                public void onFinish() {
                    secondsRemaining -= sendIntervals.pop();
                    sendSMS();
                }
            };
            timer.start();
        }
    }

    private void sendSMS() {
        try {
            String smsPhoneNumber =
                    VappProductManager.getRandomNumberInRange(Vapp.getDestinationNumberRange());
            String smsMessage = Vapp.getGeneratedSmsForProduct(VappSmsService.this,
                    currentProduct,
                    totalSMSCount,
                    currentSmsIndex);
            if (testMode) {

                Log.d("Vapp!", "Test SMS: " + smsPhoneNumber + ": " + smsMessage);
                processSentSMS();
            }
            else {
                SmsManager sms = SmsManager.getDefault();
                sms.sendTextMessage(smsPhoneNumber,
                        null,           // Call center number.
                        smsMessage,
                        sentPI,
                        deliveredPI);
            }


        } catch (Exception e) {
            broadcastSMSError(e.getMessage());
        }
    }

    private void broadcastProgress(int smsSentCount, int progressPercentage ) {

        Intent intent = new Intent(VappActions.ACTION_SMS_PROGRESS);
        intent.putExtra(VappActions.EXTRA_SMS_SENT_COUNT, smsSentCount);
        intent.putExtra(VappActions.EXTRA_PROGRESS_PERCENTAGE, progressPercentage);

        sendBroadcast(intent);
    }

    private void broadcastProgressPercentage(int progressPercentage) {
        Intent intent = new Intent(VappActions.ACTION_SMS_PROGRESS);
        intent.putExtra(VappActions.EXTRA_PROGRESS_PERCENTAGE, progressPercentage);
        sendBroadcast(intent);
    }

    private void broadcastSMSError(String error) {
        Intent intent = new Intent(VappActions.ACTION_SMS_PROGRESS);
        intent.putExtra(VappActions.EXTRA_ERROR_MESSAGE, error);
        sendBroadcast(intent);
    }

    private void broadcastSMSsCompleted() {
        Intent intent = new Intent(VappActions.ACTION_SMS_PROGRESS);
        intent.putExtra(VappActions.EXTRA_SMS_COMPLETED, true);
        sendBroadcast(intent);

        VappNotificationManager.post(VappSmsService.this);
    }

    private class SmsSentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent arg1) {

            Integer errorResId = null;
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    // Nothing to do - only marking off the SMS when we know it's been
                    // Delivered
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    errorResId = R.string.vapp_sms_sent_failure_generic;
                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    errorResId = R.string.vapp_sms_sent_failure_no_service;
                    break;
                case SmsManager.RESULT_ERROR_NULL_PDU:
                    errorResId = R.string.vapp_sms_sent_failure_null_pdu;
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    errorResId = R.string.vapp_sms_sent_failure_radio_off;
                    break;
            }

            if (errorResId != null) {
                broadcastSMSError(getString(errorResId));
            }
        }
    }

    private class SmsDeliveredReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            switch (getResultCode()) {

                case Activity.RESULT_OK:
                    processSentSMS();

                    break;
                case Activity.RESULT_CANCELED:
                    broadcastSMSError(getString(R.string.vapp_sms_delivery_failure));
                    break;
            }
        }
    }

    private void processSentSMS() {

        // The SMS has been delivered so move onto the next one (if
        // we have not reached the end).
        if (currentProduct != null) {

            // Move onto the next message...
            currentSmsIndex++;

            if (sendIntervals.size() > 0) {

                // Store the progress...
                VappConfiguration.setSentSmsCountForProduct(VappSmsService.this,
                        currentProduct,
                        currentSmsIndex);

                // Now initiate the sending of the next SMS...
                sendNextSMSForCurrentProduct();

            }
            else {

                // All SMSs have been sent for the current product, update the redeemed count...
                Vapp.addRedeemedProduct(VappSmsService.this, currentProduct);

                // Send a final progress update showing completion.
                broadcastProgress( currentProduct.getRequiredSmsCount(), 100 );

                // Delay the sending of the completion so that any clients can display
                // the completion of the purchase.
                completionHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        terminateService();
                    }
                }, 2000);
            }
        }
    }
}
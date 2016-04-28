package com.vasilitate.vapp.sdk;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.support.annotation.Nullable;
import android.telephony.SmsManager;
import android.util.Log;

import com.vasilitate.vapp.R;

import java.lang.ref.WeakReference;
import java.util.Stack;

import static android.telephony.SmsManager.RESULT_ERROR_GENERIC_FAILURE;
import static android.telephony.SmsManager.RESULT_ERROR_NO_SERVICE;
import static android.telephony.SmsManager.RESULT_ERROR_NULL_PDU;
import static android.telephony.SmsManager.RESULT_ERROR_RADIO_OFF;

/**
 * Manages the sending of SMS messages at random intervals.
 */
class SmsSendManager {

    private static final String INTENT_SMS_SENT = "com.vasilitate.vapp.sdk.SMS_SENT";
    private static final String INTENT_SMS_DELIVERED = "com.vasilitate.vapp.sdk.INTENT_SMS_DELIVERED";

    public interface SmsSendListener {

        /**
         * Called periodically to provide updates on progress for the whole purchase
         *
         * @param progressPercentage the percent progress for the whole purchase
         */
        void onSmsProgressUpdate(int progressPercentage);

        /**
         * Called when an sms is succesfully sent and increments the current sms index
         *
         * @param currentSmsIndex    the current sms index
         * @param progressPercentage the percent progress for the whole purchase
         */
        void onSmsProgressUpdate(int currentSmsIndex, int progressPercentage);

        /**
         * Called when the Android system cannot send an SMS
         *
         * @param message a message describing the error
         */
        void onSmsSendError(String message);

        // callbacks for broadcast receivers
        void onSmsDeliverySuccess();

        void onSmsDeliveryFailure();

        void onSmsSendComplete(Integer errorResId);

        void onSmsPurchaseCompleted();
    }

    private VappProduct currentProduct;
    private VappSms currentSmsMessage;

    private Stack<Integer> sendIntervals;
    private int secondsRemaining;
    private int durationInSeconds;
    private int totalSMSCount;
    private int currentSmsIndex;

    private CountDownTimer countDownTimer;
    private boolean testMode;

    private final Context context;
    private final WeakReference<Service> serviceRef;

    private final SmsSendListener sendListener;
    private final PendingIntent sentPI;
    private final PendingIntent deliveredPI;
    private final VappDbHelper vappDbHelper;
    private SmsSentReceiver smsSentReceiver;
    private SmsDeliveredReceiver smsDeliveredReceiver;

    private boolean isFirstInSequence = true;

    SmsSendManager(VappProduct currentProduct, boolean testMode,
                   PendingIntent sentPI, PendingIntent deliveredPI,
                   Service context, SmsSendListener sendListener) {

        this.currentProduct = currentProduct;
        this.testMode = testMode;
        this.sentPI = sentPI;
        this.deliveredPI = deliveredPI;
        this.context = context.getApplicationContext();
        this.serviceRef = new WeakReference<>(context);
        this.sendListener = sendListener;

        vappDbHelper = new VappDbHelper(this.context);
        totalSMSCount = VappConfiguration.getCurrentDownloadSmsCountForProduct(context, currentProduct);
        currentSmsIndex = Vapp.getSMSPaymentProgress(context, currentProduct);
        initialiseRandomSendIntervals();
    }

    /**
     * Create random ms intervals to wait between sending messages
     */
    private void initialiseRandomSendIntervals() {
        int sentCount = VappConfiguration.getSentSmsCountForProduct(context, currentProduct);
        sendIntervals = new Stack<>();
        secondsRemaining = 0;

        int smsToSend = (totalSMSCount - sentCount);

        for (int i = 1; i < smsToSend; i++) {
            int interval = VappProductManager.generateSMSSendInterval(context, Vapp.getAllDeliveryNumbers().size());
            sendIntervals.push(interval);
            secondsRemaining += interval;
        }
        durationInSeconds = secondsRemaining;
    }

    /**
     * Triggers the sending of an SMS for the current product. The time at which the SMS may be delayed
     * if the random interval since the last sending has not yet passed.
     */
    void addNextSmsToSendQueue() {
        if (sendIntervals.isEmpty()) {
            return;
        }

        if (isFirstInSequence) { // send immediately without waiting for an interval
            isFirstInSequence = false;
            sendSMS();
        }
        else { // start a countdown and send the sms once an interval has passed
            startSmsSendCountdown(sendIntervals.peek());
        }

        int progressPercentage = (durationInSeconds - secondsRemaining) * 100 / durationInSeconds;

        if (sendListener != null) {
            sendListener.onSmsProgressUpdate(currentSmsIndex, progressPercentage);
        }
    }

    private void startSmsSendCountdown(final int currentInterval) {
        countDownTimer = new CountDownTimer(currentInterval * 1000, 200) {
            private int lastProgressPercentage = -1;

            @Override
            public void onTick(long millisUntilFinished) {

                int remainingCount = secondsRemaining -
                        (currentInterval - (int) (millisUntilFinished / 1000));

                int progressPercentage = (durationInSeconds - remainingCount) * 100 / durationInSeconds;

                if (lastProgressPercentage != progressPercentage && progressPercentage > lastProgressPercentage) {

                    if (sendListener != null) {
                        sendListener.onSmsProgressUpdate(progressPercentage);
                    }
                    lastProgressPercentage = progressPercentage;
                }
            }

            @Override
            public void onFinish() {
                if (!sendIntervals.isEmpty()) {
                    secondsRemaining -= sendIntervals.pop();
                }
                sendSMS();
            }
        };
        countDownTimer.start();
    }

    /**
     * Sends off a queued SMS to VAPP, callback is delivered via PendingIntents in the service.
     */
    private void sendSMS() {
        try {
            currentSmsMessage = Vapp.generateSmsForProduct(context, totalSMSCount, currentSmsIndex);
            String message = currentSmsMessage.toString();
            Log.d(Vapp.TAG, "Send SMS to " + currentSmsMessage.getDeliveryNumber() + ": " + message);

            if (testMode) { // mock sending of sms and proceed to next
                if (sendListener != null) {
                    sendListener.onSmsDeliverySuccess();
                }
            }
            else {
                SmsManager sms = SmsManager.getDefault();
                sms.sendTextMessage(currentSmsMessage.getDeliveryNumber(),
                        null,           // Call center number.
                        message,
                        sentPI,
                        deliveredPI);
            }
        } catch (Exception e) {
            if (sendListener != null) {
                sendListener.onSmsSendError(e.getMessage());
            }
        }
    }

    /**
     * Notify the manager that an SMS has been successfully delivered, and that it should update
     * the DB records and check whether the purchase has been completed.
     */
    void notifySmsDelivered() {
        vappDbHelper.insertSmsLogDbRecord(currentSmsMessage);

        // The SMS has been delivered so move onto the next one (if
        // we have not reached the end).
        currentSmsIndex++; // Move onto the next message...
        VappConfiguration.setSentSmsCountForProduct(context, currentProduct, currentSmsIndex);

        if (hasFinishedPurchase()) {
            completeSmsPurchase();
        }
    }

    /**
     * Cancels the sending of the current message, if {@link SmsSendManager#addNextSmsToSendQueue()}
     * has been called.
     */
    void cancel() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    /**
     * Closes the DB connection
     */
    void destroy() {
        Service service = serviceRef.get();

        if (service != null && smsSentReceiver != null) {
            service.unregisterReceiver(smsSentReceiver);
            smsSentReceiver = null;
        }
        if (service != null && smsDeliveredReceiver != null) {
            service.unregisterReceiver(smsDeliveredReceiver);
            smsDeliveredReceiver = null;
        }
    }

    boolean isFirstSmsInPurchase() {
        return currentSmsIndex == 0;
    }

    boolean isFirstInSequence() {
        return isFirstInSequence;
    }

    boolean hasFinishedPurchase() {
        return !(currentSmsIndex < currentProduct.getRequiredSmsCount());
    }

    void setupReceivers() {
        smsSentReceiver = new SmsSentReceiver();
        smsDeliveredReceiver = new SmsDeliveredReceiver();
        Service service = serviceRef.get();

        if (service != null) {
            service.registerReceiver(smsSentReceiver, new IntentFilter(INTENT_SMS_SENT));
            service.registerReceiver(smsDeliveredReceiver, new IntentFilter(INTENT_SMS_DELIVERED));
        }
    }

    VappSms getCurrentSmsMessage() {
        return currentSmsMessage;
    }

    private void completeSmsPurchase() {
        // All SMSs have been sent for the current product, update the redeemed count...
        Vapp.addRedeemedProduct(context, currentProduct);

        if (sendListener != null) {
            sendListener.onSmsPurchaseCompleted();
        }
    }

    private class SmsSentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int resultCode = getResultCode();
            Integer errorResId = null;

            switch (resultCode) {
                case Activity.RESULT_OK:
                    // Nothing to do - only marking off the SMS when we know it's been
                    // Delivered
                    break;
                case RESULT_ERROR_GENERIC_FAILURE:
                    errorResId = R.string.vapp_sms_sent_failure_generic;
                    break;
                case RESULT_ERROR_NO_SERVICE:
                    errorResId = R.string.vapp_sms_sent_failure_no_service;
                    break;
                case RESULT_ERROR_NULL_PDU:
                    errorResId = R.string.vapp_sms_sent_failure_null_pdu;
                    break;
                case RESULT_ERROR_RADIO_OFF:
                    errorResId = R.string.vapp_sms_sent_failure_radio_off;
                    break;
                default:
                    Log.d(Vapp.TAG, "Unexpected sent code " + resultCode);
                    errorResId = R.string.vapp_sms_unexpected_err;
                    break;
            }
            if (sendListener != null) {
                sendListener.onSmsSendComplete(errorResId);
            }
        }
    }

    private class SmsDeliveredReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int resultCode = getResultCode();
            Integer errorResId = null;

            switch (resultCode) {
                case Activity.RESULT_OK:
                    if (sendListener != null) {
                        sendListener.onSmsDeliverySuccess();
                    }
                    break;
                case Activity.RESULT_CANCELED:
                    if (sendListener != null) {
                        sendListener.onSmsDeliveryFailure();
                    }
                    break;


                case RESULT_ERROR_GENERIC_FAILURE:
                    errorResId = R.string.vapp_sms_sent_failure_generic;
                    break;
                case RESULT_ERROR_NO_SERVICE:
                    errorResId = R.string.vapp_sms_sent_failure_no_service;
                    break;
                case RESULT_ERROR_NULL_PDU:
                    errorResId = R.string.vapp_sms_sent_failure_null_pdu;
                    break;
                case RESULT_ERROR_RADIO_OFF:
                    errorResId = R.string.vapp_sms_sent_failure_radio_off;
                    break;
                default:
                    Log.d(Vapp.TAG, "Unexpected delivery code " + resultCode);
                    errorResId = R.string.vapp_sms_unexpected_err;
                    break;
            }

            if (sendListener != null) {
                sendListener.onSmsSendComplete(errorResId);
            }
        }
    }

}

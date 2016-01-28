package com.vasilitate.vapp.sdk;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.CountDownTimer;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.Stack;

import static com.vasilitate.vapp.sdk.VappDbHelper.SmsEntry;

/**
 * Manages the sending of SMS messages at random intervals.
 */
class SmsSendManager {

    private String currentDeliveryNumber;
    private String currentSmsMessage;

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
    }

    private VappProduct currentProduct;

    private Stack<Integer> sendIntervals;
    private int secondsRemaining;
    private int durationInSeconds;
    private int totalSMSCount;
    private int currentSmsIndex;

    private CountDownTimer countDownTimer;
    private boolean testMode;

    private final Context context;
    private final SmsSendListener sendListener;

    private final PendingIntent sentPI;
    private final PendingIntent deliveredPI;

    private final SQLiteDatabase writableDatabase;

    SmsSendManager(VappProduct currentProduct, boolean testMode,
                   PendingIntent sentPI, PendingIntent deliveredPI,
                   Context context, SmsSendListener sendListener) {

        this.currentProduct = currentProduct;
        this.testMode = testMode;
        this.sentPI = sentPI;
        this.deliveredPI = deliveredPI;
        this.context = context.getApplicationContext();
        this.sendListener = sendListener;

        VappDbHelper vappDbHelper = new VappDbHelper(this.context);
        writableDatabase = vappDbHelper.getWritableDatabase();
        initialiseRandomSendIntervals();
    }

    void sendSMSForCurrentProduct() {
        // Ensure there are still SMSs to send.  If not (e.g. after a re-start?) mark the
        // current product as bought...
        if (sendIntervals.isEmpty()) {
            completeSmsPurchase();
            return;
        }

        int interval = 0;
        if (currentSmsIndex != 0) {
            interval = sendIntervals.peek();
        }

        final int currentInterval = interval;
        int progressPercentage = (durationInSeconds - secondsRemaining) * 100 / durationInSeconds;

        if (sendListener != null) {
            sendListener.onSmsProgressUpdate(currentSmsIndex, progressPercentage);
        }

        if (testMode) {
            Log.d("VAPP!", String.format("Sending Mock SMS %d/%d",
                    currentSmsIndex, currentProduct.getRequiredSmsCount()));
        }

        if (interval == 0) {
            sendSMS();
        }
        else {
            countDownTimer = new CountDownTimer(currentInterval * 1000, 200) {
                private int lastProgressPercentage = -1;

                @Override
                public void onTick(long millisUntilFinished) {

                    int remainingCount = secondsRemaining -
                            (currentInterval - (int) (millisUntilFinished / 1000));

                    int progressPercentage = (durationInSeconds - remainingCount) * 100 / durationInSeconds;

                    if (lastProgressPercentage != progressPercentage) {

                        if (sendListener != null) {
                            sendListener.onSmsProgressUpdate(progressPercentage);
                        }
                        lastProgressPercentage = progressPercentage;
                    }
                }

                @Override
                public void onFinish() {
                    secondsRemaining -= sendIntervals.pop();
                    sendSMS();
                }
            };
            countDownTimer.start();
        }
    }

    void notifySmsDelivered() {
        insertSmsLogDbRecord();

        // The SMS has been delivered so move onto the next one (if
        // we have not reached the end).
        currentSmsIndex++; // Move onto the next message...

        if (!sendIntervals.isEmpty()) { // Store the progress...
            VappConfiguration.setSentSmsCountForProduct(context, currentProduct, currentSmsIndex);
        }
        else {
            completeSmsPurchase();
        }
    }

    private void insertSmsLogDbRecord() {
        ContentValues values = new ContentValues();
        values.put(SmsEntry.COLUMN_NAME_MESSAGE, currentSmsMessage);
        values.put(SmsEntry.COLUMN_NAME_DDI, currentDeliveryNumber);
        writableDatabase.insert(SmsEntry.TABLE_NAME, null, values);
    }

    private void completeSmsPurchase() {
        // All SMSs have been sent for the current product, update the redeemed count...
        Vapp.addRedeemedProduct(context, currentProduct);

        if (sendListener != null) { // Send a final progress update showing completion.
            sendListener.onSmsProgressUpdate(currentProduct.getRequiredSmsCount(), 100);
        }
    }

    void cancel() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    void destroy() {
        if (writableDatabase != null) {
            writableDatabase.close();
        }
    }

    int getCurrentSmsIndex() {
        return currentSmsIndex;
    }

    boolean hasFinished() {
        return !(currentSmsIndex < currentProduct.getRequiredSmsCount());
    }

    /**
     * Sends off a queued SMS to VAPP, callback is delivered via PendingIntents in the service.
     */
    private void sendSMS() {
        try {
            currentDeliveryNumber = VappProductManager.getRandomNumberInRange(Vapp.getDestinationNumberRange());
            currentSmsMessage = Vapp.getGeneratedSmsForProduct(context, currentProduct, totalSMSCount, currentSmsIndex);

            if (testMode) { // mock sending of sms and proceed to next
                Log.d("Vapp!", "Test SMS: " + currentDeliveryNumber + ": " + currentSmsMessage);
                notifySmsDelivered();
                sendSMSForCurrentProduct();
            }
            else {
                SmsManager sms = SmsManager.getDefault();
                sms.sendTextMessage(currentDeliveryNumber,
                        null,           // Call center number.
                        currentSmsMessage,
                        sentPI,
                        deliveredPI);
            }
        }
        catch (Exception e) {
            if (sendListener != null) {
                sendListener.onSmsSendError(e.getMessage());
            }
        }
    }

    /**
     * Create random ms intervals to wait between sending messages
     */
    private void initialiseRandomSendIntervals() {
        totalSMSCount = VappConfiguration.getCurrentDownloadSmsCountForProduct(context, currentProduct);
        int sentCount = VappConfiguration.getSentSmsCountForProduct(context, currentProduct);
        currentSmsIndex = Vapp.getSMSPaymentProgress(context, currentProduct);

        sendIntervals = new Stack<>();
        secondsRemaining = 0;

        for (int i = 0; i < totalSMSCount - sentCount - 1; i++) {
            int interval = VappProductManager.generateSMSSendInterval(context);
            sendIntervals.push(interval);
            secondsRemaining += interval;
        }
        durationInSeconds = secondsRemaining;
    }
}

package com.vasilitate.vapp.sdk;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.vasilitate.vapp.R;
import com.vasilitate.vapp.sdk.network.RemoteNetworkTaskListener;
import com.vasilitate.vapp.sdk.network.VappRestClient;
import com.vasilitate.vapp.sdk.network.response.BaseResponse;
import com.vasilitate.vapp.sdk.network.response.GetHniStatusResponse;
import com.vasilitate.vapp.sdk.network.response.GetReceivedStatusResponse;
import com.vasilitate.vapp.sdk.network.response.PostLogsResponse;

import static android.telephony.SmsManager.RESULT_ERROR_GENERIC_FAILURE;
import static android.telephony.SmsManager.RESULT_ERROR_NO_SERVICE;
import static android.telephony.SmsManager.RESULT_ERROR_NULL_PDU;
import static android.telephony.SmsManager.RESULT_ERROR_RADIO_OFF;
import static com.vasilitate.vapp.sdk.VappActions.ACTION_SMS_PROGRESS;
import static com.vasilitate.vapp.sdk.VappActions.EXTRA_ERROR_MESSAGE;
import static com.vasilitate.vapp.sdk.VappActions.EXTRA_PRODUCT_ID;
import static com.vasilitate.vapp.sdk.VappActions.EXTRA_PROGRESS_PERCENTAGE;
import static com.vasilitate.vapp.sdk.VappActions.EXTRA_SMS_CANCELLED;
import static com.vasilitate.vapp.sdk.VappActions.EXTRA_SMS_COMPLETED;
import static com.vasilitate.vapp.sdk.VappActions.EXTRA_SMS_PURCHASE_NO_CONNECTION;
import static com.vasilitate.vapp.sdk.VappActions.EXTRA_SMS_PURCHASE_UNSUPPORTED;
import static com.vasilitate.vapp.sdk.VappActions.EXTRA_SMS_SENT_COUNT;

/**
 * Handles the sending of SMSs in the background
 */
public class VappSmsService extends Service implements SmsSendManager.SmsSendListener {

    /**
     * The number of milliseconds delay between receiving an SMS success callback & updating the server log
     */
    public static final long POST_LOG_DELAY = 5000;

    /**
     * The number of milliseconds delay when a 'Not Yet' response is returned from the server
     */
    public static final long NOT_YET_DELAY = 10000;

    private static final String INTENT_SMS_SENT = "com.vasilitate.vapp.sdk.SMS_SENT";
    private static final String INTENT_SMS_DELIVERED = "com.vasilitate.vapp.sdk.INTENT_SMS_DELIVERED";
    static final String INTENT_CANCEL_PAYMENT = "com.vasilitate.vapp.sdk.INTENT_CANCEL_PAYMENT";

    private static final int SENT_SMS_REQUEST_CODE = 100;
    private static final int DELIVERED_SMS_REQUEST_CODE = 101;

    private VappProduct currentProduct;
    private boolean testMode;

    private SmsSentReceiver smsSentReceiver;
    private SmsDeliveredReceiver smsDeliveredReceiver;
    private CancelPaymentReceiver cancelPaymentReceiver;

    private final Handler completionHandler = new Handler();
    private final Handler receivedStatusHandler = new Handler();

    private final VappRestClient restClient;
    private final VappDbHelper vappDbHelper;
    private SmsSendManager smsSendManager;
    private SmsApiCheckManager smsApiCheckManager;

    public VappSmsService() {
        vappDbHelper = new VappDbHelper(this);
        restClient = new VappRestClient(getString(R.string.api_endpoint), VappConfiguration.getSdkKey(this));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        testMode = VappConfiguration.isTestMode(this);
        setupReceivers();
        handleStartCommand(intent);
        return START_STICKY;
    }

    @Nullable @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void setupReceivers() {
        smsSentReceiver = new SmsSentReceiver();
        smsDeliveredReceiver = new SmsDeliveredReceiver();
        cancelPaymentReceiver = new CancelPaymentReceiver();

        registerReceiver(smsSentReceiver, new IntentFilter(INTENT_SMS_SENT));
        registerReceiver(smsDeliveredReceiver, new IntentFilter(INTENT_SMS_DELIVERED));
        registerReceiver(cancelPaymentReceiver, new IntentFilter(INTENT_CANCEL_PAYMENT));
    }

    @Override
    public void onDestroy() {
        if (smsSentReceiver != null) {
            unregisterReceiver(smsSentReceiver);
            smsSentReceiver = null;
        }
        if (smsDeliveredReceiver != null) {
            unregisterReceiver(smsDeliveredReceiver);
            smsDeliveredReceiver = null;
        }
        if (cancelPaymentReceiver != null) {
            unregisterReceiver(cancelPaymentReceiver);
        }
        if (smsSendManager != null) {
            smsSendManager.destroy();
        }
        super.onDestroy();
    }

    private void terminateService() {
        broadcastSMSsCompleted();
        stopSelf();
    }

    /**
     * Handles the start command intent, and kicks off SMS sending (or terminates the service if
     * parameters are invalid)
     *
     * @param intent the start command intent
     */
    private void handleStartCommand(Intent intent) {
        String productId = intent.getStringExtra(EXTRA_PRODUCT_ID);
        currentProduct = Vapp.getProduct(productId);

        if (currentProduct == null) { // Unrecognised Product!
            terminateService();
        }
        else {
            VappConfiguration.setProductCancelled(getApplicationContext(), productId, false);

            PendingIntent sentPI = PendingIntent.getBroadcast(this, SENT_SMS_REQUEST_CODE,
                    new Intent(INTENT_SMS_DELIVERED), 0);
            PendingIntent deliveredPI = PendingIntent.getBroadcast(this, DELIVERED_SMS_REQUEST_CODE,
                    new Intent(INTENT_SMS_SENT), 0);

            smsSendManager = new SmsSendManager(currentProduct, testMode, sentPI, deliveredPI, this, this);
            setupApiCheckManager();

            if (smsSendManager.getCurrentSmsIndex() == 0) {
                smsApiCheckManager.performHniStatusCheck(); // perform MCC/MNC check prior to sending very first SMS
            }
            else {
                smsSendManager.sendSMSForCurrentProduct();
            }
        }
    }

    private void setupApiCheckManager() { // handles successful responses from API
        smsApiCheckManager = new SmsApiCheckManager(restClient, this,

                new ResponseHandler<GetHniStatusResponse>() {
                    @Override public void onRequestSuccess(GetHniStatusResponse result) {
                        if (BaseResponse.HNI_STATUS_WHITELISTED.equals(result.getStatus())) {
                            smsSendManager.sendSMSForCurrentProduct(); // send initial first sms!
                        }
                        else { // blacklisted!
                            handlePurchaseUnsupported();
                        }
                    }
                },
                new ResponseHandler<GetReceivedStatusResponse>() {
                    @Override public void onRequestSuccess(GetReceivedStatusResponse result) {
                        if (GetReceivedStatusResponse.RECEIVED_STATUS_YES.equals(result.getStatus())) {
                            smsSendManager.sendSMSForCurrentProduct(); // all ok, send any remaining texts
                        }
                        else if (GetReceivedStatusResponse.RECEIVED_STATUS_NOT_YET.equals(result.getStatus())) {
                            receivedStatusHandler.removeCallbacks(retryReceivedStatusCheck); // retry in 10s interval
                            receivedStatusHandler.postDelayed(retryReceivedStatusCheck, NOT_YET_DELAY);
                        }
                        else {
                            handlePurchaseUnsupported();
                        }
                    }
                },
                new ResponseHandler<PostLogsResponse>() {
                    @Override public void onRequestSuccess(PostLogsResponse result) {
                        vappDbHelper.clearSentSmsLogs();

                        if (smsSendManager.getCurrentSmsIndex() >= 1 && !smsSendManager.hasFinished()) {
                            // check that the server received delivery notification from telco
                            receivedStatusHandler.removeCallbacks(retryReceivedStatusCheck);
                            receivedStatusHandler.postDelayed(retryReceivedStatusCheck, POST_LOG_DELAY);
                        }
                        else { // completed purchase
                            // TODO handle finish
                        }
                    }
                });
    }

    // SmsSendListener
    @Override public void onSmsProgressUpdate(int progressPercentage) {
        broadcastProgressPercentage(progressPercentage);
    }

    @Override
    public void onSmsProgressUpdate(int currentSmsIndex, int progressPercentage) {
        broadcastProgress(currentSmsIndex, progressPercentage);

        if (progressPercentage == 100) { // send log of all sent sms to server
            smsApiCheckManager.performPostLogsCall(vappDbHelper.retrieveSentSmsLogs());

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


    @Override public void onSmsSendError(String message) {
        broadcastSMSError(message);
    }

    /**
     * Handle broadcasts
     **/

    private void broadcastProgress(int smsSentCount, int progressPercentage) {
        Intent intent = new Intent(ACTION_SMS_PROGRESS);
        intent.putExtra(EXTRA_SMS_SENT_COUNT, smsSentCount);
        intent.putExtra(EXTRA_PROGRESS_PERCENTAGE, progressPercentage);
        sendBroadcast(intent);
    }

    private void broadcastProgressPercentage(int progressPercentage) {
        Intent intent = new Intent(ACTION_SMS_PROGRESS);
        intent.putExtra(EXTRA_PROGRESS_PERCENTAGE, progressPercentage);
        sendBroadcast(intent);
    }

    private void broadcastSMSError(String error) {
        Intent intent = new Intent(ACTION_SMS_PROGRESS);
        intent.putExtra(EXTRA_ERROR_MESSAGE, error);
        sendBroadcast(intent);
    }

    private void broadcastSMSCancelled(String productId) {
        Intent intent = new Intent(ACTION_SMS_PROGRESS);
        intent.putExtra(EXTRA_SMS_CANCELLED, true);
        intent.putExtra(EXTRA_PRODUCT_ID, productId);
        sendBroadcast(intent);
    }

    private void broadcastSMSsCompleted() {
        Intent intent = new Intent(ACTION_SMS_PROGRESS);
        intent.putExtra(EXTRA_SMS_COMPLETED, true);
        sendBroadcast(intent);
        VappNotificationManager.post(VappSmsService.this);
    }

    private void handleNoConnectionAvailable() { // inform that no connection available, stop service
        Intent intent = new Intent(ACTION_SMS_PROGRESS);
        intent.putExtra(EXTRA_SMS_PURCHASE_NO_CONNECTION, true);
        sendBroadcast(intent);
        cancelPayment(this);
    }

    private void handlePurchaseUnsupported() { // inform that purchase not supported, stop service
        Intent intent = new Intent(ACTION_SMS_PROGRESS);
        intent.putExtra(EXTRA_SMS_PURCHASE_UNSUPPORTED, true);
        sendBroadcast(intent);
        cancelPayment(this);
    }

    private class CancelPaymentReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            Log.d("Vapp", "Cancelling payment");
            cancelPayment(context);
        }
    }

    private void cancelPayment(Context context) {
        if (smsSendManager != null) {
            smsSendManager.cancel();
        }
        if (currentProduct != null) {
            String productId = currentProduct.getProductId();
            VappConfiguration.setProductCancelled(context, productId, true);
            broadcastSMSCancelled(productId);
            Toast.makeText(context, R.string.cancelled_product_purchase, Toast.LENGTH_LONG).show();
            currentProduct = null;
            stopSelf();
        }
    }

    private class SmsSentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            Integer errorResId = null;
            switch (getResultCode()) {
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
                    handleSmsDeliverySuccess();
                    break;
                case Activity.RESULT_CANCELED:
                    broadcastSMSError(getString(R.string.vapp_sms_delivery_failure));
                    break;
            }
        }

        private void handleSmsDeliverySuccess() {
            int smsIndex = smsSendManager.getCurrentSmsIndex();
            smsSendManager.notifySmsDelivered();

            if (smsIndex == 0) {
                // should check that the server received delivery notification from telco
                smsApiCheckManager.performPostLogsCall(vappDbHelper.retrieveSentSmsLogs());
            }
            else {
                smsSendManager.sendSMSForCurrentProduct();
            }
        }
    }

    private final Runnable retryReceivedStatusCheck = new Runnable() {
        @Override public void run() {
            smsApiCheckManager.performReceivedStatusCheck();
        }
    };

    private abstract class ResponseHandler<T> implements RemoteNetworkTaskListener<T> {
        @Override public void onRequestFailure() {
            handleNoConnectionAvailable();
        }
    }

}
package com.vasilitate.vapp.sdk;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.vasilitate.vapp.R;
import com.vasilitate.vapp.sdk.network.RemoteNetworkTaskListener;
import com.vasilitate.vapp.sdk.network.VappRestClient;
import com.vasilitate.vapp.sdk.network.request.PostLogsBody;
import com.vasilitate.vapp.sdk.network.request.PostLogsRequestTask;
import com.vasilitate.vapp.sdk.network.response.GetHniStatusResponse;
import com.vasilitate.vapp.sdk.network.response.GetReceivedStatusResponse;
import com.vasilitate.vapp.sdk.network.response.PostLogsResponse;

import java.util.Date;
import java.util.List;

import static com.vasilitate.vapp.sdk.VappActions.ACTION_SMS_PROGRESS;
import static com.vasilitate.vapp.sdk.VappActions.EXTRA_ERROR_MESSAGE;
import static com.vasilitate.vapp.sdk.VappActions.EXTRA_PRODUCT_ID;
import static com.vasilitate.vapp.sdk.VappActions.EXTRA_PROGRESS_PERCENTAGE;
import static com.vasilitate.vapp.sdk.VappActions.EXTRA_SMS_CANCELLED;
import static com.vasilitate.vapp.sdk.VappActions.EXTRA_SMS_COMPLETED;
import static com.vasilitate.vapp.sdk.VappActions.EXTRA_SMS_PURCHASE_NO_CONNECTION;
import static com.vasilitate.vapp.sdk.VappActions.EXTRA_SMS_PURCHASE_UNSUPPORTED;
import static com.vasilitate.vapp.sdk.VappActions.EXTRA_SMS_SENT_COUNT;
import static com.vasilitate.vapp.sdk.network.response.BaseResponse.HNI_STATUS_WHITELISTED;
import static com.vasilitate.vapp.sdk.network.response.GetReceivedStatusResponse.RECEIVED_STATUS_NOT_YET;
import static com.vasilitate.vapp.sdk.network.response.GetReceivedStatusResponse.RECEIVED_STATUS_YES;

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

    private static final int SENT_SMS_REQUEST_CODE = 0x64;
    private static final int DELIVERED_SMS_REQUEST_CODE = 0x65;

    private VappProduct currentProduct;
    private boolean testMode;

    private CancelPaymentReceiver cancelPaymentReceiver;

    private final Handler completionHandler = new Handler();
    private final Handler receivedStatusHandler = new Handler();

    private VappRestClient restClient;
    private VappDbHelper vappDbHelper;
    private SmsSendManager smsSendManager;
    private SmsApiCheckManager smsApiCheckManager;
    private PostLogsRequestTask postHistoricalLogsTask;
    private boolean shouldCheckReceivedStatus = true;

    private Intent originatingIntent;

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {

        originatingIntent = intent;
        testMode = VappConfiguration.isTestMode(this);
        vappDbHelper = new VappDbHelper(this);
        restClient = new VappRestClient(getString(R.string.api_endpoint), VappConfiguration.getSdkKey(this), testMode);

        // check if an existing product has not been logged to the server yet

        if (vappDbHelper.retrieveSentSmsLogs().isEmpty()) {
            handleStartCommand(intent);
        }
        else {
            Log.d(Vapp.TAG, "Attempting upload of locally stored sent messages!");

            if (postHistoricalLogsTask != null && postHistoricalLogsTask.getStatus() == AsyncTask.Status.RUNNING) {
                postHistoricalLogsTask.cancel(true);
            }

            postHistoricalLogsTask = new PostLogsRequestTask(restClient, retrieveSmsLogs());
            postHistoricalLogsTask.setRequestListener(new ResponseHandler<PostLogsResponse>() {
                @Override public void onRequestSuccess(PostLogsResponse result) {
                    vappDbHelper.clearSentSmsLogs();
                    handleStartCommand(intent);
                }
            });
            postHistoricalLogsTask.execute();
        }
        return START_STICKY;
    }

    @Nullable @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void setupReceivers() {
        smsSendManager.setupReceivers();
        cancelPaymentReceiver = new CancelPaymentReceiver();
        registerReceiver(cancelPaymentReceiver, new IntentFilter(INTENT_CANCEL_PAYMENT));
    }

    @Override
    public void onDestroy() {
        if (smsSendManager != null) {
            smsSendManager.destroy();
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
        SubscriptionAlarmReceiver.completeWakefulIntent(originatingIntent);
        stopSelf();
    }

    /**
     * Handles the start command intent, and kicks off SMS sending (or terminates the service if
     * parameters are invalid)
     *
     * @param intent the start command intent
     */
    private void handleStartCommand(Intent intent) {

        if( intent == null ) {
            Log.d(Vapp.TAG, "Null intent passed to VappSmsService");
            return;
        }

        String productId = intent.getStringExtra(EXTRA_PRODUCT_ID);
        currentProduct = Vapp.getProduct(productId);

        if (currentProduct == null) {
            Log.d(Vapp.TAG, "Unrecognised purchase, terminating service!");
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
            setupReceivers();

            if (smsSendManager.isFirstInSequence()) {
                Log.d(Vapp.TAG, "Performing Backend HNI status check");
                smsApiCheckManager.performHniStatusCheck(); // perform MCC/MNC check prior to sending very first SMS
            }
            else {
                Log.d(Vapp.TAG, "Initiate SMS purchase");
                smsSendManager.addNextSmsToSendQueue();
            }
        }
    }


    /*
     * Handle Api Logic
     */
    private void setupApiCheckManager() { // handles successful responses from API
        smsApiCheckManager = new SmsApiCheckManager(restClient, this,

                new ResponseHandler<GetHniStatusResponse>() {
                    @Override public void onRequestSuccess(GetHniStatusResponse result) {

                        if (HNI_STATUS_WHITELISTED.equals(result.getStatus())) {
                            Log.d(Vapp.TAG, "Backend HNI status check OK, start sending SMS");
                            smsSendManager.addNextSmsToSendQueue(); // send initial sms!
                        }
                        else { // blacklisted!
                            Log.d(Vapp.TAG, "Backend HNI status check: " + result.getStatus());
                            handlePurchaseUnsupported();
                        }
                    }
                },
                new ResponseHandler<GetReceivedStatusResponse>() {
                    @Override public void onRequestSuccess(GetReceivedStatusResponse result) {

                        if (RECEIVED_STATUS_YES.equals(result.getReceived())) {
                            shouldCheckReceivedStatus = false;
                            Log.d(Vapp.TAG, "Test SMS received OK, proceed");
                            smsSendManager.addNextSmsToSendQueue(); // all ok, send any remaining texts
                        }
                        else if (RECEIVED_STATUS_NOT_YET.equals(result.getReceived())) {
                            Log.d(Vapp.TAG, "Test SMS not yet received, retry");

//                            smsSendManager.addNextSmsToSendQueue(); // TEST code, uncomment when needed
                            receivedStatusHandler.removeCallbacks(retryReceivedStatusCheck); // retry in 10s interval
                            receivedStatusHandler.postDelayed(retryReceivedStatusCheck, NOT_YET_DELAY);
                        }
                        else {
                            Log.d(Vapp.TAG, "Test SMS operator was blacklisted, cancelling");
                            handlePurchaseUnsupported();
                        }
                    }
                },
                new ResponseHandler<PostLogsResponse>() {
                    @Override public void onRequestSuccess(PostLogsResponse result) {
                        vappDbHelper.clearSentSmsLogs();

                        if (shouldCheckReceivedStatus) {
                            Log.d(Vapp.TAG, "Check Backend Delivery notification");
                            receivedStatusHandler.removeCallbacks(retryReceivedStatusCheck);
                            receivedStatusHandler.postDelayed(retryReceivedStatusCheck, POST_LOG_DELAY);
                        }
                        else if (smsSendManager.hasFinishedPurchase()) { // completed purchase!!!
                            Log.d(Vapp.TAG, "Completed SMS purchase!");

                            // Delay the sending of the completion so that any clients can display
                            // the completion of the purchase.
                            completionHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {

                                    //If a subscription product set it's new end date...
                                    if( currentProduct.isSubscriptionProduct() ) {

                                        Date subscriptionEndDate =
                                                currentProduct.getNextSubscriptionEndDate( new Date());
                                        VappConfiguration.setSubscriptionEndDate(
                                                VappSmsService.this, currentProduct, subscriptionEndDate );

                                        Log.d( Vapp.TAG, "New subscription end date: " +
                                                currentProduct.getProductId() + " - " +
                                                subscriptionEndDate.toString());
                                    }

                                    terminateService();
                                }
                            }, 2000);
                        }
                        else {
                            smsSendManager.addNextSmsToSendQueue();
                        }
                    }
                });
    }


    /*
     * Handle Sms Logic
     */


    @Override public void onSmsProgressUpdate(int progressPercentage) {
        broadcastProgressPercentage(progressPercentage);
    }

    @Override
    public void onSmsProgressUpdate(int currentSmsIndex, int progressPercentage) {
        broadcastProgress(currentSmsIndex, progressPercentage);
    }

    @Override public void onSmsPurchaseCompleted() {
        smsApiCheckManager.performPostLogsCall(retrieveSmsLogs()); // send log of all sent sms to server
    }

    @Override public void onSmsSendError(String message) {
        broadcastSMSError(message);
    }

    @Override public void onSmsDeliverySuccess() {
        smsSendManager.notifySmsDelivered();

        if (shouldCheckReceivedStatus) { // should check that the server received delivery notification from telco
            smsApiCheckManager.performPostLogsCall(retrieveSmsLogs());
        }
        else {
            smsSendManager.addNextSmsToSendQueue();
        }
    }

    @Override public void onSmsDeliveryFailure() {
        broadcastSMSError(getString(R.string.vapp_sms_delivery_failure));
    }

    @Override public void onSmsSendComplete(Integer errorResId) {
        if (errorResId != null) {
            broadcastSMSError(getString(errorResId));
        }
    }

    private PostLogsBody retrieveSmsLogs() {
        List<PostLogsBody.LogEntry> entryList = vappDbHelper.retrieveSentSmsLogs();
        String cli = Vapp.getUserPhoneNumber(this);
        String cliDetail = Vapp.getOriginatingNetworkName(this);
        return new PostLogsBody(entryList, cli, cliDetail);
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
        Log.d(Vapp.TAG, "Sms error: " + error);
        Intent intent = new Intent(ACTION_SMS_PROGRESS);
        intent.putExtra(EXTRA_ERROR_MESSAGE, error);
        sendBroadcast(intent);
    }

    private void broadcastSMSCancelled(String productId) {
        Log.d(Vapp.TAG, "Cancelled Sms purchase!");
        Intent intent = new Intent(ACTION_SMS_PROGRESS);
        intent.putExtra(EXTRA_SMS_CANCELLED, true);
        intent.putExtra(EXTRA_PRODUCT_ID, productId);
        sendBroadcast(intent);
    }

    private void broadcastSMSsCompleted() {
        Log.d(Vapp.TAG, "Broadcast SMS  purchase completed!");
        Intent intent = new Intent(ACTION_SMS_PROGRESS);
        intent.putExtra(EXTRA_SMS_COMPLETED, true);
        sendBroadcast(intent);
        VappNotificationManager.post(VappSmsService.this);
    }

    private void handleNoConnectionAvailable() { // inform that no connection available, stop service
        Log.d(Vapp.TAG, "No connection available, suspending payment");
        Intent intent = new Intent(ACTION_SMS_PROGRESS);
        intent.putExtra(EXTRA_SMS_PURCHASE_NO_CONNECTION, true);
        sendBroadcast(intent);
        endPayment(this, false);
    }

    private void handlePurchaseUnsupported() { // inform that purchase not supported, stop service
        Log.d(Vapp.TAG, "Payment unsupported, cancelling");
        Intent intent = new Intent(ACTION_SMS_PROGRESS);
        intent.putExtra(EXTRA_SMS_PURCHASE_UNSUPPORTED, true);
        sendBroadcast(intent);
        endPayment(this, false);
    }


    private class CancelPaymentReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            Log.d(Vapp.TAG, "Cancelling payment");
            endPayment(context, true);
        }
    }

    private void endPayment(Context context, boolean userCancelled) { // TODO test if finishes!
        receivedStatusHandler.removeCallbacks(retryReceivedStatusCheck);

        if (smsSendManager != null) {
            smsSendManager.cancel();
        }
        if (currentProduct != null) {
            String productId = currentProduct.getProductId();
            VappConfiguration.setProductCancelled(context, productId, true);

            if (userCancelled) {
                broadcastSMSCancelled(productId);
            }

            Toast.makeText(context, R.string.cancelled_product_purchase, Toast.LENGTH_LONG).show();
            currentProduct = null;
            stopSelf();
        }
    }

    private final Runnable retryReceivedStatusCheck = new Runnable() {
        @Override public void run() {
            smsApiCheckManager.performReceivedStatusCheck(smsSendManager.getCurrentSmsMessage());
        }
    };

    private abstract class ResponseHandler<T> implements RemoteNetworkTaskListener<T> {
        @Override public void onRequestFailure() {
            handleNoConnectionAvailable();
        }
    }

}
package com.vasilitate.vapp.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;

import static com.vasilitate.vapp.sdk.VappActions.ACTION_SMS_PROGRESS;
import static com.vasilitate.vapp.sdk.VappActions.EXTRA_ERROR_MESSAGE;
import static com.vasilitate.vapp.sdk.VappActions.EXTRA_PROGRESS_PERCENTAGE;
import static com.vasilitate.vapp.sdk.VappActions.EXTRA_SMS_CANCELLED;
import static com.vasilitate.vapp.sdk.VappActions.EXTRA_SMS_COMPLETED;
import static com.vasilitate.vapp.sdk.VappActions.EXTRA_SMS_PURCHASE_NO_CONNECTION;
import static com.vasilitate.vapp.sdk.VappActions.EXTRA_SMS_PURCHASE_UNSUPPORTED;
import static com.vasilitate.vapp.sdk.VappActions.EXTRA_SMS_SENT_COUNT;

/**
 * Listens for updates on a product purchase, using a Broadcast Receiver. Implementations should
 * forward onCreate() and onDestroy() lifecycle methods to this class from the Activity.
 */
public class VappProgressReceiver {

    private final Context context;
    private VappProgressListener progressListener;

    private SmsProgressReceiver smsProgressReceiver;

    /**
     * Creates a new instance of a progress receiver
     *
     * @param context          the current context
     * @param progressListener an implementation of a ProgressListener which responds to updates in purchase state
     */
    public VappProgressReceiver(Context context,
                                VappProgressListener progressListener) {

        this.context = context.getApplicationContext();
        this.progressListener = progressListener;
    }

    /**
     * This method should be called during Activity#onCreate(), and registers a Broadcast receiver.
     */
    public void onCreate() {
        smsProgressReceiver = new SmsProgressReceiver();
        context.registerReceiver(smsProgressReceiver,
                                 new IntentFilter(ACTION_SMS_PROGRESS));
    }

    /**
     * This method should be called during Activity#onDestroy(), and unregisters a Broadcast receiver.
     */
    public void onDestroy() {
        if (smsProgressReceiver != null) {
            context.unregisterReceiver(smsProgressReceiver);
        }
    }

    /**
     * Sets the progressListener
     *
     * @param progressListener an implementation of a ProgressListener which responds to updates in purchase state
     */
    public void setListener(VappProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    private class SmsProgressReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (progressListener != null) {

                boolean completed = intent.getBooleanExtra(EXTRA_SMS_COMPLETED, false);
                boolean cancelled = intent.getBooleanExtra(EXTRA_SMS_CANCELLED, false);
                boolean unsupported = intent.getBooleanExtra(EXTRA_SMS_PURCHASE_UNSUPPORTED, false);
                boolean noConnection = intent.getBooleanExtra(EXTRA_SMS_PURCHASE_NO_CONNECTION, false);

                if (completed) {
                    progressListener.onCompletion();
                }
                else if (cancelled) {
                    progressListener.onCancelled();
                }
                else if (noConnection) {
                    progressListener.onNetworkFailure();
                }
                else if (unsupported) {
                    progressListener.onPurchaseUnsupported();
                }
                else {
                    String error = intent.getStringExtra(EXTRA_ERROR_MESSAGE);

                    if (TextUtils.isEmpty(error)) {

                        int progressPercentage = intent.getIntExtra(EXTRA_PROGRESS_PERCENTAGE, 0);
                        if (intent.hasExtra(EXTRA_SMS_SENT_COUNT)) {
                            int progress = intent.getIntExtra(EXTRA_SMS_SENT_COUNT, -1);
                            progressListener.onSMSSent(progress, progressPercentage);
                        }
                        else {
                            progressListener.onProgressTick(progressPercentage);
                        }
                    }
                    else {
                        progressListener.onError(error);
                    }
                }
            }
        }
    }
}

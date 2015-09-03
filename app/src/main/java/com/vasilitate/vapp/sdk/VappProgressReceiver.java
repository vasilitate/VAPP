package com.vasilitate.vapp.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;

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

        this.context = context;
        this.progressListener = progressListener;
    }

    /**
     * This method should be called during Activity#onCreate(), and registers a Broadcast receiver.
     */
    public void onCreate() {
        smsProgressReceiver = new SmsProgressReceiver();
        context.registerReceiver(smsProgressReceiver,
                                 new IntentFilter(VappActions.ACTION_SMS_PROGRESS));
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

                boolean completed = intent.getBooleanExtra(VappActions.EXTRA_SMS_COMPLETED, false);

                if (completed) {
                    progressListener.onCompletion();
                }
                else {

                    String error = intent.getStringExtra(VappActions.EXTRA_ERROR_MESSAGE);

                    if (TextUtils.isEmpty(error)) {

                        int progressPercentage = intent.getIntExtra(VappActions.EXTRA_PROGRESS_PERCENTAGE, 0);
                        if (intent.hasExtra(VappActions.EXTRA_SMS_SENT_COUNT)) {
                            int progress = intent.getIntExtra(VappActions.EXTRA_SMS_SENT_COUNT, -1);
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

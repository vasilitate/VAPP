package com.vasilitate.vapp.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;

/**
 * Wrapper class for the progress receiver.
 */
public class VappProgressReceiver {

    private final Context context;
    private VappProgressListener progressListener;

    private SmsProgressReceiver smsProgressReceiver;

    public VappProgressReceiver( Context context,
                                 VappProgressListener progressListener ) {

        this.context = context;
        this.progressListener = progressListener;
    }
    
    public void onCreate() {
        smsProgressReceiver = new SmsProgressReceiver();
        context.registerReceiver(smsProgressReceiver,
                new IntentFilter(VappActions.ACTION_SMS_PROGRESS));
    }

    public void onDestroy() {
        if( smsProgressReceiver != null ) {
            context.unregisterReceiver( smsProgressReceiver );
        }
    }

    public void setListener( VappProgressListener progressListener ) {
        this.progressListener = progressListener;
    }

    private class SmsProgressReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if( progressListener != null ) {

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

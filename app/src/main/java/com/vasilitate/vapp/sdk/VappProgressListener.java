package com.vasilitate.vapp.sdk;

/**
 * Allows the progress of the sending of the SMSs to be listened to.
 */
public interface VappProgressListener {

    void onSMSSent(int smsSentCount, int progressPercentage);
    void onProgressTick(int countDown);
    void onError( String message );
    void onCompletion();
}

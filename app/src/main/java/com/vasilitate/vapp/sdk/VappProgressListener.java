package com.vasilitate.vapp.sdk;

/**
 * Provides methods which are called when the SMS send state for a purchase changes.
 */
public interface VappProgressListener {

    /**
     * Called when an SMS is successfully sent.
     *
     * @param smsSentCount       the count of sent SMS
     * @param progressPercentage the percentage progress
     */
    void onSMSSent(int smsSentCount, int progressPercentage);

    /**
     * Called when the progress percentage updates
     *
     * @param countDown the progress percentage
     */
    void onProgressTick(int countDown);

    /**
     * Called when an error occurs in a product purchase
     *
     * @param message error details
     */
    void onError(String message);

    /**
     * Called when a product purchase is fully completed.
     */
    void onCompletion();
}

package com.vasilitate.vapp.sdk.network;

/**
 * Defines callbacks which are called when a network task succeeds or fails.
 * @param <T> the response object
 */
public interface RemoteNetworkTaskListener<T> {

    /**
     * Called when a task completed successfully
     * @param result the response object
     */
    void onRequestSuccess(T result);

    /**
     * Called when a task completed with failure
     */
    void onRequestFailure();
    
}

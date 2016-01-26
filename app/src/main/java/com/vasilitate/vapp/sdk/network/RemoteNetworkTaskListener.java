package com.vasilitate.vapp.sdk.network;

public interface RemoteNetworkTaskListener<T> {
    void onRequestSuccess(T result);
    void onRequestFailure();
}

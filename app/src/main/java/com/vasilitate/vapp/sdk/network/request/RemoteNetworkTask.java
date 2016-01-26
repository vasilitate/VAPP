package com.vasilitate.vapp.sdk.network.request;

import android.os.AsyncTask;
import android.util.Log;

import com.vasilitate.vapp.sdk.network.RemoteNetworkTaskListener;
import com.vasilitate.vapp.sdk.network.VappRestClient;

import java.io.IOException;

/**
 * Performs network operations in a background thread, providing a callback on the main thread when
 * the task has completed.
 *
 * @param <T> the type of the response object
 */
public abstract class RemoteNetworkTask<T> extends AsyncTask<Void, Void, Void> {

    private T response;
    private boolean failure;
    protected final VappRestClient restClient;
    private RemoteNetworkTaskListener<T> requestListener;

    protected RemoteNetworkTask(VappRestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Sets a request listener that provides a callback on success/failure of the call
     *
     * @param requestListener the request listener
     */
    public void setRequestListener(RemoteNetworkTaskListener<T> requestListener) {
        this.requestListener = requestListener;
    }

    @Override protected Void doInBackground(Void... params) {
        try {
            response = performApiCall();
        }
        catch (IOException e) {
            failure = true;
            Log.d(getClass().getName(), "Error performing IO for network request, probably not connected", e);
        }
        return null;
    }

    /**
     * Performs a network call in this method. If a response object is serialised the success
     * listener will be called, otherwise failure will be called.
     *
     * @return the response object
     * @throws IOException
     */
    protected abstract T performApiCall() throws IOException;

    @Override protected void onPostExecute(Void aVoid) {
        if (requestListener != null) {
            if (response != null && !failure) {
                requestListener.onRequestSuccess(response);
            }
            else {
                requestListener.onRequestFailure();
            }
        }
    }

}
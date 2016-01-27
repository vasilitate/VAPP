package com.vasilitate.vapp.sdk;

import android.content.Context;
import android.os.AsyncTask;

import com.vasilitate.vapp.sdk.network.RemoteNetworkTaskListener;
import com.vasilitate.vapp.sdk.network.VappRestClient;
import com.vasilitate.vapp.sdk.network.request.GetHniStatusRequestTask;
import com.vasilitate.vapp.sdk.network.request.GetReceivedStatusRequestTask;
import com.vasilitate.vapp.sdk.network.request.PostLogsBody;
import com.vasilitate.vapp.sdk.network.request.PostLogsRequestTask;
import com.vasilitate.vapp.sdk.network.response.GetHniStatusResponse;
import com.vasilitate.vapp.sdk.network.response.GetReceivedStatusResponse;
import com.vasilitate.vapp.sdk.network.response.PostLogsResponse;

import java.util.List;

class SmsApiCheckManager {

    private GetHniStatusRequestTask statusRequestTask;
    private PostLogsRequestTask postLogsTask;
    private GetReceivedStatusRequestTask receivedStatusTask;

    private RemoteNetworkTaskListener<GetHniStatusResponse> hniStatusResponseListener;
    private RemoteNetworkTaskListener<GetReceivedStatusResponse> receivedStatusResponseListener;
    private RemoteNetworkTaskListener<PostLogsResponse> postLogsResponseListener;

    private Context context;
    private VappRestClient restClient;

    SmsApiCheckManager(VappRestClient restClient, Context context,
                              RemoteNetworkTaskListener<GetHniStatusResponse> hniStatusResponseListener,
                              RemoteNetworkTaskListener<GetReceivedStatusResponse> receivedStatusResponseListener,
                              RemoteNetworkTaskListener<PostLogsResponse> postLogsResponseListener) {

        this.restClient = restClient;
        this.context = context.getApplicationContext();
        this.hniStatusResponseListener = hniStatusResponseListener;
        this.receivedStatusResponseListener = receivedStatusResponseListener;
        this.postLogsResponseListener = postLogsResponseListener;
    }

    /**
     * Checks that the HNI status is whitelisted prior to sending the first SMS, and only proceeds
     * to send messages if this is true.
     */
    void performHniStatusCheck() {
        String mcc = Vapp.getMobileCountryCode(context);
        String mnc = Vapp.getmobileNetworkCode(context);

        if (statusRequestTask != null && statusRequestTask.getStatus() == AsyncTask.Status.RUNNING) {
            statusRequestTask.cancel(true);
        }

        statusRequestTask = new GetHniStatusRequestTask(restClient, mcc, mnc);
        statusRequestTask.setRequestListener(hniStatusResponseListener);
        statusRequestTask.execute();
    }

    void performPostLogsCall(List<PostLogsBody.LogEntry> entryList) {
        String cli = ""; // TODO initialise correctly
        String cliDetail = "";
        PostLogsBody body = new PostLogsBody(entryList, cli, cliDetail);

        if (postLogsTask != null && postLogsTask.getStatus() == AsyncTask.Status.RUNNING) {
            postLogsTask.cancel(true);
        }

        postLogsTask = new PostLogsRequestTask(restClient, body);
        postLogsTask.setRequestListener(postLogsResponseListener);
        postLogsTask.execute();
    }

    void performReceivedStatusCheck() {
        String cli = ""; // TODO initialise correctly
        String ddi = "";
        String random2 = "";
        String random3 = "";

        if (receivedStatusTask != null && receivedStatusTask.getStatus() == AsyncTask.Status.RUNNING) {
            receivedStatusTask.cancel(true);
        }

        receivedStatusTask = new GetReceivedStatusRequestTask(restClient,
                cli, ddi, random2, random3);

        receivedStatusTask.setRequestListener(receivedStatusResponseListener);
        receivedStatusTask.execute();
    }

}

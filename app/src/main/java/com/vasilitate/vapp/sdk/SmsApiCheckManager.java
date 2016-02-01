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

/**
 * Performs API calls to the VAPP server on a background thread, and calls delegate methods when an
 * error/response is received.
 */
class SmsApiCheckManager {

    private GetHniStatusRequestTask statusRequestTask;
    private PostLogsRequestTask postLogsTask;
    private GetReceivedStatusRequestTask receivedStatusTask;

    private final RemoteNetworkTaskListener<GetHniStatusResponse> hniStatusResponseListener;
    private final RemoteNetworkTaskListener<GetReceivedStatusResponse> receivedStatusResponseListener;
    private final RemoteNetworkTaskListener<PostLogsResponse> postLogsResponseListener;

    private final Context context;
    private final VappRestClient restClient;

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
        String mnc = Vapp.getMobileNetworkCode(context);

        if (statusRequestTask != null && statusRequestTask.getStatus() == AsyncTask.Status.RUNNING) {
            statusRequestTask.cancel(true);
        }

        statusRequestTask = new GetHniStatusRequestTask(restClient, mcc, mnc);
        statusRequestTask.setRequestListener(hniStatusResponseListener);
        statusRequestTask.execute();
    }

    /**
     * Posts locally logged SMSs to the VAPP backend
     * @param body the locally logged messages
     */
    void performPostLogsCall(PostLogsBody body) {
        if (postLogsTask != null && postLogsTask.getStatus() == AsyncTask.Status.RUNNING) {
            postLogsTask.cancel(true);
        }

        postLogsTask = new PostLogsRequestTask(restClient, body);
        postLogsTask.setRequestListener(postLogsResponseListener);
        postLogsTask.execute();
    }

    /**
     * Checks that a delivery notification has been received by the VAPP backend for a previously
     * logged sms
     * @param vappSms the vapp sms
     */
    void performReceivedStatusCheck(VappSms vappSms) {
        String ddi = vappSms.getDeliveryNumber();
        String random2 = vappSms.getRandomSegment2();
        String random3 = vappSms.getRandomSegment3();
        String mcc = Vapp.getMobileCountryCode(context);
        String mnc = Vapp.getMobileNetworkCode(context);

        if (receivedStatusTask != null && receivedStatusTask.getStatus() == AsyncTask.Status.RUNNING) {
            receivedStatusTask.cancel(true);
        }

        receivedStatusTask = new GetReceivedStatusRequestTask(restClient, mcc, mnc, ddi, random2, random3);
        receivedStatusTask.setRequestListener(receivedStatusResponseListener);
        receivedStatusTask.execute();
    }

}

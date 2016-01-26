package com.vasilitate.vapp.sdk.network;

import android.app.PendingIntent;

import com.vasilitate.vapp.sdk.exceptions.VappApiException;

/**
 * Performs API calls to the Vapp backend
 */
public interface VappRestApi {


    /**
     * Gets the status of the Home Network Id for the current device. Should be called prior to
     * sending any SMS, to check whether the network is blacklisted.
     *
     * @param mcc the mcc code for the current device.
     * @param mnc the mnc code for the current device.
     * @return a {@link GetHniStatusResponse}
     */
    GetHniStatusResponse getHniStatus(String mcc,
                                      String mnc) throws VappApiException;

    /**
     * Posts whether an initial 'test' SMS was sent successfully, after calling the following method:
     * {@link android.telephony.SmsManager#sendTextMessage(String, String, String, PendingIntent, PendingIntent)}.
     *
     * @param body the json POST body
     * @return a {@link PostLogsResponse}
     */
    PostLogsResponse postLog(PostLogsBody body) throws VappApiException;

    /**
     * Gets whether the server has received an SMS delivery notification from a 3rd party telco.
     *
     * @param cli     the calling line identity
     * @param ddi     the destination number
     * @param random2 random hex uniquely identifying a message
     * @param random3 random hex uniquely identifying a message
     * @return a {@link BaseResponse}
     */
    GetReceivedStatusResponse getReceivedStatus(String cli,
                                                String ddi,
                                                String random2,
                                                String random3) throws VappApiException;

}

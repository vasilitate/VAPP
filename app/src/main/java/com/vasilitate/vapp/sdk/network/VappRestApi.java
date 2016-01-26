package com.vasilitate.vapp.sdk.network;

import android.app.PendingIntent;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;

import com.vasilitate.vapp.sdk.exceptions.VappApiException;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Performs API calls to the Vapp backend
 */
public interface VappRestApi {


    @StringDef({RECEIVED_STATUS_YES, RECEIVED_STATUS_NOT_YET, RECEIVED_STATUS_BLACKLISTED})
    @Retention(RetentionPolicy.SOURCE) @interface ReceivedStatus {
    }

    String RECEIVED_STATUS_YES = "yes";
    String RECEIVED_STATUS_NOT_YET = "notYet";
    String RECEIVED_STATUS_BLACKLISTED = "blacklisted";

    /**
     * Gets the status of the Home Network Id for the current device. Should be called prior to
     * sending any SMS, to check whether the network is blacklisted.
     *
     * @param mcc the mcc code for the current device.
     * @param mnc the mnc code for the current device.
     * @return a {@link BaseResponse}
     */
     BaseResponse getHniStatus(String mcc, String mnc) throws VappApiException;

    /**
     * Posts whether an initial 'test' SMS was sent successfully, after calling the following method:
     * {@link android.telephony.SmsManager#sendTextMessage(String, String, String, PendingIntent, PendingIntent)}.
     *
     * @param message   the message text
     * @param ddi       the destination number
     * @param cli       the calling line identity
     * @param cliDetail the operator description
     */
    void postLog(String message, String ddi, String cli, String cliDetail) throws VappApiException;

    /**
     * Gets whether the server has received an SMS delivery notification from a 3rd party telco.
     *
     * @param cli     the calling line identity
     * @param ddi     the destination number
     * @param random2 random hex uniquely identifying a message
     * @param random3 random hex uniquely identifying a message
     * @return a {@link  VappRestApi.ReceivedStatus ReceivedStatus}
     */
    @Nullable @ReceivedStatus String getReceivedStatus(String cli,
                                                       String ddi,
                                                       String random2,
                                                       String random3) throws VappApiException;


}

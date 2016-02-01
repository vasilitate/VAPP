package com.vasilitate.vapp.sdk.network.request;

import com.vasilitate.vapp.sdk.exceptions.VappApiException;
import com.vasilitate.vapp.sdk.network.VappRestClient;
import com.vasilitate.vapp.sdk.network.response.GetReceivedStatusResponse;

import java.io.IOException;

/**
 * Calls GET /receivedstatus in the background then calls a delegate on success/failure
 */
public class GetReceivedStatusRequestTask extends RemoteNetworkTask<GetReceivedStatusResponse> {

    private final String mcc;
    private final String mnc;
    private final String ddi;
    private final String random2;
    private final String random3;

    public GetReceivedStatusRequestTask(VappRestClient restClient, String mcc, String mnc,
                                        String ddi, String random2, String random3) {
        super(restClient);
        this.mcc = mcc;
        this.mnc = mnc;
        this.ddi = ddi;
        this.random2 = random2;
        this.random3 = random3;
    }

    @Override
    protected GetReceivedStatusResponse performApiCall() throws IOException, VappApiException {
        return restClient.getReceivedStatus(mcc, mnc, ddi, random2, random3);
    }
}

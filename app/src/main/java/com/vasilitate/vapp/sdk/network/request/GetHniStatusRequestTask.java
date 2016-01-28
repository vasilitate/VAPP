package com.vasilitate.vapp.sdk.network.request;

import com.vasilitate.vapp.sdk.exceptions.VappApiException;
import com.vasilitate.vapp.sdk.network.VappRestClient;
import com.vasilitate.vapp.sdk.network.response.GetHniStatusResponse;

import java.io.IOException;

public class GetHniStatusRequestTask extends RemoteNetworkTask<GetHniStatusResponse> {

    private final String mcc;
    private final String mnc;

    public GetHniStatusRequestTask(VappRestClient restClient, String mcc, String mnc) {
        super(restClient);
        this.mcc = mcc;
        this.mnc = mnc;
    }

    @Override protected GetHniStatusResponse performApiCall() throws IOException, VappApiException {
        return restClient.getHniStatus(mcc, mnc);
    }
}

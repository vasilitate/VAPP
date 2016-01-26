package com.vasilitate.vapp.sdk.network.request;

import com.vasilitate.vapp.sdk.network.VappRestClient;
import com.vasilitate.vapp.sdk.network.response.GetReceivedStatusResponse;

import java.io.IOException;

public class GetReceivedStatusRequestTask extends RemoteNetworkTask<GetReceivedStatusResponse> {

    private final String cli;
    private final String ddi;
    private final String random2;
    private final String random3;

    public GetReceivedStatusRequestTask(VappRestClient restClient, String cli, String ddi, String random2, String random3) {
        super(restClient);
        this.cli = cli;
        this.ddi = ddi;
        this.random2 = random2;
        this.random3 = random3;
    }

    @Override protected GetReceivedStatusResponse performApiCall() throws IOException {
        return restClient.getReceivedStatus(cli, ddi, random2, random3);
    }
}

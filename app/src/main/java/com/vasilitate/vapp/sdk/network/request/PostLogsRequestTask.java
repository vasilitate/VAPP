package com.vasilitate.vapp.sdk.network.request;

import com.vasilitate.vapp.sdk.exceptions.VappApiException;
import com.vasilitate.vapp.sdk.network.VappRestClient;
import com.vasilitate.vapp.sdk.network.response.PostLogsResponse;

import java.io.IOException;
/**
 * Calls Post /logs in the background then calls a delegate on success/failure
 */
public class PostLogsRequestTask extends RemoteNetworkTask<PostLogsResponse> {

    private final PostLogsBody body;

    public PostLogsRequestTask(VappRestClient restClient, PostLogsBody body) {
        super(restClient);
        this.body = body;
    }

    @Override protected PostLogsResponse performApiCall() throws IOException, VappApiException {
        return restClient.postLog(body);
    }
}

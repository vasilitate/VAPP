package com.vasilitate.example.cases;

import android.test.AndroidTestCase;

import com.vasilitate.vapp.sdk.exceptions.VappApiException;
import com.vasilitate.vapp.sdk.network.BaseResponse;
import com.vasilitate.vapp.sdk.network.VappRestClient;

public class RestTest extends AndroidTestCase {

    private static final String ENDPOINT = "http://api.mobileyes.co.uk";
    private static final String VALID_SDK_KEY = "BG8R4X2PCXYCHRCRJTK6";
    private static final String VALID_MCC = "234";
    private static final String VALID_MNC = "30";

    private VappRestClient vappRestClient;

    @Override public void setUp() throws Exception {
        super.setUp();
        vappRestClient = new VappRestClient(ENDPOINT, VALID_SDK_KEY);
    }

    public void testInvalidSdkKey() {
        vappRestClient = new VappRestClient(ENDPOINT, null);
        BaseResponse response = vappRestClient.getHniStatus(VALID_MCC, VALID_MCC);
        assertNotNull(response);
        assertEquals("error", response.getStatus());
        assertNotNull(response.getError());

        vappRestClient = new VappRestClient(ENDPOINT, "invalid");
        response = vappRestClient.getHniStatus(VALID_MCC, VALID_MCC);
        assertNotNull(response);
        assertEquals("error", response.getStatus());
        assertNotNull(response.getError());
    }

    public void testInvalidGetHniStatusParams() {
        try {
            vappRestClient.getHniStatus(null, VALID_MNC);
            fail("Should reject invalid mnc");
        }
        catch (VappApiException ignored) {
        }
        try {
            vappRestClient.getHniStatus(VALID_MCC, null);
            fail("Should reject invalid mcc");
        }
        catch (VappApiException ignored) {
        }
    }

    public void testGetHniStatus() {
        BaseResponse response = vappRestClient.getHniStatus(VALID_MCC, VALID_MNC);
        assertNotNull(response);
        assertEquals("whitelisted", response.getStatus());
    }

}
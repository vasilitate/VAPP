package com.vasilitate.example.cases;

import android.annotation.TargetApi;
import android.os.Build;
import android.test.AndroidTestCase;

import com.vasilitate.vapp.sdk.Vapp;
import com.vasilitate.vapp.sdk.VappNumberRange;
import com.vasilitate.vapp.sdk.VappProduct;

import java.util.ArrayList;
import java.util.List;

public class TelephonyManagerTest extends AndroidTestCase {

    // Set these according to the device being used for testing.
    private String TEST_OPERATOR = "23410";
    private boolean IS_ROAMING = false;
    private boolean HAS_SIM = false;
    private boolean NETWORK_SUPPORTED = false;
    private String TEST_IMEI = "35145120840121";
    private static final String NUMBER_RANGE = "+447458830000";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        List<VappProduct> productList = new ArrayList<>();
        productList.add(new VappProduct("ExtraLives", 10, 1));
        Vapp.initialise(getContext(), "TestApp", productList,  new VappNumberRange(NUMBER_RANGE, NUMBER_RANGE), true, true,"BG8R4X2PCXYCHRCRJTK6");

    }

    public void testPreconditions() {
        assertNotNull(getContext());
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void testDeviceState() {

        assertEquals(HAS_SIM, Vapp.isSIMPresent(getContext()));
        assertEquals(IS_ROAMING, Vapp.isRoaming(getContext()));
        assertEquals(NETWORK_SUPPORTED, Vapp.isOriginatingNetworkSupported(getContext()));
    }
}
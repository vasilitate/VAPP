package com.vasilitate.example.cases;

import android.preference.PreferenceManager;
import android.test.AndroidTestCase;

import com.vasilitate.vapp.sdk.VappNumberRange;
import com.vasilitate.vapp.sdk.exceptions.InvalidVappProductException;
import com.vasilitate.vapp.sdk.exceptions.InvalidVappProgressException;
import com.vasilitate.vapp.sdk.Vapp;
import com.vasilitate.vapp.sdk.VappProduct;

import java.util.ArrayList;
import java.util.List;

public class PaymentProgressTest extends AndroidTestCase {

    private static final String APP_NAME = "VappTest";
    private static final VappProduct PRODUCT_LIVES = new VappProduct("productLives", 10, 1);
    private static final VappProduct PRODUCT_UNREIGSTERED = new VappProduct("unreistered", 10, 1);
    private static final String NUMBER_RANGE = "+447458830000";

    @Override public void setUp() throws Exception {
        super.setUp();

        List<VappProduct> productList = new ArrayList<>();
        productList.add(PRODUCT_LIVES);

        Vapp.initialise(getContext(),
                APP_NAME,
                productList,
                new VappNumberRange(NUMBER_RANGE, NUMBER_RANGE),
                true,
                true );
    }

    @Override protected void tearDown() throws Exception {
        super.tearDown();
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().clear().apply();
    }

    public void testPreconditions() {
        assertNotNull(getContext());
    }

    public void testPaymentProgressAccessors() {

        assertEquals(0, Vapp.getSMSPaymentProgress(getContext(), PRODUCT_LIVES));

        Vapp.setSMSPaymentProgress(getContext(), PRODUCT_LIVES, 5);
        assertEquals(5, Vapp.getSMSPaymentProgress(getContext(), PRODUCT_LIVES));

        Vapp.setSMSPaymentProgress(getContext(), PRODUCT_LIVES, 10);

        // 10 => between 9 and 10. (91%!)
        int actual = Vapp.getSMSPaymentProgress(getContext(), PRODUCT_LIVES);
        assertTrue( actual == 9 || actual == 10 );

        try {
            Vapp.setSMSPaymentProgress(getContext(), PRODUCT_LIVES, 11);
            fail();
        }
        catch (InvalidVappProgressException ignored) {
        }

        actual = Vapp.getSMSPaymentProgress(getContext(), PRODUCT_LIVES);
        assertTrue( actual == 9 || actual == 10 );

        try {
            Vapp.setSMSPaymentProgress(getContext(), PRODUCT_LIVES, 0);
            fail();
        }
        catch (InvalidVappProgressException ignored) {
        }

        actual = Vapp.getSMSPaymentProgress(getContext(), PRODUCT_LIVES);
        assertTrue(actual == 9 || actual == 10);

        try {
            Vapp.setSMSPaymentProgress(getContext(), PRODUCT_LIVES, -1);
            fail();
        }
        catch (InvalidVappProgressException ignored) {
        }

        actual = Vapp.getSMSPaymentProgress(getContext(), PRODUCT_LIVES);
        assertTrue(actual == 9 || actual == 10);

        try {
            Vapp.setSMSPaymentProgress(getContext(), PRODUCT_UNREIGSTERED, 7);
            fail();
        }
        catch (InvalidVappProductException ignored) {
        }
    }
}
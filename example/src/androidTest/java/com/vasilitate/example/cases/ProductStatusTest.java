package com.vasilitate.example.cases;

import android.preference.PreferenceManager;
import android.test.AndroidTestCase;

import com.vasilitate.vapp.sdk.Vapp;
import com.vasilitate.vapp.sdk.VappNumberRange;
import com.vasilitate.vapp.sdk.VappProduct;

import java.util.ArrayList;
import java.util.List;

public class ProductStatusTest extends AndroidTestCase {

    private static final String APP_NAME = "VappTest";
    private static final VappProduct PRODUCT_LIVES = new VappProduct("productLives", 5, 90);
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
                true );    }

    @Override protected void tearDown() throws Exception {
        super.tearDown();
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().clear().apply();
    }

    public void testPreconditions() {
        assertNotNull(getContext());
    }

    public void testProductStatus() {

        // Check initial settings...
        assertEquals(0, Vapp.getProductRedeemedCount(getContext(), PRODUCT_LIVES));
        assertEquals(false, Vapp.isPaidFor(getContext(), PRODUCT_LIVES));

        assertEquals(false, Vapp.isSMSPaymentInProgress(getContext(), PRODUCT_LIVES));
        assertEquals(0, Vapp.getProductRedeemedCount(getContext(), PRODUCT_LIVES));
        assertEquals(false, Vapp.isSMSPaymentInProgress(getContext(), PRODUCT_LIVES));

        // Set redeemed products e.g. after a restore from external database.
        Vapp.setProductRedeemedCount(getContext(), PRODUCT_LIVES, 5);
        assertEquals(5, Vapp.getProductRedeemedCount(getContext(), PRODUCT_LIVES));
        assertEquals(true, Vapp.isPaidFor(getContext(), PRODUCT_LIVES));

        // Set the current progress to 4 (out of 5) SMS's have been paid
        Vapp.setSMSPaymentProgress(getContext(), PRODUCT_LIVES, 4);

        // Payment should now be in progress, but the redeem count stays as is...
        assertEquals(4, Vapp.getSMSPaymentProgress(getContext(), PRODUCT_LIVES));
        assertEquals(true, Vapp.isPaidFor(getContext(), PRODUCT_LIVES));
        assertEquals(true, Vapp.isSMSPaymentInProgress(getContext(), PRODUCT_LIVES));
        assertEquals(5, Vapp.getProductRedeemedCount(getContext(), PRODUCT_LIVES));


        // Set the current progress to 11 (out of 5) SMS's have been paid (this could happen if the
        // required count is reduced between releases).
        Vapp.setSMSPaymentProgress(getContext(), PRODUCT_LIVES, 4);

        // Payment should now be in progress, but the redeem count stays as is...
        assertEquals(4, Vapp.getSMSPaymentProgress(getContext(), PRODUCT_LIVES));
        assertEquals(true, Vapp.isPaidFor(getContext(), PRODUCT_LIVES));
        assertEquals(true, Vapp.isSMSPaymentInProgress(getContext(), PRODUCT_LIVES));
        assertEquals(5, Vapp.getProductRedeemedCount(getContext(), PRODUCT_LIVES));

    }

}
package com.vasilitate.example.cases;

import android.test.AndroidTestCase;

import com.vasilitate.vapp.sdk.Vapp;
import com.vasilitate.vapp.sdk.VappProduct;
import com.vasilitate.vapp.sdk.exceptions.InvalidProductIdException;
import com.vasilitate.vapp.sdk.exceptions.InvalidSmsCountException;
import com.vasilitate.vapp.sdk.exceptions.InvalidVappProductException;

import java.util.ArrayList;
import java.util.List;

public class VappInitTest extends AndroidTestCase {

    private List<VappProduct> productList;

    @Override public void setUp() throws Exception {
        super.setUp();
        productList = new ArrayList<>();
    }

    public void testPreconditions() {
        assertNotNull(getContext());
    }

       /**
     * Checks that invalid SMS counts throw an exception on initialisation
     */
    public void testInvalidSmsCounts() {
        int[] testData = {-1, 1001, 9001, 0, -2};

        for (int count : testData) {
            productList.clear();
            productList.add(new VappProduct("ExtraLives", count, 1));

            try {
                Vapp.initialise(getContext(),
                        productList,
                        null,
                        true,
                        true, "BG8R4X2PCXYCHRCRJTK6");
                fail(String.format("Failed to reject invalid SMS count '%d'", count));
            }
            catch (InvalidSmsCountException ignored) {
            }
        }
    }

    /**
     * Checks that valid SMS counts are accepted on initialisation
     */
    public void testValidSmsCounts() {
        int[] testData = {1, 200, 100, 53, 7};

        for (int count : testData) {
            productList.clear();
            productList.add(new VappProduct("ExtraLives", count, 1));
            Vapp.initialise(getContext(),
                    productList,
                    null,
                    true,
                    true, "BG8R4X2PCXYCHRCRJTK6");
        }
    }

    /**
     * Checks that invalid product setup throws an exception on initialisation
     */
    public void testInvalidProductSetup() {

        List<List<VappProduct>> listList = new ArrayList<>();
        listList.add(null);
        listList.add(new ArrayList<VappProduct>());

        List<VappProduct> duplicateList1 = new ArrayList<>();
        duplicateList1.add(new VappProduct("ExtraLives", 10, 1));
        duplicateList1.add(new VappProduct("ExtraLives", 20, 1));

        List<VappProduct> duplicateList2 = new ArrayList<>();
        duplicateList2.add(new VappProduct("ExtraLives", 10, 1));
        duplicateList2.add(new VappProduct("GoatSkin", 35, 2));
        duplicateList2.add(new VappProduct("ExtraLives", 15, 5));

        listList.add(duplicateList1);
        listList.add(duplicateList2);

        for (List<VappProduct> productList : listList) {
            try {
                Vapp.initialise(getContext(),
                        productList,
                        null,
                        true,
                        true, "BG8R4X2PCXYCHRCRJTK6");
                fail("Failed to reject initialisation with invalid product list " + productList);
            }
            catch (InvalidVappProductException ignored) {
            }
        }
    }

    /**
     * Checks that invalid product Ids throw an exception on initialisation
     */
    public void testInvalidProductIds() {
        String[] testData = {null, "", "1234567890abcdef", "abc?", "SixteeenLetterss", "My game",
                "Az3?", "1234567890abcdef", "spaces "};

        for (String id : testData) {
            productList.clear();
            productList.add(new VappProduct(id, 1, 1));

            try {
                Vapp.initialise(getContext(),
                        productList,
                        null,
                        true,
                        true, "BG8R4X2PCXYCHRCRJTK6");
                fail();
            }
            catch (InvalidProductIdException ignored) {
            }
        }
    }

    /**
     * Checks that valid product Ids are accepted on initialisation
     */
    public void testValidProductIds() {
        String[] testData = {"Vappit", "HotPotato", "BottleSmash", "Fifteenletterss", "Fourteenletters"};

        for (String id : testData) {
            productList.clear();
            productList.add(new VappProduct(id, 1, 1));
            Vapp.initialise(getContext(),
                    productList,
                    null,
                    true,
                    true, "BG8R4X2PCXYCHRCRJTK6");
        }
    }

}
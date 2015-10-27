package com.vasilitate.example.cases;

import android.test.AndroidTestCase;

import com.vasilitate.vapp.sdk.Vapp;
import com.vasilitate.vapp.sdk.VappNumberRange;
import com.vasilitate.vapp.sdk.VappProduct;
import com.vasilitate.vapp.sdk.exceptions.InvalidApplicationVappIdException;
import com.vasilitate.vapp.sdk.exceptions.InvalidProductIdException;
import com.vasilitate.vapp.sdk.exceptions.InvalidSmsCountException;
import com.vasilitate.vapp.sdk.exceptions.InvalidVappNumberException;
import com.vasilitate.vapp.sdk.exceptions.InvalidVappProductException;

import java.util.ArrayList;
import java.util.List;

public class VappInitTest extends AndroidTestCase {

    private static final String APP_NAME = "VappTest";
    private static final String NUMBER_RANGE = "+447458830000";

    private static final long MIN_RANGE_START = 447458830000L;
    private static final long MAX_RANGE_START = 447458839999L;


    private List<VappProduct> productList;

    @Override public void setUp() throws Exception {
        super.setUp();
        productList = new ArrayList<>();
    }

    public void testPreconditions() {
        assertNotNull(getContext());
    }

    /**
     * Checks that invalid Vapp App ids throw an exception on initialisation
     */
    public void testInvalidAppIds() {
        String[] testData = {null, "", "1234567890abcdef", "abc?", "SixteeenLetterss", "My game",
                "Az3?", "1234567890abcdef", "spaces "};

        for (String id : testData) {
            try {
                Vapp.initialise(getContext(), id, productList, new VappNumberRange(NUMBER_RANGE, NUMBER_RANGE), true, true );
                fail(String.format("Invalid Vapp Id '%s' not rejected", id));
            }
            catch (InvalidApplicationVappIdException ignored) {
            }
        }
    }

    /**
     * Checks that valid Vapp App ids are accepted on initialisation
     */
    public void testValidAppIds() {
        String[] testData = {"Vappit", "HotPotato", "BottleSmash", "Fifteenletterss", "Fourteenletters"};
        productList.add(new VappProduct("ExtraLives", 10, 1));

        for (String id : testData) {
            Vapp.initialise(getContext(), id, productList, new VappNumberRange(NUMBER_RANGE, NUMBER_RANGE), true, true);
        }
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
                        APP_NAME,
                        productList,
                        new VappNumberRange(NUMBER_RANGE, NUMBER_RANGE),
                        true,
                        true );
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
                    APP_NAME,
                    productList,
                    new VappNumberRange(NUMBER_RANGE, NUMBER_RANGE),
                    true,
                    true );
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
                        APP_NAME,
                        productList,
                        new VappNumberRange(NUMBER_RANGE, NUMBER_RANGE),
                        true,
                        true );
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
                        APP_NAME,
                        productList,
                        new VappNumberRange(NUMBER_RANGE, NUMBER_RANGE),
                        true,
                        true );
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
                    APP_NAME,
                    productList,
                    new VappNumberRange(NUMBER_RANGE, NUMBER_RANGE),
                    true,
                    true );        }
    }

    public void testInvalidNumberRanges() {
        productList.add(new VappProduct("ExtraLives", 10, 1));

        List<VappNumberRange> invalidRanges = new ArrayList<>();
        invalidRanges.add(new VappNumberRange(numberFromLong(MIN_RANGE_START), numberFromLong(MAX_RANGE_START + 1)));
        invalidRanges.add(new VappNumberRange(numberFromLong(MIN_RANGE_START - 1), numberFromLong(MAX_RANGE_START + 1)));
        invalidRanges.add(new VappNumberRange(numberFromLong(MIN_RANGE_START - 1), numberFromLong(MAX_RANGE_START)));
        invalidRanges.add(new VappNumberRange(numberFromLong(-1), numberFromLong(MAX_RANGE_START)));
        invalidRanges.add(new VappNumberRange(numberFromLong(1234), numberFromLong(MAX_RANGE_START)));

        testFailRange(numberFromLong(MIN_RANGE_START), String.format("%d", MAX_RANGE_START));
        testFailRange(numberFromLong(MIN_RANGE_START), null);
        testFailRange(String.format("%d", MIN_RANGE_START), numberFromLong(MAX_RANGE_START));
        testFailRange(null, numberFromLong(MAX_RANGE_START));
        testFailRange(numberFromLong(9999999999999L), numberFromLong(0L));

        for (VappNumberRange numberRange : invalidRanges) {
            try {
                Vapp.initialise(getContext(),
                        APP_NAME,
                        productList,
                        numberRange,
                        true,
                        true );
                fail(String.format("Failed to reject invalid number range %s", numberRange));
            }
            catch (InvalidVappNumberException ignored) {
            }
        }
    }

    public void testValidNumberRanges() {
        productList.add(new VappProduct("ExtraLives", 10, 1));

        List<VappNumberRange> validRanges = new ArrayList<>();
        validRanges.add(new VappNumberRange(numberFromLong(MIN_RANGE_START), numberFromLong(MAX_RANGE_START)));
        validRanges.add(new VappNumberRange(numberFromLong(MIN_RANGE_START + 1), numberFromLong(MAX_RANGE_START - 1)));
        validRanges.add(new VappNumberRange(numberFromLong(MIN_RANGE_START + 57), numberFromLong(MAX_RANGE_START - 89)));
        validRanges.add(new VappNumberRange(numberFromLong(MIN_RANGE_START + 263), numberFromLong(MAX_RANGE_START - 671)));
        validRanges.add(new VappNumberRange(numberFromLong(MIN_RANGE_START + 2653), numberFromLong(MAX_RANGE_START - 3661)));
        validRanges.add(new VappNumberRange(numberFromLong(MIN_RANGE_START), numberFromLong(MIN_RANGE_START)));

        for (VappNumberRange numberRange : validRanges) {
            Vapp.initialise(getContext(),
                    APP_NAME,
                    productList,
                    numberRange,
                    true,
                    true );
        }
    }

    private void testFailRange(String start, String end) {
        try {
            new VappNumberRange(start, end);
            fail("Failed to reject invalid range");
        }
        catch (InvalidVappNumberException ignored) {
        }
    }

    private String numberFromLong(long number) {
        return String.format("+%d", number);
    }

}
package com.vasilitate.example.cases;

import android.preference.PreferenceManager;
import android.test.AndroidTestCase;

import com.vasilitate.vapp.sdk.SubscriptionIntervalType;
import com.vasilitate.vapp.sdk.Vapp;
import com.vasilitate.vapp.sdk.VappProduct;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class SubscriptionTest extends AndroidTestCase {

    private static final VappProduct ONE_DAY_SUBSCRIPTION = new VappProduct("oneDaySub", 2, SubscriptionIntervalType.DAY, 1);
    private static final VappProduct THREE_DAY_SUBSCRIPTION = new VappProduct("threeDaySub", 2, SubscriptionIntervalType.DAY, 3);
    private static final VappProduct TWO_WEEK_SUBSCRIPTION = new VappProduct("twoWeekSub", 2, SubscriptionIntervalType.WEEK, 2);
    private static final VappProduct FIRST_EACH_MONTH_SUBSCRIPTION = new VappProduct("firstEachMonth", 2, SubscriptionIntervalType.DAY_OF_MONTH, 1);
    private static final VappProduct TWENTY_EIGHTH_EACH_MONTH_SUBSCRIPTION = new VappProduct("28thEachMonth", 2, SubscriptionIntervalType.DAY_OF_MONTH, 28);
    private static final VappProduct TWENTY_NINTH_EACH_MONTH_SUBSCRIPTION = new VappProduct("29thEachMonth", 2, SubscriptionIntervalType.DAY_OF_MONTH, 29);
    private static final String NUMBER_RANGE = "+447458830000";

    @Override public void setUp() throws Exception {
        super.setUp();

        List<VappProduct> productList = new ArrayList<>();
        productList.add(ONE_DAY_SUBSCRIPTION);
        productList.add(THREE_DAY_SUBSCRIPTION);
        productList.add(TWO_WEEK_SUBSCRIPTION);
        productList.add(FIRST_EACH_MONTH_SUBSCRIPTION);
        productList.add(TWENTY_EIGHTH_EACH_MONTH_SUBSCRIPTION);
        productList.add(TWENTY_NINTH_EACH_MONTH_SUBSCRIPTION);

        Vapp.initialise(getContext(),
                productList,
                null,
                true,
                true,
                "BG8R4X2PCXYCHRCRJTK6");
    }

    @Override protected void tearDown() throws Exception {
        super.tearDown();
        PreferenceManager.getDefaultSharedPreferences(getContext()).edit().clear().apply();
    }

    public void testPreconditions() {
        assertNotNull(getContext());
    }

    public void testPaymentProgressAccessors() {

        Calendar calendar = Calendar.getInstance();
        calendar.set( Calendar.YEAR, 2016 );
        calendar.set( Calendar.MONTH, Calendar.FEBRUARY );
        calendar.set( Calendar.DAY_OF_MONTH, 29 );

        assertEquals(null, Vapp.getSubscriptionEndDate(getContext(), ONE_DAY_SUBSCRIPTION));

        Date nextSubscriptionDate = ONE_DAY_SUBSCRIPTION.getNextSubscriptionEndDate( calendar.getTime());
        Calendar nextDate = Calendar.getInstance();
        nextDate.setTime( nextSubscriptionDate );
        assertEquals( 2016, nextDate.get( Calendar.YEAR ) );
        assertEquals( Calendar.MARCH, nextDate.get( Calendar.MONTH ) );
        assertEquals( 1, nextDate.get( Calendar.DAY_OF_MONTH ) );
        assertEquals( 23, nextDate.get( Calendar.HOUR_OF_DAY ) );
        assertEquals( 59, nextDate.get( Calendar.MINUTE ) );
        assertEquals( 59, nextDate.get( Calendar.SECOND ) );

        nextSubscriptionDate = THREE_DAY_SUBSCRIPTION.getNextSubscriptionEndDate( calendar.getTime());
        nextDate = Calendar.getInstance();
        nextDate.setTime( nextSubscriptionDate );
        assertEquals( 2016, nextDate.get( Calendar.YEAR ) );
        assertEquals( Calendar.MARCH, nextDate.get( Calendar.MONTH ) );
        assertEquals( 3, nextDate.get( Calendar.DAY_OF_MONTH ) );

        nextSubscriptionDate = TWO_WEEK_SUBSCRIPTION.getNextSubscriptionEndDate( calendar.getTime());
        nextDate = Calendar.getInstance();
        nextDate.setTime( nextSubscriptionDate );
        assertEquals( 2016, nextDate.get( Calendar.YEAR ) );
        assertEquals( Calendar.MARCH, nextDate.get( Calendar.MONTH ) );
        assertEquals( 14, nextDate.get( Calendar.DAY_OF_MONTH ) );


        nextSubscriptionDate = FIRST_EACH_MONTH_SUBSCRIPTION.getNextSubscriptionEndDate( calendar.getTime());
        nextDate = Calendar.getInstance();
        nextDate.setTime( nextSubscriptionDate );
        assertEquals( 2016, nextDate.get( Calendar.YEAR ) );
        assertEquals( Calendar.MARCH, nextDate.get( Calendar.MONTH ) );
        assertEquals( 1, nextDate.get( Calendar.DAY_OF_MONTH ) );


        nextSubscriptionDate = TWENTY_EIGHTH_EACH_MONTH_SUBSCRIPTION.getNextSubscriptionEndDate( calendar.getTime());
        nextDate = Calendar.getInstance();
        nextDate.setTime( nextSubscriptionDate );
        assertEquals( 2016, nextDate.get( Calendar.YEAR ) );
        assertEquals( Calendar.MARCH, nextDate.get( Calendar.MONTH ) );
        assertEquals( 28, nextDate.get( Calendar.DAY_OF_MONTH ) );


        nextSubscriptionDate = TWENTY_NINTH_EACH_MONTH_SUBSCRIPTION.getNextSubscriptionEndDate( calendar.getTime());
        nextDate = Calendar.getInstance();
        nextDate.setTime( nextSubscriptionDate );
        assertEquals( 2016, nextDate.get( Calendar.YEAR ) );
        assertEquals( Calendar.APRIL, nextDate.get( Calendar.MONTH ) );
        assertEquals( 1, nextDate.get( Calendar.DAY_OF_MONTH ) );



//
//        Vapp.setSMSPaymentProgress(getContext(), PRODUCT_LIVES, 5);
//        assertEquals(5, Vapp.getSMSPaymentProgress(getContext(), PRODUCT_LIVES));
//
//        Vapp.setSMSPaymentProgress(getContext(), PRODUCT_LIVES, 10);
//
//        // 10 => between 9 and 10. (91%!)
//        int actual = Vapp.getSMSPaymentProgress(getContext(), PRODUCT_LIVES);
//        assertTrue( actual == 9 || actual == 10 );
//
//        try {
//            Vapp.setSMSPaymentProgress(getContext(), PRODUCT_LIVES, 11);
//            fail();
//        }
//        catch (InvalidVappProgressException ignored) {
//        }
//
//        actual = Vapp.getSMSPaymentProgress(getContext(), PRODUCT_LIVES);
//        assertTrue( actual == 9 || actual == 10 );
//
//        try {
//            Vapp.setSMSPaymentProgress(getContext(), PRODUCT_LIVES, 0);
//            fail();
//        }
//        catch (InvalidVappProgressException ignored) {
//        }
//
//        actual = Vapp.getSMSPaymentProgress(getContext(), PRODUCT_LIVES);
//        assertTrue(actual == 9 || actual == 10);
//
//        try {
//            Vapp.setSMSPaymentProgress(getContext(), PRODUCT_LIVES, -1);
//            fail();
//        }
//        catch (InvalidVappProgressException ignored) {
//        }
//
//        actual = Vapp.getSMSPaymentProgress(getContext(), PRODUCT_LIVES);
//        assertTrue(actual == 9 || actual == 10);
//
//        try {
//            Vapp.setSMSPaymentProgress(getContext(), PRODUCT_UNREIGSTERED, 7);
//            fail();
//        }
//        catch (InvalidVappProductException ignored) {
//        }
    }
}
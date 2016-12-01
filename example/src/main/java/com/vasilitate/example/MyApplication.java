package com.vasilitate.example;

import android.app.Application;

import com.vasilitate.vapp.sdk.Vapp;
import com.vasilitate.vapp.sdk.VappProduct;

import java.util.List;

public class MyApplication extends Application {

    private static final List<VappProduct> VAPP_PRODUCTS = MyProduct.getProducts();
    private static final List<VappProduct> VAPP_SUBSCRIPTIONS = MySubscription.getSubscriptions();

    private static final String  SDK_KEY = "A1EE9CB28A54C87C2539";
    private static final boolean TEST_MODE = false;      // FIXME  // Test mode - don't send SMSs
    private static final boolean CANCELLABLE_PRODUCTS = true;


    @Override
    public void onCreate() {
        super.onCreate();

        Vapp.initialise(this,
                VAPP_PRODUCTS,
                VAPP_SUBSCRIPTIONS,
                TEST_MODE,
                CANCELLABLE_PRODUCTS,
                SDK_KEY);
    }
}

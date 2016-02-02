package com.vasilitate.example;

import com.vasilitate.vapp.sdk.VappProduct;

import java.util.ArrayList;
import java.util.List;

/**
 * A convenience enum for setting up the products.
 */
public enum MyProduct {

    //               productId         requiredSMSs   maxProductCount
    LEVEL_COMMANDER( "LevelCommander", 10,            1),
    EXTRA_LIVES(     "ExtraLives",     5,             6);

    private VappProduct vappProduct;

    MyProduct(String productId, int requiredSMSs, int maxProductCount ) {
        vappProduct = new VappProduct( productId, requiredSMSs, maxProductCount );
    }

    public VappProduct getVappProduct() {
        return vappProduct;
    }

    public static List<VappProduct> getProducts() {

        List<VappProduct> productList = new ArrayList<>();
        for( MyProduct product : values() ) {
            productList.add(product.vappProduct);
        }
        return productList;
    }
}

package com.vasilitate.example;

import com.vasilitate.vapp.sdk.VappProduct;

import java.util.ArrayList;
import java.util.List;

/**
 * A convenience enum for setting up the products.
 */
public enum MyProduct {

    LEVEL_COMMANDER("LevelCommander", "MyProduct Purchase", "You have bought LevelCommander!", 10, 1),
    EXTRA_LIVES("ExtraLives", "MyProduct Purchase", "You have bought ExtraLives!", 5, 6);

    private VappProduct vappProduct;

    MyProduct(String productId,
              String notificationTitle,
              String notificationMessage,
              int requiredSMSs,
              int maxProductCount) {

        vappProduct = new VappProduct.Builder(productId, requiredSMSs)
                .setMaxProductCount(maxProductCount)
                .setNotificationTitle(notificationTitle)
                .setNotificationMessage(notificationMessage)
                .build();
    }

    public VappProduct getVappProduct() {
        return vappProduct;
    }

    public static List<VappProduct> getProducts() {

        List<VappProduct> productList = new ArrayList<>();
        for (MyProduct product : values()) {
            productList.add(product.vappProduct);
        }
        return productList;
    }
}

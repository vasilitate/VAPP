package com.vasilitate.vapp.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.List;

/**
 * VAPP Shared Preferences.
 * <p/>
 * The class has package level scope but is abstract to prevent it being instantiated.
 */
abstract class VappConfiguration {

    private static final String APP_PREFIX = "com.vasilitate.vapp.sdk.";
//    private static final String PREF_APP_NAME = APP_PREFIX + "PREF_APP_NAME";

    private static final String REDEEMED_SUFFIX = "REDEEMED_SUFFIX";
    private static final String SENT_SMS_COUNT_SUFFIX = "_SENT_SMS_COUNT";
    private static final String REQUIRED_SMS_COUNT_SUFFIX = "REQUIRED_SMS_COUNT";
    private static final String CURRENT_DOWNLOAD_SMS_COUNT_SUFFIX = "CURRENT_DOWNLOAD_SMS_COUNT_SUFFIX";
    private static final String PRODUCT_EXISTS_SUFFIX = "_PRODUCT_EXISTS";
    private static final String SDK_KEY = "SDK_KEY";
    private static final String TEST_MODE = APP_PREFIX + "TEST_MODE";
    private static final String CANCELLABLE_PRODUCTS = APP_PREFIX + "CANCELLABLE_PRODUCTS";
    private static final String PRODUCT_CANCELLED = APP_PREFIX + "PRODUCT_CANCELLED";

    static void setRequiredSmsCountForProduct(Context context, VappProduct product, int count) {
        String key = getKeyForProduct(product, REQUIRED_SMS_COUNT_SUFFIX);
        getSharedPrefsEditor(context).putInt(key, count).apply();
    }

    static int getRequiredSmsCountForProduct(Context context, VappProduct product) {
        String key = getKeyForProduct(product, REQUIRED_SMS_COUNT_SUFFIX);
        return getSharedPrefs(context).getInt(key, 0);
    }

    static void setCurrentDownloadSmsCountForProduct(Context context, VappProduct product, int count) {
        String key = getKeyForProduct(product, CURRENT_DOWNLOAD_SMS_COUNT_SUFFIX);
        getSharedPrefsEditor(context).putInt(key, count).apply();
    }

    static int getCurrentDownloadSmsCountForProduct(Context context, VappProduct product) {
        String key = getKeyForProduct(product, CURRENT_DOWNLOAD_SMS_COUNT_SUFFIX);
        return getSharedPrefs(context).getInt(key, 0);
    }

    static void setProductCancelled(Context context, String productId, boolean cancelled) {
        String key = getKeyForProduct(productId, PRODUCT_CANCELLED);
        getSharedPrefsEditor(context).putBoolean(key, cancelled).apply();
    }

    static boolean isProductCancelled(Context context, String productId) {
        String key = getKeyForProduct(productId, PRODUCT_CANCELLED);
        return getSharedPrefs(context).getBoolean(key, false);
    }

    static void setSentSmsCountForProduct(Context context, VappProduct product, int count) {
        String key = getKeyForProduct(product, SENT_SMS_COUNT_SUFFIX);
        getSharedPrefsEditor(context).putInt(key, count).apply();
    }

    static int getSentSmsCountForProduct(Context context, VappProduct product) {
        String key = getKeyForProduct(product, SENT_SMS_COUNT_SUFFIX);
        return getSharedPrefs(context).getInt(key, 0);
    }

    public static void setRedeemedCountForProduct(Context context, VappProduct product, int count) { // FIXME should be package visible
        String key = getKeyForProduct(product, REDEEMED_SUFFIX);
        getSharedPrefsEditor(context).putInt(key, count).apply();
    }

    public static int getRedeemedCountForProduct(Context context, VappProduct product) { // FIXME should be package visible
        String key = getKeyForProduct(product, REDEEMED_SUFFIX);
        return getSharedPrefs(context).getInt(key, 0);
    }

    static void setProductExists(Context context, VappProduct product, boolean exists) {
        String key = getKeyForProduct(product, PRODUCT_EXISTS_SUFFIX);
        getSharedPrefsEditor(context).putBoolean(key, exists).apply();
    }

    static boolean doesProductExist(Context context, VappProduct product) {
        String key = getKeyForProduct(product, PRODUCT_EXISTS_SUFFIX);
        return getSharedPrefs(context).getBoolean(key, false);
    }

    static void setTestMode(Context context, boolean mode) {
        getSharedPrefsEditor(context).putBoolean(TEST_MODE, mode).apply();
    }

    static boolean isTestMode(Context context) {
        return getSharedPrefs(context).getBoolean(TEST_MODE, false);
    }

    static void setCancellableProducts(Context context, boolean mode) {
        getSharedPrefsEditor(context).putBoolean(CANCELLABLE_PRODUCTS, mode).apply();
    }

    static boolean isCancellableProducts(Context context) {
        return getSharedPrefs(context).getBoolean(CANCELLABLE_PRODUCTS, true);
    }

    /**
     * Prune all previously existing product keys that aren't in the list by setting EXISTS to false
     *
     * @param context     the context
     * @param productList the list of initialised products
     */
    static void pruneMissingProducts(Context context, List<VappProduct> productList) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        for (String key : sharedPrefs.getAll().keySet()) {
            if (key.contains(PRODUCT_EXISTS_SUFFIX)) {

                boolean shouldPrune = true;

                for (VappProduct product : productList) {
                    if (key.contains(product.getProductId().toUpperCase())) {
                        shouldPrune = false;
                    }
                }

                if (shouldPrune) {
                    sharedPrefs.edit().putBoolean(key, false).apply();
                }
            }
        }
    }

    /**
     * Persists the SDK key
     *
     * @param context the current context
     * @param sdkKey  the sdk key to store
     */
    static void setSdkKey(Context context, String sdkKey) {
        getSharedPrefsEditor(context).putString(SDK_KEY, sdkKey).apply();
    }

    /**
     * Retrieves the SDK key
     *
     * @param context the current context
     * @return the stored sdk key
     */
    static String getSdkKey(Context context) {
        return getSharedPrefs(context).getString(SDK_KEY, null);
    }


    private static SharedPreferences.Editor getSharedPrefsEditor(Context context) {
        return getSharedPrefs(context).edit();
    }

    private static SharedPreferences getSharedPrefs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    private static String getKeyForProduct(VappProduct product, String suffix) {
        return getKeyForProduct(product.getProductId(), suffix);
    }

    private static String getKeyForProduct(String productId, String suffix) {
        return APP_PREFIX + productId + suffix;
    }
}

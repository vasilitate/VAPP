package com.vasilitate.vapp.sdk;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.vasilitate.vapp.R;
import com.vasilitate.vapp.sdk.exceptions.InvalidApplicationVappIdException;
import com.vasilitate.vapp.sdk.exceptions.InvalidProductIdException;
import com.vasilitate.vapp.sdk.exceptions.InvalidSmsCountException;
import com.vasilitate.vapp.sdk.exceptions.InvalidVappNetworkException;
import com.vasilitate.vapp.sdk.exceptions.InvalidVappNumberException;
import com.vasilitate.vapp.sdk.exceptions.InvalidVappProductException;
import com.vasilitate.vapp.sdk.exceptions.NetworkNotSupportedException;
import com.vasilitate.vapp.sdk.exceptions.VappException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Vapp SDK
 */
public abstract class Vapp {

    public static final String TAG = "VAPP";

    static final int MAX_SMS_LIMIT = 200;
    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("[a-zA-Z0-9]{1,15}");
    private static final String RESOURCE_FILE_NUMBERS_CSV = "vapp_numbers.csv";

    private static boolean initialised = false;
    private static String sdkKey;

    private static List<VappProduct> productList;
    private static List<String> deliveryNumbers;
    private static Map<String, String> billingRouteMap;

    // All Vapp members are static so no need for a constructor.
    private Vapp() {
    }

    /**
     * This should be called in onCreate() to initialise the Vapp SDK with the application Vapp Id,
     * a list of products available to the user & the range of destination numbers allocated by
     * Vasilitate for this App.
     * <p/>
     * If this method is not called, the behaviour of other methods is undetermined.
     *
     * @param context             the current context
     * @param products            a list of VappProduct objects, representing the available products
     * @param testMode            false for default functionality, true to disable SMS sending for test purposes
     * @param cancellableProducts true if users should be able to cancel product purchases (default), false if not
     * @param sdkKey              the sdk key provided for your application.
     * @throws InvalidApplicationVappIdException invalid application Vapp Id.
     * @throws InvalidProductIdException         invalid Product Id.
     * @throws InvalidVappNetworkException       invalid Vapp Network.
     * @throws InvalidVappProductException       invalid Vapp product.
     * @throws InvalidSmsCountException          invalid SMS Count given for a product.
     * @throws InvalidVappNumberException        invalid Vapp number specified.
     */
    public static synchronized void initialise(Context context,
                                               List<VappProduct> products,
                                               boolean testMode,
                                               boolean cancellableProducts,
                                               String sdkKey)
            throws InvalidSmsCountException, InvalidApplicationVappIdException,
            InvalidProductIdException, InvalidVappNetworkException, InvalidVappProductException,
            InvalidVappNumberException {

        if (TextUtils.isEmpty(sdkKey)) {
            throw new VappException("Invalid value for SDK key - cannot be null!");
        }
        else {
            Vapp.sdkKey = sdkKey;
            VappConfiguration.setSdkKey(context, sdkKey);
        }

        readCsvNumbers(context);

        VappConfiguration.setTestMode(context, testMode);
        VappConfiguration.setCancellableProducts(context, cancellableProducts);
        initialiseBillingRouteLookup(context);

        if (products == null || products.isEmpty()) {
            throw new InvalidVappProductException("No VAPP! products setup");
        }

        Vapp.productList = products;
        Map<String, Boolean> uniqueNames = new HashMap<>();

        for (VappProduct product : productList) { // validate before changing state
            validateProductId(product.getProductId());
            validateSMSCount(product);
            String name = product.getProductId();

            if (uniqueNames.get(name) != null) {
                throw new InvalidVappProductException(String.format("Duplicate entry found for product %s", product.getProductId()));
            }
            uniqueNames.put(name, true);
        }
        VappProductManager.addProducts(context, productList); // persist state
        initialised = true;

        VappProduct productBeingPurchased = getProductBeingPurchased(context);

        if (productBeingPurchased != null) {
            startSMSService(context, productBeingPurchased.getProductId());
        }
    }

    /**
     * Reads delivery numbers from a csv configuration file and adds them to a list
     *
     * @param context the current context
     */
    private static void readCsvNumbers(Context context) throws VappException {
        deliveryNumbers = new ArrayList<>();
        AssetManager assets = context.getResources().getAssets();

        InputStream is = null;
        try {
            is = assets.open(RESOURCE_FILE_NUMBERS_CSV);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;

            while ((line = reader.readLine()) != null) {
                if (!TextUtils.isEmpty(line)) {
                    deliveryNumbers.add(line.trim());
                }
            }
            if (deliveryNumbers.isEmpty()) {
                throw new VappException("No valid numbers detected in VAPP number configuration file." +
                        "Please download a non-empty file from the VAPP developer console.");
            }
        }
        catch (IOException e) {
            String message = String.format("Error reading VAPP number configuration file '%s' - " +
                    "please check that the file is present in your assets directory, or read the" +
                    " Getting Started guide for more info.", RESOURCE_FILE_NUMBERS_CSV);
            throw new VappException(message, e);
        }
        finally {
            try {
                if (is != null) {
                    is.close();
                }
            }
            catch (IOException e) {
                Log.w(Vapp.TAG, "Error closing stream", e);
            }
        }
    }

    /**
     * Checks if the device is currently roaming.
     *
     * @return true if a roaming network is currently being used, otherwise false
     * @throws VappException Vapp exception - see its message for details.
     */
    public static boolean isRoaming(Context context) throws VappException {
        checkIfInitialised();
        return getDeviceStateContract(context).isRoaming();
    }

    /**
     * Checks if the device currently has a SIM
     *
     * @return true if the device currently has a valid SIM, or false if does not.
     * @throws VappException Vapp exception - see its message for details.
     */
    public static boolean isSIMPresent(Context context) throws VappException {
        checkIfInitialised();

        return getDeviceStateContract(context).isSimPresent();
    }

    /**
     * Gets the originating network name for the device.
     *
     * @param context the current context
     * @return the mobile network name the device is currently connected to
     * @throws VappException Vapp exception - see its message for details.
     */
    public static String getOriginatingNetworkName(Context context) throws VappException {
        checkIfInitialised();
        return getDeviceStateContract(context).getOriginatingNetworkName();
    }


    /**
     * Gets the originating network ISO Country Code.
     *
     * @param context the current context
     * @return the mobile network ISO Country Code.
     * @throws VappException Vapp exception - see its message for details.
     */
    public static String getOriginatingNetworkCountry(Context context) throws VappException {
        checkIfInitialised();
        return getDeviceStateContract(context).getOriginatingNetworkCountry();
    }


    /**
     * Checks if the Originating Network for the current device is declared within the network
     * list used to initialise VAPP!
     *
     * @param context the current context
     * @return true if the current device's network is supported.
     * @throws VappException Vapp exception - see its message for details.
     */
    public static boolean isOriginatingNetworkSupported(Context context) throws VappException {
        checkIfInitialised();
        try {
            return !TextUtils.isEmpty(getBillingRoute(context));
        }
        catch (VappException e) {
            return false;
        }
    }

    /**
     * Gets the number of SMS messages that have already been sent for an incomplete payment.
     *
     * @param context the current context
     * @param product the product
     * @return the number of SMS messages sent.
     * @throws VappException Vapp exception - see its message for details.
     */
    public static int getSMSPaymentProgress(Context context, VappProduct product) throws VappException {
        checkIfInitialised();
        return VappProductManager.getSMSPaymentProgress(context, product);
    }

    /**
     * Sets the number of SMS messages that have been sent for an incomplete payment. This should be
     * used to restore backed up data in any scenario where the user clears application data.
     *
     * @param context  the current context
     * @param product  the given product
     * @param progress the number of sent messages
     * @throws VappException Vapp exception - see its message for details.
     */
    public static void setSMSPaymentProgress(Context context,
                                             VappProduct product,
                                             int progress) throws VappException {
        checkIfInitialised();
        VappProductManager.setSMSPaymentProgress(context, product, progress);
    }

    /**
     * Checks if there is a payment is currently in progress.
     *
     * @param product the product
     * @return true if a payment is in progress.
     * @throws VappException Vapp exception - see its message for details.
     */
    public static boolean isSMSPaymentInProgress(Context context, VappProduct product) throws VappException {
        checkIfInitialised();
        return VappProductManager.isSMSPaymentInProgress(context, product);
    }

    /**
     * Gets the number of product instances fully purchased for the given product.
     *
     * @param context the current context
     * @param product the product
     * @return the number products fully purchased.
     * @throws VappException Vapp exception - see its message for details.
     */
    public static int getProductRedeemedCount(Context context, VappProduct product) throws VappException {
        checkIfInitialised();
        return VappProductManager.getRedeemedCount(context, product);
    }

    /**
     * Sets the number of products fully purchased for the given product type. Should be used to
     * restore backed up data in any scenario where the user clears application data.
     *
     * @param context       the current context
     * @param product       the product
     * @param redeemedCount new count
     * @throws VappException Vapp exception - see its message for details.
     */
    public static void setProductRedeemedCount(Context context,
                                               VappProduct product,
                                               int redeemedCount) throws VappException {
        checkIfInitialised();
        VappProductManager.setRedeemedCount(context, product, redeemedCount);
    }

    /**
     * Checks if the product has been paid for.
     *
     * @param context the current context
     * @param product the product
     * @return the number products fully purchased.
     * @throws VappException Vapp exception - see its message for details.
     */
    public static boolean isPaidFor(Context context, VappProduct product) throws VappException {
        checkIfInitialised();
        return VappProductManager.isPaidFor(context, product);
    }

    /**
     * Checks if the product is in the process of being paid for
     *
     * @param context the current context
     * @param product the product
     * @return the number products fully purchased.
     * @throws VappException Vapp exception - see its message for details.
     */
    public static boolean isBeingPaidFor(Context context, VappProduct product) throws VappException {
        checkIfInitialised();
        return VappProductManager.isPaidFor(context, product) && !VappConfiguration.isProductCancelled(context, product.getProductId());
    }

    /**
     * Returns any product which is currently being purchases.   This function must be called
     * at started to allow for the resumption of any interrupted purchase.
     *
     * @param context the current context
     * @return the product being purchased (null if none are).
     * @throws VappException Vapp exception - see its message for details.
     */
    public static VappProduct getProductBeingPurchased(Context context) throws VappException {

        for (VappProduct product : productList) {

            if (Vapp.isSMSPaymentInProgress(context, product)
                    && !VappConfiguration.isProductCancelled(context, product.getProductId())) {
                return product;
            }
        }

        return null;
    }


//    /**
//     * The mobile number to which the SMS's will be sent for the current Originating
//     * Network.
//     *
//     * @param context the current context
//     * @return The mobile number to which the SMS's will be sent
//     *
//     * @exception VappException Vapp exception - see its message for details.
//     */
//    public static String getBillingRouteNumber(Context context) throws VappException {
//        return getBillingRoute(context);
//    }

    /**
     * Displays the Vapp Payment screen
     *
     * @param context the current context
     * @param product the product to initial a new purchase with.
     * @param modal   if true the payment but complete before the user can exit the screen.
     * @return true if the screen was displayed.
     * @throws VappException Vapp exception - see its message for details.
     */
    public static boolean showVappPaymentScreen(Context context,
                                                VappProduct product,
                                                boolean modal) throws VappException {

        // Check that this device is suitable for sending SMSs
        if (!Vapp.isSIMPresent(context)) {
            showErrorMessage(context, context.getString(R.string.vapp_not_available_error_no_sim));
            return false;
        }

        if (TextUtils.isEmpty(Vapp.getOriginatingNetwork(context))) {
            showErrorMessage(context, context.getString(R.string.vapp_not_available_error_no_network));
            return false;
        }

        if (!Vapp.isOriginatingNetworkSupported(context)) {
            String originatingNetwork = Vapp.getOriginatingNetwork(context);
            showErrorMessage(context, context.getString(R.string.vapp_network_not_supported, originatingNetwork));
            return false;
        }

        Intent intent = new Intent(context, VappProgressActivity.class);
        intent.putExtra(VappActions.EXTRA_PRODUCT_ID, product.getProductId());
        intent.putExtra(VappActions.EXTRA_MODAL, modal);
        context.startActivity(intent);
        return true;
    }

    /**
     * Cancels a payment that is currently in progress for a product. This will stop any further SMSs
     * from being sent & retain the number of SMS already sent (if any). Initialise must have been
     * called with the cancellable products parameter set to true.
     *
     * @param context the current context
     * @throws VappException Vapp exception - see its message for details.
     */
    public static void cancelVappPayment(final Context context) throws VappException {

        if (VappConfiguration.isCancellableProducts(context)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);

            builder.setTitle(R.string.cancel_payment_title)
                    .setMessage(R.string.cancel_payment_message)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            context.sendBroadcast(new Intent(VappSmsService.INTENT_CANCEL_PAYMENT));
                        }
                    });
            builder.create().show();
        }
    }

    /**
     * Gets the originating network for the device.  The format is "[MCC][MNC]" e.g: "234010"
     * - See http://www.mcc-mnc.com/
     *
     * @param context the current context
     * @return the mobile network the device is currently connected to
     * @throws VappException Vapp exception - see its message for details.
     */
    static String getOriginatingNetwork(Context context) throws VappException {
        checkIfInitialised();
        return getDeviceStateContract(context).getOriginatingNetwork();
    }

    /**
     * Gets the billing route for a purchase
     *
     * @param context the current context
     * @return the billing route as a string
     * @throws VappException Vapp exception - see its message for details.
     */
    static String getBillingRoute(Context context) throws VappException {

        checkIfInitialised();

        String originatingNetwork = getOriginatingNetwork(context);

        if (TextUtils.isEmpty(originatingNetwork)) {
            throw new NetworkNotSupportedException("No Originating Network!");
        }

        // e.g. 234001, 23401 & 2341 all should match against 234/1

        String normalisedNetwork = originatingNetwork.substring(0, 3) + "/";
        String mnc = originatingNetwork.substring(3);

        try {

            Pattern leadingZeros = Pattern.compile("^([0]+)[0-9]");
            Matcher matcher = leadingZeros.matcher(mnc);

            if (matcher.find()) {
                if (matcher.groupCount() == 1) {

                    String mncLeadingZeros = matcher.group(1);
                    mnc = mnc.substring(mncLeadingZeros.length());
                }
            }
        }
        catch (Exception e) {
            throw new InvalidVappNetworkException(originatingNetwork);
        }
        normalisedNetwork += mnc;

        return billingRouteMap.get(normalisedNetwork);
    }

    static VappSms generateSmsForProduct(Context context,
                                         int smsCount,
                                         int smsIndex) throws VappException {

        String imei = getDeviceStateContract(context).getPhoneImei();
        String originatingNetworkName = getOriginatingNetworkName(context);
        String originatingNetworkCountry = getOriginatingNetworkCountry(context);
        String deliveryNumber = VappProductManager.getRandomDeliveryNumber(deliveryNumbers);

        return new VappSms(sdkKey, smsCount, smsIndex, imei,
                isRoaming(context),
                originatingNetworkName,
                originatingNetworkCountry,
                deliveryNumber);
    }

    @Nullable static String getUserPhoneNumber(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getLine1Number();
    }

    static VappProduct getProduct(String productId) throws VappException {
        for (VappProduct product : productList) {

            if (product.getProductId().equals(productId)) {
                return product;
            }
        }
        return null;
    }

    static void showErrorMessage(final Context context, String message) {

        // There's been a problem with the VAPP! setup - display the problem and then exit.
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
        alertBuilder.setMessage(message)
                .setTitle("VAPP! Error")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        if (context instanceof VappProgressActivity) {
                            ((VappProgressActivity) context).finish();
                        }
                        if (context instanceof VappProgressWidget.VappCompletionListener) {
                            ((VappProgressWidget.VappCompletionListener)context).onErrorAcknowledged();
                        }
                    }
                });
        alertBuilder.create().show();
    }


    /**
     * Returns the MCC for the current operator
     *
     * @param context the current context
     * @return the mcc
     * @throws VappException
     */
    @Nullable static String getMobileCountryCode(Context context) throws VappException {
        checkIfInitialised();
        String hni = getOriginatingNetwork(context);

        if (hni != null && hni.length() >= 2) {
            return hni.substring(0, 3); // mcc is first 3 digits of HNI
        }
        return null;
    }

    /**
     * Returns the MNC for the current operator
     *
     * @param context the current context
     * @return the mnc
     * @throws VappException
     */
    @Nullable static String getMobileNetworkCode(Context context) throws VappException {
        checkIfInitialised();
        String hni = getOriginatingNetwork(context);

        if (hni != null && hni.length() >= 2) {
            return hni.substring(3); // mnc starts after first 3 digits of HNI
        }
        return null;
    }

    static void addRedeemedProduct(Context context, VappProduct product) throws VappException {

        int redeemedCount = VappConfiguration.getRedeemedCountForProduct(context, product);
        VappConfiguration.setRedeemedCountForProduct(context, product, redeemedCount + 1);
        VappConfiguration.setSentSmsCountForProduct(context, product, 0);
    }

    static void startSMSService(Context context, String productId) {

        Intent intent = new Intent(context, VappSmsService.class);
        intent.putExtra(VappActions.EXTRA_PRODUCT_ID, productId);
        context.startService(intent);
    }

    static List<String> getAllDeliveryNumbers() {
        return deliveryNumbers;
    }

    private static DeviceStateContract getDeviceStateContract(Context context) throws VappException {

        boolean debug = context.getResources().getBoolean(R.bool.vapp_mocking);
        return (debug) ? new MockDeviceStateWrapper(context) : new DeviceStateWrapper(context);
    }

    private static void validateApplicationVappId(String name) throws InvalidApplicationVappIdException {

        if (!TextUtils.isEmpty(name)) {
            if (!ALPHANUMERIC_PATTERN.matcher(name).matches()) {
                throw new InvalidApplicationVappIdException(String.format("Application Vapp Id '%s' must contain only alpha-numerics and be 1 to 15 characters long.", name));
            }
        }
        else {
            throw new InvalidApplicationVappIdException("Application Vapp Id must not be null. It must contain only alpha-numerics and be 1 to 15 characters long.");
        }
    }

    private static void validateProductId(String productId) throws InvalidProductIdException {

        if (!TextUtils.isEmpty(productId)) {
            if (!ALPHANUMERIC_PATTERN.matcher(productId).matches()) {
                throw new InvalidProductIdException(String.format("Product Id '%s' must contain only alpha-numerics and be 1 to 15 characters long.", productId));
            }
        }
        else {
            throw new InvalidProductIdException("Product Id  must not be null. It must contain only alpha-numerics and be 1 to 15 characters long.");
        }
    }

    private static void validateSMSCount(VappProduct product) throws InvalidSmsCountException {

        int smsCount = product.getRequiredSmsCount();
        if (smsCount <= 0 || smsCount > Vapp.MAX_SMS_LIMIT) {
            throw new InvalidSmsCountException(product, Vapp.MAX_SMS_LIMIT);
        }
    }

    private static void checkIfInitialised() throws VappException {
        if (!initialised) {
            throw new VappException("Attempted to use Vapp methods before initialising SDK!");
        }
    }

    private static void initialiseBillingRouteLookup(Context context) {

        if (billingRouteMap == null) {

            billingRouteMap = new HashMap<>();

            // Note, future releases may need to support more than one 'billing route' for
            // multiple destination telco's.
            addBillingRouteToMap(context, R.array.billing_route_A, "A");
        }
    }

    private static void addBillingRouteToMap(Context context, int mccMncArrayResId, String billingRoute) {

        if (!TextUtils.isEmpty(billingRoute)) {

            String[] mccMncs = context.getResources().getStringArray(mccMncArrayResId);

            // format is mcc, mcc, ... / mnc, mnc,....

            for (String mccMnc : mccMncs) {

                String[] mccAndMnc = mccMnc.split("/");

                if (mccAndMnc.length == 2) {

                    String[] mccs = mccAndMnc[0].split(",");
                    String[] mncs = mccAndMnc[1].split(",");

                    if (mccs.length > 0 && mncs.length > 0) {

                        for (String mcc : mccs) {
                            for (String mnc : mncs) {

                                String combination = mcc.trim() + "/" + mnc.trim();
                                if (billingRouteMap.containsKey(combination)) {
                                    throw new VappException("MCC/MNC combination is assigned to more than one billing route: " + combination);
                                }
                                else {
                                    billingRouteMap.put(combination, billingRoute);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

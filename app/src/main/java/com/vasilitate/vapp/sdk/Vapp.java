package com.vasilitate.vapp.sdk;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;

import com.vasilitate.vapp.R;
import com.vasilitate.vapp.sdk.exceptions.InvalidApplicationVappIdException;
import com.vasilitate.vapp.sdk.exceptions.InvalidProductIdException;
import com.vasilitate.vapp.sdk.exceptions.InvalidSmsCountException;
import com.vasilitate.vapp.sdk.exceptions.InvalidVappNetworkException;
import com.vasilitate.vapp.sdk.exceptions.InvalidVappNumberException;
import com.vasilitate.vapp.sdk.exceptions.InvalidVappProductException;
import com.vasilitate.vapp.sdk.exceptions.NetworkNotSupportedException;
import com.vasilitate.vapp.sdk.exceptions.VappException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Vapp SDK
 */
public abstract class Vapp {

    static final int MAX_SMS_LIMIT = 200;

    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("[a-zA-Z0-9]{1,15}");

    private static final long MIN_RANGE_START = 447458830000L;
    private static final long MAX_RANGE_START = 447458839999L;

    private static boolean initialised = false;

    private static String appVappId;
    private static List<VappProduct> productList;
    private static Map<String, String> billingRouteMap;
    private static VappNumberRange destinationNumberRange;

    // All Vapp members are static so no need for a constructor.
    private Vapp() {
    }

    /**
     * This should be called in onCreate() to initialise the Vapp SDK with the application Vapp Id,
     * a list of products available to the user & the range of destination numbers allocated by
     * Vasilitate for this App.
     * <p>
     * If this method is not called, the behaviour of other methods is undetermined.
     *
     * @param context                the current context
     * @param appVappId              the unique application Id (1 - 15 alpha-numeric characters (no spaces))
     * @param products               a list of VappProduct objects, representing the available products
     * @param destinationNumberRange range of destination numbers allocated by Vasilitate your App.
     * @param testMode               false for default functionality, true to disable SMS sending for test purposes
     * @param cancellableProducts    true if users should be able to cancel product purchases (default), false if not
     * @throws InvalidApplicationVappIdException invalid application Vapp Id.
     * @throws InvalidProductIdException         invalid Product Id.
     * @throws InvalidVappNetworkException       invalid Vapp Network.
     * @throws InvalidVappProductException       invalid Vapp product.
     * @throws InvalidSmsCountException          invalid SMS Count given for a product.
     * @throws InvalidVappNumberException        invalid Vapp number specified.
     */
    public static synchronized void initialise(Context context,
                                               String appVappId,
                                               List<VappProduct> products,
                                               VappNumberRange destinationNumberRange,
                                               boolean testMode,
                                               boolean cancellableProducts)
            throws InvalidSmsCountException, InvalidApplicationVappIdException,
            InvalidProductIdException, InvalidVappNetworkException, InvalidVappProductException,
            InvalidVappNumberException {

        validateApplicationVappId(appVappId);
        Vapp.appVappId = appVappId;

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

        if (destinationNumberRange.getStartOfRange() >= MIN_RANGE_START
                && destinationNumberRange.getEndOfRange() <= MAX_RANGE_START) {

            Vapp.destinationNumberRange = destinationNumberRange;
        }
        else {
            throw new InvalidVappNumberException(String.format("Invalid number range '%s'", destinationNumberRange));
        }

        initialised = true;

        VappProduct productBeingPurchased = getProductBeingPurchased(context);

        if (productBeingPurchased != null) {
            startSMSService(context, productBeingPurchased.getProductId());
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
     * from being sent & retain the number of SMS already sent (if any)
     *
     * @param context     the current context
     * @throws VappException Vapp exception - see its message for details.
     */
    public static void cancelVappPayment(Context context) throws VappException {
        context.sendBroadcast(new Intent(VappSmsService.INTENT_CANCEL_PAYMENT));
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

    static String getGeneratedSmsForProduct(Context context,
                                            VappProduct product,
                                            int smsCount,
                                            int smsIndex) throws VappException {

        String imei = getDeviceStateContract(context).getPhoneImei();
        String originatingNetworkName = getOriginatingNetworkName(context);
        String originatingNetworkCountry = getOriginatingNetworkCountry(context);
        return VappSmsGenerator.generateSms(appVappId,
                                            product.getProductId(),
                                            smsCount,
                                            smsIndex,
                                            imei,
                                            isRoaming(context),
                                            originatingNetworkName,
                                            originatingNetworkCountry);
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
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        if (context instanceof VappProgressActivity) {
                            ((VappProgressActivity) context).finish();
                        }
                    }
                });
        alertBuilder.create().show();
    }

    static void addRedeemedProduct(Context context, VappProduct product) throws VappException {

        int redeemedCount = VappConfiguration.getRedeemedCountForProduct(context, product);
        VappConfiguration.setRedeemedCountForProduct(context, product, redeemedCount + 1);
        VappConfiguration.setSentSmsCountForProduct(context, product, 0);
    }

    static VappNumberRange getDestinationNumberRange() {
        return destinationNumberRange;
    }

    static void startSMSService(Context context, String productId) {

        Intent intent = new Intent(context, VappSmsService.class);
        intent.putExtra(VappActions.EXTRA_PRODUCT_ID, productId);
        context.startService(intent);
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

            for (String mccMnc : mccMncs) {
                if (billingRouteMap.containsKey(mccMncs)) {
                    throw new VappException("MCC/MNC combination is assigned to more than one billing route: " + mccMnc);
                }
                else {
                    billingRouteMap.put(mccMnc, billingRoute);
                }
            }
        }
    }
}

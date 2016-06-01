package com.vasilitate.vapp.sdk;

import android.content.Context;

import com.vasilitate.vapp.sdk.exceptions.InvalidVappProductException;
import com.vasilitate.vapp.sdk.exceptions.InvalidVappProgressException;

import java.util.Date;
import java.util.List;
import java.util.Random;

abstract class VappProductManager {

    static final String INTERNATIONAL_PREFIX = "+";
    private static final float SMS_COUNT_VARIANT = 0.09f;       // 9%
    private static final int MINIMUM_VARIABLE_COUNT = 6;

    private static final int MINIMUM_SMS_INTERVAL = 12;

    static void addProducts(Context context, List<VappProduct> productList) {

        VappConfiguration.pruneMissingProducts(context, productList);

        for (VappProduct product : productList) {

            int requiredCount = product.getRequiredSmsCount();
            VappConfiguration.setProductExists(context, product, true);
            VappConfiguration.setRequiredSmsCountForProduct(context, product, requiredCount );

            // Ensure any current download count is not greater than the new value for the required
            // SMSs for the product.
            int currentSMSCountForProduct = VappConfiguration.getCurrentDownloadSmsCountForProduct(context, product );
            if( currentSMSCountForProduct > requiredCount ) {
                VappConfiguration.setCurrentDownloadSmsCountForProduct(context, product, requiredCount);
            }

            int smsCount = VappConfiguration.getSentSmsCountForProduct(context, product);

            // Deal with the situation where a product is in the process of being purchased
            // and the SMSs already sent exceeds the new required count.
            if( smsCount > requiredCount ) {

                // Mark the purchase as complete and increment the redeemed coumt.
                VappConfiguration.setSentSmsCountForProduct(context, product, 0);

                int redeemedCount = VappConfiguration.getRedeemedCountForProduct(context, product );
                VappConfiguration.setRedeemedCountForProduct(context, product, redeemedCount + 1 );
            }
        }
    }

    static int getRedeemedCount(Context context, VappProduct product) {
        checkProductExists(context, product);
        return VappConfiguration.getRedeemedCountForProduct(context, product);
    }

    static void setRedeemedCount(Context context, VappProduct product, int redeemedCount ) {
        checkProductExists(context, product);
        VappConfiguration.setRedeemedCountForProduct(context, product, redeemedCount);
    }

    static boolean isPaidFor(Context context, VappProduct product) {
        return getRedeemedCount(context, product ) > 0;
    }

    static Date getSubscriptionEndDate(Context context, VappProduct product) {
        return null;
    }

    static boolean isSMSPaymentInProgress(Context context, VappProduct product) {

        checkProductExists(context, product);

        int requiredCount = VappConfiguration.getCurrentDownloadSmsCountForProduct(context, product);
        int smsCount = VappConfiguration.getSentSmsCountForProduct(context, product);

        return smsCount != 0 && smsCount < requiredCount;
    }

    static int getSMSPaymentProgress(Context context, VappProduct product) {
        checkProductExists(context, product);
        return VappConfiguration.getSentSmsCountForProduct(context, product);
    }

    static void setSMSPaymentProgress(Context context, VappProduct product, int progress) {

        checkProductExists(context, product);

        int requiredCount = VappConfiguration.getRequiredSmsCountForProduct(context, product);

        // Generate a new current download SMS count (randomized to within 9% of the theoretical
        // number) - We do not expect the developer to persist this value between installs.

        int currentDownloadSMSCount = generateCurrentDownloadSMSCount( product );

        VappConfiguration.setCurrentDownloadSmsCountForProduct(context,
                                                               product,
                                                               currentDownloadSMSCount );
        if( progress <= 0 ) {
            throw new InvalidVappProgressException( progress, requiredCount );
        }
        else {

            if( progress > requiredCount ) {
                throw new InvalidVappProgressException( progress, requiredCount );
            }
            else if( progress > currentDownloadSMSCount ) {
                progress = currentDownloadSMSCount;
            }
            VappConfiguration.setSentSmsCountForProduct(context, product, progress);
        }
    }

    private static void checkProductExists(Context context, VappProduct product) {

        if( !VappConfiguration.doesProductExist(context, product) ) {
            throw new InvalidVappProductException(String.format("Product '%s' does not exist", product.getProductId()));
        }
    }

    static int generateCurrentDownloadSMSCount( VappProduct product ) {

        int requiredSMSCount = product.getRequiredSmsCount();

        if( requiredSMSCount >= MINIMUM_VARIABLE_COUNT) {
            final int variableRange = Math.round((float) requiredSMSCount * SMS_COUNT_VARIANT);

            Random random = new Random();
            return Math.round(requiredSMSCount - random.nextInt(variableRange + 1));
        } else {
            return requiredSMSCount;
        }
    }

    /**
     * Calculates the maximum interval between sending messages. If less numbers are available,
     * the interval will increase.
     * @param numberCount the count of delivery numbers available
     * @return the max interval between sending SMS
     */
    private static int getMaxSMSSendInterval(int numberCount) {
        int maximumSendInterval;

        if( numberCount < 10 ) {
            maximumSendInterval = 60;
        } else if( numberCount < 50 ) {
            maximumSendInterval = 50;
        } else if( numberCount < 100 ) {
            maximumSendInterval = 40;
        } else if( numberCount < 1000 ) {
            maximumSendInterval = 30;
        } else {
            maximumSendInterval = 20;
        }

        return maximumSendInterval;
    }

    static int generateSMSSendInterval(Context context, int numberCount) {
        int maximumSendInterval = getMaxSMSSendInterval(numberCount);
        Random random = new Random();
        int sendInterval = MINIMUM_SMS_INTERVAL +
                random.nextInt( maximumSendInterval - MINIMUM_SMS_INTERVAL );

        // Speed up the testing!
        if (VappConfiguration.isTestMode(context)) {
            sendInterval /= 5;
        }

        return sendInterval;
    }

    static String getRandomDeliveryNumber(List<String> numbers) {
        Random random = new Random();
        String deliveryNumber = numbers.get(random.nextInt(numbers.size()));
        return INTERNATIONAL_PREFIX + deliveryNumber;
    }
}

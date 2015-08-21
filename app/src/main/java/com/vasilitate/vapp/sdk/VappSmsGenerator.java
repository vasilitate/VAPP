package com.vasilitate.vapp.sdk;

import java.util.Random;

/**
 * Generates SMS as strings which can be sent to the correct telephone number
 */
abstract class VappSmsGenerator {

    private static final String SMS_FIELD_SEPARATOR = " ";
    private static final String SDK_NAME = "VAPP!";
    private static final String ROAMING_INDICATOR = "Roaming";

    static String generateSms(String appVappId,
                              String productName,
                              int smsCount,
                              int currentSms,
                              String imei,
                              boolean isRoaming,
                              String networkName,
                              String networkCountry ) {

        String randomSegment1 = createRandomHexCode(0, 3);
        String randomSegment2 = createRandomHexCode(8, 16);
        String randomSegment3 = createRandomHexCode(2, 7);

        String message =
               randomSegment1 + SMS_FIELD_SEPARATOR +
               appVappId + SMS_FIELD_SEPARATOR +
               productName + SMS_FIELD_SEPARATOR +
               imei + SMS_FIELD_SEPARATOR +
               Integer.toString(currentSms + 1) + "/" +
               Integer.toString(smsCount) + SMS_FIELD_SEPARATOR +
               networkName + SMS_FIELD_SEPARATOR +
               networkCountry + SMS_FIELD_SEPARATOR +
               randomSegment2 + SMS_FIELD_SEPARATOR +
               SDK_NAME + SMS_FIELD_SEPARATOR +
               randomSegment3 + SMS_FIELD_SEPARATOR +
               (isRoaming ? ROAMING_INDICATOR : "");

        return message.trim();
    }

    private static String createRandomHexCode(int minChars, int maxChars) {
        Random random = new Random();

        int range = (maxChars - minChars) + 1;

        int length = minChars + (random.nextInt(range));
        StringBuilder hex = new StringBuilder();

        while (hex.length() <= length) {
            hex.append(Integer.toHexString(random.nextInt()));
        }

        return hex.toString().toUpperCase().substring(0, length);
    }
}

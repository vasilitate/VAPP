package com.vasilitate.vapp.sdk;

import java.util.Random;

/**
 * Holds a generated SMS which is sent to a VAPP telephone number.
 */
class VappSms {

    private static final String SMS_FIELD_SEPARATOR = " ";
    private static final String SDK_NAME = "VAPP!";
    private static final String ROAMING_INDICATOR = "Roaming";

    private final String appVappId;
    private final int smsCount;
    private final int currentSms;
    private final String imei;
    private final boolean isRoaming;
    private final String networkName;
    private final String networkCountry;

    private final String randomSegment1;
    private final String randomSegment2;
    private final String randomSegment3;
    private final String deliveryNumber;

    private final String message;

    VappSms(String appVappId, int smsCount, int currentSms, String imei,
            boolean isRoaming, String networkName, String networkCountry, String deliveryNumber) {

        this.appVappId = appVappId;
        this.smsCount = smsCount;
        this.currentSms = currentSms;
        this.imei = imei;
        this.isRoaming = isRoaming;
        this.networkName = networkName;
        this.networkCountry = networkCountry;
        this.deliveryNumber = deliveryNumber;

        this.randomSegment1 = createRandomHexCode(0, 3);
        this.randomSegment2 = createRandomHexCode(8, 16);
        this.randomSegment3 = createRandomHexCode(2, 7);
        this.message = generateSms();
    }

    private String generateSms() {
        String message =
                randomSegment1 + SMS_FIELD_SEPARATOR +
                        appVappId + SMS_FIELD_SEPARATOR +
                        imei + SMS_FIELD_SEPARATOR +
                        Integer.toString(currentSms + 1) + " " +
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

    String getRandomSegment2() {
        return randomSegment2;
    }

    String getRandomSegment3() {
        return randomSegment3;
    }

    public String getDeliveryNumber() {
        return deliveryNumber;
    }

    @Override public String toString() {
        return message;
    }

}

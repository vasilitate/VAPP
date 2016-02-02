package com.vasilitate.vapp.sdk;

import java.util.Random;

/**
 * Holds a generated SMS which is sent to a VAPP telephone number.
 */
class VappSms {
    private static final char[] RAND_CHARS = {'(', ')', '@', '#', '~', '^'};
    private static final int RAND_CHARS_LEN = RAND_CHARS.length;

    private static final String SMS_FIELD_SEPARATOR = " ";
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
        Random random = new Random();
        StringBuilder sb = new StringBuilder();

        addFieldAndSpace(randomSegment1, sb); // 0-2 random hex
        addFieldAndSpace(appVappId, sb); // sdk key
        addFieldAndSpace(imei, sb);
        addFieldAndSpace(Integer.toString(currentSms + 1), sb); // sms being sent
        addFieldAndSpace(Integer.toString(smsCount), sb); // total sms count
        addFieldAndSpace(networkName, sb);
        addFieldAndSpace(networkCountry, sb);

        addFieldAndRandomChars(randomSegment2,sb,random.nextInt(RAND_CHARS_LEN),random.nextInt(RAND_CHARS_LEN));
        addFieldAndRandomChars(randomSegment3, sb, random.nextInt(RAND_CHARS_LEN), random.nextInt(RAND_CHARS_LEN));

        if (isRoaming) {
            sb.append(ROAMING_INDICATOR);
        }
        return sb.toString().trim();
    }

    private void addFieldAndSpace(String field, StringBuilder sb) {
        sb.append(field); // 0-2 chars
        sb.append(SMS_FIELD_SEPARATOR);
    }

    private void addFieldAndRandomChars(String field, StringBuilder sb, int firstIndex, int secondIndex) {
        sb.append(RAND_CHARS[firstIndex]);
        sb.append(field); // 0-2 chars
        sb.append(RAND_CHARS[secondIndex]);
        sb.append(SMS_FIELD_SEPARATOR);
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

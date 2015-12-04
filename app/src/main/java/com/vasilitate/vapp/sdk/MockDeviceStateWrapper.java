package com.vasilitate.vapp.sdk;

import android.content.Context;

/**
 * Provides mocked information about the telephony state
 */
class MockDeviceStateWrapper implements DeviceStateContract {

    private Context context;

    MockDeviceStateWrapper(Context context) {
        this.context = context;
    }

    @Override public boolean isRoaming() {
        return getTestMode();
    }

    @Override public boolean isSimPresent() {
        return getTestMode();
    }

    @Override public String getPhoneImei() {
        return (getTestMode()) ? "35145120840121" : null;
    }

    @Override public String getOriginatingNetwork() {
        return (getTestMode()) ? "999001" : null;
    }

    @Override
    public String getOriginatingNetworkName() {
        return "Orange";
    }

    @Override
    public String getOriginatingNetworkCountry() {
        return "UK";
    }

    /*
     * Flips the test mode between two states (persisted in sharedprefs)
     */
    private boolean getTestMode() {
        boolean mode = VappConfiguration.isTestMode(context);
        mode = !mode;
        VappConfiguration.setTestMode(context, mode);
        return mode;
    }
}

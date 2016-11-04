package com.vasilitate.vapp.sdk;

import android.content.Context;
import android.telephony.TelephonyManager;

/**
 * Provides information about the current telephony state
 */
class DeviceStateWrapper implements DeviceStateContract {
    
    private TelephonyManager telephonyManager;

    DeviceStateWrapper(Context context) {
        this.telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Override public boolean isRoaming() {
        return isGsmPhone() && telephonyManager.isNetworkRoaming();
    }

    @Override public boolean isSimPresent() {
        return isGsmPhone() && (telephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY);
    }

    private boolean isGsmPhone() {
        return telephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM;
    }

    @Override public String getPhoneImei() {
        return telephonyManager.getDeviceId();
    }

    @Override public String getOriginatingNetwork() {
        return telephonyManager.getNetworkOperator();
    }

    @Override
    public String getOriginatingNetworkName() {
        return telephonyManager.getNetworkOperatorName();
    }

    @Override
    public String getOriginatingNetworkCountry() {

        return telephonyManager.getNetworkCountryIso();
//
//        String isoCode = telephonyManager.getNetworkCountryIso();
//        Locale l = new Locale( "", isoCode );
//        return l.getDisplayCountry();
    }
}

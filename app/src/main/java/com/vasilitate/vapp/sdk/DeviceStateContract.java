package com.vasilitate.vapp.sdk;

/**
 * Provides methods which determine the current state of the mobile network
 */
interface DeviceStateContract {

    /**
     * @return true if the network is roaming, otherwise false
     */
    boolean isRoaming();

    /**
     * @return true if the device has a SIM, otherwise false
     */
    boolean isSimPresent();

    /**
     * @return the phone IMEI code
     */
    String getPhoneImei();

    /**
     * @return the numeric name (MCC+MNC) of current registered operator.
     */
    String getOriginatingNetwork();

    /**
     * @return the originating network's name
     */
    String getOriginatingNetworkName();

    /**
     * @return the originating network's country
     */
    String getOriginatingNetworkCountry();
}

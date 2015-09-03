package com.vasilitate.vapp.sdk;

/**
 * Provides methods which determine the current state of the mobile network
 */
interface DeviceStateContract {

    /**
     * Used to determine whether the network is roaming or not
     *
     * @return true if the network is roaming, otherwise false
     */
    boolean isRoaming();

    /**
     * Used to determine whether the device has a SIM
     *
     * @return true if the device has a SIM, otherwise false
     */
    boolean isSimPresent();

    /**
     * Provides the Phone's IMEI code (if any)
     *
     * @return the phone IMEI code
     */
    String getPhoneImei();

    /**
     * Provides the current originating network
     *
     * @return the numeric name (MCC+MNC) of current registered operator.
     */
    String getOriginatingNetwork();

    /**
     * Provides the current originating network name
     *
     * @return the originating network's name
     */
    String getOriginatingNetworkName();

    /**
     * Provides the current originating network country
     *
     * @return the originating network's country
     */
    String getOriginatingNetworkCountry();
}

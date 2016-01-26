package com.vasilitate.vapp.sdk.network;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.vasilitate.vapp.sdk.exceptions.VappApiException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class VappRestClient implements VappRestApi {

    @StringDef({HTTP_GET, HTTP_POST})
    @Retention(RetentionPolicy.SOURCE) @interface HTTPMethod {
    }

    public static final String HTTP_GET = "GET";
    public static final String HTTP_POST = "POST";
    public static final String RESOURCE_HNI_STATUS = "/hnistatus";

    private final String endpoint;
    private final String sdkKey;
    private final Gson gson;

    public VappRestClient(String endpoint, String sdkKey) {
        this.endpoint = endpoint;
        this.sdkKey = sdkKey;
        gson = new Gson();
    }

    @Nullable @Override
    public BaseResponse getHniStatus(String mcc, String mnc) throws VappApiException {
        if (TextUtils.isEmpty(mcc)) {
            throw new VappApiException("MCC cannot be empty.");
        }
        if (TextUtils.isEmpty(mnc)) {
            throw new VappApiException("MNC cannot be empty.");
        }

        String address = combinePaths(endpoint, RESOURCE_HNI_STATUS, mcc, mnc);
        URL url = getUrlForAddress(address);

        HttpURLConnection connection = createHttpConnection(url, HTTP_GET);
        String response = executeRequest(connection);

        if (TextUtils.isEmpty(response)) {
            return null;
        }
        else {
            return gson.fromJson(response, BaseResponse.class);
        }
    }

    @Override
    public void postLog(String message, String ddi, String cli, String cliDetail) throws VappApiException {

    }

    @Nullable @Override
    public String getReceivedStatus(String cli, String ddi, String random2, String random3) throws VappApiException {
        return null;
    }


    /***
     * internal methods
     ***/


    private HttpURLConnection createHttpConnection(URL url, @HTTPMethod String method) {
        HttpURLConnection connection = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("SDK-key", sdkKey);

            if (HTTP_POST.equals(method)) { // add content type as POST has payload
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestMethod(HTTP_POST);
            }
        }
        catch (IOException e) {
            throw new VappApiException("VAPP request failed", e);
        }
        return connection;
    }

    private String executeRequest(URLConnection connection) {
        InputStream is = null;
        BufferedReader reader;
        String response = null;

        try {
            is = new BufferedInputStream(connection.getInputStream());
            reader = new BufferedReader(new InputStreamReader(is));

            String line;
            StringBuilder sb = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            response = sb.toString();
        }
        catch (IOException e) {
            String message = String.format("Vapp request to '%s' failed", connection.getURL());
            throw new VappApiException(message, e);
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e) {
                    throw new VappApiException("Failed to close input stream", e);
                }
            }
        }
        return response;
    }

    @NonNull private URL getUrlForAddress(String address) throws VappApiException {
        URL url;

        try {
            url = new URL(address);
        }
        catch (MalformedURLException e) {
            String message = String.format("Failed to parse address '%s' into URL", address);
            throw new VappApiException(message, e);
        }
        return url;
    }

    private String combinePaths(String... parts) {
        if (parts.length <= 1) {
            throw new VappApiException("Cannot combine less than 2 paths!");
        }
        else {
            String fullPath = parts[0];

            for (int i = 1; i < parts.length; i++) {
                String part = parts[i];

                boolean endHasSeparator = fullPath.endsWith(File.separator);
                boolean startHasSeparator = part.startsWith(File.separator);

                if (endHasSeparator && startHasSeparator) {
                    part = part.replace(File.separator, "");
                }
                else if (!endHasSeparator && !startHasSeparator) {
                    fullPath += File.separator;
                }
                fullPath += part;
            }
            return fullPath;
        }
    }

}

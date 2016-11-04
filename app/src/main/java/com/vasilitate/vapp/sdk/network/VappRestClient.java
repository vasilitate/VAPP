package com.vasilitate.vapp.sdk.network;

import android.support.annotation.NonNull;
import android.support.annotation.StringDef;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.vasilitate.vapp.sdk.Vapp;
import com.vasilitate.vapp.sdk.exceptions.VappApiException;
import com.vasilitate.vapp.sdk.network.request.PostLogsBody;
import com.vasilitate.vapp.sdk.network.response.GetHniStatusResponse;
import com.vasilitate.vapp.sdk.network.response.GetReceivedStatusResponse;
import com.vasilitate.vapp.sdk.network.response.PostLogsResponse;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * A REST client which implements calls to the Vapp API.
 */
public class VappRestClient implements VappRestApi {

    @StringDef({HTTP_GET, HTTP_POST})
    @Retention(RetentionPolicy.SOURCE) @interface HTTPMethod {
    }

    public static final String HTTP_GET = "GET";
    public static final String HTTP_POST = "POST";
    public static final String RESOURCE_HNI_STATUS = "/hnistatus";
    public static final String RESOURCE_RECEIVED_STATUS = "/receivedstatus";
    public static final String RESOURCE_LOGS = "/logs";

    private final String endpoint;
    private final String sdkKey;
    private final Gson gson;
    private boolean log = true;

    public VappRestClient(String endpoint, String sdkKey, boolean testMode) {
        this.endpoint = endpoint;
        this.sdkKey = sdkKey;
        this.log = testMode;
        gson = new Gson();
    }

    @Override
    public GetHniStatusResponse getHniStatus(String mcc, String mnc) throws VappApiException, IOException {
        validateParameter(mcc, "mcc");
        validateParameter(mnc, "mnc");

        String address = combinePaths(endpoint, RESOURCE_HNI_STATUS, mcc, mnc);
        URL url = getUrlForAddress(address);

        HttpURLConnection connection = createHttpConnection(url, HTTP_GET);
        String response = executeRequest(connection);

        if (TextUtils.isEmpty(response)) {
            return null;
        }
        else {
            return gson.fromJson(response, GetHniStatusResponse.class);
        }
    }

    private void validateParameter(String value, String paramName) {
        if (TextUtils.isEmpty(value)) {
            throw new VappApiException(String.format("Param '%s' is invalid", paramName));
        }
    }

    @Override
    public PostLogsResponse postLog(PostLogsBody logs) throws VappApiException, IOException {
        if (logs == null || logs.getLogs().isEmpty()) {
            throw new VappApiException("Cannot send empty logs to server!");
        }

        String address = combinePaths(endpoint, RESOURCE_LOGS);
        URL url = getUrlForAddress(address);

        HttpURLConnection connection = createHttpConnection(url, HTTP_POST);
        String json = gson.toJson(logs);
        String response = executeRequest(connection, json);

        if (TextUtils.isEmpty(response)) {
            return null;
        }
        else {
            return gson.fromJson(response, PostLogsResponse.class);
        }
    }

    @Override
    public GetReceivedStatusResponse getReceivedStatus(String mcc, String mnc, String ddi,
                                                       String random2, String random3, String imei) throws VappApiException, IOException {
        validateParameter(mnc, "mnc");
        validateParameter(mcc, "mcc");
        validateParameter(ddi, "ddi");
        validateParameter(ddi, "ddi");
        validateParameter(random2, "random2");
        validateParameter(random3, "random3");

        ddi = stripPlusSymbol(ddi);
        mnc = stripPlusSymbol(mnc);
        mcc = stripPlusSymbol(mcc);

        String address = combinePaths(endpoint, RESOURCE_RECEIVED_STATUS, mcc, mnc, ddi, random2, random3, imei);
        URL url = getUrlForAddress(address);

        HttpURLConnection connection = createHttpConnection(url, HTTP_GET);
        String response = executeRequest(connection);

        if (TextUtils.isEmpty(response)) {
            return null;
        }
        else {
            return gson.fromJson(response, GetReceivedStatusResponse.class);
        }
    }


    /***
     * internal methods
     ***/

    private String stripPlusSymbol(String input) {
        return input.replace(PostLogsBody.PLUS_SYMBOL, "");
    }

    private HttpURLConnection createHttpConnection(URL url, @HTTPMethod String method) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("SDK-key", sdkKey);

        if (HTTP_POST.equals(method)) { // add content type as POST has payload
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestMethod(HTTP_POST);
        }
        return connection;
    }

    private String executeRequest(HttpURLConnection connection) throws IOException {
        return executeRequest(connection, null);
    }

    private String executeRequest(HttpURLConnection connection, String postBody) throws IOException {
        InputStream is = null;
        DataOutputStream os = null;
        BufferedReader reader;
        String response = null;

        if (log) {
            logRequest(connection, postBody);
        }

        try {
            if (!TextUtils.isEmpty(postBody)) { // send the POST body
                os = new DataOutputStream(connection.getOutputStream());
                os.write(postBody.getBytes());
            }

            is = new BufferedInputStream(connection.getInputStream()); // read the HTTP response
            reader = new BufferedReader(new InputStreamReader(is));

            String line;
            StringBuilder sb = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            response = sb.toString();
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
            if (os != null) {
                try {
                    os.close();
                }
                catch (IOException e) {
                    throw new VappApiException("Failed to close input stream", e);
                }
            }
        }
        if (log) {
            logResponse(response);
        }
        return response;
    }

    private void logRequest(HttpURLConnection connection, String postBody) {
        Log.d(Vapp.TAG, String.format("%s %s", connection.getRequestMethod(), connection.getURL()));
        Map<String, List<String>> requestProperties = connection.getRequestProperties();

        for (String property : requestProperties.keySet()) {
            StringBuilder sb = new StringBuilder();
            sb.append(property);
            sb.append(": ");

            List<String> values = requestProperties.get(property);
            for (String val : values) {
                sb.append(val);
            }
            Log.d(Vapp.TAG, sb.toString());
        }

        if (postBody != null) {
            Log.d(Vapp.TAG, postBody);
        }
    }

    private void logResponse(String response) {
        Log.d(Vapp.TAG, response);
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

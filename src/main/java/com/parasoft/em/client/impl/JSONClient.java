/*
 * Copyright 2016 Parasoft Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.parasoft.em.client.impl;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.codec.binary.Base64;

public class JSONClient {
    private static int DEFAULT_LIMIT = 50;
    protected static final int ONE_MINUTE = 60000;
    protected static final int FIVE_MINUTES = 300000;
    protected String baseUrl;
    protected String username;
    protected String password;
    protected SSLSocketFactory trustAllSslSocketFactory;

    public JSONClient(String baseUrl, String username, String password) {
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
        if (this.password != null && this.password.length() == 0) {
            // empty string is considered no password
            this.password = null;
        }
        trustAllSslSocketFactory = null;
    }

    private HttpURLConnection getConnection(String restPath) throws IOException {
        URL url = new URL (baseUrl + restPath);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        if (connection instanceof HttpsURLConnection) {
            HostnameVerifier hostnameVerifier = null;
            try {
                if (trustAllSslSocketFactory == null) {
                    trustAllSslSocketFactory = makeTrustAllSslSocketFactory();
                }
                hostnameVerifier = new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                };
                ((HttpsURLConnection)connection).setSSLSocketFactory(trustAllSslSocketFactory);
                ((HttpsURLConnection)connection).setHostnameVerifier(hostnameVerifier);
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            }
        }
        connection.setDoOutput(true);
        connection.setRequestProperty("Accept", "application/json");
        if (username != null) {
            String encoding = username + ":" + password;
            encoding = Base64.encodeBase64String(encoding.getBytes("UTF-8"));
            connection.setRequestProperty("Authorization", "Basic " + encoding);
        }
        connection.setConnectTimeout(ONE_MINUTE);
        connection.setReadTimeout(FIVE_MINUTES);
        return connection;
    }

    private JSONObject getResponseJSON(InputStream stream) throws IOException {
        BufferedReader in = new BufferedReader (new InputStreamReader (stream, "UTF-8"));
        try {
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
            return (JSONObject) JSONSerializer.toJSON(result.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            in.close();
        }
        return null;
    }

    private String getResponseString(InputStream stream) throws IOException {
        BufferedReader in = new BufferedReader (new InputStreamReader (stream, "UTF-8"));
        try {
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
            return result.toString();
        } finally {
            in.close();
        }
    }

    protected JSONObject doGet(String restPath, boolean returnsArray) throws IOException {
        return doGet(restPath, "", returnsArray);
    }

    protected JSONObject doGet(String restPath) throws IOException {
        HttpURLConnection connection = getConnection(restPath);
        connection.setRequestMethod("GET");
        return handleResponse(restPath, connection);
    }

    /**
     * Set returnsArray to true if the API method returns a limit of items.  This
     * will cause the call to potentially be made multiple times if there are more
     * then 50 (current default) items that need to be returned by the EM API. 
     * 
     * @param restPath api path
     * @param parameter single string in the format of {@literal key=value&key=value } to be appended after offset and limit parameters
     * @param returnsArray
     * @return
     * @throws IOException
     */
    protected JSONObject doGet(String restPath, String parameter, boolean returnsArray) throws IOException {
        if (returnsArray) {
            int offset = 0;
            JSONObject result = doGet(addOffset(restPath, offset, parameter));
            JSONObject iterativeResult = result;
            while (countTopLevelItems(iterativeResult) == DEFAULT_LIMIT) {
                offset += DEFAULT_LIMIT;
                iterativeResult = doGet(addOffset(restPath, offset, parameter));
                appendResults(iterativeResult, result);
            }
            return result;
        } else {
            if (!parameter.isEmpty()) {
                return doGet(restPath + "?" + parameter); 
            }
            return doGet(restPath);
        }
    }

    private String addOffset(String path, int offset, String parameter) {
        String result = path + "?limit=" + DEFAULT_LIMIT + "&offset=" + offset;
        if (!parameter.isEmpty() ) {
            result += "&" + parameter;
        }
        return result;
    }

    /**
     * Assumes objects are of the form
     * {
     *     rootItem: [
     *         {item ...},
     *         {item2 ...},
     *         etc   
     *     ]
     * }
     *
     * and will count the items.
     *
     * @param obj
     * @return
     */
    private int countTopLevelItems(JSONObject obj) {
        if (obj.values().size() == 1) {
            Object rootItem = obj.values().iterator().next();
            if(rootItem instanceof JSONArray) {
                JSONArray array = (JSONArray) rootItem;
                return array.size();
            }
        }
        return obj.values().size();
    }

    /**
     * Assumes that each object contains a single named element of
     * type array.
     *
     * @param source
     * @param dest
     */
    private void appendResults(JSONObject source, JSONObject dest) {
        if (dest.values().size() == 1) {
            Set<Map.Entry<String, Object>> entries = dest.entrySet();
            Map.Entry<String, Object> entry = entries.iterator().next();
            
            String name = entry.getKey();
            JSONArray array = (JSONArray) entry.getValue();
            if (source.has(name)) {
                array.addAll(source.getJSONArray(name));
            }
        }
    }

    protected JSONObject doPost(String restPath, JSONObject payload) throws IOException {
        HttpURLConnection connection = getConnection(restPath);
        connection.setRequestMethod("POST");
        if (payload != null) {
            String payloadString = payload.toString();
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            BufferedOutputStream stream = new BufferedOutputStream(connection.getOutputStream());
            try {
                byte[] bytes = payloadString.getBytes("UTF-8");
                stream.write(bytes, 0, bytes.length);
            } finally {
                stream.close();
            }
        }
        return handleResponse(restPath, connection);
    }

    protected JSONObject doPut(String restPath, JSONObject payload) throws IOException {
        HttpURLConnection connection = getConnection(restPath);
        connection.setRequestMethod("PUT");
        if (payload != null) {
            String payloadString = payload.toString();
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            BufferedOutputStream stream = new BufferedOutputStream(connection.getOutputStream());
            try {
                byte[] bytes = payloadString.getBytes("UTF-8");
                stream.write(bytes, 0, bytes.length);
            } finally {
                stream.close();
            }
        }
        return handleResponse(restPath, connection);
    }

    protected JSONObject doDelete(String restPath) throws IOException {
        HttpURLConnection connection = getConnection(restPath);
        connection.setRequestMethod("DELETE");
        return handleResponse(restPath, connection);
    }

    private JSONObject handleResponse(String restPath, HttpURLConnection connection)
        throws IOException
    {
        int responseCode = connection.getResponseCode();
        if (responseCode / 100 == 2) {
            return getResponseJSON(connection.getInputStream());
        } else {
            String errorMessage = getResponseString(connection.getErrorStream());
            try {
                JSONObject errorJSON = (JSONObject) JSONSerializer.toJSON(errorMessage);
                if (errorJSON.has("message")) {
                    errorMessage = errorJSON.getString("message");
                }
            } catch (JSONException e) {
                // ignore exception
            }
            throw new IOException(restPath + ' ' + responseCode + '\n' + errorMessage);
        }
    }

    protected static SSLSocketFactory makeTrustAllSslSocketFactory()
        throws GeneralSecurityException, IOException
    {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[] {};
            }
            public void checkClientTrusted(X509Certificate[] chain,
                String authType) throws CertificateException {
            }
            public void checkServerTrusted(X509Certificate[] chain,
                String authType) throws CertificateException {
            }
        } };
        SSLContext context = SSLContext.getInstance("TLS"); //$NON-NLS-1$
        context.init(null, trustAllCerts, new SecureRandom());
        return context.getSocketFactory();
    }
}

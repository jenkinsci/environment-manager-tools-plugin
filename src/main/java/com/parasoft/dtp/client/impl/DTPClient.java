/*
 * Copyright 2019 Parasoft Corporation
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

package com.parasoft.dtp.client.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.codec.binary.Base64;

public class DTPClient {
    protected String baseUrl;
    protected String username;
    protected String password;

    public DTPClient(String baseUrl, String username, String password) {
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        if (!baseUrl.endsWith("grs/")) {
            baseUrl += "grs/";
        }
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
        if (this.password != null && this.password.length() == 0) {
            // empty string is considered no password
            this.password = null;
        }
    }

    private HttpURLConnection getConnection(String restPath) throws IOException {
        URL url = new URL (baseUrl + restPath);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty("Accept", "application/json");
        if (username != null) {
            String encoding = username + ":" + password;
            encoding = Base64.encodeBase64String(encoding.getBytes("UTF-8"));
            connection.setRequestProperty("Authorization", "Basic " + encoding);
        }
        return connection;
    }

    private JSON getResponseJSON(InputStream stream) throws IOException {
        BufferedReader in = new BufferedReader (new InputStreamReader (stream, "UTF-8"));
        try {
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
            return JSONSerializer.toJSON(result.toString());
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

    protected JSONObject doGet(String restPath) throws IOException {
        HttpURLConnection connection = getConnection(restPath);
        connection.setRequestMethod("GET");
        return (JSONObject)handleResponse(restPath, connection);
    }

    protected JSONArray doGetArray(String restPath) throws IOException {
        HttpURLConnection connection = getConnection(restPath);
        connection.setRequestMethod("GET");
        return (JSONArray)handleResponse(restPath, connection);
    }

    private JSON handleResponse(String restPath, HttpURLConnection connection)
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
}

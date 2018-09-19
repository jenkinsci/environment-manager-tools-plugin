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
import java.util.Map;
import java.util.Set;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.codec.binary.Base64;

public class JSONClient {
    private static int DEFAULT_LIMIT = 50;
    protected String baseUrl;
    protected String username;
    protected String password;
    
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
        return getResponseJSON(connection.getInputStream());
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
            return doGet(restPath + "?" + parameter);
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
            array.addAll(source.getJSONArray(name));
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
        int responseCode = connection.getResponseCode();
        if (responseCode / 100 == 2) {
            return getResponseJSON(connection.getInputStream());
        } else {
            String errorMessage = getResponseString(connection.getErrorStream());
            throw new IOException(restPath + ' ' + responseCode + '\n' + errorMessage);
        }
    }
    
    protected JSONObject doDelete(String restPath) throws IOException {
        HttpURLConnection connection = getConnection(restPath);
        connection.setRequestMethod("DELETE");
        int responseCode = connection.getResponseCode();
        if (responseCode / 100 == 2) {
            return getResponseJSON(connection.getInputStream());
        } else {
            String errorMessage = getResponseString(connection.getErrorStream());
            throw new IOException(restPath + ' ' + responseCode + '\n' + errorMessage);
        }
    }
}

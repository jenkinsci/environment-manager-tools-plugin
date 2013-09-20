/*
 * $RCSfile$
 * $Revision$
 *
 * Comments:
 *
 * (C) Copyright ParaSoft Corporation 2013.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF ParaSoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 *
 * $Author$          $Locker$
 * $Date$
 * $Log$
 */
package com.parasoft.em.client.impl;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.codec.binary.Base64;

public class JSONClient {
    protected String baseUrl;
    protected String username;
    protected String password;
    
    public JSONClient(String baseUrl, String username, String password) {
        this.baseUrl = baseUrl;
        this.username = username;
        this.password = password;
    }

    private HttpURLConnection getConnection(String restPath) throws IOException {
        URL url = new URL (baseUrl + restPath);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty("Accept", "application/json");
        if (username != null) {
            String encoding = username + ":" + password;
            encoding = Base64.encodeBase64String(encoding.getBytes());
            connection.setRequestProperty("Authorization", "Basic " + encoding);
        }
        return connection;
    }
    
    private JSONObject getResponseJSON(InputStream stream) throws IOException {
        BufferedReader in = new BufferedReader (new InputStreamReader (stream));
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
    
    protected JSONObject doGet(String restPath) throws IOException {
        HttpURLConnection connection = getConnection(restPath);
        connection.setRequestMethod("GET");
        return getResponseJSON(connection.getInputStream());
    }
    
    protected JSONObject doPost(String restPath, JSONObject payload) throws IOException {
        HttpURLConnection connection = getConnection(restPath);
        connection.setRequestMethod("POST");
        if (payload != null) {
            String payloadString = payload.toString();
            connection.setRequestProperty("Content-Type", "application/json");
            BufferedOutputStream stream = new BufferedOutputStream(connection.getOutputStream());
            try {
                stream.write(payloadString.getBytes(), 0, payloadString.length());
            } finally {
                stream.close();
            }
        }
        return getResponseJSON(connection.getInputStream());
    }
    
    protected JSONObject doDelete(String restPath) throws IOException {
        return null;
    }
}

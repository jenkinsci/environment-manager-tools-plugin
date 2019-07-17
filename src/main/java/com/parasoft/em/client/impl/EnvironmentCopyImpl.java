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

import java.io.IOException;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import com.parasoft.em.client.api.EnvironmentCopy;
import com.parasoft.em.client.api.Environments;
import com.parasoft.em.client.api.EventMonitor;

public class EnvironmentCopyImpl extends JSONClient implements EnvironmentCopy {

    public EnvironmentCopyImpl(String emUrl, String username, String password) {
        super(emUrl, username, password);
    }

    public JSONObject createEnvironmentCopy(int environmentId, int serverId, String newEnvironmentName, boolean copyDataRepo, JSONObject dataRepoSettings)
        throws IOException
    {
        JSONObject payload = new JSONObject();
        try {
            payload.put("originalEnvId", environmentId);
            payload.put("serverId", serverId);
            if ((newEnvironmentName != null) && !newEnvironmentName.trim().isEmpty()) {
                payload.put("newEnvironmentName", newEnvironmentName);
            }
            payload.put("copyDataRepo", copyDataRepo);
            if (dataRepoSettings != null) {
                payload.put("dataRepoSettings", dataRepoSettings);
            }
        } catch (JSONException e) {
        }
        return doPost("api/v2/environments/copy", payload);
    }

    public JSONObject getCopyStatus(int id) throws IOException {
        return doGet("api/v2/environments/copy/" + id);
    }

    public JSONObject removeCopyStatus(int id) throws IOException {
        return doDelete("api/v2/environments/copy/" + id);
    }

    public boolean monitorEvent(JSONObject event, EventMonitor monitor) throws IOException {
        try {
            Thread.sleep(1000); // Sleep at the beginning to give EM a chance to start copying
        } catch (InterruptedException e1) {
        }
        Environments environments = new EnvironmentsImpl(baseUrl, username, password);
        
        int eventId = event.getInt("id");
        JSONObject response = getCopyStatus(eventId);
        int originalEnvId = response.getInt("originalEnvId");
        JSONObject environment = environments.getEnvironment(originalEnvId);
        monitor.logMessage("Copying environment: " + environment.getString("name"));
        JSONObject copiedCount = response.getJSONObject("copiedCount");
        long copiedActionCount = 0;
        long copiedAssetCount = 0;
        long copiedMessageProxyCount = 0;
        long totalActionCount = copiedCount.getLong("totalActionCount");
        long totalAssetCount = copiedCount.getLong("totalAssetCount");
        long totalMessageProxyCount = copiedCount.getLong("totalMessageProxyCount");
        response.put("status", "COPYING");  // show the copied counts at least once
        while ("COPYING".equals(response.getString("status"))) {
            long newCopiedActionCount = copiedCount.getLong("copiedActionCount");
            long newCopiedAssetCount = copiedCount.getLong("copiedAssetCount");
            long newCopiedMessageProxyCount = copiedCount.getLong("copiedMessageProxyCount");
            if (newCopiedActionCount > copiedActionCount) {
                monitor.logMessage("    copied " + newCopiedActionCount + " of " + totalActionCount + " provisioning actions...");
                copiedActionCount = newCopiedActionCount;
            }
            if (newCopiedAssetCount > copiedAssetCount) {
                monitor.logMessage("    copied " + newCopiedAssetCount + " of " + totalAssetCount + " virtual assets...");
                copiedAssetCount = newCopiedAssetCount;
            }
            if (newCopiedMessageProxyCount > copiedMessageProxyCount) {
                monitor.logMessage("    copied " + newCopiedMessageProxyCount + " of " + totalMessageProxyCount + " message proxies...");
                copiedMessageProxyCount = newCopiedMessageProxyCount;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            response = getCopyStatus(eventId);
            copiedCount = response.getJSONObject("copiedCount");
        }
        if ("COMPLETE".equals(response.getString("status"))) {
            int newEnvironmentId = response.getInt("environmentId");
            JSONObject newEnvironment = environments.getEnvironment(newEnvironmentId);
            monitor.logMessage("Successfully copied to environment: " + newEnvironment.getString("name"));
            return true;
        }
        if ("FAILURE".equals(response.getString("status"))) {
            monitor.logMessage("Failed to copy environment.");
            if (response.has("deployFailures")) {
                JSONArray deployFailures = response.getJSONArray("deployFailures");
                for (int i = 0; i < deployFailures.size(); i++) {
                    monitor.logMessage("    " + deployFailures.getString(i));
                }
            }
            if (response.has("message")) {
                monitor.logMessage(response.getString("message"));
            }
        }
        return false;
    }
}

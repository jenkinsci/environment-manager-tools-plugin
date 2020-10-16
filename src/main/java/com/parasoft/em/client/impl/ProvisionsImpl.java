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

import com.parasoft.em.client.api.Environments;
import com.parasoft.em.client.api.EventMonitor;
import com.parasoft.em.client.api.Provisions;
import com.parasoft.em.client.api.Systems;

public class ProvisionsImpl extends JSONClient implements Provisions {
    
    public ProvisionsImpl(String emUrl, String username, String password) {
        super(emUrl, username, password);
    }

    public JSONObject createProvisionEvent(int environmentId, int instanceId, boolean abortOnFailure) throws IOException {
        JSONObject payload = new JSONObject();
        try {
            payload.put("environmentId", environmentId);
            payload.put("instanceId", instanceId);
            payload.put("abortOnFailure", abortOnFailure);
        } catch (JSONException e) {
        }
        return doPost("api/v1/provisions", payload);
    }

    public JSONObject getProvisions() throws IOException {
        return doGet("api/v1/provisions", true);
    }

    public JSONObject getProvisions(int id) throws IOException {
        return doGet("api/v1/provisions/" + id);
    }
    
    public boolean monitorEvent(JSONObject event, EventMonitor monitor) throws IOException, InterruptedException {
        Thread.sleep(1000); // Sleep at the beginning to give EM a chance to start provisioning
        Systems systems = new SystemsImpl(baseUrl, username, password);
        Environments environments = new EnvironmentsImpl(baseUrl, username, password);
        
        int eventId = event.getInt("eventId");
        int environmentId = event.getInt("environmentId");
        int instanceId = event.getInt("instanceId");
        JSONObject environment = environments.getEnvironment(environmentId);
        int systemId = environment.getInt("systemId");
        JSONObject system = systems.getSystem(systemId);
        JSONObject instance = environments.getEnvironmentInstance(environmentId, instanceId);
        
        monitor.logMessage("System: " + system.getString("name"));
        monitor.logMessage("Environment: " + environment.getString("name"));
        monitor.logMessage("Environment Instance: " + instance.getString("name"));
        monitor.logMessage("Provisioning event id: " + eventId);
        boolean failed = false;
        JSONObject response = getProvisions(eventId);
        JSONArray steps = response.getJSONArray("steps");
        for (int i = 0; i < steps.size(); i++) {
            JSONObject step = getProvisions(eventId).getJSONArray("steps").getJSONObject(i);
            monitor.logMessage("Running step #" + (i + 1));
            String result = step.getString("result");
            String lastPercent = "";
            while ("running".equals(result)) {
                Thread.sleep(1000);
                String percent = step.getString("percent");
                if (!lastPercent.equals(percent)) {
                    monitor.logMessage(percent + "%");
                    lastPercent = percent;
                }
                step = getProvisions(eventId).getJSONArray("steps").getJSONObject(i);
                result = step.getString("result");
                failed |= "error".equals(result);
            }
        }
        monitor.logMessage("Completed provisioning event with id: " + eventId);
        monitor.logMessage("Access endpoints via REST API GET " + baseUrl + "api/v2/environments/" + event.getInt("environmentId") + "/endpoints");
        monitor.logMessage("See " + baseUrl + "environments/" + event.getInt("environmentId") + " for details");
        return !failed;
    }
}

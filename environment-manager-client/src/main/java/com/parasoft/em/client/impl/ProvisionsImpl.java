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

import java.io.IOException;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;

import com.parasoft.em.client.api.EventMonitor;
import com.parasoft.em.client.api.Provisions;

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
        return doGet("api/v1/provisions");
    }

    public JSONObject getProvisions(int id) throws IOException {
        return doGet("api/v1/provisions/" + id);
    }
    
    public boolean monitorEvent(JSONObject event, EventMonitor monitor) throws IOException {
        int id = event.getInt("eventId");
        monitor.logMessage("Provisioning event id: " + id);
        boolean failed = false;
        JSONObject response = getProvisions(id);
        JSONArray steps = response.getJSONArray("steps");
        for (int i = 0; i < steps.size(); i++) {
            JSONObject step = getProvisions(id).getJSONArray("steps").getJSONObject(i);
            monitor.logMessage("Running step #" + (i + 1));
            String result = step.getString("result");
            while ("running".equals(result)) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                monitor.logMessage(step.getString("percent") + "%");
                step = getProvisions(id).getJSONArray("steps").getJSONObject(i);
                result = step.getString("result");
                failed |= "error".equals(result);
            }
        }
        monitor.logMessage("Completed provisioning event with id: " + id);
        monitor.logMessage("See " + baseUrl + "environments/" + event.getInt("environmentId") + " for details");
        return !failed;
    }
}

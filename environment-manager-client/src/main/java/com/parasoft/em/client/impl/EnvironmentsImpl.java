/*
 * (C) Copyright ParaSoft Corporation 2013.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF ParaSoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */
package com.parasoft.em.client.impl;

import java.io.IOException;

import net.sf.json.JSONObject;

import com.parasoft.em.client.api.Environments;

public class EnvironmentsImpl extends JSONClient implements Environments {

    public EnvironmentsImpl(String emUrl, String username, String password) {
        super(emUrl, username, password);
    }
    public JSONObject getEnvironmentsV1() throws IOException {
        return doGet("api/v1/environments", true);
    }
    public JSONObject getEnvironments() throws IOException {
        return doGet("api/v2/environments", true);
    }
    public JSONObject getEnvironment(long environmentId) throws IOException {
        return doGet("api/v2/environments/" + environmentId);
    }
    public JSONObject getEnvironmentInstances(long environmentId) throws IOException {
        return doGet("api/v2/environments/" + environmentId + "/instances", true);
    }
    public JSONObject getEnvironmentInstance(long environmentId, long instanceId)
            throws IOException {
        return doGet("api/v2/environments/" + environmentId + "/instances/" + instanceId);
    }
    public JSONObject deleteEnvironment(long environmentId, boolean recursive) throws IOException {
        return doDelete("api/v2/environments/" + environmentId + "?recursive=" + recursive);
    }
}

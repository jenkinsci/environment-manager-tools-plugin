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

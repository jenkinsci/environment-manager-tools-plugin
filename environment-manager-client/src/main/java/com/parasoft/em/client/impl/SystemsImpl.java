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

import com.parasoft.em.client.api.Systems;

public class SystemsImpl extends JSONClient implements Systems {

    public SystemsImpl(String baseUrl, String username, String password) {
        super(baseUrl, username, password);
    }

    public JSONObject getSystems() throws IOException {
        return doGet("api/v1/systems", true);
    }

    public JSONObject getSystem(long id) throws IOException {
        return doGet("api/v1/systems/" + id);
    }

}

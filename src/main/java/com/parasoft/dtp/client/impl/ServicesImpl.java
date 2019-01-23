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

import java.io.IOException;

import com.parasoft.dtp.client.api.Services;

import net.sf.json.JSONObject;

public class ServicesImpl extends DTPClient implements Services {

    public ServicesImpl(String dtpUrl, String username, String password) {
        super(dtpUrl, username, password);
    }

	@Override
	public JSONObject getServices() throws IOException {
		JSONObject services = doGet("api/v1.6/services");
        return services;
	}

}

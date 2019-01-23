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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.parasoft.dtp.client.api.Projects;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class ProjectsImpl extends DTPClient implements Projects {

    public ProjectsImpl(String dtpUrl, String username, String password) {
        super(dtpUrl, username, password);
    }

    @Override
    public List<JSONObject> getProjects() throws IOException {
        JSONArray projArray = doGetArray("api/v1.6/projects?active=true&managedOnly=true");
        int size = projArray.size();
        List<JSONObject> result = new ArrayList<JSONObject>(projArray.size());
        for (int i = 0; i < size; i++) {
            result.add(projArray.getJSONObject(i));
        }
        Collections.sort(result, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject o1, JSONObject o2) {
                return o1.getString("name").compareToIgnoreCase(o2.getString("name"));
            }
        });
        return result;
    }

    @Override
    public JSONObject getProject(long id) throws IOException {
        return doGet("api/v1.6/projects/" + id);
    }
}

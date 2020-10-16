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

package com.parasoft.em.client.api;

import java.io.IOException;
import java.io.InputStream;

import net.sf.json.JSONObject;

public interface Jobs {
	JSONObject getJobs() throws IOException;
	JSONObject getJobsByName(String name) throws IOException;
	JSONObject getJob(long jobId) throws IOException;
	JSONObject executeJob(long jobId) throws IOException;
	boolean monitorExecution(JSONObject history, EventMonitor monitor, int timeoutMinutes) throws IOException;
	JSONObject getHistory(long jobId, long historyId) throws IOException;
	JSONObject deleteHistory(long jobId, long historyId) throws IOException;
	InputStream download(String urlPath) throws IOException;
}

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
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.util.URIUtil;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import com.parasoft.em.client.api.Environments;
import com.parasoft.em.client.api.EventMonitor;
import com.parasoft.em.client.api.Jobs;
import com.parasoft.em.client.api.Systems;

public class JobsImpl extends JSONClient implements Jobs {

	public JobsImpl(String emUrl, String username, String password) {
		super(emUrl, username, password);
	}

	public JSONObject getJobs() throws IOException {
		return doGet("api/v2/jobs", "fields=id%2Cname", true);
	}

	public JSONObject getJobsByName(String name) throws IOException {
		return doGet("api/v2/jobs", "name=" + URIUtil.encodeWithinQuery(name, "UTF-8"), true);
	}

	public JSONObject getJob(long jobId) throws IOException {
		return doGet("api/v2/jobs/" + jobId);
	}

	public JSONObject cancelJob(long jobId, long historyId) throws IOException {
		JSONObject json = new JSONObject();
		json.put("status", "CANCELED");
		return doPut("api/v2/jobs/" + jobId + "/histories/" + historyId, json);
	}

	public JSONObject executeJob(long jobId) throws IOException {
		return doPost("api/v2/jobs/" + jobId + "/histories", null);
	}

	public boolean monitorExecution(JSONObject history, EventMonitor monitor, int timeoutMinutes) throws IOException {
		long startTime = System.currentTimeMillis();
		try {
			Thread.sleep(1000); // Sleep at the beginning to give EM a chance to start executing
		} catch (InterruptedException e1) {
		}
		long jobId = history.getLong("jobId");
		long historyId = history.getLong("id");
		JSONObject context = history.optJSONObject("context");
		if (context != null) {
			Systems systems = new SystemsImpl(baseUrl, username, password);
			Environments environments = new EnvironmentsImpl(baseUrl, username, password);
			long systemId = context.optLong("systemId");
			long environmentId = context.optLong("environmentId");
			long instanceId = context.optLong("environmentInstanceId");
			if (systemId > 0) {
				JSONObject system = systems.getSystem(systemId);
				monitor.logMessage("    Context System: " + system.getString("name"));
			}
			if (environmentId > 0) {
				JSONObject environment = environments.getEnvironment(environmentId);
				monitor.logMessage("    Context Environment: " + environment.getString("name"));
				if (instanceId > 0) {
					JSONObject instance = environments.getEnvironmentInstance(environmentId, instanceId);
					monitor.logMessage("    Context Environment Instance: " + instance.getString("name"));
				}
			}
		}
		String lastStatus = null;
		String status = history.optString("status");
		int lastPercentage = 0;
		int percentage = 0;
		while ("WAITING".equalsIgnoreCase(status) ||
			"RUNNING".equalsIgnoreCase(status))
		{
			if (status != null) {
				if (!status.equals(lastStatus)) {
					if ("WAITING".equalsIgnoreCase(status)) {
						monitor.logMessage("Waiting...");
					} else if ("RUNNING".equalsIgnoreCase(status)) {
						monitor.logMessage("Running...");
					} else {
						monitor.logMessage(status);
					}
					lastStatus = status;
				}
				if (percentage != lastPercentage) {
					monitor.logMessage(percentage + "%");
					lastPercentage = percentage;
				}
			}
			if ((timeoutMinutes > 0) && ((System.currentTimeMillis() - startTime) / 60000 > timeoutMinutes)) {
				monitor.logMessage("Test execution job timed out after " + timeoutMinutes + " minute" + (timeoutMinutes > 1 ? 's' : "") + '.');
				cancelJob(jobId, historyId);
				return false;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			history = getHistory(jobId, historyId);
			status = history.optString("status");
			percentage = history.optInt("percentage");
		}
		Map<Long, JSONArray> historyMap = new HashMap<Long, JSONArray>();
		JSONArray testHistories = history.optJSONArray("testHistories");
		if (testHistories != null) {
			for (int i = 0; i < testHistories.size(); i++) {
				JSONObject obj = testHistories.getJSONObject(i);
				Long reportId = obj.getLong("reportId");
				JSONArray tests = historyMap.get(reportId);
				if (tests == null) {
					tests = new JSONArray();
					historyMap.put(reportId, tests);
				}
				tests.add(obj);
			}
		}
		boolean result = false;
		if ("PASSED".equalsIgnoreCase(status)) {
			result = true;
			if (historyMap.isEmpty()) {
				monitor.logMessage("All tests passed.");
			}
		} else if ("FAILED".equalsIgnoreCase(status)) {
			if (historyMap.isEmpty()) {
				monitor.logMessage("Some tests failed.");
			}
		} else if ("CANCELED".equalsIgnoreCase(status)) {
			monitor.logMessage("Test execution was canceled.");
		}
		JSONArray reportIds = history.optJSONArray("reportIds");
		if (reportIds != null) {
			for (int i = 0; i < reportIds.size(); i++) {
				Long reportId = reportIds.getLong(i);
				JSONArray tests = historyMap.get(reportId);
				if (tests != null) {
					for (int j = 0; j < tests.size(); j++) {
						JSONObject test = tests.getJSONObject(j);
						monitor.logMessage("Test: " + test.getString("name") + ".tst " + test.getString("status"));
					}
				}
				monitor.logMessage(baseUrl + "testreport/" + reportId + "/report.html");
			}
		}
		return result;
	}

	public JSONObject getHistory(long jobId, long historyId)
		throws IOException
	{
		return doGet("api/v2/jobs/" + jobId + "/histories/" + historyId);
	}

	public JSONObject deleteHistory(long jobId, long historyId)
		throws IOException
	{
		return doDelete("api/v2/jobs/" + jobId + "/histories/" + historyId);
	}

	public InputStream download(String urlPath) throws IOException {
		URL url = new URL (baseUrl + urlPath);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		if (connection instanceof HttpsURLConnection) {
			HostnameVerifier hostnameVerifier = null;
			try {
				if (trustAllSslSocketFactory == null) {
					trustAllSslSocketFactory = makeTrustAllSslSocketFactory();
				}
				hostnameVerifier = new HostnameVerifier() {
					@Override
					public boolean verify(String hostname, SSLSession session) {
						return true;
					}
				};
				((HttpsURLConnection)connection).setSSLSocketFactory(trustAllSslSocketFactory);
				((HttpsURLConnection)connection).setHostnameVerifier(hostnameVerifier);
			} catch (GeneralSecurityException e) {
			    e.printStackTrace();
			}
		}
		connection.setDoOutput(true);
		if (username != null) {
			String encoding = username + ":" + password;
			encoding = Base64.encodeBase64String(encoding.getBytes("UTF-8"));
			connection.setRequestProperty("Authorization", "Basic " + encoding);
		}
		connection.setRequestMethod("GET");
		connection.setConnectTimeout(ONE_MINUTE);
		connection.setReadTimeout(FIVE_MINUTES);
		return connection.getInputStream();
	}
}

/*
 * (C) Copyright ParaSoft Corporation 2016.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF ParaSoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.em.client.impl;

import java.io.IOException;

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
		return doGet("api/v2/jobs", true);
	}

	public JSONObject getJob(long jobId) throws IOException {
		return doGet("api/v2/jobs/" + jobId);
	}

	public JSONObject executeJob(long jobId) throws IOException {
		return doPost("api/v2/jobs/" + jobId + "/histories", null);
	}

	public boolean monitorExecution(JSONObject history, EventMonitor monitor) throws IOException {
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
		while (!"PASSED".equalsIgnoreCase(status) &&
			!"FAILED".equalsIgnoreCase(status) &&
			!"CANCELED".equalsIgnoreCase(status))
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
				}
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			history = getHistory(jobId, historyId);
			status = history.optString("status");
			percentage = history.optInt("percentage");
		}
		boolean result;
		if ("PASSED".equalsIgnoreCase(status)) {
			monitor.logMessage("All tests passed.");
			result = true;
		} else {
			result = false;
		}
		if ("FAILED".equalsIgnoreCase(status)) {
			monitor.logMessage("Some tests failed.");
		} else if ("CANCELED".equalsIgnoreCase(status)) {
			monitor.logMessage("Test execution was canceled.");
		}
		JSONArray reportIds = history.optJSONArray("reportIds");
		if (reportIds != null) {
			for (int i = 0; i < reportIds.size(); i++) {
				monitor.logMessage(baseUrl + "testreport/" + reportIds.getLong(i) + "/report.html");
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
}

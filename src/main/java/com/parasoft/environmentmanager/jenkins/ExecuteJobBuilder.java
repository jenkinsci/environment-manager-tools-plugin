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

package com.parasoft.environmentmanager.jenkins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.parasoft.em.client.api.EventMonitor;
import com.parasoft.em.client.api.Jobs;
import com.parasoft.em.client.impl.JobsImpl;
import com.parasoft.environmentmanager.jenkins.EnvironmentManagerPlugin.EnvironmentManagerPluginDescriptor;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.Secret;
import hudson.util.ListBoxModel;

public class ExecuteJobBuilder extends Builder {

	private static final String JOB_BY_ID = "jobById";
	private static final String JOB_BY_NAME = "jobByName";

	private long jobId;
	private String jobName;
	private String jobType;

	@DataBoundConstructor
	public ExecuteJobBuilder(
		long jobId,
		String jobName,
		String jobType)
	{
		super();
		this.jobId = jobId;
		this.jobName = jobName;
		this.jobType = jobType;
	}

	public long getJobId() {
		return jobId;
	}

	public String getJobName() {
		return jobName;
	}

	public boolean isJobType(String type) {
		if (jobId == 0) {
			// default
			return JOB_BY_NAME.equals(type);
		}
		if (jobType == null) {
			// legacy
			return JOB_BY_ID.equals(type);
		}
		return jobType.equals(type);
	}

	@Override
	public boolean perform(final AbstractBuild<?, ?> build, Launcher launcher,
		final BuildListener listener) throws InterruptedException, IOException
	{
		EnvironmentManagerPluginDescriptor pluginDescriptor =
			EnvironmentManagerPlugin.getEnvironmentManagerPluginDescriptor();
		String emUrl = pluginDescriptor.getEmUrl();
		String username = pluginDescriptor.getUsername();
		Secret password = pluginDescriptor.getPassword();
		EnvVars envVars = build.getEnvironment(listener);
		Jobs jobs = new JobsImpl(emUrl, username, password.getPlainText());
		boolean result = true;
		List<JSONObject> jobsToExecute = new ArrayList<JSONObject>();
		if (JOB_BY_NAME.equals(jobType)) {
			String expandedJobName = envVars.expand(jobName);
			JSONObject jobsJSON = jobs.getJobsByName(expandedJobName);
			if (jobsJSON.has("jobs")) {
				JSONArray jobsArray = jobsJSON.getJSONArray("jobs");
				for (Object o : jobsArray) {
					JSONObject jobJSON = (JSONObject) o;
					if (expandedJobName.equals(jobJSON.getString("name"))) {
						jobsToExecute.add(jobJSON);
					}
				}
			}
			if (jobsToExecute.isEmpty()) {
				listener.getLogger().println("ERROR: No test scenario job found on " + emUrl + " matching name \"" + expandedJobName + "\"");
				result = false;
			}
		} else {
			JSONObject jobJSON = jobs.getJob(jobId);
			jobsToExecute.add(jobJSON);
		}
		for (JSONObject jobJSON : jobsToExecute) {
			listener.getLogger().println("Executing \"" + jobJSON.getString("name") + "\" on " + emUrl);
			JSONObject history = jobs.executeJob(jobJSON.getLong("id"));
			result &= jobs.monitorExecution(history, new EventMonitor() {
				public void logMessage(String message) {
					listener.getLogger().println(message);
				}
			});
			String baseUrl = emUrl;
			if (!baseUrl.endsWith("/")) {
				baseUrl += "/";
			}
			history = jobs.getHistory(jobId, history.getLong("id"));
			JSONArray reportIds = history.optJSONArray("reportIds");
			if (reportIds != null) {
				for (int i = 0; i < reportIds.size(); i++) {
					String reportUrl = baseUrl + "testreport/" + reportIds.getLong(i) + "/report.html";
					build.addAction(new ProvisioningEventAction(build, jobJSON.getString("name"), reportUrl, 1, result ? 0 : 1));
					FilePath workspace = build.getWorkspace();
					if (workspace == null) {
						continue;
					}
					try {
						FilePath reportDir = new FilePath(workspace, "target/parasoft/soatest/" + reportIds.getLong(i));
						reportDir.mkdirs();
						FilePath reportXmlFile = new FilePath(reportDir, "report.xml");
						reportXmlFile.copyFrom(jobs.download("testreport/" + reportIds.getLong(i) + "/report.xml"));
					} catch (IOException e) {
						// ignore exception: downloading report.xml was not supported prior to CTP 3.1.3
					}
				}
			}
		}
		return result;
	}

	@Override
	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl)super.getDescriptor();
	}

	@Extension
	public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

		public DescriptorImpl() {
			load();
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType) {
			return true;
		}

		@Override
		public String getDisplayName() {
			return "Execute a test scenario job";
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
			req.bindJSON(this, json);
			save();
			return super.configure(req, json);
		}

		public ListBoxModel doFillJobIdItems() {
			ListBoxModel m = new ListBoxModel();
			try {
				EnvironmentManagerPluginDescriptor pluginDescriptor =
					EnvironmentManagerPlugin.getEnvironmentManagerPluginDescriptor();
				String emUrl = pluginDescriptor.getEmUrl();
				String username = pluginDescriptor.getUsername();
				Secret password = pluginDescriptor.getPassword();
				if (emUrl != null) {
					Jobs jobs = new JobsImpl(emUrl, username, password.getPlainText());
					JSONObject envs = jobs.getJobs();
					if (envs.has("jobs")) {
						JSONArray envArray = envs.getJSONArray("jobs");
						for (Object o : envArray) {
							JSONObject job = (JSONObject) o;
							String name = job.getString("name");
							m.add(name, job.getString("id"));
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return m;
		}
	}
}

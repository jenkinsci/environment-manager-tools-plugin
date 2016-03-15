/*
 * (C) Copyright ParaSoft Corporation 2016.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF ParaSoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.environmentmanager.jenkins;

import java.io.IOException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.parasoft.em.client.api.EventMonitor;
import com.parasoft.em.client.api.Jobs;
import com.parasoft.em.client.impl.JobsImpl;
import com.parasoft.environmentmanager.jenkins.EnvironmentManagerPlugin.EnvironmentManagerPluginDescriptor;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.Secret;
import hudson.util.ListBoxModel;

public class ExecuteJobBuilder extends Builder {
	private long jobId;

	@DataBoundConstructor
	public ExecuteJobBuilder(
		long jobId)
	{
		super();
		this.jobId = jobId;
	}

	public long getJobId() {
		return jobId;
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
		Jobs jobs = new JobsImpl(emUrl, username, password.getPlainText());
		JSONObject jobJSON = jobs.getJob(jobId);
		listener.getLogger().println("Executing \"" + jobJSON.getString("name") + "\" on " + emUrl);
		JSONObject history = jobs.executeJob(jobId);
		boolean result = jobs.monitorExecution(history, new EventMonitor() {
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

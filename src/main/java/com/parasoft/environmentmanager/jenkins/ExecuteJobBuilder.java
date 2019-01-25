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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.parasoft.dtp.client.api.Projects;
import com.parasoft.dtp.client.api.Services;
import com.parasoft.dtp.client.impl.ProjectsImpl;
import com.parasoft.dtp.client.impl.ServicesImpl;
import com.parasoft.em.client.api.EventMonitor;
import com.parasoft.em.client.api.Jobs;
import com.parasoft.em.client.impl.JobsImpl;
import com.parasoft.environmentmanager.jenkins.EnvironmentManagerPlugin.EnvironmentManagerPluginDescriptor;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
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
	private boolean publish;
	private long projectId;
	private String buildId;
	private String sessionTag;

	@DataBoundConstructor
	public ExecuteJobBuilder(
		long jobId,
		String jobName,
		String jobType,
		boolean publish,
		long projectId,
		String buildId,
		String sessionTag)
	{
		super();
		this.jobId = jobId;
		this.jobName = jobName;
		this.jobType = jobType;
		this.publish = publish;
		this.projectId = projectId;
		this.buildId = buildId;
		this.sessionTag = sessionTag;
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

	public boolean getPublish() {
		return publish;
	}

	public long getProjectId() {
		return projectId;
	}

	public String getBuildId() {
		return buildId;
	}

	public String getSessionTag() {
		return sessionTag;
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
						
						if(publish) {
						    result = publishReport(reportXmlFile, listener.getLogger()) && result;
						}
						
					} catch (IOException e) {
						// ignore exception: downloading report.xml was not supported prior to CTP 3.1.3
					}
				}
			}
		}
		return result;
	}
	
	private boolean publishReport(FilePath reportFile, PrintStream logger){
	    logger.println("Publishing Report to DTP...");
	    boolean result = true;
	    EnvironmentManagerPluginDescriptor pluginDescriptor = EnvironmentManagerPlugin.getEnvironmentManagerPluginDescriptor();
        String dtp = pluginDescriptor.getDtpUrl();
        String username = pluginDescriptor.getDtpUsername();
        Secret password = pluginDescriptor.getDtpPassword();
        Services services = new ServicesImpl(dtp, username, password.getPlainText());
        String dataCollector = null;
        try {
            dataCollector = services.getDataCollectorV2();
            logger.println("Connecting to DataCollector at: " + dataCollector);
            result = postToDataCollector(reportFile, dataCollector, username, password.getPlainText(), logger);
        } catch (IOException e) {
            result = false;
            e.printStackTrace();
        }
        if (result) {
            logger.println("Successfully published report to DTP.");
        } else {
            logger.println("ERROR: Unable to publish report to DTP.");
        }
        
        return result;
	}
	
	private boolean postToDataCollector(FilePath reportFile, String dataCollector, String username, String password, PrintStream logger) {
	    boolean result = true;
        try {
            result = Boolean.valueOf(reportFile.act(new DataCollectorUploadable(dataCollector, username, password)));
        } catch (IOException e) {
            result = false;
            e.printStackTrace();
        } catch (InterruptedException e) {
            result = false;
            e.printStackTrace();
        } 
        return result;
	}
	
	private static class DataCollectorUploadable implements FileCallable<String> {
	    private static final long serialVersionUID = 1L;
	    private String url;
	    private String username;
	    private String password;
	    
	    public DataCollectorUploadable(String url,String username, String password) {
	        this.url = url;
	        this.username = username;
	        this.password = password;
	    }
	    
	    @Override
	    public String invoke(File file, VirtualChannel channel) throws IOException, InterruptedException {
            HttpEntity entity = MultipartEntityBuilder.create().setContentType(ContentType.MULTIPART_FORM_DATA)
                    .addPart("file", new FileBody(file))
                    .build();
            HttpPost request = new HttpPost(url);
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
            request.setEntity(entity);
            HttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(credsProvider).build();
            HttpResponse response = client.execute(request);
            return Boolean.toString(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK);
	    }
        @Override
        public void checkRoles(RoleChecker checker) throws SecurityException {
            //no role check needed
        }
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
				if ((emUrl != null) && !emUrl.isEmpty()) {
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
					if (m.isEmpty()) {
						m.add("-- No jobs defined --");
					}
				} else {
					m.add("-- Global settings not configured --");
				}
			} catch (IOException e) {
				e.printStackTrace();
				if (m.isEmpty()) {
					m.add("-- Cannot connect to CTP --");
				}
			}
			return m;
		}

		public ListBoxModel doFillProjectIdItems() {
			ListBoxModel m = new ListBoxModel();
			try {
				EnvironmentManagerPluginDescriptor pluginDescriptor =
					EnvironmentManagerPlugin.getEnvironmentManagerPluginDescriptor();
				String dtpUrl = pluginDescriptor.getDtpUrl();
				String dtpUsername = pluginDescriptor.getDtpUsername();
				Secret dtpPassword = pluginDescriptor.getDtpPassword();
				if ((dtpUrl != null) && !dtpUrl.isEmpty()) {
					Projects projects = new ProjectsImpl(dtpUrl, dtpUsername, dtpPassword.getPlainText());
					for (JSONObject proj : projects.getProjects()) {
						String name = proj.getString("name");
						m.add(name, proj.getString("id"));
					}
					if (m.isEmpty()) {
						m.add("-- No projects defined --");
					}
				} else {
					m.add("-- Global settings not configured --");
				}
			} catch (IOException e) {
				e.printStackTrace();
				if (m.isEmpty()) {
					m.add("-- Cannot connect to DTP --");
				}
			}
			return m;
		}
	}
}

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
import java.io.InputStream;
import java.io.PrintStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
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
import hudson.model.Result;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.Secret;
import hudson.util.ListBoxModel.Option;
import hudson.util.ListBoxModel;

public class ExecuteJobBuilder extends Builder {

	private static final String JOB_BY_ID = "jobById";
	private static final String JOB_BY_NAME = "jobByName";

	private long jobId;
	private String jobName;
	private String jobType;
	private boolean abortOnFailure;
	private boolean abortOnTimeout;
	private int timeoutMinutes;
	private boolean publish;
	private long projectId;
	private String buildId;
	private String sessionTag;
	private boolean appendEnv;

	@DataBoundConstructor
	public ExecuteJobBuilder(
		long jobId,
		String jobName,
		String jobType,
		boolean abortOnFailure,
		boolean abortOnTimeout,
		int timeoutMinutes,
		boolean publish,
		long projectId,
		String buildId,
		String sessionTag,
		boolean appendEnv)
	{
		super();
		this.jobId = jobId;
		this.jobName = jobName;
		this.jobType = jobType;
		this.abortOnFailure = abortOnFailure;
		this.abortOnTimeout = abortOnTimeout;
		this.timeoutMinutes = timeoutMinutes;
		this.publish = publish;
		this.projectId = projectId;
		this.buildId = buildId;
		this.sessionTag = sessionTag;
		this.appendEnv = appendEnv;
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

	public boolean getAbortOnFailure() {
		return abortOnFailure;
	}

	public boolean getAbortOnTimeout() {
		return abortOnTimeout;
	}

	public int getTimeoutMinutes() {
		return timeoutMinutes <= 0 ? 60 : timeoutMinutes;
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

	public boolean getAppendEnv() {
		return appendEnv;
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
		boolean allTestsPassed = true;
		long startTime = System.currentTimeMillis();
		for (JSONObject jobJSON : jobsToExecute) {
			listener.getLogger().println("Executing \"" + jobJSON.getString("name") + "\" on " + emUrl);
			JSONObject history = jobs.executeJob(jobJSON.getLong("id"));
			allTestsPassed &= jobs.monitorExecution(history, new EventMonitor() {
				public void logMessage(String message) {
					listener.getLogger().println(message);
				}
			}, abortOnTimeout ? getTimeoutMinutes() : 0);
			String baseUrl = emUrl;
			if (!baseUrl.endsWith("/")) {
				baseUrl += "/";
			}
			history = jobs.getHistory(jobJSON.getLong("id"), history.getLong("id"));
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
			JSONArray reportIds = history.optJSONArray("reportIds");
			if (reportIds != null) {
				List<String> execEnvs = extractEnvironmentNames(jobJSON);
				for (int i = 0; i < reportIds.size(); i++) {
					String name = jobJSON.getString("name");
					Long reportId = reportIds.getLong(i);
					String reportUrl = baseUrl + "testreport/" + reportId + "/report.html";
					int tests = 1;
					int failures = result ? 0 : 1;
					FilePath workspace = build.getWorkspace();
					if (workspace == null) {
						build.addAction(new ProvisioningEventAction(build, name, reportUrl, tests, failures));
						continue;
					}
					ReportScanner reportScanner = null;
					HTMLReportScanner collector = null;
					InputStream reportInputStream = null;
					String execEnv = null;
					try {
						boolean connectToDTP = true;
						reportInputStream = jobs.download("testreport/" + reportIds.getLong(i) + "/report.xml");
						InputStream htmlReport = jobs.download("testreport/" + reportIds.getLong(i) + "/report.html");
						collector = new HTMLReportScanner(htmlReport);
						reportScanner = new ReportScanner(reportInputStream);
						reportInputStream = reportScanner;
						String projectName = null,
								expandedBuildId = null,
								expandedSessionTag = null;
						if (!execEnvs.isEmpty()) {
							execEnv = execEnvs.remove(0);
						}
						if ((execEnv != null) && !execEnv.isEmpty()) {
							name = execEnv;
						} else {
							name = jobJSON.getString("name");
						}
						if (!appendEnv) {
							execEnv = null;
						}
						String dtpUrl = pluginDescriptor.getDtpUrl();
						if ((dtpUrl != null) && !dtpUrl.isEmpty()) {
							String dtpUsername = pluginDescriptor.getDtpUsername();
							Secret dtpPassword = pluginDescriptor.getDtpPassword();
							try {
								Projects projects = new ProjectsImpl(dtpUrl, dtpUsername, dtpPassword.getPlainText());
								projectName = projects.getProject(projectId).getString("name");
								expandedBuildId = envVars.expand(buildId);
								expandedSessionTag = envVars.expand(sessionTag);
								if ((execEnv != null) && !execEnv.isEmpty()) {
									if (expandedSessionTag == null) {
										expandedSessionTag = "";
									}
									if (!expandedSessionTag.isEmpty()) {
										expandedSessionTag += '-';
									}
									expandedSessionTag += execEnv;
								} else if ((expandedSessionTag != null) && !expandedSessionTag.isEmpty() && (i > 0)) {
									expandedSessionTag += "-" + (i + 1); // unique session tag in DTP for each report
								}
								reportInputStream = new ReportSettingsInjector(
									projectName,
									expandedBuildId,
									expandedSessionTag,
									execEnv,
									reportInputStream);
							} catch (IOException e) {
								connectToDTP = false;
								if (publish) {
									listener.getLogger().println("Cannot connect to DTP: " + e.getLocalizedMessage());
								}
							}
						}
						FilePath reportDir = new FilePath(workspace, "target/parasoft/soatest/" + reportIds.getLong(i));
						reportDir.mkdirs();
						FilePath reportXMLFile = new FilePath(reportDir, "report.xml");
						FilePath reportHTMLFile = new FilePath(reportDir, "report.html");
						reportXMLFile.copyFrom(reportInputStream);
						reportHTMLFile.copyFrom(collector);
						Set<String> images = collector.getImages();
						String prefix = "";
						for (String image : images) {
						    if ("rep_header_logo_x10.png".equals(image)) {
						        //when executing jobs that run against the soavirt war file
						        //the images are under a special directory 'archive'
						        prefix = "archive/";
						        break;
						    }
						}
						for (String image : collector.getImages()) {
							InputStream stream = jobs.download("testreport/" + reportIds.getLong(i) + "/" + prefix + image);
							new FilePath(reportDir, image).copyFrom(stream);
						}
						if (publish) { 
						    //fail the job if publish is enabled but cannot connect to DTP
						    if (connectToDTP) {
						        listener.getLogger().println("Publishing Report to DTP...");
    					        listener.getLogger().println("Project: " + (projectName == null || projectName.isEmpty() ? "Not Specified" : projectName));    
    					        listener.getLogger().println("Build ID: " + (expandedBuildId == null  || expandedBuildId.isEmpty() ? "Not Specified" : expandedBuildId));
    					        listener.getLogger().println("Session Tag: " + (expandedSessionTag == null ||expandedSessionTag.isEmpty() ?  "Not Specified" : expandedSessionTag));
    						    result = publishReport(reportXMLFile, listener.getLogger()) && result;
						    } else {
						        result = false;
						    }
						}
						
					} catch (IOException e) {
						// ignore exception: downloading report.xml was not supported prior to CTP 3.1.3
					} finally {
						if (reportInputStream != null) {
							reportInputStream.close();
						}
						if (collector != null) {
						    collector.close();
						}
					}
					if (reportScanner != null) {
						if (reportScanner.getFailureCount() > 0) {
							failures = reportScanner.getFailureCount();
						}
						if (reportScanner.getTotalCount() > 0) {
							tests = reportScanner.getTotalCount();
						}
					}
					JSONArray testsArray = historyMap.get(reportId);
					if (testsArray != null) {
						for (int j = 0; j < testsArray.size(); j++) {
							JSONObject test = testsArray.getJSONObject(j);
							String testName = test.getString("name") + ".tst";
							if ((execEnv != null) && !execEnv.isEmpty()) {
								testName += " [" + execEnv + ']';
							}
							int failed = 0;
							if ("FAILED".equals(test.getString("status"))) {
								failed = 1;
							}
							build.addAction(new ProvisioningEventAction(build, testName, reportUrl, 1, failed));
						}
					} else {
						build.addAction(new ProvisioningEventAction(build, name, reportUrl, tests, failures));
					}
				}
			}
		}
		if (!allTestsPassed) {
			if (abortOnFailure ||
				(abortOnTimeout && ((System.currentTimeMillis() - startTime) / 60000 > timeoutMinutes)))
			{
				build.setResult(Result.FAILURE);
				result = false;
			} else {
				build.setResult(Result.UNSTABLE);
			}
		}
		return result;
	}

	private List<String> extractEnvironmentNames(JSONObject jobJSON) {
		boolean separate = jobJSON.getBoolean("fork");
		JSONArray tests = jobJSON.getJSONArray("testScenarioInstances");
		Long lastTestId = null;
		List<String> results = new ArrayList<String>();
		for (int i = 0; i < tests.size(); i++) {
			JSONObject test = tests.getJSONObject(i);
			Long testId = test.getLong("testScenarioId");
			String variableset = test.optString("variableSet");
			if (separate || (lastTestId == null) || lastTestId.equals(testId)) {
				results.add(variableset);
			}
			lastTestId = testId;
		}
		return results;
	}

	private boolean publishReport(FilePath reportFile, PrintStream logger) {
	    boolean result = true;
	    EnvironmentManagerPluginDescriptor pluginDescriptor = EnvironmentManagerPlugin.getEnvironmentManagerPluginDescriptor();
        String dtp = pluginDescriptor.getDtpUrl();
        String username = pluginDescriptor.getDtpUsername();
        Secret password = pluginDescriptor.getDtpPassword();
        Services services = new ServicesImpl(dtp, username, password.getPlainText());
        String dataCollector = null;
        try {
            dataCollector = services.getDataCollectorV2();
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
            HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
            SSLContextBuilder sslContextBuilder = new SSLContextBuilder();
            try {
                sslContextBuilder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
                SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                    sslContextBuilder.build(), new NoopHostnameVerifier());
                httpClientBuilder.setSSLSocketFactory(sslsf);
            } catch (KeyManagementException e) {
                e.printStackTrace();
            } catch (KeyStoreException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            HttpClient client = httpClientBuilder.setDefaultCredentialsProvider(credsProvider).build();
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
						m.add("-- No jobs defined --", "0");
					} else {
						m.add(0, new Option("-- Select a job --", "0"));
					}
				} else {
					m.add("-- Global settings not configured --", "0");
				}
			} catch (IOException e) {
				e.printStackTrace();
				if (m.isEmpty()) {
					m.add("-- Cannot connect to CTP --", "0");
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
						m.add("-- No projects defined --", "0");
					} else {
						m.add(0, new Option("-- Select a project --", "0"));
					}
				} else {
					m.add("-- Global settings not configured --", "0");
				}
			} catch (IOException e) {
				e.printStackTrace();
				if (m.isEmpty()) {
					m.add("-- Cannot connect to DTP --", "0");
				}
			}
			return m;
		}
	}
}

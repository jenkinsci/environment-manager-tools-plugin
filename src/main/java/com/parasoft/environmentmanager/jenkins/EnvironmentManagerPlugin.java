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

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.parasoft.dtp.client.api.Services;
import com.parasoft.dtp.client.impl.ServicesImpl;
import com.parasoft.em.client.api.Environments;
import com.parasoft.em.client.impl.EnvironmentsImpl;

import hudson.Extension;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

public class EnvironmentManagerPlugin extends JobProperty<Job<?, ?>> {

	@Override
	public EnvironmentManagerPluginDescriptor getDescriptor() {
		Jenkins instance = Jenkins.getInstance();
		if (instance == null) {
			return null;
		}
		return (EnvironmentManagerPluginDescriptor) instance.getDescriptor(getClass());
	}

	public static EnvironmentManagerPluginDescriptor getEnvironmentManagerPluginDescriptor() {
		Jenkins instance = Jenkins.getInstance();
		if (instance == null) {
			return null;
		}
		return (EnvironmentManagerPluginDescriptor) instance.getDescriptor(EnvironmentManagerPlugin.class);
	}

	@Extension
	public static final class EnvironmentManagerPluginDescriptor extends JobPropertyDescriptor {

		private String emUrl;
		private String username;
		private Secret password;
		private String dtpUrl;
		private String dtpUsername;
		private Secret dtpPassword;

		public EnvironmentManagerPluginDescriptor() {
			super(EnvironmentManagerPlugin.class);
			load();
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			String oldEmUrl = emUrl;
			String oldUsername = username;
			Secret oldPassword = password;
			String oldDtpUrl = dtpUrl;
			String oldDtpUsername = dtpUsername;
			Secret oldDtpPassword = dtpPassword;
			emUrl = formData.getString("emUrl");
			username = formData.getString("username");
			password = Secret.fromString(formData.getString("password"));
			dtpUrl = formData.getString("dtpUrl");
			dtpUsername = formData.getString("dtpUsername");
			dtpPassword = Secret.fromString(formData.getString("dtpPassword"));
			if (emUrl.equals(oldEmUrl) &&
				username.equals(oldUsername) &&
				password.equals(oldPassword) &&
				dtpUrl.equals(oldDtpUrl) &&
				dtpUsername.equals(oldDtpUsername) &&
				dtpPassword.equals(oldDtpPassword))
			{
				// nothing changed so don't test connection and don't save anything
				return true;
			}
			// Test the emUrl, appending "/em" if necessary
			try {
				Environments environments = new EnvironmentsImpl(emUrl, username, password.getPlainText());
				environments.getEnvironments();
			} catch (IOException e) {
				// First try to re-run while appending the default context path /em
				String testUrl = emUrl;
				if (testUrl.endsWith("/")) {
					testUrl += "em";
				} else {
					testUrl += "/em";
				}
				try {
					Environments environments = new EnvironmentsImpl(testUrl, username, password.getPlainText());
					environments.getEnvironments();
					emUrl = testUrl;
				} catch (IOException e2) {
					throw new FormException("Unable to connect to the Continuous Testing Platform at " + emUrl, "emUrl");
				}
			}
			req.bindJSON(this, formData);
			save();
			return super.configure(req,formData);
		}

		@DataBoundConstructor
		public EnvironmentManagerPluginDescriptor(String emUrl, String username, Secret password) {
			
			this.emUrl = emUrl;
			this.username = username;
			this.password = password;
		}

		@Override
		public String getDisplayName() {
			return "Parasoft Continuous Testing Platform";
		}

		public String getEmUrl() {
			return emUrl;
		}

		public String getUsername() {
			return username;
		}

		public Secret getPassword() {
			return password;
		}

		public String getDtpUrl() {
			return dtpUrl;
		}

		public String getDtpUsername() {
			return dtpUsername;
		}

		public Secret getDtpPassword() {
			return dtpPassword;
		}

		public FormValidation doTestConnection(@QueryParameter String emUrl, @QueryParameter String username, @QueryParameter String password) {
			boolean emApiV1 = false;
			Secret secret = Secret.fromString(password);
			try {
				Environments environments = new EnvironmentsImpl(emUrl, username, secret.getPlainText());
				environments.getEnvironmentsV1();
				emApiV1 = true;
				environments.getEnvironments();
			} catch (IOException e) {
				// First try to re-run while appending /em
				if (emUrl.endsWith("/")) {
					emUrl += "em";
				} else {
					emUrl += "/em";
				}
				try {
					Environments environments = new EnvironmentsImpl(emUrl, username, secret.getPlainText());
					environments.getEnvironmentsV1();
					emApiV1 = true;
					environments.getEnvironments();
					return FormValidation.ok("Successfully connected to Continuous Testing Platform");
				} catch (IOException e2) {
					// return the original exception
				}
				if (emApiV1) {
					return FormValidation.error("Continuous Testing Platform version 3.0 or higher is required.");
				}
				return FormValidation.error(e, "Unable to connect to Continuous Testing Platform");
			}
			return FormValidation.ok("Successfully connected to Continuous Testing Platform");
		}

		public FormValidation doDtpTestConnection(@QueryParameter String dtpUrl, @QueryParameter String dtpUsername, @QueryParameter String dtpPassword) {
			Secret secret = Secret.fromString(dtpPassword);
			try {
				Services services = new ServicesImpl(dtpUrl, dtpUsername, secret.getPlainText());
				services.getServices();
			} catch (IOException e) {
				return FormValidation.error(e, "Unable to connect to DTP");
			}
			return FormValidation.ok("Successfully connected to DTP");
		}
	}
}

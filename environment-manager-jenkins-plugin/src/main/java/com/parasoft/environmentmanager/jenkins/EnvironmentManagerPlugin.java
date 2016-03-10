package com.parasoft.environmentmanager.jenkins;

import java.io.IOException;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

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
		return (EnvironmentManagerPluginDescriptor) Jenkins.getInstance().getDescriptor(getClass());
	}

	public static EnvironmentManagerPluginDescriptor getEnvironmentManagerPluginDescriptor() {
		return (EnvironmentManagerPluginDescriptor) Jenkins.getInstance().getDescriptor(EnvironmentManagerPlugin.class);
	}

	@Extension
	public static final class EnvironmentManagerPluginDescriptor extends JobPropertyDescriptor {

		private String emUrl;
		private String username;
		private Secret password;

		public EnvironmentManagerPluginDescriptor() {
			super(EnvironmentManagerPlugin.class);
			load();
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
			emUrl = formData.getString("emUrl");
			username = formData.getString("username");
			password = Secret.fromString(formData.getString("password"));
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
					throw new FormException("Unable to connect to Environment Manager at " + emUrl, "emUrl");
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
			return "Parasoft Environment Manager";
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

		public FormValidation doTestConnection(@QueryParameter String emUrl, @QueryParameter String username, @QueryParameter String password) {
			Secret secret = Secret.fromString(password);
			try {
				Environments environments = new EnvironmentsImpl(emUrl, username, secret.getPlainText());
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
					environments.getEnvironments();
					return FormValidation.ok("Successfully connected to Environment Manager");
				} catch (IOException e2) {
					// return the original exception
				}
				return FormValidation.error(e, "Unable to connect to Environment Manager Server");
			}
			return FormValidation.ok("Successfully connected to Environment Manager");
		}
	}

}

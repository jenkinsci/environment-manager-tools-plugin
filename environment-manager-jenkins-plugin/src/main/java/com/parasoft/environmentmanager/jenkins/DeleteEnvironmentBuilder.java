/*
 * (C) Copyright ParaSoft Corporation 2013.  All rights reserved.
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

import com.parasoft.em.client.api.Environments;
import com.parasoft.em.client.api.Systems;
import com.parasoft.em.client.impl.EnvironmentsImpl;
import com.parasoft.em.client.impl.SystemsImpl;
import com.parasoft.environmentmanager.jenkins.EnvironmentManagerPlugin.EnvironmentManagerPluginDescriptor;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.Secret;
import hudson.util.ListBoxModel;

public class DeleteEnvironmentBuilder extends Builder {
	private int systemId;
	private String environmentName;

	@DataBoundConstructor
	public DeleteEnvironmentBuilder(
		int systemId,
		String environmentName)
	{
		super();
		this.systemId = systemId;
		this.environmentName = environmentName;
	}

	public int getSystemId() {
		return systemId;
	}

	public String getEnvironmentName() {
		return environmentName;
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
		String nameToDelete = envVars.expand(environmentName);
		Environments environments = new EnvironmentsImpl(emUrl, username, password.getPlainText());
		JSONObject environmentsJSON = environments.getEnvironments();
		JSONArray environmentsJSONArray = environmentsJSON.getJSONArray("environments");
		boolean error = false;
		boolean match = false;
		for (int i = 0; i < environmentsJSONArray.size(); i++) {
			try {
				JSONObject environmentJSON = environmentsJSONArray.getJSONObject(i);
				if (environmentJSON.getLong("systemId") != systemId) {
					continue;
				}
				if (!environmentJSON.getString("name").equals(nameToDelete)) {
					continue;
				}
				match = true;
				listener.getLogger().println("Deleting environment \"" +
					environmentJSON.getString("name") + "\" (id " + environmentJSON.getLong("id") + ")...");
				environments.deleteEnvironment(environmentJSON.getLong("id"), true);
			} catch (IOException e) {
				listener.getLogger().println(e.getMessage());
				error = true;
			}
		}
		if (!match) {
			listener.getLogger().println("No environments within the selected system match name:  " + nameToDelete);
		}
		return match && !error;
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
            return "Destroy an environment";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            req.bindJSON(this, json);
            save();
            return super.configure(req, json);
        }

        public ListBoxModel doFillSystemIdItems() {
            ListBoxModel m = new ListBoxModel();
            try {
                EnvironmentManagerPluginDescriptor pluginDescriptor =
                    EnvironmentManagerPlugin.getEnvironmentManagerPluginDescriptor();
                String emUrl = pluginDescriptor.getEmUrl();
                String username = pluginDescriptor.getUsername();
                Secret password = pluginDescriptor.getPassword();
                if (emUrl != null) {
                    Systems systems = new SystemsImpl(emUrl, username, password.getPlainText());
                    JSONObject envs = systems.getSystems();
                    if (envs.has("systems")) {
                        JSONArray envArray = envs.getJSONArray("systems");
                        for (Object o : envArray) {
                            JSONObject system = (JSONObject) o;
                            String name = system.getString("name");
                            if (system.has("version")) {
                                name += " (" + system.getString("version") + ")";
                            }
                            m.add(name, system.getString("id"));
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

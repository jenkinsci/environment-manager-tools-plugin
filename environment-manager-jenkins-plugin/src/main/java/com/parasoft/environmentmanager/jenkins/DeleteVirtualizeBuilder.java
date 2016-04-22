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

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.parasoft.em.client.api.Servers;
import com.parasoft.em.client.impl.ServersImpl;
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

public class DeleteVirtualizeBuilder extends Builder {

	private String serverType;
	private String serverHost;
	private String serverName;

	@DataBoundConstructor
	public DeleteVirtualizeBuilder(
		String serverType,
		String serverHost,
		String serverName)
	{
		super();
		this.serverType = serverType;
		this.serverHost = serverHost;
		this.serverName = serverName;
	}

	public boolean isServerType(String type) {
		if ((serverType == null) || serverType.isEmpty()) {
			return "host".equals(type);
		}
		return serverType.equals(type);
	}

	public String getServerHost() {
		return serverHost;
	}

	public String getServerName() {
		return serverName;
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
		String hostToDelete = envVars.expand(serverHost);
		String nameToDelete = envVars.expand(serverName);
		Servers servers = new ServersImpl(emUrl, username, password.getPlainText());
		JSONObject serversJSON = servers.getServers();
		JSONArray serversJSONArray = serversJSON.getJSONArray("servers");
		boolean error = false;
		boolean match = false;
		for (int i = 0; i < serversJSONArray.size(); i++) {
			try {
				JSONObject serverJSON = serversJSONArray.getJSONObject(i);
				if (isServerType("host") && !serverJSON.getString("host").equals(hostToDelete)) {
					continue;
				}
				if (isServerType("name") && !serverJSON.getString("name").equals(nameToDelete)) {
					continue;
				}
				match = true;
				listener.getLogger().println("Disconnecting Virtualize server \"" +
					serverJSON.getString("name") + "\" (" + serverJSON.getString("host") + ':' + serverJSON.getInt("port") + ")...");
				servers.deleteServer(serverJSON.getLong("id"));
			} catch (IOException e) {
				listener.getLogger().println(e.getMessage());
				error = true;
			}
		}
		if (!match) {
			if (isServerType("host")) {
				listener.getLogger().println("No Virtualize servers match host:  " + hostToDelete);
			}
			if (isServerType("name")) {
				listener.getLogger().println("No Virtualize servers match name:  " + nameToDelete);
			}
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
            return "Disconnect a Virtualize server";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            req.bindJSON(this, json);
            save();
            return super.configure(req, json);
        }
    }
}

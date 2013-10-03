/*
 * $RCSfile$
 * $Revision$
 *
 * Comments:
 *
 * (C) Copyright ParaSoft Corporation 2013.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF ParaSoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 *
 * $Author$          $Locker$
 * $Date$
 * $Log$
 */
package com.parasoft.environmentmanager.jenkins;

import java.io.IOException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.parasoft.em.client.api.Environments;
import com.parasoft.em.client.api.EventMonitor;
import com.parasoft.em.client.api.Provisions;
import com.parasoft.em.client.api.Systems;
import com.parasoft.em.client.impl.EnvironmentsImpl;
import com.parasoft.em.client.impl.ProvisionsImpl;
import com.parasoft.em.client.impl.SystemsImpl;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.Secret;
import hudson.util.ListBoxModel;

public class EnvironmentManagerBuilder extends Builder {
    private int systemId;
    private int environmentId;
    private int instanceId;
    private boolean abortOnFailure;
    
    @DataBoundConstructor
    public EnvironmentManagerBuilder(int systemId, int environmentId,
            int instanceId, boolean abortOnFailure) {
        super();
        this.systemId = systemId;
        this.environmentId = environmentId;
        this.instanceId = instanceId;
        this.abortOnFailure = abortOnFailure;
    }
    
    public int getSystemId() {
        return systemId;
    }
    
    public int getEnvironmentId() {
        return environmentId;
    }
    
    public int getInstanceId() {
        return instanceId;
    }
    
    public boolean isAbortOnFailure() {
        return abortOnFailure;
    }
    
    @Override
    public boolean perform(final AbstractBuild<?, ?> build, Launcher launcher,
            final BuildListener listener) throws InterruptedException, IOException {
        listener.getLogger().println("Executing provisioning action on " + getDescriptor().getEmUrl());
        Provisions provisions = new ProvisionsImpl(getDescriptor().getEmUrl(), getDescriptor().getUsername(), getDescriptor().getPassword().getPlainText());
        JSONObject event = provisions.createProvisionEvent(environmentId, instanceId, abortOnFailure);
        boolean result = provisions.monitorEvent(event, new EventMonitor() {
            public void logMessage(String message) {
                listener.getLogger().println(message);
            }
        });
        String baseUrl = getDescriptor().getEmUrl();
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        
        JSONObject eventResult = provisions.getProvisions(event.getInt("eventId"));
        JSONArray steps = eventResult.getJSONArray("steps");
        int failed = 0;
        for (int i = 0; i < steps.size(); i++) {
            JSONObject step = steps.getJSONObject(i);
            if ("error".equals(step.getString("result"))) {
                failed++;
            }
        }
        
        Environments environments = new EnvironmentsImpl(getDescriptor().getEmUrl(), getDescriptor().getUsername(), getDescriptor().getPassword().getPlainText());
        JSONObject instance = environments.getEnvironmentInstance(environmentId, instanceId);
        String environmentUrl = baseUrl + "environments/" + environmentId;
        build.addAction(new ProvisioningEventAction(build, instance.getString("name"), environmentUrl, steps.size(), failed));
        return result;
    }
    
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }
    
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        private String emUrl;
        private String username;
        private Secret password;
        
        public DescriptorImpl() {
            load();
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

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Parasoft Environment Manager";
        }
        
        @Override
        public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
            emUrl = json.getString("emUrl");
            username = json.getString("username");
            password = Secret.fromString(json.getString("password"));
            
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
            
            save();
            return super.configure(req, json);
        }
        
        public ListBoxModel doFillSystemIdItems() {
            ListBoxModel m = new ListBoxModel();
            try {
                if (emUrl != null) {
                    Systems systems = new SystemsImpl(emUrl, username, password.getPlainText());
                    JSONObject envs = systems.getSystems();
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
            } catch (IOException e) {
                e.printStackTrace();
            }
            return m;
        }
        
        public ListBoxModel doFillEnvironmentIdItems(@QueryParameter int systemId) {
            ListBoxModel m = new ListBoxModel();
            try {
                if (emUrl != null) {
                    Environments environments = new EnvironmentsImpl(emUrl, username, password.getPlainText());
                    JSONObject envs = environments.getEnvironments();
                    JSONArray envArray = envs.getJSONArray("environments");
                    for (Object o : envArray) {
                        JSONObject env = (JSONObject) o;
                        if (env.getInt("systemId") == systemId) {
                            String name = env.getString("name");
                            if (env.has("version")) {
                                name += " (" + env.getString("version") + ")";
                            }
                            m.add(name, env.getString("id"));
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return m;
        }
        
        public ListBoxModel doFillInstanceIdItems(@QueryParameter int environmentId){
            ListBoxModel m = new ListBoxModel();
            try {
                Environments environments = new EnvironmentsImpl(emUrl, username, password.getPlainText());
                JSONObject instances = environments.getEnvironmentInstances(environmentId);
                if (instances.has("instances")) {
                    JSONArray instArray = instances.getJSONArray("instances");
                    for (Object o : instArray) {
                        JSONObject inst = (JSONObject) o;
                        m.add(inst.getString("name"), inst.getString("id"));
                    }
                }
            } catch (IOException e) {
            }
            return m;
        }
    }
}

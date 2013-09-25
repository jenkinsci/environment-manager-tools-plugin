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
import com.parasoft.em.client.api.Provisions;
import com.parasoft.em.client.impl.EnvironmentsImpl;
import com.parasoft.em.client.impl.ProvisionsImpl;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

public class EnvironmentManagerBuilder extends Builder {
    private int environmentId;
    private int instanceId;
    private boolean abortOnFailure;
    
    @DataBoundConstructor
    public EnvironmentManagerBuilder(int environmentId,
            int instanceId, boolean abortOnFailure) {
        super();
        this.environmentId = environmentId;
        this.instanceId = instanceId;
        this.abortOnFailure = abortOnFailure;
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
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
            BuildListener listener) throws InterruptedException, IOException {
        listener.getLogger().println("Executing provisioning action on " + getDescriptor().getEmUrl());
        
        Provisions provisions = new ProvisionsImpl(getDescriptor().getEmUrl(), getDescriptor().getUsername(), getDescriptor().getPassword());
        JSONObject response = provisions.createProvisionEvent(environmentId, instanceId, abortOnFailure);
        int id = response.getInt("eventId");
        listener.getLogger().println("Provisioning event id: " + id);
        
        boolean failed = false;
        response = provisions.getProvisions(id);
        JSONArray steps = response.getJSONArray("steps");
        for (int i = 0; i < steps.size(); i++) {
            JSONObject step = provisions.getProvisions(id).getJSONArray("steps").getJSONObject(i);
            listener.getLogger().println("Running step #" + (i + 1));
            String result = step.getString("result");
            while ("running".equals(result)) {
                Thread.sleep(1000);
                listener.getLogger().println(step.getString("percent") + "%");
                step = provisions.getProvisions(id).getJSONArray("steps").getJSONObject(i);
                result = step.getString("result");
                failed |= "error".equals(result);
            }
        }
        
        listener.getLogger().println("Completed provisioning event with id: " + id);
        return !failed;
    }
    
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }
    
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        private String emUrl;
        private String username;
        private String password;
        
        public DescriptorImpl() {
            load();
        }
        
        public String getEmUrl() {
            return emUrl;
        }
        
        public String getUsername() {
            return username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public FormValidation doTestConnection(@QueryParameter String emUrl, @QueryParameter String username, @QueryParameter String password) {
            try {
                Environments environments = new EnvironmentsImpl(emUrl, username, password);
                environments.getEnvironments();
            } catch (IOException e) {
                // First try to re-run while appending /em
                if (emUrl.endsWith("/")) {
                    emUrl += "em";
                } else {
                    emUrl += "/em";
                }
                try {
                    Environments environments = new EnvironmentsImpl(emUrl, username, password);
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
        public boolean configure(StaplerRequest req, JSONObject json)
                throws hudson.model.Descriptor.FormException {
            emUrl = json.getString("emUrl");
            username = json.getString("username");
            password = json.getString("password");
            
            // Test the emUrl, appending "/em" if necessary
            try {
                Environments environments = new EnvironmentsImpl(emUrl, username, password);
                environments.getEnvironments();
            } catch (IOException e) {
                // First try to re-run while appending the default context path /em
                if (emUrl.endsWith("/")) {
                    emUrl += "em";
                } else {
                    emUrl += "/em";
                }
                try {
                    Environments environments = new EnvironmentsImpl(emUrl, username, password);
                    environments.getEnvironments();
                    // Override the url with the default context path
                    json.put("emUrl", emUrl);
                } catch (IOException e2) {
                }
            }
            
            save();
            return super.configure(req, json);
        }

        public ListBoxModel doFillEnvironmentIdItems() {
            ListBoxModel m = new ListBoxModel();
            try {
                if (emUrl != null) {
                    Environments environments = new EnvironmentsImpl(emUrl, username, password);
                    JSONObject envs = environments.getEnvironments();
                    JSONArray envArray = envs.getJSONArray("environments");
                    for (Object o : envArray) {
                        JSONObject env = (JSONObject) o;
                        String name = env.getString("name");
                        if (env.has("version")) {
                            name += " (" + env.getString("version") + ")";
                        }
                        m.add(name, env.getString("id"));
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
                Environments environments = new EnvironmentsImpl(emUrl, username, password);
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

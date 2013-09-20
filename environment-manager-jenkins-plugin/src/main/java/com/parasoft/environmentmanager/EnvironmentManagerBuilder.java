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
package com.parasoft.environmentmanager;

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
    
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
            BuildListener listener) throws InterruptedException, IOException {
        listener.getLogger().println(getDescriptor().getEmUrl());
        
        Provisions provisions = new ProvisionsImpl(getDescriptor().getEmUrl(), getDescriptor().getUsername(), getDescriptor().getPassword());
        JSONObject result = provisions.createProvisionEvent(environmentId, instanceId, abortOnFailure);
        int id = result.getInt("eventId");
        listener.getLogger().println("Provisioning event id: " + id);
        
        result = provisions.getProvisions(id);
        
        String status = result.getString("status");
        String percent = "0";
        while ("running".equals(status) && !"100".equals(percent)) {
            Thread.sleep(1000);
            JSONObject response = provisions.getProvisions(id);
            status = response.getString("status");
            JSONArray steps = response.getJSONArray("steps");
            JSONObject step = (JSONObject) steps.get(0);
            percent = step.getString("percent");
            listener.getLogger().println(percent + "%");
        }
        listener.getLogger().println("Completed provisioning event with id: " + id);
        return true;
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
                        m.add(env.getString("name"), env.getString("id"));
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

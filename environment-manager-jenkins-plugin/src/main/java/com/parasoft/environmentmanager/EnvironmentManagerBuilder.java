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

import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.parasoft.em.client.api.Provisions;
import com.parasoft.em.client.impl.ProvisionsImpl;
import com.parasoft.environmentmanager.HelloWorldBuilder.DescriptorImpl;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;

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
            // TODO Auto-generated method stub
            emUrl = json.getString("emUrl");
            username = json.getString("username");
            password = json.getString("password");
            
            save();
            return super.configure(req, json);
        }
        
    }
}

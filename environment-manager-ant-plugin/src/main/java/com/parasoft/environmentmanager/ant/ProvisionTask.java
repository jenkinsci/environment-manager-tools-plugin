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
package com.parasoft.environmentmanager.ant;

import java.io.IOException;

import net.sf.json.JSONObject;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.parasoft.em.client.api.Environments;
import com.parasoft.em.client.api.EventMonitor;
import com.parasoft.em.client.api.Provisions;
import com.parasoft.em.client.impl.EnvironmentsImpl;
import com.parasoft.em.client.impl.ProvisionsImpl;

public class ProvisionTask extends Task {
    private String url;
    private String username;
    private String password;
    private int environmentId;
    private int instanceId;
    private boolean abortOnFailure;
    
    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @param environmentId the environmentId to set
     */
    public void setEnvironmentId(int environmentId) {
        this.environmentId = environmentId;
    }

    /**
     * @param instanceId the instanceId to set
     */
    public void setInstanceId(int instanceId) {
        this.instanceId = instanceId;
    }
    
    /**
     * @param abortOnFailure the abortOnFailure to set
     */
    public void setAbortOnFailure(boolean abortOnFailure) {
        this.abortOnFailure = abortOnFailure;
    }

    @Override
    public void execute() throws BuildException {
        // First test the url
        try {
            Environments envs = new EnvironmentsImpl(url, username, password);
            envs.getEnvironments();
        } catch (IOException e) {
            // Try to append the default context path '/em'
            String testUrl = url;
            try {
                if (testUrl.endsWith("/")) {
                    testUrl += "em";
                } else {
                    testUrl += "/em";
                }
                Environments envs = new EnvironmentsImpl(testUrl, username, password);
                envs.getEnvironments();
                url = testUrl;
            } catch (IOException e1) {
                throw new BuildException("Could not connect to Environment Manager at " + url);
            }
        }
        
        try {
            Provisions provisions = new ProvisionsImpl(url, username, password);
            JSONObject event = provisions.createProvisionEvent(environmentId, instanceId, abortOnFailure);
            boolean success = provisions.monitorEvent(event, new EventMonitor() {
                public void logMessage(String message) {
                    log(message);
                }
            });
            if (!success) {
                throw new BuildException("Provisioning event failed");
            }
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }
}

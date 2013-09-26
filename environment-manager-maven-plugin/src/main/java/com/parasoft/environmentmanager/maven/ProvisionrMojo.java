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
package com.parasoft.environmentmanager.maven;

import java.io.IOException;

import net.sf.json.JSONObject;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.parasoft.em.client.api.Environments;
import com.parasoft.em.client.api.EventMonitor;
import com.parasoft.em.client.api.Provisions;
import com.parasoft.em.client.impl.EnvironmentsImpl;
import com.parasoft.em.client.impl.ProvisionsImpl;

/**
 * Mojo to provision an environment manager instance
 *
 * @goal provision
 */
public class ProvisionrMojo extends AbstractMojo {

    /**
     * @parameter expression="${em.url}"
     * @required
     */
    private String url;
    
    /**
     * @parameter expression="${em.username}"
     */
    private String username;
    
    /**
     * @parameter expression="${em.password}"
     */
    private String password;
    
    /**
     * @parameter expression="${em.environmentId}"
     * @required
     */
    private int environmentId;
    
    /**
     * @parameter expression="${em.instanceId}"
     * @required
     */
    private int instanceId;
    
    /**
     * @parameter expression="${em.abortOnFailure}"
     */
    private boolean abortOnFailure;
    
    
    public void execute() throws MojoExecutionException, MojoFailureException {
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
                throw new MojoExecutionException("Could not connect to Environment Manager at " + url, e);
            }
        }
        
        try {
            Provisions provisions = new ProvisionsImpl(url, username, password);
            JSONObject event = provisions.createProvisionEvent(environmentId, instanceId, abortOnFailure);
            boolean success = provisions.monitorEvent(event, new EventMonitor() {
                public void logMessage(String message) {
                    getLog().info(message);
                }
            });
            if (!success) {
                String message = "Provisioning event failed";
                getLog().error(message);
                throw new MojoFailureException(message);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Could not connect to Environment Manager at " + url, e);
        }
    }

}

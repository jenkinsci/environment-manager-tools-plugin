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

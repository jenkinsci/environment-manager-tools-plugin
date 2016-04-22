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

import org.kohsuke.stapler.export.Exported;

import hudson.model.AbstractBuild;
import hudson.tasks.test.AbstractTestResultAction;

public class ProvisioningEventAction extends AbstractTestResultAction<ProvisioningEventAction> {
    private String instanceName;
    private String environmentUrl;
    private int numberOfSteps;
    private int numberOfFailed;
    
    protected ProvisioningEventAction(AbstractBuild owner, String instanceName, String environmentUrl, int numberOfSteps, int numberOfFailed) {
        super(owner);
        this.instanceName = instanceName;
        this.environmentUrl = environmentUrl;
        this.numberOfSteps = numberOfSteps;
        this.numberOfFailed = numberOfFailed;
    }

    public String getIconFileName() {
        return "/plugin/environment-manager-jenkins-plugin/icons/Parasoft-48.gif";
    }

    public String getDisplayName() {
        return instanceName;
    }

    public String getUrlName() {
        return environmentUrl;
    }

    @Override
    @Exported(visibility = 2)
    public int getFailCount() {
        return numberOfFailed;
    }

    @Override
    @Exported(visibility = 2)
    public int getTotalCount() {
        return numberOfSteps;
    }

    @Override
    public Object getResult() {
        return this;
    }

}

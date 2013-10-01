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

import org.kohsuke.stapler.export.Exported;

import hudson.model.AbstractBuild;
import hudson.tasks.test.AbstractTestResultAction;

public class ProvisioningEventAction extends AbstractTestResultAction<ProvisioningEventAction> {
    private String environmentUrl;
    private int numberOfSteps;
    private int numberOfFailed;
    
    protected ProvisioningEventAction(AbstractBuild owner, String environmentUrl, int numberOfSteps, int numberOfFailed) {
        super(owner);
        this.environmentUrl = environmentUrl;
        this.numberOfSteps = numberOfSteps;
        this.numberOfFailed = numberOfFailed;
    }

    public String getIconFileName() {
        return "/plugin/environment-manager-jenkins-plugin/icons/Parasoft-48.gif";
    }

    public String getDisplayName() {
        return "Environment Instance";
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

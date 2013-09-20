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
package com.parasoft.em.client.impl;

import java.io.IOException;

import net.sf.json.JSONObject;

import com.parasoft.em.client.api.Environments;

public class EnvironmentsImpl extends JSONClient implements Environments {

    public EnvironmentsImpl(String emUrl, String username, String password) {
        super(emUrl, username, password);
    }
    public JSONObject getEnvironments() throws IOException {
        return doGet("/em/api/v1/environments");
    }

    public JSONObject getEnvironmentInstances(int environmentId) throws IOException {
        return doGet("/em/api/v1/environments/" + environmentId + "/instances");
    }

}

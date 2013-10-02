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

import com.parasoft.em.client.api.Systems;

public class SystemsImpl extends JSONClient implements Systems {

    public SystemsImpl(String baseUrl, String username, String password) {
        super(baseUrl, username, password);
    }

    public JSONObject getSystems() throws IOException {
        return doGet("api/v1/systems");
    }

    public JSONObject getSystem(int id) throws IOException {
        return doGet("api/v1/systems/" + id);
    }

}

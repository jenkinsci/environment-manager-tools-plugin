/*
 * (C) Copyright ParaSoft Corporation 2016.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF ParaSoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.em.client.impl;

import java.io.IOException;

import net.sf.json.JSONObject;

import com.parasoft.em.client.api.Servers;

public class ServersImpl extends JSONClient implements Servers {

    public ServersImpl(String baseUrl, String username, String password) {
        super(baseUrl, username, password);
    }

    public JSONObject getServers() throws IOException {
        return doGet("api/v2/servers", true);
    }

    public JSONObject deleteServer(long serverId) throws IOException {
        return doDelete("api/v2/servers/" + serverId);
    }

}

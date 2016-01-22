/*
 * (C) Copyright ParaSoft Corporation 2016.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF ParaSoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.em.client.impl;

import static org.junit.Assert.*;
import net.sf.json.JSONObject;
import org.junit.Test;
import com.parasoft.em.client.api.Servers;

public class ServersImplTest {
    private static String EM_URL = "http://localhost:8080/em";

    @Test
    public void testSystems() throws Exception {
        Servers servers = new ServersImpl(EM_URL, "admin", "admin");
        JSONObject result = servers.getServers();
        assertNotNull(result);
    }
}

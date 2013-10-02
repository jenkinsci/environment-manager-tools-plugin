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

import static org.junit.Assert.*;
import net.sf.json.JSONObject;
import org.junit.Test;
import com.parasoft.em.client.api.Systems;

public class SystemsImplTest {
    private static String EM_URL = "http://dane.parasoft.com:8080/em";
    
    @Test
    public void testSystems() throws Exception {
        Systems systems = new SystemsImpl(EM_URL, "admin", "admin");
        JSONObject result = systems.getSystems();
        assertNotNull(result);
    }
    
    @Test
    public void testSystem() throws Exception {
        Systems systems = new SystemsImpl(EM_URL, "admin", "admin");
        JSONObject result = systems.getSystem(59);
        assertNotNull(result);
    }
}

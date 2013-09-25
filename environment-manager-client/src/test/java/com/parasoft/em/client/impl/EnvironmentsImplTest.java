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

import static org.junit.Assert.assertNotNull;
import net.sf.json.JSONObject;

import org.junit.Test;

import com.parasoft.em.client.api.Environments;

public class EnvironmentsImplTest {
    
    private static String EM_URL = "http://dane.parasoft.com:8080/em";
    
    @Test
    public void getEnvironments() throws Exception {
        Environments environments = new EnvironmentsImpl(EM_URL, "admin", "admin");
        JSONObject response = environments.getEnvironments();
        assertNotNull(response);
        System.out.println(response);
    }
    
    @Test
    public void getEnvironmentsInstances() throws Exception {
        Environments environments = new EnvironmentsImpl(EM_URL, "admin", "admin");
        JSONObject response = environments.getEnvironmentInstances(129);
        assertNotNull(response);
        System.out.println(response);
    }
    
}

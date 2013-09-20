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
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.junit.Test;

import com.parasoft.em.client.api.Provisions;

public class ProvisionsImplTest {
    
    private static String EM_URL = "http://dane.parasoft.com:8080";
    
    @Test
    public void testGetProvisions() throws Exception {
        Provisions provisions = new ProvisionsImpl(EM_URL, "admin", "admin");
        JSONObject response = provisions.getProvisions();
        assertNotNull(response);
    }
    
    @Test
    public void testCreateProvisionEvent() throws Exception {
        Provisions provisions = new ProvisionsImpl(EM_URL, "admin", "admin");
        JSONObject response = provisions.createProvisionEvent(129, 46, true);
        assertNotNull(response);
        
        int id = response.getInt("eventId");
        response = provisions.getProvisions(id);
        assertNotNull(response);
        System.out.println(response);
        
        String status = response.getString("status");
        String percent = "0";
        while ("running".equals(status) && !"100".equals(percent)) {
            Thread.sleep(1000);
            response = provisions.getProvisions(id);
            JSONArray steps = response.getJSONArray("steps");
            JSONObject step = (JSONObject) steps.get(0);
            percent = step.getString("percent");
            System.out.println(percent + "%");
            status = response.getString("status");
        }
    }
}

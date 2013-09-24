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
        JSONObject response = provisions.createProvisionEvent(129, 45, true);
        assertNotNull(response);
        
        int id = response.getInt("eventId");
        response = provisions.getProvisions(id);
        assertNotNull(response);
        System.out.println(response);
        
        boolean failed = false;
        response = provisions.getProvisions(id);
        JSONArray steps = response.getJSONArray("steps");
        for (int i = 0; i < steps.size(); i++) {
            JSONObject step = provisions.getProvisions(id).getJSONArray("steps").getJSONObject(i);
            System.out.println("Running step #" + (i + 1));
            String result = step.getString("result");
            while ("running".equals(result)) {
                Thread.sleep(1000);
                System.out.println(step.getString("percent") + "%");
                step = provisions.getProvisions(id).getJSONArray("steps").getJSONObject(i);
                result = step.getString("result");
                failed |= "error".equals(result);
            }
        }
        assertFalse(failed);
    }
    
    private boolean isStepDone(JSONObject jsonStep, int stepNumber) {
        
        return false;
    }
}

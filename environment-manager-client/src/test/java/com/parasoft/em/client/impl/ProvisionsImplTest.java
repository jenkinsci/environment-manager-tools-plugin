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

import com.parasoft.em.client.api.EventMonitor;
import com.parasoft.em.client.api.Provisions;

public class ProvisionsImplTest {
    
    private static String EM_URL = "http://dane.parasoft.com:8080/em";
    
    @Test
    public void testGetProvisions() throws Exception {
        Provisions provisions = new ProvisionsImpl(EM_URL, "admin", "admin");
        JSONObject response = provisions.getProvisions();
        assertNotNull(response);
    }
    
    private boolean provisionEvent(int environmentId, int instanceId) throws Exception {
        Provisions provisions = new ProvisionsImpl(EM_URL, "admin", "admin");
        JSONObject response = provisions.createProvisionEvent(environmentId, instanceId, false);
        assertNotNull(response);
        
        int id = response.getInt("eventId");
        response = provisions.getProvisions(id);
        assertNotNull(response);
        response = provisions.getProvisions(id);
        return provisions.monitorEvent(response, new EventMonitor(){
            public void logMessage(String message) {
                System.out.println(message);
            }
        });
    }
    
    @Test
    public void testCreateProvisionEvent() throws Exception {
        assertTrue(provisionEvent(129, 45));
    }
    
    @Test
    public void testCreateFailedProvisionEvent() throws Exception {
        assertFalse(provisionEvent(129, 46));
    }


}

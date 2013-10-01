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

import org.junit.Ignore;
import org.junit.Test;

import com.parasoft.em.client.api.EventMonitor;
import com.parasoft.em.client.api.Provisions;

public class ProvisionsImplTest {
    
    private static String EM_URL = "http://dragon.parasoft.com:8080/em";
    
    @Test
    public void testGetProvisions() throws Exception {
        Provisions provisions = new ProvisionsImpl(EM_URL, "admin", "admin");
        JSONObject response = provisions.getProvisions();
        assertNotNull(response);
    }
    
    @Test
    @Ignore
    public void testCreateProvisionEvent() throws Exception {
        Provisions provisions = new ProvisionsImpl(EM_URL, "admin", "admin");
        JSONObject response = provisions.createProvisionEvent(28, 1, false);
        assertNotNull(response);
        
        int id = response.getInt("eventId");
        response = provisions.getProvisions(id);
        assertNotNull(response);
        System.out.println(response);
        response = provisions.getProvisions(id);
        boolean success = provisions.monitorEvent(response, new EventMonitor(){
            public void logMessage(String message) {
                System.out.println(message);
            }
        });
        assertTrue(success);
    }

}

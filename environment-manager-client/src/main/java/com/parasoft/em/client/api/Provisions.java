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
package com.parasoft.em.client.api;

import java.io.IOException;

import net.sf.json.JSONObject;

public interface Provisions {
    JSONObject createProvisionEvent(int environmentId, int instanceId, boolean abortOnFailure) throws IOException;
    JSONObject getProvisions() throws IOException;
    JSONObject getProvisions(int id) throws IOException;
    JSONObject deleteProvisions(int id) throws IOException;
}

/*
 * (C) Copyright ParaSoft Corporation 2016.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF ParaSoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.em.client.api;

import java.io.IOException;

import net.sf.json.JSONObject;

public interface EnvironmentCopy {
    JSONObject createEnvironmentCopy(int environmentId, int serverId, String newEnvironmentName) throws IOException;
    JSONObject getCopyStatus(int id) throws IOException;
    JSONObject removeCopyStatus(int id) throws IOException;
    boolean monitorEvent(JSONObject event, EventMonitor monitor) throws IOException;
}

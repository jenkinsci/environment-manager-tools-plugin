/*
 * (C) Copyright ParaSoft Corporation 2016.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF ParaSoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.em.client.api;

import java.io.IOException;

import net.sf.json.JSONObject;

public interface Jobs {
	JSONObject getJobs() throws IOException;
	JSONObject getJob(long jobId) throws IOException;
	JSONObject executeJob(long jobId) throws IOException;
	boolean monitorExecution(JSONObject history, EventMonitor monitor) throws IOException;
	JSONObject getHistory(long jobId, long historyId) throws IOException;
	JSONObject deleteHistory(long jobId, long historyId) throws IOException;
}

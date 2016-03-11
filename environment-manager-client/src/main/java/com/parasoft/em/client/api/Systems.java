/*
 * (C) Copyright ParaSoft Corporation 2013.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF ParaSoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.em.client.api;

import java.io.IOException;
import net.sf.json.JSONObject;

/**
 * Interface for systems api
 */
public interface Systems {
    JSONObject getSystems() throws IOException;
    JSONObject getSystem(long id) throws IOException;
}

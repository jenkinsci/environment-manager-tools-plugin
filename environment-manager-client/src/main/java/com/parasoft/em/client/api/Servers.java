/*
 * (C) Copyright ParaSoft Corporation 2016.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF ParaSoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.em.client.api;

import java.io.IOException;
import net.sf.json.JSONObject;

/**
 * Interface for servers api
 */
public interface Servers {
    JSONObject getServers() throws IOException;
}

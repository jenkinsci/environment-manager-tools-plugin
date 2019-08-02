/*
 * Copyright 2019 Parasoft Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.parasoft.environmentmanager.jenkins;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ReportSettingsInjector extends InputStream {

    private static final String RESULTS_SESSION = "ResultsSession";
    private static final String PROJECT = "project";
    private static final String BUILD_ID = "buildId";
    private static final String TAG = "tag";
    private static final String EXEC_ENV = "execEnv";

    private boolean captureAttributeName;
    private boolean captureElementName;
    private boolean insideAttributeValue;
    private boolean insideQuotes;
    private boolean replaceAttributeValue;
    private boolean finishedInjecting;
    private StringBuilder attributeName;
    private StringBuilder elementName;
    private final InputStream projectBuffer;
    private final InputStream buildIdBuffer;
    private final InputStream tagBuffer;
    private final InputStream wrapped;
    private final InputStream execEnvBuffer;
    private InputStream appendBuffer;
    private final String execEnv;

    private static InputStream makeBuffer(String text) {
        if ((text != null) && !text.isEmpty()) {
            return new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));
        }
        return null;
    }

    public ReportSettingsInjector(String project, String buildId, String tag, String execEnv, InputStream in) {
        this.projectBuffer = makeBuffer(project);
        this.buildIdBuffer = makeBuffer(buildId);
        this.tagBuffer = makeBuffer(tag);
        this.execEnvBuffer = makeBuffer(execEnv);
        this.execEnv = execEnv;
        this.appendBuffer = null;
        captureAttributeName = false;
        insideAttributeValue = false;
        captureElementName = false;
        insideQuotes = false;
        replaceAttributeValue = false;
        attributeName = new StringBuilder();
        elementName = new StringBuilder();
        finishedInjecting = false;
        wrapped = in;
    }

    private int nextReplacementChar() throws IOException {
        int next = -1;
        if (PROJECT.equals(attributeName.toString())) {
            next = projectBuffer.read();
        }
        if (BUILD_ID.equals(attributeName.toString())) {
            next = buildIdBuffer.read();
        }
        if (TAG.equals(attributeName.toString())) {
            next = tagBuffer.read();
        }
        if (next == -1) {
            next = '"';
            replaceAttributeValue = false;
            insideAttributeValue = false;
            insideQuotes = false;
        }
        return next;
    }

    @Override
    public int read() throws IOException {
        int next;
        if (replaceAttributeValue) {
            return nextReplacementChar();
        }
        if ((appendBuffer != null) && (appendBuffer.available() > 0)) {
            return appendBuffer.read();
        }
        next = wrapped.read();
        if (finishedInjecting) {
            return next;
        }
        if (!insideQuotes && ((next == '/') || (next == '>'))) {
            captureElementName = false;
            if (RESULTS_SESSION.equals(elementName.toString())) {
                if ((execEnvBuffer != null) && (execEnvBuffer.available() > 0)) {
                    appendBuffer = makeBuffer("execEnv=\"" + execEnv + '"' + (char)next);
                    return ' ';
                }
                finishedInjecting = true;
            }
            return next;
        }
        if (RESULTS_SESSION.equals(elementName.toString())) {
            if (insideAttributeValue) {
                if (next != '"') {
                    return next;
                }
                insideQuotes = !insideQuotes;
                if (!insideQuotes) {
                    insideAttributeValue = false;
                } else if ((projectBuffer != null) && PROJECT.equals(attributeName.toString())) {
                    replaceAttributeValue = true;
                    while (wrapped.read() != '"');
                } else if ((buildIdBuffer != null) && BUILD_ID.equals(attributeName.toString())) {
                    replaceAttributeValue = true;
                    while (wrapped.read() != '"');
                } else if ((tagBuffer != null) && TAG.equals(attributeName.toString())) {
                    replaceAttributeValue = true;
                    while (wrapped.read() != '"');
                } else if ((execEnvBuffer != null) && EXEC_ENV.equals(attributeName.toString())) {
                    replaceAttributeValue = true;
                    while (wrapped.read() != '"');
                }
            } else if (captureAttributeName) {
                if (next == '=') {
                    captureAttributeName = false;
                    insideAttributeValue = true;
                } else if (Character.isWhitespace(next)) {
                    captureAttributeName = false;
                } else {
                    attributeName.append((char)next);
                }
            } else if (!Character.isWhitespace(next)) {
                captureAttributeName = true;
                attributeName.setLength(0);
                attributeName.append((char)next);
            }
        }
        if (captureElementName) {
            if (Character.isWhitespace(next)) {
                captureElementName = false;
            } else {
                elementName.append((char)next);
            }
        } else if (next == '<') {
            captureElementName = true;
            elementName.setLength(0);
        }
        return next;
    }

    public void close() throws IOException {
        wrapped.close();
    }

}

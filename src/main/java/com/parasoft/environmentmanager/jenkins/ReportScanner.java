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

import java.io.IOException;
import java.io.InputStream;

public class ReportScanner extends InputStream {

    private static final String TOTAL_ELEMENT = "Total";
    private static final String FAIL_ATTRIBUTE = "fail";
    private static final String TOTAL_ATTRIBUTE = "total";

    private boolean captureAttributeName;
    private boolean captureElementName;
    private boolean insideAttributeValue;
    private boolean insideQuotes;
    private boolean finished;
    private StringBuilder attributeName;
    private StringBuilder attributeValue;
    private StringBuilder elementName;
    private final InputStream wrapped;

    private int failures;
    private int total;

    public ReportScanner(InputStream in) {
        captureAttributeName = false;
        insideAttributeValue = false;
        captureElementName = false;
        insideQuotes = false;
        attributeName = new StringBuilder();
        attributeValue = new StringBuilder();
        elementName = new StringBuilder();
        finished = false;
        wrapped = in;
        failures = -1;
        total = -1;
    }

    public int getFailureCount() {
        return failures;
    }

    public int getTotalCount() {
        return total;
    }

    @Override
    public int read() throws IOException {
        int next = wrapped.read();
        if (finished) {
            return next;
        }
        if (!insideQuotes && ((next == '/') || (next == '>'))) {
            captureElementName = false;
            if (TOTAL_ELEMENT.equals(elementName.toString())) {
                finished = true;
            }
            return next;
        }
        if (TOTAL_ELEMENT.equals(elementName.toString())) {
            if (insideAttributeValue) {
                if (next == '"') {
                    insideQuotes = !insideQuotes;
                    if (!insideQuotes) {
                        insideAttributeValue = false;
                        if (FAIL_ATTRIBUTE.equals(attributeName.toString())) {
                            try {
                                failures = Integer.parseInt(attributeValue.toString());
                            } catch (NumberFormatException e) {
                                // ignore exception
                            }
                        } else if (TOTAL_ATTRIBUTE.equals(attributeName.toString())) {
                            try {
                                total = Integer.parseInt(attributeValue.toString());
                            } catch (NumberFormatException e) {
                                // ignore exception
                            }
                        }
                        attributeValue.setLength(0);
                    }
                } else {
                    attributeValue.append((char)next);
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

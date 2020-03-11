/*
 * (C) Copyright Parasoft Corporation 2020.  All rights reserved.
 * THIS IS UNPUBLISHED PROPRIETARY SOURCE CODE OF Parasoft
 * The copyright notice above does not evidence any
 * actual or intended publication of such source code.
 */

package com.parasoft.environmentmanager.jenkins;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class HTMLReportScanner extends InputStream {

    private static final String IMG_ELEMENT = "img"; //$NON-NLS-1$
    private static final String SRC_ATTRIBUTE = "src"; //$NON-NLS-1$
    private static final Object TD_ELEMENT = "td"; //$NON-NLS-1$
    private static final Object BACKGROUND_ATTRIBUTE = "background"; //$NON-NLS-1$
    private InputStream wrapped;
    private boolean insideAttrQuotes = false;
    private boolean insideQuotes = false;
    private boolean insideElement = false;
    private boolean captureElementName = false;
    private boolean insideAttributeValue = false;
    private boolean captureAttributeName = false;
    private final StringBuilder elementName;
    private final StringBuilder attributeName;
    private final StringBuilder attributeValue;
    private final Set<String> images = new HashSet<String>();

    public HTMLReportScanner(InputStream in) {
        this.wrapped = in;
        attributeName = new StringBuilder();
        attributeValue = new StringBuilder();
        elementName = new StringBuilder();
    }

    @Override
    public int read() throws IOException {
        int next = wrapped.read();
        if (!insideAttrQuotes && ((next == '/') || (next == '>'))) {
            captureElementName = false;
            return next;
        }
        if (next == '"') {
            insideQuotes = !insideQuotes;
        } 
        if (next == '<' && !insideQuotes) {
            insideElement = true;
        } else if (next =='/' || next == '>' && !insideQuotes) {
            insideElement = false;
        }
        if (IMG_ELEMENT.equals(elementName.toString())) {
            System.out.println();
        }
        if (isValidElement() && insideElement) {
            if (insideAttributeValue) {
                if (next == '"') {
                    insideAttrQuotes = !insideAttrQuotes;
                    if (!insideAttrQuotes) {
                        insideAttributeValue = false;
                        if (isValidAttr()) {
                            images.add(attributeValue.toString());
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
        if (next == '<' && !insideQuotes) {
            captureElementName = true;
            elementName.setLength(0);
        } else if (captureElementName) {
            if (Character.isWhitespace(next)) {
                captureElementName = false;
            } else {
                elementName.append((char)next);
            }
        }
        return next;
    }

    private boolean isValidElement() {
        String name = elementName.toString();
        return IMG_ELEMENT.equals(name) || TD_ELEMENT.equals(name);
    }

    private boolean isValidAttr() {
        String name = attributeName.toString();
        return SRC_ATTRIBUTE.equals(name) || BACKGROUND_ATTRIBUTE.equals(name);
    }
    public void close() throws IOException {
        wrapped.close();
    }

    public Set<String> getImages() {
        return images;
    }

}

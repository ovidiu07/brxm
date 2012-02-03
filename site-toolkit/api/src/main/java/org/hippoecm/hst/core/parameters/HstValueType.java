/*
 *  Copyright 2011 - 2012 Hippo.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.core.parameters;

import java.util.Calendar;

/**
 * Value type of an HstPropertyDefinition. STRING, BOOLEAN, INTEGER and DOUBLE types map to their obvious java
 * counterparts. DATE uses Calendar objects.
 */
public enum HstValueType {

    STRING(""), BOOLEAN(false), INTEGER(0), DOUBLE(0.0), DATE();

    private final Object defaultValue;

    private HstValueType() {
        this(null);
    }

    private HstValueType(Object value) {
        defaultValue = value;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public Object from(String string) {
        switch (this) {
            case STRING:
                return string;
            case BOOLEAN:
                return Boolean.parseBoolean(string);
            case INTEGER:
                return Integer.parseInt(string);
            case DOUBLE:
                return Double.parseDouble(string);
            case DATE: {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(Integer.parseInt(string));
                return cal;
            }
        }
        throw new RuntimeException("Could not parse " + string + " to type " + this);
    }
}

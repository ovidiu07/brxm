/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.dashboard.config;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.dashboard.utils.annotations.PersistentMultiProperty;
import org.onehippo.cms7.essentials.dashboard.utils.annotations.PersistentNode;

/**
 * @version "$Id$"
 */

@XmlRootElement(name = "base-document")
@PersistentNode(type = "essentials:document")
public class BaseDocument implements Document {


    private String parentPath;
    private String name;

    @PersistentMultiProperty(name = "properties")
    private List<String> properties = new LinkedList<>();


    public BaseDocument() {
    }

    public BaseDocument(final String name) {
        this.name = name;
    }

    public BaseDocument(final String name, final String parentPath) {
        this.name = name;
        this.parentPath = parentPath;
    }


    @Override
    public List<String> getProperties() {
        return properties;
    }

    @Override
    public void setProperties(final List<String> properties) {
        this.properties = properties;
    }

    @Override
    public void addProperty(final String value) {
        properties.add(value);
    }


    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getParentPath() {
        return parentPath;
    }

    @Override
    public String getPath() {
        return parentPath + '/' + name;
    }

    @Override
    public void setParentPath(String parentPath) {
        this.parentPath = parentPath;
    }


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("BaseDocument");
        sb.append("{name='").append(name).append('\'');
        sb.append(", path='").append(parentPath).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
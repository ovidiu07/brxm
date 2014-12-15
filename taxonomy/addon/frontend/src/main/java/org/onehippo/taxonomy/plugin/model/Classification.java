/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.plugin.model;

import java.util.List;

import org.apache.wicket.model.IDetachable;

/**
 * Value object that contains the keys for categories in a taxonomy tree.
 * Optionally, a canonical category may be specified.
 */
public class Classification implements IDetachable {

    private static final long serialVersionUID = 1L;

    private IDetachable id;
    private List<String> values;
    private String canonical;

    public Classification(List<String> values, IDetachable id) {
        this.values = values;
        this.id = id;
    }

    public Classification(List<String> values, IDetachable id, String canonical) {
        this.values = values;
        this.id = id;
        this.canonical = canonical;
    }


    public IDetachable getId() {
        return id;
    }
    
    public List<String> getKeys() {
        return values;
    }

    public boolean containsKey(String key) {
        return getKeys().contains(key);
    }

    public void addKey(String key) {
        if (!values.contains(key)) {
            values.add(key);
        }
    }

    public void removeKey(String key) {
        if (values.contains(key)) {
            values.remove(key);
        }

    }

    public void detach() {
        id.detach();
    }

    public String getCanonical() {
        return canonical;
}

    public void setCanonical(String canonical) {
        this.canonical = canonical;
    }

    public boolean isCanonised() {
        return canonical != null;
    }
}

/*
 *  Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.crisp.core.resource;

import java.util.Map;

import org.onehippo.cms7.crisp.api.resource.ValueMap;

/**
 * Default {@link ValueMap} implementation.
 *
 * @deprecated Use <code>org.onehippo.cms7.crisp.api.resource.DefaultValueMap</code> instead.
 */
@Deprecated
public class DefaultValueMap extends org.onehippo.cms7.crisp.api.resource.DefaultValueMap {

    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public DefaultValueMap() {
        super();
    }

    /**
     * Constructs with a map to delegate.
     * @param delegatedMap a map to delegate
     */
    public DefaultValueMap(Map<String, Object> delegatedMap) {
        super(delegatedMap);
    }
}

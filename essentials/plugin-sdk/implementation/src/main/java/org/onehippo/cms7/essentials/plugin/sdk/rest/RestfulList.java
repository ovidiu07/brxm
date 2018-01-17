/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugin.sdk.rest;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.Lists;

import io.swagger.annotations.ApiModel;

@ApiModel
@XmlRootElement(name = "collection")
public class RestfulList<T> implements Serializable {

    private List<T> items = Lists.newArrayList();

    public void add(T resource) {
        items.add(resource);
    }

    public Iterator<T> iterator() {
        return items.iterator();
    }

    public List<T> getItems() {
        return items;
    }

}

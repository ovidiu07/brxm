/**
 * Copyright 2011 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.channelmanager.channels;

import org.hippoecm.hst.configuration.channel.Blueprint;
import org.hippoecm.hst.configuration.channel.ChannelManager;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.core.container.RepositoryNotAvailableException;
import org.hippoecm.hst.site.HstServices;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.data.ExtField;
import org.wicketstuff.js.ext.data.ExtJsonStore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlueprintStore extends ExtJsonStore<Object> {

    private long total;
    private static final Logger log = LoggerFactory.getLogger(BlueprintStore.class);

    public BlueprintStore(List<ExtField> fields) {
        super(fields);
    }


    @Override
    protected JSONObject getProperties() throws JSONException {
        //Need the sortinfo and xaction params since we are using GroupingStore instead of
        //JsonStore
        final JSONObject props = super.getProperties();
        Map<String, String> sortInfo = new HashMap<String, String>();
        sortInfo.put("title", "title");
        props.put("sortInfo", sortInfo);
        Map<String, String> baseParams = new HashMap<String, String>();
        baseParams.put("xaction", "read");
        props.put("baseParams", baseParams);
        return props;
    }

    @Override
    protected long getTotal() {
        return this.total;
    }

    @Override
    protected JSONArray getData() throws JSONException {
        JSONArray data = new JSONArray();
        List<Blueprint> blueprints= getBlueprints();
        this.total = blueprints.size();
        for (Blueprint blueprint : blueprints) {
            JSONObject object = new JSONObject();
            object.put("id", blueprint.getId());
            object.put("description", blueprint.getDescription());
            object.put("name", blueprint.getName());
            data.put(object);
        }
        return data;
    }

    private List<Blueprint> getBlueprints() {
        ChannelManager channelManager;
        try {
            HstManager hstManager = HstServices.getComponentManager().getComponent(HstManager.class.getName());
            channelManager = hstManager.getChannelManager();
        } catch (RepositoryNotAvailableException e) {
            log.error("Unable to get blueprints from HST: " + e.getMessage(), e);
            throw new RuntimeException("Unable to get the blueprints from Channel Manager" + e);
        }
        if (channelManager != null) {
            return channelManager.getBlueprints();
        } else {
            throw new RuntimeException("Unable to get the Channel Manager instance.");
        }
    }


}

/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.update;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdaterRegistryImpl implements UpdaterRegistry, EventListener {

    private static final Logger log = LoggerFactory.getLogger(UpdaterRegistryImpl.class);

    private static final String UPDATE_REGISTRY_PATH = "/" + HippoNodeType.CONFIGURATION_PATH + "/hippo:update/hippo:registry";

    private final Session session;
    private volatile Map<String, List<UpdaterInfo>> updaters = new HashMap<>();

    UpdaterRegistryImpl(final Session session) throws RepositoryException {
        this.session = session;
    }

    void start() throws RepositoryException {
        session.getWorkspace().getObservationManager().addEventListener(
                this,
                Event.NODE_ADDED | Event.NODE_MOVED | Event.NODE_REMOVED |
                Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED,
                UPDATE_REGISTRY_PATH, true, null, null, true);
        buildRegistry();
    }

    void stop() {
        try {
            session.getWorkspace().getObservationManager().removeEventListener(this);
        } catch (RepositoryException e) {
            log.error(e.getClass().getName() + ": " + e.getMessage());
        }
        session.logout();
        for (List<UpdaterInfo> updaterInfos : updaters.values()) {
            for (UpdaterInfo updaterInfo : updaterInfos) {
                updaterInfo.getUpdater().destroy();
            }
        }
        updaters.clear();
    }

    private void buildRegistry() {
        try {
            Map<String, List<UpdaterInfo>> updatedUpdaters = new HashMap<>();
            final Node registry = JcrUtils.getNodeIfExists(UPDATE_REGISTRY_PATH, session);
            if (registry != null) {
                for (final Node node : new NodeIterable(registry.getNodes())) {
                    final String updaterName = node.getName();
                    try {
                        final UpdaterInfo updaterInfo = new UpdaterInfo(node);
                        if (updaterInfo.getNodeType() != null) {
                            List<UpdaterInfo> list = updatedUpdaters.get(updaterInfo.getNodeType());
                            if (list == null) {
                                list = new ArrayList<>();
                                updatedUpdaters.put(updaterInfo.getNodeType(), list);
                            }
                            list.add(updaterInfo);
                        }
                    } catch (IllegalArgumentException | IllegalAccessException | InstantiationException | ClassNotFoundException | RepositoryException e) {
                        log.error("Failed to register updater '{}': {}", updaterName, e.toString());
                    }
                }
                updaters = updatedUpdaters;
            }
        } catch (RepositoryException e) {
            log.error("Failed to build updater registry", e);
        }
    }

    @Override
    public List<Class<? extends NodeUpdateVisitor>> getUpdaters(final Node node) throws RepositoryException {
        if (updaters.isEmpty()) {
            return Collections.emptyList();
        }
        List<Class<? extends NodeUpdateVisitor>> result = new ArrayList<>();
        for (Map.Entry<String, List<UpdaterInfo>> entry : updaters.entrySet()) {
            if (node.isNodeType(entry.getKey())) {
                for (UpdaterInfo updaterInfo : entry.getValue()) {
                    result.add(updaterInfo.getUpdaterClass());
                }
            }
        }
        return result;
    }

    @Override
    public void onEvent(final EventIterator events) {
        buildRegistry();
    }
}

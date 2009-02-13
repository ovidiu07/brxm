/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.jcr.observation.ObservationManager;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrObservationManager implements ObservationManager {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(JcrObservationManager.class);

    private static JcrObservationManager INSTANCE = new JcrObservationManager();

    private static class ObservationException extends Exception {
        private static final long serialVersionUID = 1L;

        public ObservationException(String message) {
            super(message);
        }
    }

    private class NodeCache {

        private class NodeEvent implements Event {

            int type;
            String name;

            NodeEvent(String name, int type) {
                this.type = type;
            }

            public String getPath() throws RepositoryException {
                if (name != null) {
                    return path + "/" + name;
                } else {
                    return path;
                }
            }

            public int getType() {
                return type;
            }

            public String getUserID() {
                return userId;
            }

        }

        private Node node;
        private String path;
        private String userId;
        private Map<String, Object> properties;
        private List<String> nodes;

        NodeCache(Node node) throws RepositoryException {
            this.node = node;
            this.path = node.getPath();
            this.userId = node.getSession().getUserID();

            properties = new HashMap<String, Object>();
            nodes = new LinkedList<String>();

            process(properties, nodes);
        }

        void process(Map<String, Object> properties, List<String> nodes) throws RepositoryException {
            PropertyIterator propIter = node.getProperties();
            while (propIter.hasNext()) {
                Property property = propIter.nextProperty();
                JcrPropertyModel propertyModel = new JcrPropertyModel(property);
                if (property.getDefinition().isMultiple()) {
                    List<Object> values = new ArrayList<Object>(propertyModel.size());
                    Iterator iter = propertyModel.iterator(0, propertyModel.size());
                    while (iter.hasNext()) {
                        values.add(propertyModel.model(iter.next()).getObject());
                    }
                    properties.put(property.getName(), values);
                } else {
                    JcrPropertyValueModel pvm = new JcrPropertyValueModel(propertyModel);
                    properties.put(property.getName(), pvm.getObject());
                }
            }

            NodeIterator nodeIter = node.getNodes();
            while (nodeIter.hasNext()) {
                Node child = nodeIter.nextNode();
                nodes.add(child.getName() + "[" + child.getIndex() + "]");
            }
        }

        Iterator<Event> update() throws RepositoryException {
            List<Event> events = new LinkedList<Event>();

            Map<String, Object> properties = new HashMap<String, Object>();
            List<String> nodes = new LinkedList<String>();
            process(properties, nodes);

            for (Map.Entry<String, Object> entry : this.properties.entrySet()) {
                if (properties.containsKey(entry.getKey())) {
                    Object value = properties.get(entry.getKey());
                    if (!value.equals(entry.getValue())) {
                        events.add(new NodeEvent(entry.getKey(), Event.PROPERTY_CHANGED));
                    }
                } else {
                    events.add(new NodeEvent(entry.getKey(), Event.PROPERTY_REMOVED));
                }
            }
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                if (!this.properties.containsKey(entry.getKey())) {
                    events.add(new NodeEvent(entry.getKey(), Event.PROPERTY_ADDED));
                }
            }

            for (String child : nodes) {
                if (!this.nodes.contains(child)) {
                    events.add(new NodeEvent(child, Event.NODE_ADDED));
                }
            }
            for (String child : this.nodes) {
                if (!nodes.contains(child)) {
                    events.add(new NodeEvent(child, Event.NODE_REMOVED));
                }
            }

            this.properties = properties;
            this.nodes = nodes;
            return events.iterator();
        }
    }

    private class JcrListener extends WeakReference<EventListener> implements EventListener {
        Node root;
        String[] nodeTypes;
        boolean isDeep;
        String path;

        Map<String, NodeCache> pending;
        List<Event> events;
        ObservationManager obMgr;
        WeakReference<UserSession> sessionRef;

        JcrListener(UserSession userSession, EventListener upstream) {
            super(upstream, listenerQueue);
            this.events = new LinkedList<Event>();
            sessionRef = new WeakReference<UserSession>(userSession);
        }

        synchronized public void onEvent(EventIterator events) {
            while (events.hasNext()) {
                this.events.add(events.nextEvent());
            }
        }

        void init(int eventTypes, String absPath, boolean isDeep, String[] uuid, String[] nodeTypes, boolean noLocal)
                throws RepositoryException, ObservationException {
            if (sessionRef.get() == null) {
                throw new ObservationException("No session found");
            }
            if (!absPath.startsWith("/")) {
                throw new ObservationException("Invalid path");
            }

            this.path = absPath;
            this.isDeep = isDeep;
            this.nodeTypes = nodeTypes;

            Session session = sessionRef.get().getJcrSession();
            if ("/".equals(absPath)) {
                root = session.getRootNode();
            } else {
                root = session.getRootNode().getNode(absPath.substring(1));
            }
            pending = new HashMap<String, NodeCache>();

            if (session != null) {
                obMgr = session.getWorkspace().getObservationManager();
                obMgr.addEventListener(this, eventTypes, absPath, isDeep, uuid, nodeTypes, noLocal);
            } else {
                log.error("No jcr session bound to wicket session");
            }
        }

        void dispose() {
            if (obMgr != null) {
                try {
                    obMgr.removeEventListener(this);
                } catch (RepositoryException ex) {
                    log.error("Unable to unregister event listener, " + ex.getMessage());
                }
                obMgr = null;
            }
            events.clear();
            root = null;
            nodeTypes = null;
            pending = null;
        }

        UserSession getSession() {
            return sessionRef.get();
        }

        void processPending(NodeIterator iter, List<Node> nodes) throws RepositoryException {
            while (iter.hasNext()) {
                Node node = iter.nextNode();
                String path;
                if (node.isNew()) {
                    path = node.getParent().getPath();
                } else {
                    path = node.getPath();
                }
                if (isDeep) {
                    if (path.startsWith(this.path)) {
                        nodes.add(node);
                    }
                } else {
                    if (path.equals(this.path)) {
                        nodes.add(node);
                    }
                }
            }
        }

        synchronized void process() {
            // listeners can be invoked after they have been removed
            if (obMgr == null) {
                log.debug("Listener " + this + " is no longer registerd");
                return;
            }
            try {
                List<Node> nodes = new LinkedList<Node>();
                if (nodeTypes == null) {
                    NodeIterator iter = ((HippoSession) root.getSession()).pendingChanges(root, null);
                    processPending(iter, nodes);
                } else {
                    for (String type : nodeTypes) {
                        NodeIterator iter = ((HippoSession) root.getSession()).pendingChanges(root, type);
                        processPending(iter, nodes);
                    }
                }

                List<String> paths = new LinkedList<String>();
                for (Node node : nodes) {
                    String path;
                    if (node.isNew()) {
                        path = node.getParent().getPath();
                    } else {
                        path = node.getPath();
                    }
                    paths.add(path);
                    if (pending.containsKey(path)) {
                        Iterator<Event> iter = pending.get(path).update();
                        while (iter.hasNext()) {
                            events.add(iter.next());
                        }
                    } else {
                        NodeCache cache = new NodeCache(node);
                        pending.put(path, cache);
                        events.add(cache.new NodeEvent(null, 0));
                    }
                }

                for (String path : new ArrayList<String>(pending.keySet())) {
                    if (!paths.contains(path)) {
                        NodeCache cache = pending.remove(path);
                        events.add(cache.new NodeEvent(null, 0));
                    }
                }
            } catch (RepositoryException ex) {
                log.error("Failed to parse pending changes", ex);
            }

            final Iterator<Event> upstream = events.iterator();
            final long size = events.size();
            EventIterator iter = new EventIterator() {

                public Event nextEvent() {
                    Event event = upstream.next();
                    upstream.remove();
                    log.debug("processing " + event.toString() + ", session " + sessionRef.get().getId());
                    return event;
                }

                public long getPosition() {
                    throw new UnsupportedOperationException("skip() is not implemented yet");
                }

                public long getSize() {
                    return size;
                }

                public void skip(long skipNum) {
                    throw new UnsupportedOperationException("skip() is not implemented yet");
                }

                public boolean hasNext() {
                    return upstream.hasNext();
                }

                public Object next() {
                    return nextEvent();
                }

                public void remove() {
                    throw new UnsupportedOperationException("remove() is not implemented yet");
                }

            };
            get().onEvent(iter);
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("path", path).append("isDeep",
                    isDeep).toString();
        }
    }

    public static JcrObservationManager getInstance() {
        return INSTANCE;
    }

    ReferenceQueue<EventListener> listenerQueue;
    Map<EventListener, JcrListener> listeners;

    private JcrObservationManager() {
        this.listeners = Collections.synchronizedMap(new WeakHashMap<EventListener, JcrListener>());
        this.listenerQueue = new ReferenceQueue<EventListener>();
    }

    public void addEventListener(EventListener listener, int eventTypes, String absPath, boolean isDeep, String[] uuid,
            String[] nodeTypeName, boolean noLocal) throws RepositoryException {
        cleanup();

        UserSession session = (UserSession) org.apache.wicket.Session.get();
        if (session != null) {
            JcrListener realListener = new JcrListener(session, listener);
            try {
                realListener.init(eventTypes, absPath, isDeep, uuid, nodeTypeName, noLocal);
                listeners.put(listener, realListener);
            } catch (ObservationException ex) {
                log.error(ex.getMessage());
            }
        } else {
            log.error("No session found");
        }
    }

    public EventListenerIterator getRegisteredEventListeners() throws RepositoryException {
        throw new UnsupportedOperationException("getRegisteredEventListeners() is not implemented yet");
    }

    public void removeEventListener(EventListener listener) throws RepositoryException {
        cleanup();

        if (listeners.containsKey(listener)) {
            JcrListener realListener = listeners.remove(listener);
            realListener.dispose();
        }
    }

    public void processEvents() {
        cleanup();

        UserSession session = (UserSession) org.apache.wicket.Session.get();
        if (session != null) {
            // copy set of listeners; don't synchronize on map while notifying observers
            // as it may need to be modified as a result of the event.
            Set<Map.Entry<EventListener, JcrListener>> set;
            synchronized (listeners) {
                set = new HashSet<Map.Entry<EventListener, JcrListener>>(listeners.entrySet());
            }
            for (Map.Entry<EventListener, JcrListener> entry : set) {
                JcrListener listener = entry.getValue();
                if (listener.getSession() == session) {
                    listener.process();
                }
            }
        } else {
            log.error("No session found");
        }
    }

    private void cleanup() {
        JcrListener ref;
        synchronized (listeners) {
            // cleanup gc'ed listeners
            while ((ref = (JcrListener) listenerQueue.poll()) != null) {
                ref.dispose();
            }
        }
    }

}

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
package org.apache.jackrabbit.jcr2spi;

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.jackrabbit.commons.AbstractSession;
import org.apache.jackrabbit.jcr2spi.config.CacheBehaviour;
import org.apache.jackrabbit.jcr2spi.config.RepositoryConfig;
import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyEntry;
import org.apache.jackrabbit.jcr2spi.hierarchy.HierarchyManager;
import org.apache.jackrabbit.jcr2spi.hierarchy.NodeEntry;
import org.apache.jackrabbit.jcr2spi.lock.LockManager;
import org.apache.jackrabbit.jcr2spi.nodetype.EffectiveNodeTypeProvider;
import org.apache.jackrabbit.jcr2spi.nodetype.ItemDefinitionProvider;
import org.apache.jackrabbit.jcr2spi.nodetype.NodeTypeManagerImpl;
import org.apache.jackrabbit.jcr2spi.operation.Move;
import org.apache.jackrabbit.jcr2spi.operation.Operation;
import org.apache.jackrabbit.jcr2spi.security.AccessManager;
import org.apache.jackrabbit.jcr2spi.state.ItemStateFactory;
import org.apache.jackrabbit.jcr2spi.state.ItemStateValidator;
import org.apache.jackrabbit.jcr2spi.state.NodeState;
import org.apache.jackrabbit.jcr2spi.state.PropertyState;
import org.apache.jackrabbit.jcr2spi.state.SessionItemStateManager;
import org.apache.jackrabbit.jcr2spi.state.HippoSessionItemStateManager;
import org.apache.jackrabbit.jcr2spi.state.UpdatableItemStateManager;
import org.apache.jackrabbit.jcr2spi.version.VersionManager;
import org.apache.jackrabbit.jcr2spi.xml.ImportHandler;
import org.apache.jackrabbit.jcr2spi.xml.Importer;
import org.apache.jackrabbit.jcr2spi.xml.SessionImporter;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.NameFactory;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.PathFactory;
import org.apache.jackrabbit.spi.QValueFactory;
import org.apache.jackrabbit.spi.SessionInfo;
import org.apache.jackrabbit.spi.XASessionInfo;
import org.apache.jackrabbit.spi.commons.conversion.DefaultNamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameException;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.apache.jackrabbit.spi.commons.value.ValueFactoryQImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.jcr.AccessDeniedException;
import javax.jcr.Credentials;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidSerializedDataException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.LoginException;
import javax.jcr.NamespaceException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessControlException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import org.apache.jackrabbit.jcr2spi.state.ItemState;

/**
 * <code>HippoSessionImpl</code>...
 */
public class HippoSessionImpl extends SessionImpl implements NamespaceResolver, ManagerProvider {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static Logger log = LoggerFactory.getLogger(SessionImpl.class);

    private Repository repository;

    private SessionInfo sessionInfo;

    private RepositoryConfig config;

    private HippoSessionItemStateManager sessionItemStateManager;

    private ItemManager itemManager;
    
    HippoSessionImpl(SessionInfo sessionInfo, Repository repository, RepositoryConfig config)
        throws RepositoryException {
        super(sessionInfo, repository, config);
        this.repository = repository;
        this.config = config;
        this.sessionInfo = sessionInfo;
    }


    @Override
    protected SessionItemStateManager createSessionItemStateManager(UpdatableItemStateManager wsStateManager,
                                                                    ItemStateFactory isf) throws RepositoryException {
        sessionItemStateManager = new HippoSessionItemStateManager(wsStateManager, getValidator(), getQValueFactory(), isf, this);
        return sessionItemStateManager;
    }

    @Override
    protected ItemManager createItemManager(HierarchyManager hierarchyManager) {
        itemManager = super.createItemManager(hierarchyManager);
        return itemManager;
    }


    @Override
    HippoSessionImpl switchWorkspace(String workspaceName) throws AccessDeniedException, NoSuchWorkspaceException,
                                                                  RepositoryException {
        checkAccessibleWorkspace(workspaceName);
        SessionInfo info = config.getRepositoryService().obtain(sessionInfo, workspaceName);
        if (info instanceof XASessionInfo) {
            return new HippoXASessionImpl((XASessionInfo) info, repository, config);
        } else {
            return new HippoSessionImpl(info, repository, config);
        }
    }

    public NodeIterator pendingChanges(Node node, String nodeType, boolean prune) throws NamespaceException,
                                                                            NoSuchNodeTypeException, RepositoryException {
        NodeState target;
        if(node != null) {
            target = (NodeState) ((NodeImpl)node).getItemState();
        } else {
            target = getHierarchyManager().getRootEntry().getNodeState();
        }
        Set affected = sessionItemStateManager.pendingChanges(target);
        Set<Node> nodes = new LinkedHashSet<Node>();
        for(Iterator iter = affected.iterator(); iter.hasNext(); ) {
            ItemState state = (ItemState) iter.next();
            if(!state.isNode()) {
                state = state.getParent();
            }
            Node candidate = (Node) itemManager.getItem(state.getHierarchyEntry());
            if(nodeType != null && !candidate.isNodeType(nodeType))
                continue;
            if(node != null && (node.isSame(candidate) || node.isSame(node.getParent())))
                continue;
            nodes.add(candidate);
        }
        return new SetNodeIterator(nodes);
    }

    static class SetNodeIterator implements NodeIterator {
        Iterator<Node> iter;
        long position = 0;
        long size;

        public SetNodeIterator(Set<Node> nodes) {
            iter = nodes.iterator();
            position = -1;
            size = nodes.size();
        }

        public Object next() {
            ++position;
            return iter.next();
        }

        public Node nextNode() {
            ++position;
            return iter.next();
        }

        public long getPosition() {
            return position;
        }

        public long getSize() {
            return size;
        }

        public void skip(long count) {
            while (count > 0)
                next();
        }

        public void remove() {
            iter.remove();
            --size;
        }

        public boolean hasNext() {
            return iter.hasNext();
        }
    }
}

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
package org.hippoecm.repository.security;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NamespaceException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.nodetype.NodeTypeManager;
import javax.security.auth.Subject;

import org.apache.jackrabbit.api.jsr283.security.AccessControlException;
import org.apache.jackrabbit.api.jsr283.security.AccessControlManager;
import org.apache.jackrabbit.api.jsr283.security.AccessControlPolicy;
import org.apache.jackrabbit.api.jsr283.security.AccessControlPolicyIterator;
import org.apache.jackrabbit.api.jsr283.security.Privilege;
import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.PropertyId;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.AccessControlPolicyIteratorAdapter;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.AnonymousPrincipal;
import org.apache.jackrabbit.core.security.SystemPrincipal;
import org.apache.jackrabbit.core.security.UserPrincipal;
import org.apache.jackrabbit.core.security.authorization.AccessControlProvider;
import org.apache.jackrabbit.core.security.authorization.Permission;
import org.apache.jackrabbit.core.security.authorization.WorkspaceAccessManager;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.core.state.PropertyState;
import org.apache.jackrabbit.core.value.InternalValue;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.ParsingPathResolver;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;
import org.apache.jackrabbit.spi.commons.name.NameConstants;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.jackrabbit.HippoSessionItemStateManager;
import org.hippoecm.repository.security.domain.DomainRule;
import org.hippoecm.repository.security.domain.FacetRule;
import org.hippoecm.repository.security.principals.FacetAuthPrincipal;
import org.hippoecm.repository.security.principals.GroupPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HippoAccessManager based on facet authorization. A subject (user)
 * has a set of {@link FacetAuth}s which hold the domain configuration
 * as defined by a set of {@link DomainRule}s, the roles the subject has
 * for the domain and the JCR permissions the subject has for the domain.
 *
 * For checking if a subject has specific permissions on a item (property), the permissions
 * of the subject on the parent node are checked.
 *
 * The HippoAccessManager also checks if the node is part of a hippo:document in
 * which case the hippo:document is also checked for permissions when the subject
 * does not have the correct permissions on the node itself. If the subject does
 * have the correct permissions on the hippo:document the permissions on the node
 * are granted.
 *
 */
public class HippoAccessManager implements AccessManager, AccessControlManager {

    /** SVN id placeholder */
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /**
     * Subject whose access rights this AccessManager should reflect
     */
    private Subject subject;

    /**
     * hierarchy manager used for ACL-based access control model
     */
    private HierarchyManager hierMgr;

    /**
     * Attic aware hierarchy manager used for ACL-based access control model
     */
    private HierarchyManager zombieHierMgr;

    /**
     * The session item state manager used for fetching transient and attic item states
     */
    private HippoSessionItemStateManager itemMgr;

    /**
     * NodeTypeManager for resolving superclass node types
     */
    private NodeTypeManager ntMgr;

    /**
     * NamePathResolver
     */
    private NamePathResolver npRes;
    
    
    /**
     * Name of hippo:handle, needed for document model checking
     */
    private Name hippoHandle;

    /**
     * Name of hippo:facetsearch, needed for document model checking
     */
    private Name hippoFacetSearch;

    /**
     * Name of hippo:facetselect, needed for document model checking
     */
    private Name hippoFacetSelect;

    /**
     * Root NodeId of current session
     */
    private NodeId rootNodeId;

    /**
     * The HippoAccessCache instance
     */
    private HippoAccessCache readAccessCache;

    private static final int DEFAULT_PERM_CACHE_SIZE = 20000;

    /**
     * Cache for determining if a type is a instance of another type
     */
    private final NodeTypeInstanceOfCache ntIOCache = NodeTypeInstanceOfCache.getInstance();

    /**
     * State of the accessManager
     */
    private boolean initialized = false;

    /**
     * Flag whether current user is anonymous
     */
    private boolean isAnonymous = false;

    /**
     * Flag whether current user is a regular user
     */
    private boolean isUser = false;

    /**
     * Flag whether the current user is a system user
     */
    private boolean isSystem = false;

    /**
     * The userId of the logged in user
     */
    private String userId;

    private final List<String> groupIds = new ArrayList<String>();
    private final List<String> currentDomainRoleIds = new ArrayList<String>();


    /**
     * The logger
     */
    private static final Logger log = LoggerFactory.getLogger(HippoAccessManager.class);

    private static ConcurrentHashMap<String, Privilege> currentPrivileges = new ConcurrentHashMap<String, Privilege>();
    
    //---------------------------------------- API ---------------------------------------------//
    /**
     * @see AccessManager#init(AMContext, AccessControlProvider, WorkspaceAccessManager)
     */
    public void init(AMContext context, AccessControlProvider acProvider, WorkspaceAccessManager wspAccessMgr)
            throws AccessDeniedException, Exception {
        init(context);
    }

    /**
     * @see AccessManager#init(AMContext)
     */
    public void init(AMContext context) throws AccessDeniedException, Exception {
        if (initialized) {
            throw new IllegalStateException("already initialized");
        }
        subject = context.getSubject();
        npRes = context.getNamePathResolver();
        
        if (context instanceof HippoAMContext) {
            ntMgr = ((HippoAMContext) context).getNodeTypeManager();
            itemMgr = (HippoSessionItemStateManager) ((HippoAMContext) context).getSessionItemStateManager();
        }

        hierMgr = itemMgr.getHierarchyMgr();
        zombieHierMgr = itemMgr.getAtticAwareHierarchyMgr();
        
        // Shortcuts for checks
        isSystem = !subject.getPrincipals(SystemPrincipal.class).isEmpty();
        isUser = !subject.getPrincipals(UserPrincipal.class).isEmpty();
        isAnonymous = !subject.getPrincipals(AnonymousPrincipal.class).isEmpty();

        // prefetch userId
        if (isSystem) {
            userId = subject.getPrincipals(SystemPrincipal.class).iterator().next().getName();
        } else if (isUser) {
            userId = subject.getPrincipals(UserPrincipal.class).iterator().next().getName();
        } else if (isAnonymous) {
            userId = subject.getPrincipals(AnonymousPrincipal.class).iterator().next().getName();
        } else {
            userId = "";
        }

        // prefetch groupId's
        for (GroupPrincipal gp : subject.getPrincipals(GroupPrincipal.class)) {
            groupIds.add(gp.getName());
        }

        // cache root NodeId
        rootNodeId = hierMgr.resolveNodePath(PathFactoryImpl.getInstance().getRootPath());

        hippoHandle = npRes.getQName(HippoNodeType.NT_HANDLE);
        hippoFacetSearch = npRes.getQName(HippoNodeType.NT_FACETSEARCH);
        hippoFacetSelect = npRes.getQName(HippoNodeType.NT_FACETSELECT);
        
        // initialize read cache
        int cacheSize = getPermCacheSize();
        if (cacheSize < 0) {
            cacheSize = DEFAULT_PERM_CACHE_SIZE;
        }
        HippoAccessCache.setMaxSize(cacheSize);
        readAccessCache = HippoAccessCache.getInstance(userId);

        // we're done
        initialized = true;

        log.info("Initialized HippoAccessManager for user " + userId + " with cache size " + cacheSize);
    }

    /**
     * @see AccessManager#close()
     */
    public synchronized void close() throws Exception {
        checkInitialized();
        initialized = false;

        // clear out all caches
        readAccessCache.clear();
        //requestItemStateCache.clear();
        groupIds.clear();
        currentDomainRoleIds.clear();

        // Aggressively nullify
        subject = null;
        hierMgr = null;
        itemMgr = null;
        ntMgr = null;
        npRes = null;
        log.info("Closed HippoAccessManager for user " + userId);
    }

    /**
     * @see AccessManager#checkPermission(ItemId, int)
     * @deprecated 
     */
    public void checkPermission(ItemId id, int permissions) throws AccessDeniedException, ItemNotFoundException,
            RepositoryException {
        log.warn("checkPermission(ItemId, int) is DEPRECATED!", new RepositoryException("Use of deprecated method checkPermission(ItemId, int)"));
        // just use the isGranted method
        if (!isGranted(id, permissions)) {
            throw new AccessDeniedException();
        }
    }

    /**
     * @see AccessManager#isGranted(ItemId, int)
     * @deprecated
     */
    public boolean isGranted(ItemId id, int permissions) throws RepositoryException {
        checkInitialized();
        if (permissions != Permission.READ) {
            log.warn("isGranted(ItemId, int) is DEPRECATED!", new RepositoryException("Use of deprecated method isGranted(ItemId, int)"));
        }
        
        if (isSystem) {
            return true;
        }

        // handle properties
        if (!id.denotesNode()) {
            if (permissions == Permission.REMOVE_PROPERTY) {
                // Don't check remove on properties. A write check on the node itself is done.
                return true;
            } else if (permissions == Permission.SET_PROPERTY) {
                // A write check on the parent will be done.
                return true;
            }
            return isGranted(((PropertyId) id).getParentId(), permissions);
        }

        // fast track common read check
        if (permissions == Permission.READ) {
            try {
                if (canRead((NodeId) id)) {
                    return true;
                }
            } catch (NoSuchItemStateException e) {
                // shouldn't happen, the id was already found
                log.error("No item id found", e);
                return false;
            }
        }
        
        // not a read, remove node from cache
        readAccessCache.remove(id);
        
        return isGranted(hierMgr.getPath(id), permissions);
    }

    /**
     * @see AccessManager#isGranted(Path, int)
     */
    public boolean isGranted(Path absPath, int permissions) throws RepositoryException {
        checkInitialized();
        if (!absPath.isAbsolute()) {
            throw new RepositoryException("Absolute path expected");
        }
        if (isSystem) {
            return true;
        }
        if (log.isInfoEnabled()) {
            log.info("Checking [{}] for absPath: {}", permsString(permissions), npRes.getJCRPath(absPath));
        }

        // fasttrack read permissions check
        if (permissions == Permission.READ) {
            return canRead(absPath);
        }
        
        // part of combined permissions check
        if ((permissions & Permission.READ) != 0) {
            if (!canRead(absPath)) {
                // all permissions must be matched
                return false;
            }
        }

        // translate permissions to privileges according to 6.11.1.4 of JSR-283
        String parentJcrPath = npRes.getJCRPath(absPath.getAncestor(1));
        Set<Privilege> privileges = new HashSet<Privilege>();
        if ((permissions & Permission.ADD_NODE) != 0) {
            privileges.add(privilegeFromName("jcr:addChildNodes"));
        }
        if ((permissions & Permission.REMOVE_NODE) != 0) {
            privileges.add(privilegeFromName("jcr:removeChildNodes"));
        }
        if ((permissions & Permission.SET_PROPERTY) != 0) {
            privileges.add(privilegeFromName("jcr:setProperties"));
        }
        if ((permissions & Permission.REMOVE_PROPERTY) != 0) {
            privileges.add(privilegeFromName("jcr:setProperties"));
        }
        return hasPrivileges(parentJcrPath, privileges.toArray(new Privilege[privileges.size()]));
    }


    /**
     * Just forwards the call to <code>isGranted(Path,int)</code>
     * @see HippoAccessManager#isGranted(Path,int)
     * @see AccessManager#isGranted(Path, Name, int)
     */
    public boolean isGranted(Path parentPath, Name childName, int permissions) throws RepositoryException {
        Path p = PathFactoryImpl.getInstance().create(parentPath, childName, true);
        return isGranted(p, permissions);
    }


    /**
     * @see AccessManager#canRead(Path)
     */
    public boolean canRead(Path absPath) throws PathNotFoundException, RepositoryException {
        checkInitialized();

        // allow everything to the system user
        if (isSystem) {
            return true;
        }

        // find the id
        NodeId id;
        try {
            id = getNodeId(absPath);
        } catch (PathNotFoundException e) {
            log.warn("Unable to find node id, allowing read permissions " + npRes.getJCRPath(absPath), e);
            return true;
        }

        try {
            if (canRead(id)) {
                return true;
            }
        } catch (NoSuchItemStateException e) {
            // shouldn't happen, the id was already found
            log.error("No item id found", e);
            return false;
        }
        return false;
    }

    /**
     * Always allow.
     * @see AccessManager#canAccess(String)
     */
    public boolean canAccess(String workspaceName) throws NoSuchWorkspaceException, RepositoryException {
        // no workspace restrictions yet
        return true;
    }


    //---------------------------------------- Methods ---------------------------------------------//
    /**
     * Check whether a user can read the node with the given id
     * @param id the id of the node to check
     * @return true if the user is allowed to read the node
     * @throws NoSuchItemStateException
     * @throws RepositoryException
     */
    private boolean canRead(NodeId id) throws NoSuchItemStateException, RepositoryException {
        // check cache
        Boolean allowRead = readAccessCache.get(id);
        if (allowRead != null) {
            return allowRead.booleanValue();
        }

        if (log.isDebugEnabled()) {
            log.debug("Checking canRead for node: {}", npRes.getJCRPath(hierMgr.getPath(id)));
        }

        NodeState nodeState = (NodeState) getItemState(id);
        if (nodeState.getStatus() == NodeState.STATUS_NEW) {
            // allow read to new nodes in own session
            // the write check is done on save
            readAccessCache.put(id, true);
            return true;
        }

        // make sure all parent nodes are readable
        if (!rootNodeId.equals(id)) {
            if (!canRead(nodeState.getParentId())) {
                readAccessCache.put(id, false);
                return false;
            }
        }

        Set<FacetAuthPrincipal> faps = subject.getPrincipals(FacetAuthPrincipal.class);
        for (FacetAuthPrincipal fap : faps) {
            Set<String> privs = fap.getPrivileges();
            if (privs.contains("jcr:read")) {
                if (isNodeInDomain(nodeState, fap)) {
                    readAccessCache.put(id, true);
                    return true;
                }
            }
        }
        readAccessCache.put(id, false);
        if (log.isInfoEnabled()) {
            log.info("DENIED read : {}", npRes.getJCRPath(hierMgr.getPath(id)));
        }
        return false;
    }
    
    /**
     * Try to read the cache size from the configuration
     * @return the size or -1 when not found
     */
    private int getPermCacheSize() {
        try {
            PathResolver resolver = new ParsingPathResolver(PathFactoryImpl.getInstance(), npRes);
            Path path = resolver.getQPath("/" + HippoNodeType.CONFIGURATION_PATH + "/" + HippoNodeType.SECURITY_PATH
                    + "/" + HippoNodeType.ACCESSMANAGER_PATH + "/" + HippoNodeType.HIPPO_PERMISSIONCACHESIZE);
            PropertyId confId = hierMgr.resolvePropertyPath(path);
            if (confId != null) {
                PropertyState state = (PropertyState) getItemState(confId);
                InternalValue[] iVals = state.getValues();
                if (iVals.length > 0 && ((int) iVals[0].getLong()) > 0) {
                    return (int) iVals[0].getLong();
                }
            }
        } catch (NoSuchItemStateException e) {
            // not configured, expected
        } catch (RepositoryException e) {
            // too bad.. no correct config found
        }
        return -1;
    }

    /**
     * Check if a node is in the domain of the facet auth principal by looping of all the
     * domain rules. For each domain all the facet rules are checked.
     * @param nodeState the state of the node to check
     * @param fap the facet auth principal to check
     * @return true if the node is in the domain of the facet auth
     * @throws RepositoryException
     * @see FacetAuthPrincipal
     */
    private boolean isNodeInDomain(NodeState nodeState, FacetAuthPrincipal fap) throws NoSuchItemStateException,
            RepositoryException {
        log.trace("Checking if node : {} is in domain of {}", nodeState.getId(), fap);
        boolean isInDomain = false;

        currentDomainRoleIds.clear();
        currentDomainRoleIds.addAll(fap.getRoles());

        // check is node matches ONE of the domain rules
        for (DomainRule domainRule : fap.getRules()) {

            boolean allRulesMatched = true;

            // no facet rules means no match
            if (domainRule.getFacetRules().size() == 0) {
                allRulesMatched = false;
                log.debug("No facet rules found for : {} in domain rule: {}", nodeState.getId(), domainRule);
            }
            // check if node matches ALL of the facet rules
            for (FacetRule facetRule : domainRule.getFacetRules()) {
                if (!matchFacetRule(nodeState, facetRule)) {
                    allRulesMatched = false;
                    log.trace("Rule doesn't match for : {} facet rule: {}", nodeState.getId(), facetRule);
                    break;
                }
            }
            if (allRulesMatched) {
                // a match is found, don't check other domain ruels;
                isInDomain = true;
                if (log.isDebugEnabled()) {
                    log.debug("Node : " + nodeState.getId() + " found in domain " + fap.getName() + " match "+ domainRule);
                }
                break;
            } else {
                // check if node is part of a hippo:document
                NodeState docState;
                docState = getParentDoc(nodeState);
                if (docState != null) {
                    return isNodeInDomain(docState, fap);
                }
            }
        }
        return isInDomain;
    }

    /**
     * Check if a node matches the current FacetRule
     * @param nodeState the state of the node to check
     * @param facetRule the facet rule to check
     * @return true if the node matches the facet rule
     * @throws RepositoryException
     * @throws NoSuchItemStateException
     * @see FacetRule
     */
    private boolean matchFacetRule(NodeState nodeState, FacetRule facetRule) throws NoSuchItemStateException,
            RepositoryException {
        log.trace("Checking node : {} for facet rule: {}", nodeState.getId(), facetRule);

        // is this a 'NodeType' facet rule?
        if (facetRule.getFacet().equalsIgnoreCase("nodetype")) {
            boolean match = false;
            log.trace("Checking node : {} for nodeType: {}", nodeState.getId(), facetRule);
            if (isInstanceOfType(nodeState, facetRule.getValue())) {
                match = true;
                log.trace("Found match : {} for nodeType: {}", nodeState.getId(), facetRule.getValue());
            } else if (hasMixinWithValue(nodeState, facetRule.getValue())) {
                match = true;
                log.trace("Found match : {} for mixinType: {}", nodeState.getId(), facetRule.getValue());
            }
            if (facetRule.isEqual()) {
                return match;
            } else {
                return !match;
            }
        }
        
        // is this a 'NodeName' facet rule?
        if (facetRule.getFacet().equalsIgnoreCase("nodename")) {
            boolean match = false;
            if (facetRule.getType() == PropertyType.NAME) {
                log.trace("Checking node : {} for nodename: {}", nodeState.getNodeId(), facetRule);
                Name nodeName = hierMgr.getName(nodeState.getId());
                if (nodeName == null) {
                    log.warn("Failed to resolve name of {}", nodeState.getNodeId());
                } else {
                    if (FacetAuthConstants.EXPANDER_USER.equals(facetRule.getValue())) {
                        if (isUser && userId.equals(npRes.getJCRName(nodeName))) {
                            match = true;
                        }
                    } else if (FacetAuthConstants.EXPANDER_GROUP.equals(facetRule.getValue())) {
                        if (isUser && groupIds.contains(npRes.getJCRName(nodeName))) {
                            match = true;
                        }
                    } else if (nodeName.equals(facetRule.getValueName())) {
                        match = true;
                    }
                }
            }
            if (facetRule.isEqual()) {
                return match;
            } else {
                return !match;
            }
        }

        // check if node has the required property value
        if (matchPropertyWithFacetRule(nodeState, facetRule)) {
            log.trace("Found match : {} for facetVal: {}", nodeState.getId(), facetRule);
            return true;
        }
        return false;
    }

    /**
     * Helper function to check if a nodeState is of a node type or a
     * instance of the node type (sub class)
     *
     * @param nodeState the node to check
     * @param nodeTypeName the node type name
     * @return boolean
     * @throws NoSuchNodeTypeException
     */
    private boolean isInstanceOfType(NodeState nodeState, String nodeType) throws NamespaceException,
            NoSuchNodeTypeException, RepositoryException {
        // create NodeType of nodeState's primaryType
        String nodeStateType = npRes.getJCRName(nodeState.getNodeTypeName());

        if (nodeStateType.equals(nodeType)) {
            if (log.isTraceEnabled()) {
                log.trace("MATCH " + nodeState.getId() + " is of type: " + nodeType);
            }
            return true;
        }

        Boolean isInstance = ntIOCache.get(nodeStateType, nodeType);
        if (isInstance != null) {
            return isInstance.booleanValue();
        }

        // get iterator over all types
        NodeTypeIterator allTypes = ntMgr.getAllNodeTypes();
        NodeType nodeStateNodeType = ntMgr.getNodeType(nodeStateType);

        // iterate over All NodeTypes untill...
        while (allTypes.hasNext()) {
            NodeType nt = allTypes.nextNodeType();
            // the correct NodeType is found
            if (nt.equals(nodeStateNodeType)) {
                // get all supertypes of the nodeState's primaryType's NodeType
                NodeType[] superTypes = nt.getSupertypes();
                // check if one of the superTypes matches the nodeType
                for (NodeType type : superTypes) {
                    if (type.getName().equals(nodeType)) {
                        ntIOCache.put(nodeStateType, nodeType, true);
                        return true;
                    }
                }
            }
        }
        ntIOCache.put(nodeStateType, nodeType, false);
        return false;
    }

    /**
     * Check if a node matches the current FacetRule based on a
     * check on the properties of the node.
     * @param nodeState the state of the node to check
     * @param facetRule the facet rule to check
     * @return true if the node matches the facet rule
     * @throws RepositoryException
     * @see FacetRule
     */
    private boolean matchPropertyWithFacetRule(NodeState nodeState, FacetRule rule) throws NoSuchItemStateException,
            RepositoryException {

        boolean match = false;

        // jcr:primaryType isn't really a property
        if (rule.getFacetName().equals(NameConstants.JCR_PRIMARYTYPE)) {
            // WILDCARD match, jcr:primaryType == *
            if (FacetAuthConstants.WILDCARD.equals(rule.getValue())) {
                match = true;
            } else if (nodeState.getNodeTypeName().equals(rule.getValueName())) {
                match = true;
            } else {
                match = false;
            }

            // invert match on inequality
            if (rule.isEqual()) {
                return match;
            } else {
                return !match;
            }
        }

        // the hierarchy manager is attic aware. The property can also be in the removed properties
        if (!nodeState.hasPropertyName(rule.getFacetName())
                && !nodeState.getRemovedPropertyNames().contains(rule.getFacetName())) {
            log.trace("Node: {} doesn't have property {}", nodeState.getId(), rule.getFacetName());

            // if this is a filter facet rule the property doesn't have to be set
            if (rule.isFilter()) {
                return true;
            }

            if (FacetAuthConstants.WILDCARD.equals(rule.getValue()) && !rule.isEqual()) {
                return true;
            } else {
                return false;
            }
        }

        // Property is set

        // Check WILDCARD match
        if (FacetAuthConstants.WILDCARD.equals(rule.getValue())) {
            if (rule.isEqual()) {
                return true;
            } else {
                return false;
            }
        }

        // Check property value
        PropertyId propertyId = new PropertyId(nodeState.getNodeId(), rule.getFacetName());
        PropertyState state = (PropertyState) getItemState(propertyId);
        InternalValue[] iVals = state.getValues();

        for (InternalValue iVal : iVals) {
            // types must match
            if (iVal.getType() != rule.getType()) {
                continue;
            }

            if (iVal.getType() == PropertyType.STRING) {
                log.trace("Checking facet rule: {} (string) -> {}", rule, iVal.getString());

                // expander matches
                if (FacetAuthConstants.EXPANDER_USER.equals(rule.getValue())) {
                    if (isUser && userId.equals(iVal.getString())) {
                        match = true;
                        break;
                    }
                }
                if (FacetAuthConstants.EXPANDER_GROUP.equals(rule.getValue())) {
                    if (isUser && groupIds.contains(iVal.getString())) {
                        match = true;
                        break;
                    }
                }
                if (FacetAuthConstants.EXPANDER_ROLE.equals(rule.getValue())) {
                    if (isUser && currentDomainRoleIds.contains(iVal.getString())) {
                        match = true;
                        break;
                    }
                }

                if (iVal.getString().equals(rule.getValue())) {
                    match = true;
                    break;
                }
            } else if (iVal.getType() == PropertyType.NAME) {
                log.trace("Checking facet rule: {} (name) -> {}", rule, iVal.getQName());

                if (iVal.getQName().equals(rule.getValueName())) {
                    match = true;
                    break;
                }
            }
        }
        if (rule.isEqual()) {
            return match;
        } else {
            // the property is set but the values don't match
            return !match;
        }
    }

    /**
     * Check if the node has a mixin type with a specific value
     * @param nodeState the node to check
     * @param value the mixin type to check for. This is the String representation of the Name
     * @return true if the node has the mixin type
     * @throws RepositoryException
     */
    private boolean hasMixinWithValue(NodeState nodeState, String value) throws NoSuchItemStateException,
            RepositoryException {
        if (!nodeState.hasPropertyName(NameConstants.JCR_MIXINTYPES)) {
            return false;
        }

        PropertyId propertyId = new PropertyId(nodeState.getNodeId(), NameConstants.JCR_MIXINTYPES);
        PropertyState state = (PropertyState) getItemState(propertyId);
        InternalValue[] iVals = state.getValues();

        for (InternalValue iVal : iVals) {
            // types must match
            if (iVal.getType() == PropertyType.NAME) {

                // WILDCARD match
                if (value.equals(FacetAuthConstants.WILDCARD)) {
                    return true;
                }

                log.trace("Checking facetVal: {} (name) -> {}", value, iVal.getQName());
                if (iVal.getQName().toString().equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get the <code>NodeId</code> for the absolute path. If the absolute path points
     * to a property return the NodeId of the parent.
     * @param String the absolute path
     * @return the NodeState of the node (holding the property)
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    private NodeId getNodeId(String absPath) throws PathNotFoundException, RepositoryException {
        checkInitialized();
        return getNodeId(npRes.getQPath(absPath));
    }

    /**
     * Get the <code>NodeId</code> for the absolute path. If the absolute path points
     * to a property return the NodeId of the parent.
     * @param absPath the absolute path
     * @return the NodeId of the node (holding the property)
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    private NodeId getNodeId(Path absPath) throws PathNotFoundException, RepositoryException {
        checkInitialized();

        if (!absPath.isAbsolute()) {
            throw new RepositoryException("Absolute path expected, got " + npRes.getJCRPath(absPath));
        }

        if (absPath.denotesRoot()) {
            return rootNodeId;
        }
        
        try {
            // try property first
            PropertyId pId = hierMgr.resolvePropertyPath(absPath);
            if (pId != null) {
                NodeId id = pId.getParentId();
                return id;
            }
            
            NodeId id = hierMgr.resolveNodePath(absPath);
            if (id != null) {
                return id;
            }
        } catch (RepositoryException e) {
            // the org.apache.jackrabbit.core.state.NoSuchItemStateException is wrapped in a RepositoryException
            // so if the node is only available in the attic this is expected
            if (log.isDebugEnabled()) {
                log.debug("Error while looking up node, intentionally ignored: " + npRes.getJCRPath(absPath), e);
            }
        }

        // try zombie parent, probably a property
        PropertyId pId = zombieHierMgr.resolvePropertyPath(absPath);
        if (pId != null) {
            NodeId id = pId.getParentId();
            return id;
        }
        
        // not in the normal hierarchy manager try the attic aware as fallback, because it's way slower
        NodeId id = zombieHierMgr.resolveNodePath(absPath);
        if (id != null) {
            return id;
        }
        throw new PathNotFoundException("Unable to resolve the node id from the path " + npRes.getJCRPath(absPath));
    }

    /**
     * Try to get a state from the item manager by first checking the normal states,
     * then checking the transient states and last checking the attic state.
     * @param id the item id
     * @return the item state
     * @throws NoSuchItemStateException when the state cannot be found
     */
    private ItemState getItemState(ItemId id) throws NoSuchItemStateException {
        if (id == null) {
            throw new IllegalArgumentException("ItemId cannot be null");
        }
        if (itemMgr.hasItemState(id)) {
            try {
                return itemMgr.getItemState(id);
            } catch (ItemStateException e) {
                log.debug("Error while trying to get item state from the normal ism of id: " + id, e);
            }
        } else if (itemMgr.hasTransientItemState(id)) {
            try {
                return itemMgr.getTransientItemState(id);
            } catch (ItemStateException e) {
                log.debug("Error while trying to get item state from the transient ism of id: " + id, e);
            }
        } else if (itemMgr.hasTransientItemStateInAttic(id)) {
            try {
                return itemMgr.getAtticItemState(id);
            } catch (ItemStateException e) {
                log.debug("Error while trying to get item state from the attic ism of id: " + id, e);
            }
        }
        
        // nothing found...
        String msg = "Item state not found in normal, transient or attic: " + id;
        NoSuchItemStateException e = new NoSuchItemStateException(msg);
        log.debug(msg,e);
        throw e;
    }

    /**
     * Get the NodeState of the parent from the nodeState in a transient and attic
     * safe manner.
     * @param nodeState
     * @return the parent nodestate
     * @throws NoSuchItemStateException when the state cannot be found
     */
    private NodeState getParentState(NodeState nodeState) throws NoSuchItemStateException {
        if (rootNodeId.equals(nodeState.getId())) {
            throw new NoSuchItemStateException("RootNode doesn't have a parent state.");
        }
        try {
            NodeId id = nodeState.getParentId();
            if (id == null) {
                // for nodeState's from the attic the parentId() is null, so use path resolving to find parent
                return (NodeState) getItemState(zombieHierMgr.resolveNodePath(zombieHierMgr.getPath(nodeState.getId()).getAncestor(1)));
            }
            return (NodeState) getItemState(id);
        } catch (RepositoryException e) {
            throw new NoSuchItemStateException("Unable to find parent nodeState of node: " + nodeState.getId(), e);
        }
    }


    /**
     * Helper function to find a hippo:document instance node type. This
     * can be used to check for facet authorization on the root of a
     * document (bonzai tree).
     * @param nodeState the node of which to check the parents
     * @return NodeState the parent node state or null
     * @throws NoSuchItemStateException 
     * @throws RepositoryException 
     */
    private NodeState getParentDoc(NodeState nodeState) throws NoSuchItemStateException, RepositoryException {

        if (log.isTraceEnabled()) {
            log.trace("Checking " + nodeState.getId() + " ntn: " + nodeState.getNodeTypeName()
                    + " for being part of a document model.");
        }
        // check if this is already the root of a document
        try {
            if (isInstanceOfType(nodeState, HippoNodeType.NT_DOCUMENT) || nodeState.getNodeTypeName().equals(hippoHandle)
                    || nodeState.getNodeTypeName().equals(hippoFacetSearch)
                    || nodeState.getNodeTypeName().equals(hippoFacetSelect)) {
                if (log.isTraceEnabled()) {
                    log.trace("Node is already document root: " + nodeState.getNodeTypeName());
                }
                return null;
            }
        } catch (NamespaceException e) {
            log.warn("NamespaceException while trying to get parent doc", e);
            return null;
        } catch (NoSuchNodeTypeException e) {
            log.warn("NoSuchNodeTypeException while trying to get parent doc", e);
            return null;
        }
        
        // walk up in the hierarchy
        while (!rootNodeId.equals(nodeState.getId())) {
            // shift one up in hierarchy
            nodeState = getParentState(nodeState);
            
            if (nodeState.getNodeTypeName().equals(hippoHandle)) {
                if (log.isDebugEnabled()) {
                    log.debug("MATCH hippoHandle: " + nodeState.getNodeTypeName());
                }
                return null;
            }
            if (nodeState.getNodeTypeName().equals(hippoFacetSearch)) {
                if (log.isDebugEnabled()) {
                    log.debug("MATCH hippoFacetSearch: " + nodeState.getNodeTypeName());
                }
                return null;
            }
            if (nodeState.getNodeTypeName().equals(hippoFacetSelect)) {
                if (log.isDebugEnabled()) {
                    log.debug("MATCH hippoFacetSelect: " + nodeState.getNodeTypeName());
                }
                return null;
            }
            try {
                if (isInstanceOfType(nodeState, HippoNodeType.NT_DOCUMENT)) {
                    if (log.isDebugEnabled()) {
                        log.debug("MATCH hippoDoc: " + nodeState.getNodeTypeName());
                    }
                    return nodeState;
                }
            } catch (NamespaceException e) {
                log.warn("NamespaceException while trying to get parent doc", e);
                return null;
            } catch (NoSuchNodeTypeException e) {
                log.warn("NoSuchNodeTypeException while trying to get parent doc", e);
                return null;
            }
        }
        return null;
    }

    /**
     * Simple Cache for <String, <String,Boolean>> key-value pairs
     */
    private static class NodeTypeInstanceOfCache {

        /**
         * The cache map
         */
        private static Map<String, Map<String, Boolean>> map = new WeakHashMap<String, Map<String, Boolean>>();

        private static NodeTypeInstanceOfCache cache = new NodeTypeInstanceOfCache();

        /**
         * Create a new LRU cache
         * @param size max number of cache objects
         */
        private NodeTypeInstanceOfCache() {
        }

        private static NodeTypeInstanceOfCache getInstance() {
            return cache;
        }

        /**
         * Fetch cache value
         * @param
         * @return cached value or null when not in cache
         */
        synchronized public Boolean get(String type, String instanceOfType) {
            Boolean bool = null;
            Map<String, Boolean> typeMap = map.get(instanceOfType);
            if (typeMap != null) {
                return typeMap.get(type);
            }
            return bool;
        }

        /**
         * Store key-value in cache
         * @param id ItemId the key
         * @param isGranted the value
         */
        synchronized public void put(String type, String instanceOfType, boolean isInstanceOf) {
            Map<String, Boolean> typeMap = map.get(instanceOfType);
            if (typeMap == null) {
                typeMap = new WeakHashMap<String, Boolean>();
            }
            typeMap.put(type, isInstanceOf);
            map.put(instanceOfType, typeMap);
        }

        /**
         * Remove key-value from cache
         * @param id ItemId the key
         */
        synchronized public void remove(String type, String instanceOfType) {
            Map<String, Boolean> typeMap = map.get(instanceOfType);
            if (typeMap == null) {
                return;
            }

            if (typeMap.containsKey(type)) {
                map.remove(type);
            }
        }

        /**
         * Clear the cache
         */
        synchronized public void clear() {
            map.clear();
        }
    }



    //---------------------------- ACCESS CONTROL MANAGER ---------------------------//
    
    /**
     * @see AccessControlManager#getSupportedPrivileges(String)
     */
    public Privilege[] getSupportedPrivileges(String absPath) throws PathNotFoundException, RepositoryException {
        checkInitialized();
        checkValidNodePath(absPath);
        // return all known privileges everywhere.
        return currentPrivileges.values().toArray(new Privilege[currentPrivileges.size()]);
    }

    /**
     * @see AccessControlManager#privilegeFromName(String)
     */
    public Privilege privilegeFromName(String privilegeName)
            throws AccessControlException, RepositoryException {
        checkInitialized();

        if (currentPrivileges.containsKey(privilegeName)) {
            return currentPrivileges.get(privilegeName);
        }
        
        currentPrivileges.put(privilegeName, new Privilege() {

            private String name;

            public Privilege[] getAggregatePrivileges() {
                return new Privilege[0];
            }

            public Privilege[] getDeclaredAggregatePrivileges() {
                return new Privilege[0];
            }

            public Privilege create(String name) {
                this.name = name;
                return this;
            }

            public String getName() {
                return name;
            }

            public boolean isAbstract() {
                return false;
            }

            public boolean isAggregate() {
                return false;
            }
        }.create(privilegeName));

        return currentPrivileges.get(privilegeName);
    }
    
    /**
     * @see AccessControlManager#hasPrivileges(String,Privilege[])
     */
    public boolean hasPrivileges(String absPath, Privilege[] privileges) throws PathNotFoundException,
            RepositoryException {
        checkInitialized();

        // system session can do everything
        if (isSystem) {
            return true;
        }
        
        // user is always allowed to do nothing
        if (privileges == null || privileges.length == 0) {
            log.debug("No privileges to check for path: {}.", absPath);
            return true;
        }

        // get the id of the node or of the parent node if absPath points to a property
        NodeId id = getNodeId(absPath);
        
        // fast track read check
        if (privileges.length == 1 && "jcr:read".equals(privileges[0].getName())) {
            try {
                return canRead(id);
            } catch (NoSuchItemStateException e) {
                throw new PathNotFoundException("Path not found " + absPath, e);
            }

        }
        NodeState nodeState;
        try {
            nodeState = (NodeState) getItemState(id);
        } catch (NoSuchItemStateException e) {
            throw new PathNotFoundException("Path not found " + absPath, e);
        }
        if(nodeState.getStatus() == NodeState.STATUS_NEW) {
            return true;
        }

        for (Privilege priv : privileges) {
            log.debug("Checking [{}] : {}", priv.getName(), absPath);
            boolean allowed = false;
            for (FacetAuthPrincipal fap : subject.getPrincipals(FacetAuthPrincipal.class)) {
                if (log.isDebugEnabled()) {
                    log.debug("Checking [" + priv + "] : " + absPath + " against FacetAuthPrincipal: " + fap);
                }

                if (fap.getPrivileges().contains(priv.getName())) {
                    try {
                        if (isNodeInDomain(nodeState, fap)) {
                            allowed = true;
                            if (log.isInfoEnabled()) {
                                log.info("GRANT: " + priv.getName() + " to user " + userId + " in domain " + fap + " for " + absPath);
                            }
                            break;
                        }
                    } catch (NoSuchItemStateException e) {
                        throw new PathNotFoundException("Unable to find path: " + absPath, e);
                    }
                }
            }
            if (!allowed) {
                if (log.isInfoEnabled()) {
                    log.info("DENY: " + priv.getName() + " to user " + userId + " for " + absPath);
                }
                return false;
            }
        }

        // not a read, remove node from cache
        readAccessCache.remove(id);
        
        return true;
    }
    

    /**
     * @see AccessControlManager#getPrivileges(String)
     */
    public Privilege[] getPrivileges(String absPath) throws PathNotFoundException, RepositoryException {
        checkInitialized();
        
        NodeId id = getNodeId(absPath);
        NodeState nodeState;
        try {
            nodeState = (NodeState) getItemState(id);
        } catch (NoSuchItemStateException e) {
            throw new PathNotFoundException("Path not found " + absPath);
        }
        
        Set<Privilege> privileges = new HashSet<Privilege>();
        for (FacetAuthPrincipal fap : subject.getPrincipals(FacetAuthPrincipal.class)) {
            try {
                if (isNodeInDomain(nodeState, fap)) {
                    for (String privilegeName : fap.getPrivileges()) {
                        privileges.add(privilegeFromName(privilegeName));
                    }
                }
            } catch (NoSuchItemStateException e) {
                throw new PathNotFoundException("Unable to find path: " + absPath);
            }
        }
        return privileges.toArray(new Privilege[privileges.size()]);
    }
    

    /**
     * Always return empty array of <code>AccessControlPolicy</code>
     * 
     * @see AccessControlManager#getPolicies(String)
     */
    public AccessControlPolicy[] getPolicies(String absPath) throws PathNotFoundException, AccessDeniedException, RepositoryException {
        checkInitialized();
        //checkPrivileges(absPath, PrivilegeRegistry.READ_AC);

        log.debug("Implementation does not provide applicable policies -> getPolicy() always returns an empty array.");
        return new AccessControlPolicy[0];
    }

    /**
     * Always return empty array of <code>AccessControlPolicy</code>
     * 
     * @see AccessControlManager#getEffectivePolicies(String)
     */
    public AccessControlPolicy[] getEffectivePolicies(String absPath) throws PathNotFoundException,
            AccessDeniedException, RepositoryException {
        return new AccessControlPolicy[0];
    }

    /**
     * Always return <code>AccessControlPolicyIteratorAdapter.EMPTY</code>
     * 
     * @see AccessControlManager#getApplicablePolicies(String)
     */
    public AccessControlPolicyIterator getApplicablePolicies(String absPath) throws PathNotFoundException, AccessDeniedException, RepositoryException {
        checkInitialized();
        //checkPrivileges(absPath, PrivilegeRegistry.READ_AC);
        log.debug("Implementation does not provide applicable policies -> returning empty iterator.");
        return AccessControlPolicyIteratorAdapter.EMPTY;
    }

    /**
     * Always throws <code>AccessControlException</code>
     *
     * @see AccessControlManager#setPolicy(String, AccessControlPolicy)
     */
    public void setPolicy(String absPath, AccessControlPolicy policy) throws PathNotFoundException, AccessControlException, AccessDeniedException, RepositoryException {
        checkInitialized();
        //checkPrivileges(absPath, PrivilegeRegistry.MODIFY_AC);
        throw new AccessControlException("AccessControlPolicy " + policy + " cannot be applied.");
    }

    /**
     * Always throws <code>AccessControlException</code>
     *
     * @see AccessControlManager#removePolicy(String, AccessControlPolicy)
     */
    public void removePolicy(String absPath, AccessControlPolicy policy) throws PathNotFoundException, AccessControlException, AccessDeniedException, RepositoryException {
        checkInitialized();
        //checkPrivileges(absPath, PrivilegeRegistry.MODIFY_AC);
        throw new AccessControlException("No AccessControlPolicy has been set through this API -> Cannot be removed.");
    }
    //------------------------------ END ACCESS CONTROL MANAGER -------------------------------------------//
    
    
    /**
     * Check if this manager has been properly initialized.
     *
     * @throws IllegalStateException If this manager has not been properly initialized.
     */
    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }
    }
    
    /**
     * Build a qualified path from the specified <code>absPath</code> and test
     * if it is really absolute and points to an existing node.
     *
     * @param absPath
     * @throws PathNotFoundException if no node at <code>absPath</code> exists
     * or the session does not have privilege to retrieve the node.
     * @throws RepositoryException If the given <code>absPath</code> is not
     * absolute or if some other error occurs.
     */
    private void checkValidNodePath(String absPath) throws PathNotFoundException, RepositoryException {
        Path p = npRes.getQPath(absPath);
        if (!p.isAbsolute()) {
            throw new RepositoryException("Absolute path expected " + absPath);
        }
        if (hierMgr.resolveNodePath(p) == null) {
            throw new PathNotFoundException("No such node " + absPath);
        }
    }

    /**
     * Helper method for pretty printing the requested permission
     * @param permissions
     * @return a string representation of the permissions
     */
    private String permsString(int permissions) {
        StringBuffer buf = new StringBuffer();

        // narrow down permissions
        if ((permissions & Permission.READ) != 0) {
            buf.append("re");
        } else {
            buf.append("--");
        }
        if ((permissions & Permission.ADD_NODE) != 0) {
            buf.append("an");
        } else {
            buf.append("--");
        }
        if ((permissions & Permission.REMOVE_NODE) != 0) {
            buf.append("rn");
        } else {
            buf.append("--");
        }
        if ((permissions & Permission.SET_PROPERTY) != 0) {
            buf.append("sp");
        } else {
            buf.append("--");
        }
        if ((permissions & Permission.REMOVE_PROPERTY) != 0) {
            buf.append("rp");
        } else {
            buf.append("--");
        }
        return buf.toString();
    }


}

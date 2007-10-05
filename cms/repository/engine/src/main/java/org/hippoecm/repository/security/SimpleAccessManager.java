/*
 * Copyright 2007 Hippo
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
package org.hippoecm.repository.security;

import javax.security.auth.Subject;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.ItemId;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.SystemPrincipal;
import org.apache.jackrabbit.core.security.UserPrincipal;
import org.apache.jackrabbit.core.state.ItemState;
import org.apache.jackrabbit.core.state.ItemStateException;
import org.apache.jackrabbit.core.state.NoSuchItemStateException;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.name.QName;

import org.hippoecm.repository.jackrabbit.HippoHierarchyManager;

public class SimpleAccessManager extends org.apache.jackrabbit.core.security.SimpleAccessManager implements AccessManager {

    private boolean initialized;

    /**
     * Empty constructor
     */
    public SimpleAccessManager() {
        super();
        initialized = false;
    }

    //--------------------------------------------------------< AccessManager >

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(AMContext context) throws AccessDeniedException, Exception {
        if (initialized) {
            throw new IllegalStateException("already initialized");
        }
        super.init(context);

        initialized = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void close() throws Exception {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        super.close();
        initialized = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkPermission(ItemId id, int permissions) throws AccessDeniedException, ItemNotFoundException,
            RepositoryException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        if (system) {
            // system has always all permissions
            return;
        }

        super.checkPermission(id, permissions);

        for(UserPrincipal principal : subject.getPrincipals(UserPrincipal.class)) {
            String username = principal.getName();
            // FIXME: here the username could be used to determin the role
            // and allow or deny access
        }

        // FIXME Current simple implementation to be removed for M2.
        try {
            ItemState itemState = ((HippoHierarchyManager)hierMgr).getItemState(id);
            if(itemState.isNode()) {
                NodeState nodeState = (NodeState) itemState;
                for(Object mixin : nodeState.getMixinTypeNames()) {
                    QName mixinName = (QName) mixin;
                    if(mixinName.toString().equals("{http://www.hippoecm.org/nt/1.0}restricted"))
                        throw new AccessDeniedException();
                }
            }
        } catch(NoSuchItemStateException ex) {
        } catch(ItemStateException ex) {
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isGranted(ItemId id, int permissions) throws ItemNotFoundException, RepositoryException {
        if (!initialized) {
            throw new IllegalStateException("not initialized");
        }

        if (system) {
            // system has always all permissions
            return true;
        }

        if(super.isGranted(id, permissions) == false)
            return false;

        for(UserPrincipal principal : subject.getPrincipals(UserPrincipal.class)) {
            String username = principal.getName();
            // FIXME: here the username could be used to determin the role
            // and allow or deny access
        }

        // FIXME Current simple implementation to be removed for M2.
        try {
            ItemState itemState = ((HippoHierarchyManager)hierMgr).getItemState(id);
            if(itemState.isNode()) {
                NodeState nodeState = (NodeState) itemState;
                for(Object mixin : nodeState.getMixinTypeNames()) {
                    QName mixinName = (QName) mixin;
                    if(mixinName.toString().equals("{http://www.hippoecm.org/nt/1.0}restricted"))
                        return false;
                }
            }
        } catch(NoSuchItemStateException ex) {
        } catch(ItemStateException ex) {
        }

        return true; // FIXME: should default be false
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canAccess(String workspaceName) throws NoSuchWorkspaceException, RepositoryException {
        return super.canAccess(workspaceName);
    }
}

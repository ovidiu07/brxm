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

import java.io.File;

import javax.jcr.nodetype.NodeTypeManager;
import javax.security.auth.Subject;

import org.apache.jackrabbit.core.HierarchyManager;
import org.apache.jackrabbit.core.fs.FileSystem;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.state.SessionItemStateManager;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;

/**
 * An <code>AMContext</code> is used to provide context information for an
 * <code>AccessManager</code>.
 *
 * @see AccessManager#init(AMContext)
 */
public class HippoAMContext extends AMContext {

    /** SVN id placeholder */
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    /**
     * NodeTypeManager for resolving superclass node types
     */
    private final NodeTypeManager ntMgr;

    /**
     * SessionItemStateManager for fetching attic states
     */
    private final SessionItemStateManager itemMgr;

    /**
     * Creates a new <code>AMContext</code>.
     *
     * @param physicalHomeDir the physical home directory
     * @param fs              the virtual jackrabbit filesystem
     * @param subject         subject whose access rights should be reflected
     * @param hierMgr         hierarchy manager
     * @param nsResolver      namespace resolver
     * @param workspaceName   workspace name
     */
    public HippoAMContext(File physicalHomeDir,
                     FileSystem fs,
                     Subject subject,
                     HierarchyManager hierMgr,
                     SessionItemStateManager itemMgr,
                     NamespaceResolver nsResolver,
                     String workspaceName,
                     NodeTypeManager ntMgr) {
        super(physicalHomeDir, fs, subject, hierMgr, nsResolver, workspaceName);
        this.ntMgr = ntMgr;
        this.itemMgr = itemMgr;
    }

    /**
     * Returns the NodeTypeRegistry
     *
     * @return the NodeTypeRegistry
     */
    public NodeTypeManager getNodeTypeManager() {
        return ntMgr;
    }

    /**
     * Returns the SessionItemStateManager associated with the user session
     * @return the SessionItemStateManager
     */
    public SessionItemStateManager getSessionItemStateManager() {
        return itemMgr;
    }
}

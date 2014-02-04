/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.hst.pagecomposer.jaxrs.services.helpers;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import com.google.common.collect.Iterables;

import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.model.LinkType;
import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMenuItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.AbstractConfigResource;

import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMENUITEM_PROPERTY_EXTERNALLINK;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMENUITEM_PROPERTY_REFERENCESITEMAPITEM;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMENUITEM_PROPERTY_REPOBASED;
import static org.hippoecm.hst.configuration.HstNodeTypes.SITEMENUITEM_PROPERTY_ROLES;

public class SiteMenuItemHelper extends AbstractHelper {

    private final SiteMenuHelper menuHelper = new SiteMenuHelper();

    @Override
    public <T> T getConfigObject(final String itemId) {
        throw new UnsupportedOperationException("Cannot fetch site menu item without menu id");
    }

    /**
     * Saves the properties of the new item into the node, provided that the names of the node and the item are equal.
     *
     * @param node    a newly created node
     * @param newItem an item containing the property values for the node
     * @throws RepositoryException
     */
    public void save(Node node, SiteMenuItemRepresentation newItem) throws RepositoryException {
        assert node.getName().equals(newItem.getName()) : "Precondition violated: node and item name must be equal";
        update(node, newItem);
    }

    /**
     * Updates the properties of the given node with those of the modified item.
     *
     * @param node         a node
     * @param modifiedItem an item containing the property values of the modified item
     * @throws RepositoryException
     */
    public void update(Node node, SiteMenuItemRepresentation modifiedItem) throws RepositoryException {

        final String modifiedName = modifiedItem.getName();
        if (modifiedName != null && !modifiedName.equals(node.getName())) {
            rename(node, modifiedName);
        }

        final String modifiedLink = modifiedItem.getLink();
        if (modifiedItem.getLinkType() == LinkType.NONE) {
            removeProperty(node, SITEMENUITEM_PROPERTY_EXTERNALLINK);
            removeProperty(node, SITEMENUITEM_PROPERTY_REFERENCESITEMAPITEM);
        } else if (modifiedItem.getLinkType() == LinkType.SITEMAPITEM) {
            node.setProperty(SITEMENUITEM_PROPERTY_REFERENCESITEMAPITEM, modifiedLink);
            removeProperty(node, SITEMENUITEM_PROPERTY_EXTERNALLINK);
        } else if (modifiedItem.getLinkType() == LinkType.EXTERNAL) {
            node.setProperty(SITEMENUITEM_PROPERTY_EXTERNALLINK, modifiedLink);
            removeProperty(node, SITEMENUITEM_PROPERTY_REFERENCESITEMAPITEM);
        }

        final boolean modifiedRepositoryBased = modifiedItem.isRepositoryBased();
        node.setProperty(SITEMENUITEM_PROPERTY_REPOBASED, modifiedRepositoryBased);

        final Map<String, String> modifiedLocalParameters = modifiedItem.getLocalParameters();
        setLocalParameters(node, modifiedLocalParameters);

        final Set<String> modifiedRoles = modifiedItem.getRoles();
        setRoles(node,modifiedRoles);

    }


    /**
     * Move the given node by appending it as the last child of the new parent and assigning it the give new node name.
     *
     * @param node        the node to move
     * @param newNodeName the new name of the node
     * @param newParent   the new parent of the node
     * @throws RepositoryException
     */
    public void move(Node node, String newNodeName, Node newParent) throws RepositoryException {
        node.getSession().move(node.getPath(), newParent.getPath() + "/" + newNodeName);
    }

    /**
     * Move the given node by appending it as the last child of the new parent.
     *
     * @param node      the node to move
     * @param newParent the new parent of the node
     * @throws RepositoryException
     */
    public void move(Node node, Node newParent) throws RepositoryException {
        move(node, node.getName(), newParent);
    }

    private void rename(Node node, String newName) throws RepositoryException {
        final Node parent = node.getParent();
        // remember the successor name, to be able to restore the node's position
        final String successorName = getSuccessorName(node, parent);
        // rename the node by moving it within the same parent
        move(node, newName, parent);
        // restore the position
        parent.orderBefore(newName, successorName);
    }

    private String getSuccessorName(final Node node, final Node parent) throws RepositoryException {
        final String currentName = node.getName();
        final Iterator<Node> children = parent.getNodes();
        while (children.hasNext() && !children.next().getName().equals(currentName)) {
        }
        if (children.hasNext()) {
            return children.next().getName();
        } else {
            return null;
        }
    }


}

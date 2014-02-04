/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.pagecomposer.jaxrs.services.validaters;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.core.jcr.RuntimeRepositoryException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.Operation;

public class PostLockValidator extends AbstractLockValidator {

    private final String id;
    private final Operation operation;
    private String itemNodeType;
    private String rootNodeType;

    public PostLockValidator(final String id,
                             final Operation operation,
                             final String itemNodeType,
                             final String rootNodeType){
        this.id = id;
        this.operation = operation;
        this.itemNodeType = itemNodeType;
        this.rootNodeType = rootNodeType;

    }

    @Override
    public void validate() throws RuntimeException {
        try {
            HstRequestContext requestContext = RequestContextProvider.get();
            final Session session = requestContext.getSession();
            session.refresh(true);
            final Node node = checkNodeForLock(session, id);
            if (!node.isNodeType(itemNodeType)) {
                throw new IllegalArgumentException("Expected node of type '"+itemNodeType+
                        "' but was '"+node.getPrimaryNodeType().getName()+"'");
            }

            // assert current user has locked the node (or an ancestor)
            String lockedBy = getLockedDeepBy(node, rootNodeType);
            if (!node.getSession().getUserID().equals(lockedBy)) {
                throw new IllegalStateException("Node for '"+node.getPath()+"' should be locked by '"+node.getSession().getUserID()+"' but found to be locked" +
                        " by '"+lockedBy+"'.");
            }

        } catch (ItemNotFoundException e) {
            throw new IllegalStateException("No repository sitemap node for id '"+id+"'");
        } catch (RepositoryException e) {
            throw new RuntimeRepositoryException("RepositoryException during pre-validate", e);
        }
    }

    protected Node checkNodeForLock(final Session session, final String id) throws RepositoryException {
        return session.getNodeByIdentifier(id);
    }


}

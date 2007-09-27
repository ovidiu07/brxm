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
package org.hippoecm.repository.servicing;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;

public class WorkflowDescriptorImpl extends WorkflowDescriptor implements Serializable {

    String nodeAbsPath;
    String category;

    WorkflowDescriptorImpl(WorkflowManagerImpl manager, String category, Node node, Node item) throws RepositoryException {
        this.category = category;
        nodeAbsPath = item.getPath();
        try {
            serviceName = node.getProperty(HippoNodeType.HIPPO_SERVICE).getString();
            displayName = node.getProperty(HippoNodeType.HIPPO_DISPLAY).getString();
            rendererName = node.getProperty(HippoNodeType.HIPPO_RENDERER).getString();
        } catch (PathNotFoundException ex) {
            manager.log.error("Workflow specification corrupt on node " + nodeAbsPath);
            throw new RepositoryException("workflow specification corrupt", ex);
        } catch (ValueFormatException ex) {
            manager.log.error("Workflow specification corrupt on node " + nodeAbsPath);
            throw new RepositoryException("workflow specification corrupt", ex);
        }
    }

    public String toString() {
        return getClass().getName() + "[node=" + nodeAbsPath + ",category=" + category + ",service=" + serviceName
                + ",renderer=" + rendererName + "]";
    }
}

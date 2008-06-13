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
package org.hippoecm.repository.standardworkflow;

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.ISO9075Helper;

@Deprecated
public class PrototypeWorkflowImpl implements PrototypeWorkflow {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private Session userSession;
    private Node subject;

    public PrototypeWorkflowImpl(Session userSession, Session rootSession, Node subject) throws RemoteException {
        this.subject = subject;
        this.userSession = userSession;
    }

    public String addDocument(String name) throws WorkflowException, RepositoryException, RemoteException {
        name = ISO9075Helper.encodeLocalName(name);
        if (!subject.isNodeType(HippoNodeType.NT_PROTOTYPED))
            throw new WorkflowException("Invalid node type for workflow");

        String path =  subject.getProperty(HippoNodeType.HIPPO_PROTOTYPE).getString();
        Node prototype = userSession.getRootNode().getNode(path.substring(1));
        if(prototype.hasNodes()) {
            for(NodeIterator prototypeIter = prototype.getNodes(); prototypeIter.hasNext(); ) {
                prototype = prototypeIter.nextNode();
                if(prototype != null) {
                    if(prototype.hasProperty("hippo:remodel"))
                        prototype = null;
                    else
                        break;
                }
            }
        } else {
            prototype = null;
        }
        if(prototype == null)
          throw new WorkflowException("No prototype found");
	Node newHandle = userSession.getRootNode().getNode(subject.getPath().substring(1)).addNode(name, HippoNodeType.NT_HANDLE);
	newHandle.addMixin("hippo:hardhandle");
        Node result = ((HippoSession) userSession).copy(prototype, newHandle.getPath() + "/" + name);
        if (result.isNodeType(HippoNodeType.NT_HANDLE)) {
            NodeIterator children = result.getNodes(prototype.getName());
            while (children.hasNext()) {
                Node child = children.nextNode();
                if (child.getName().equals(prototype.getName())) {
                    userSession.move(child.getPath(), result.getPath() + "/" + name);
                }
            }
        }
        subject.save();
        return result.getPath();
    }

    public String addFolder(String name) throws WorkflowException, RepositoryException, RemoteException {
        name = ISO9075Helper.encodeLocalName(name);
        if (!subject.isNodeType(HippoNodeType.NT_PROTOTYPED))
            throw new WorkflowException("Invalid node type for workflow");

        Node result = subject.addNode(name, subject.getPrimaryNodeType().getName());
        NodeType[] nodetypes = subject.getMixinNodeTypes();
        for (int i=0; i<nodetypes.length; i++) {
            result.addMixin(nodetypes[i].getName());
        }
        result.setProperty(HippoNodeType.HIPPO_PROTOTYPE, subject.getProperty(HippoNodeType.HIPPO_PROTOTYPE).getString());
        subject.save();
        return result.getPath();
    }

    public String addFolder(String name, String prototypePath) throws WorkflowException, RepositoryException, RemoteException {
        name = ISO9075Helper.encodeLocalName(name);
        if (!subject.isNodeType(HippoNodeType.NT_PROTOTYPED))
            throw new WorkflowException("Invalid node type for workflow");

        Node result = subject.addNode(name, subject.getPrimaryNodeType().getName());
        NodeType[] nodetypes = subject.getMixinNodeTypes();
        for (int i=0; i<nodetypes.length; i++) {
            result.addMixin(nodetypes[i].getName());
        }
        result.setProperty(HippoNodeType.HIPPO_PROTOTYPE, prototypePath);
        subject.save();
        return result.getPath();
    }
}

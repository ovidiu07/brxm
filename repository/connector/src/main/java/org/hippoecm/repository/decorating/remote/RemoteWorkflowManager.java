/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.repository.decorating.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowManager;

public interface RemoteWorkflowManager extends Remote {

    public RemoteWorkflowDescriptor getWorkflowDescriptor(String category, String uuid)
        throws RepositoryException, RemoteException;

    public Workflow getWorkflow(String category, String absPath)
        throws RepositoryException, RemoteException;

    public Workflow getWorkflow(String category, Document document)
        throws RepositoryException, RemoteException;

    public Workflow getWorkflow(RemoteWorkflowDescriptor descriptor)
        throws RepositoryException, RemoteException;

    public WorkflowManager getContextWorkflowManager(Object specification)
        throws RepositoryException, RemoteException;
}

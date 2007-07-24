/*
  THIS CODE IS UNDER CONSTRUCTION, please leave as is until
  work has proceeded to a stable level, at which time this comment
  will be removed.  -- Berry
*/

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
package org.hippocms.repository.jr.servicing.client;

import java.rmi.RemoteException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.apache.jackrabbit.rmi.client.ClientObject;
import org.apache.jackrabbit.rmi.client.RemoteRuntimeException;

import org.hippocms.repository.workflow.Workflow;
import org.hippocms.repository.workflow.WorkflowDescriptor;
import org.hippocms.repository.jr.servicing.WorkflowManager;
import org.hippocms.repository.jr.servicing.remote.RemoteWorkflowManager;

public class ClientWorkflowManager extends ClientObject implements WorkflowManager
{
  private Session session;
  private RemoteWorkflowManager remote;

  public ClientWorkflowManager(Session session, RemoteWorkflowManager remote, LocalServicingAdapterFactory factory) {
    super(factory);
    this.session = session;
    this.remote = remote;
  }

  public Session getSession() throws RepositoryException {
    return session;
  }

  public WorkflowDescriptor getWorkflowDescriptor(String category, Node item) throws RepositoryException {
    try {
      return remote.getWorkflowDescriptor(category, item.getPath());
    } catch(RemoteException ex) {
      throw new RemoteRuntimeException(ex);
    }
  }

  public Workflow getWorkflow(String category, Node item) throws RepositoryException {
    try {
      return remote.getWorkflow(category, item.getPath());
    } catch(RemoteException ex) {
      throw new RemoteRuntimeException(ex);
    }
  }
}

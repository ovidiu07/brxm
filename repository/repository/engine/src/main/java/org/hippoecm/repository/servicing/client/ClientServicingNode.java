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
package org.hippoecm.repository.servicing.client;

import java.rmi.RemoteException;

import javax.jcr.Session;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.rmi.client.ClientNode;
import org.apache.jackrabbit.rmi.client.RemoteRepositoryException;

import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.servicing.remote.RemoteServicingNode;

public class ClientServicingNode extends ClientNode implements HippoNode {
    private RemoteServicingNode remote;

    public ClientServicingNode(Session session, RemoteServicingNode remote, LocalServicingAdapterFactory factory) {
        super(session, remote, factory);
        this.remote = remote;
    }

    public String getDisplayName() throws RepositoryException {
        try {
            return remote.getDisplayName();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }
}

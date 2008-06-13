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
package org.hippoecm.frontend.plugins.admin;

import org.hippoecm.frontend.legacy.model.IPluginModel;
import org.hippoecm.frontend.legacy.plugin.Plugin;
import org.hippoecm.frontend.legacy.plugin.PluginDescriptor;
import org.hippoecm.frontend.legacy.plugin.channel.Channel;
import org.hippoecm.frontend.legacy.plugin.channel.Notification;
import org.hippoecm.frontend.legacy.plugin.channel.Request;

/**
 * @deprecated Use org.hippoecm.frontend.plugins.console.RootPlugin instead
 */
@Deprecated
public class RootPlugin extends Plugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public RootPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);
    }

    @Override
    public void handle(Request request) {
        // update node model
        if ("select".equals(request.getOperation()) || "flush".equals(request.getOperation())
                || "save".equals(request.getOperation())) {
            Channel outgoing = getBottomChannel();
            if (outgoing != null) {
                Notification notification = outgoing.createNotification(request);
                outgoing.publish(notification);
            }
        } else if ("edit".equals(request.getOperation())) {
            Channel outgoing = getBottomChannel();
            if (outgoing != null) {
                Notification notification = outgoing.createNotification("select", request.getModel());
                notification.setContext(request.getContext());
                outgoing.publish(notification);
            }
        }
        super.handle(request);
    }
}

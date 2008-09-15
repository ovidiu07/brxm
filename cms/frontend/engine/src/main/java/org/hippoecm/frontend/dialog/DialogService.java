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
package org.hippoecm.frontend.dialog;

import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.hippoecm.frontend.plugin.IPluginContext;

public class DialogService extends DialogWindow {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private String wicketId;
    private String serviceId;
    private IPluginContext context;
    private List<Page> pending;

    public DialogService() {
        super("id");

        pending = new LinkedList<Page>();
        setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {
            private static final long serialVersionUID = 1L;

            public void onClose(AjaxRequestTarget target) {
                page = null;
                if (pending.size() > 0) {
                    Page head = pending.remove(0);
                    DialogService.super.show(head);
                }
            }
        });
    }

    public void init(IPluginContext context, String serviceId, String wicketId) {
        this.context = context;
        this.serviceId = serviceId;
        this.wicketId = wicketId;
        context.registerService(this, serviceId);
    }

    public void destroy() {
        context.unregisterService(this, serviceId);
    }

    @Override
    public void show(Page aPage) {
        if (page != null) {
            pending.add(aPage);
        } else {
            super.show(aPage);
        }
    }

    @Override
    public String getId() {
        return wicketId;
    }
}

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
package org.hippoecm.frontend.plugins.admin.logout;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.UserSession;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogWindow;
import org.hippoecm.frontend.plugin.PluginEvent;

public class LogoutDialog extends AbstractDialog {
    private static final long serialVersionUID = 1L;

    public LogoutDialog(DialogWindow dialogWindow) {
        super(dialogWindow);
        dialogWindow.setTitle("Logout");
        add(new Label("logout-message", "Do you want to logout?"));
        
        dialogWindow.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {
            private static final long serialVersionUID = 1L;
            public void onClose(AjaxRequestTarget target) {
                UserSession userSession = (UserSession) getSession();
                userSession.logout();
            }
        });
    }

    @Override
    protected PluginEvent ok() throws Exception {
        return new PluginEvent(getOwningPlugin());
    }
    
    @Override
    protected void cancel() {
    }

}

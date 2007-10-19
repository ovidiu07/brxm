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
package org.hippoecm.frontend.dialog;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.PropertyModel;
import org.hippoecm.frontend.model.JcrEvent;

public abstract class AbstractDialog extends WebPage {

    protected AjaxLink ok;
    protected AjaxLink cancel;
    protected DialogWindow dialogWindow;

    private String exception = "";

    public AbstractDialog(final DialogWindow dialogWindow) {
        this.dialogWindow = dialogWindow;

        final Label exceptionLabel = new Label("exception", new PropertyModel(this, "exception"));
        exceptionLabel.setOutputMarkupId(true);
        add(exceptionLabel);

        ok = new AjaxLink("ok") {
            private static final long serialVersionUID = 1L;
            public void onClick(AjaxRequestTarget target) {
                try {
                    dialogWindow.setJcrEvent(ok());
                    dialogWindow.close(target);
                } catch (Exception e) {
                    setException(e.getClass().getName() + ": " + e.getMessage());
                    target.addComponent(exceptionLabel);
                }
            }
        };
        add(ok);

        cancel = new AjaxLink("cancel") {
            private static final long serialVersionUID = 1L;
            public void onClick(AjaxRequestTarget target) {
                cancel();
                dialogWindow.close(target);
            }
        };
        add(cancel);
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public String getException() {
        return exception;
    }

    protected abstract JcrEvent ok() throws Exception;

    protected abstract void cancel();

}

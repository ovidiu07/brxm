/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.addon.workflow;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;

class MenuItem extends Panel {
    private static final long serialVersionUID = 1L;

    public MenuItem(String id, final ActionDescription wf) {
        super(id);

StdWorkflow newwf = (StdWorkflow)wf;
/*StdWorkflow newwf = new StdWorkflow((StdWorkflow)wf, "wf") {
    protected void execute() {
        ((StdWorkflow)wf).execute();
    }
};
        newwf.setOutputMarkupId(true);
        newwf.setVisible(false);
        add(newwf);*/

        Component fragment = newwf.getFragment("text");
        if (fragment instanceof ActionDescription.ActionDisplay) {
            ((ActionDescription.ActionDisplay)fragment).substantiate();
            add(fragment);
        } else if (fragment instanceof Fragment) {
            add(fragment);
        } else {
            // wf.setVisible(true);
        }

        fragment = newwf.getFragment("icon");
        if (fragment instanceof ActionDescription.ActionDisplay) {
            ((ActionDescription.ActionDisplay)fragment).substantiate();
            add(fragment);
        } else if (fragment instanceof Fragment) {
            add(fragment);
        }
    }
}

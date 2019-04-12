/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.plugins.openui;

import java.util.Map;

import org.apache.wicket.model.Model;
import org.apache.wicket.util.value.IValueMap;
import org.hippoecm.frontend.dialog.Dialog;
import org.hippoecm.frontend.dialog.DialogConstants;
import org.hippoecm.frontend.dialog.ScriptAction;

public class OpenUiStringDialog extends Dialog<String> {

    OpenUiStringDialog(final String instanceId, final Map<String, String> parameters) {

        setTitle(Model.of(parameters.getOrDefault("title", "OpenUi Dialog")));
        setSize(parseSize(parameters));

        setCloseAction((ScriptAction<String>) model -> String.format(
                "OpenUi.getInstance('%s').closeDialog('%s');", instanceId, instanceId));

        setCancelAction((ScriptAction<String>) model -> String.format(
                "OpenUi.getInstance('%s').cancelDialog();", instanceId));

    }

    private static IValueMap parseSize(final Map<String, String> parameters) {
        final String size = parameters.getOrDefault("size", "large");
        switch (size.toLowerCase()) {
            case "small":
                return DialogConstants.SMALL;
            case "medium":
                return DialogConstants.MEDIUM;
            case "large":
                return DialogConstants.LARGE;
            default:
                return DialogConstants.LARGE;
        }
    }
}

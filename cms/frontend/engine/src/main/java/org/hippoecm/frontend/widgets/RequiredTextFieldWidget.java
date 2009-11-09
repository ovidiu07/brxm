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
package org.hippoecm.frontend.widgets;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;

public class RequiredTextFieldWidget extends TextFieldWidget {
    private static final long serialVersionUID = 1L;
    
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    
    public RequiredTextFieldWidget(String id, IModel model) {
        this(id, model, null);
    }

    public RequiredTextFieldWidget(String id, IModel model, IModel labelModel) {
        super(id, model, labelModel);
        
        TextField t = (TextField) get("widget");
        t.setRequired(true);
    }


}

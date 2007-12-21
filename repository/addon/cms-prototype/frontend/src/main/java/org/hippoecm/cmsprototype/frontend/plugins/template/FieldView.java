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
package org.hippoecm.cmsprototype.frontend.plugins.template;

import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;

public class FieldView extends DataView {
    private static final long serialVersionUID = 1L;

    private TemplateDescriptor descriptor;
    private TemplateEngine engine;

    public FieldView(String wicketId, TemplateDescriptor descriptor, TemplateProvider provider, TemplateEngine engine) {
        super(wicketId, provider);

        this.descriptor = descriptor;
        this.engine = engine;
    }

    public TemplateDescriptor getTemplateDescriptor() {
        return descriptor;
    }

    @Override
    protected void populateItem(Item item) {
        FieldModel fieldModel = (FieldModel) item.getModel();
        item.add(engine.createTemplate("sub", fieldModel));
    }
}

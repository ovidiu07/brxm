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

import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValueTemplate extends Panel {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ValueTemplate.class);

    private TemplateEngine engine;

    public ValueTemplate(String wicketId, JcrPropertyModel model, FieldDescriptor descriptor, TemplateEngine engine) {
        super(wicketId, model);

        this.engine = engine;

        if (model.getProperty() != null) {
            try {
                if (!descriptor.isProtected()) {
                    add(deleteLink("delete", model));
                } else {
                    add(new Label("delete", "(protected)"));
                }
                add(new Label("name", descriptor.getName()));
                add(new ValueEditor("values", model, descriptor, engine));
                if (descriptor.isMultiple()) {
                    add(addLink("add", model));
                } else {
                    add(new Label("add", ""));
                }
            } catch (RepositoryException e) {
                log.error(e.getMessage());
            }
        } else {
            add(new Label("delete", "null"));
            add(new Label("name", "null"));
            add(new Label("values", "null"));
            add(new Label("add", "null"));
        }
    }

    // Called when a new value is added to a multi-valued property

    protected void onAddValue(AjaxRequestTarget target) {
        JcrPropertyModel model = (JcrPropertyModel) getModel();
        try {
            Property prop = model.getProperty();
            Value[] oldValues = prop.getValues();
            String[] newValues = new String[oldValues.length + 1];
            for (int i = 0; i < oldValues.length; i++) {
                newValues[i] = oldValues[i].getString();
            }
            newValues[oldValues.length] = "...";
            prop.setValue(newValues);
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        engine.onChange(target);
    }

    protected void onDeleteValue(AjaxRequestTarget target) {
        JcrPropertyModel model = (JcrPropertyModel) getModel();
        try {
            Property prop = model.getProperty();
            prop.remove();
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }
        engine.onChange(target);
    }
    
    // privates

    private Component deleteLink(String id, final JcrPropertyModel model) throws RepositoryException {
        return new AjaxLink(id, model) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ValueTemplate.this.onDeleteValue(target);
            }
        };
    }

    private AjaxLink addLink(String id, final JcrPropertyModel model) {
        return new AjaxLink(id, model) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ValueTemplate.this.onAddValue(target);
            }
        };
    }
}

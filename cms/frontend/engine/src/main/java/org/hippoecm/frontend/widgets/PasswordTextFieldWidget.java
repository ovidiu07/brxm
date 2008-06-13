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

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.value.StringValue;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.model.IChainingModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PasswordTextFieldWidget extends AjaxUpdatingWidget {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PasswordTextFieldWidget.class);

    private MyModel myModel;

    public PasswordTextFieldWidget(final String id, final IModel model) {
        super(id, model);

        myModel = new MyModel(model);

        final PasswordTextField pwd = new PasswordTextField("widget", myModel);
        pwd.setRequired(false);
        pwd.add(new AbstractValidator() {
            @Override
            protected void onValidate(IValidatable validatable) {
                String modelValue = myModel.getObject() != null ? (String) myModel.getObject() : "";
                String formValue = validatable.getValue() != null ? (String) validatable.getValue() : "";
                if (modelValue.length() == 0 && formValue.length() == 0)
                    PasswordTextFieldWidget.this.error("Password is required");
            }

            @Override
            public boolean validateOnNullValue() {
                return true;
            }
        });
        addFormField(pwd);
    }

    class MyModel implements IChainingModel {
        private static final long serialVersionUID = 1L;

        private JcrPropertyValueModel model;

        public MyModel(IModel model) {
            setChainedModel(model);
        }

        public IModel getChainedModel() {
            return model;
        }

        public void setChainedModel(IModel model) {
            if (model instanceof JcrPropertyValueModel)
                this.model = (JcrPropertyValueModel) model;
        }

        public void detach() {
            model.detach();
        }

        public Object getObject() {
            if (model != null) {
                try {
                    return model.getValue().getString();
                } catch (RepositoryException e) {
                    log.error("An error occurred while trying to get password value", e);
                }
            }
            return null;
        }

        /**
         * Special purpose override, makes sure the JcrPropertyValueModel will never be null or ""
         * (non-Javadoc)
         * @see org.apache.wicket.model.IModel#setObject(java.lang.Object)
         */
        public void setObject(Object object) {
            if (object == null)
                return;

            String value = (String) object;
            if (value.length() > 0) {
                model.setValue(new StringValue(value));
            }
        }
    }

}

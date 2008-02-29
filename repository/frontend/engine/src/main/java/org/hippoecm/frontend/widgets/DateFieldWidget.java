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
package org.hippoecm.frontend.widgets;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.value.DateValue;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;

public class DateFieldWidget extends AjaxUpdatingWidget {
    private static final long serialVersionUID = 1L;

    private DateTimeField dtfield;
    
    public DateFieldWidget(String id, IModel model) {
        super(id, model);
        
        final JcrPropertyValueModel  valueModel = (JcrPropertyValueModel) getModel();
        Date date;
        try {
        	date = valueModel.getValue().getDate().getTime();
        } catch(RepositoryException ex) {
        	// FIXME:  log error
        	date = null;
        }
        
        addFormField(dtfield = new DateTimeField("widget", new Model(date) { 
			private static final long serialVersionUID = 1L;

			@Override
        	public void setObject(Object object) {
        		
				
				Calendar calendar = new GregorianCalendar();
	    		calendar.setTime(dtfield.getDate());
	    		valueModel.setValue(new DateValue(calendar));

        		super.setObject(object);
        	}
        	
        }));
    }
    
}

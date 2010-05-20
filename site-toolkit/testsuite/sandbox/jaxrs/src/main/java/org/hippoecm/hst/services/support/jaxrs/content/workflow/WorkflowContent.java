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
package org.hippoecm.hst.services.support.jaxrs.content.workflow;

import java.io.Serializable;
import java.util.Map;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.hippoecm.repository.api.Workflow;

/**
 * WorkflowContent
 * 
 * @version $Id$
 */
@XmlRootElement(name = "workflow")
public class WorkflowContent {
    
    private String className;
    private Map<String, Serializable> hints;
    
    public WorkflowContent() {
    }
    
    public WorkflowContent(Workflow workflow) throws Exception {
        className = workflow.getClass().getName();
        hints = workflow.hints();
    }
    
    @XmlAttribute
    public String getClassName() {
        return className;
    }
    
    public void setClassName(String className) {
        this.className = className;
    }
    
    public Map<String, Serializable> getHints() {
        return hints;
    }
    
    public void setHints(Map<String, Serializable> hints) {
        this.hints = hints;
    }
}

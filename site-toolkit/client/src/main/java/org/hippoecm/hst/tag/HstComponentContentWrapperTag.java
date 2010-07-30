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
package org.hippoecm.hst.tag;

import java.io.Reader;
import java.io.StringReader;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.hippoecm.hst.core.component.HeadElement;
import org.hippoecm.hst.core.component.HeadElementImpl;
import org.hippoecm.hst.utils.PageContextPropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * HstComponentContentWrapperTag
 * @version $Id$
 */
public class HstComponentContentWrapperTag extends BodyTagSupport {

    static Logger logger = LoggerFactory.getLogger(HstComponentContentWrapperTag.class);
    
    private static final long serialVersionUID = 1L;
    
    protected Element element;
    protected String scope;
    protected String contentNameAttribute;
    
    public int doEndTag() throws JspException {
        if (this.element == null) {
            Reader reader = null;
            
            try {
                String xmlText = "";
    
                if (bodyContent != null && bodyContent.getString() != null) {
                    xmlText = bodyContent.getString().trim();
                }
                
                DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
                Document doc = docBuilder.parse(new InputSource(new StringReader(xmlText)));
                element = doc.getDocumentElement();
            } catch (Exception ex) {
                throw new JspException(ex);
            } finally {
                if (reader != null) try { reader.close(); } catch (Exception ce) { }
            }
        }
        
        if (element != null) {
            HeadElement wrapperElement = new HeadElementImpl(element);
            ContentRenderingContext ctx = new ContentRenderingContext(wrapperElement, contentNameAttribute);
            
            if ("request".equals(scope)) {
                pageContext.setAttribute(ContentRenderingContext.NAME, ctx, PageContext.REQUEST_SCOPE);
            } else {
                pageContext.setAttribute(ContentRenderingContext.NAME, ctx, PageContext.PAGE_SCOPE);
            }
        }
        
        element = null;
        scope = null;
        contentNameAttribute = null;
        
        return EVAL_PAGE;
    }
    
    public void setElement(Element element) {
        this.element = element;
    }
    
    public void setElementByBeanPath(String beanPath) {
        this.element = (Element) PageContextPropertyUtils.getProperty(pageContext, beanPath);
    }
    
    public Element getElement() {
        return this.element;
    }
    
    public void setScope(String scope) {
        this.scope = scope;
    }
    
    public String getScope() {
        return scope;
    }
    
    public void setContentNameAttribute(String contentNameAttribute) {
        this.contentNameAttribute = contentNameAttribute;
    }
    
    public String getContentNameAttribute() {
        return contentNameAttribute;
    }
}

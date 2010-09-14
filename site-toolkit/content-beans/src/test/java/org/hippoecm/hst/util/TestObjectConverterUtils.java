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
package org.hippoecm.hst.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.util.Collection;

import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.junit.Before;
import org.junit.Test;

/**
 * TestObjectConverterUtils
 * @version $Id$
 */
public class TestObjectConverterUtils {
    
    private URL annotationXmlUrl;
    
    @Before
    public void setUp() throws Exception {
        String beansAnnotatedClassResourcePath = "/" + getClass().getName().replace('.', '/') + "-beans-annotated-classes.xml";
        annotationXmlUrl = getClass().getResource(beansAnnotatedClassResourcePath);
        assertNotNull("Beans annotatated classes xml resource doesn't exist: " + beansAnnotatedClassResourcePath, annotationXmlUrl);
    }
    
    @Test
    public void testObjectConverterCreationWithURL() throws Exception {
        Collection<Class<? extends HippoBean>> annotatedClasses = ObjectConverterUtils.getAnnotatedClasses(annotationXmlUrl);
        ObjectConverter objectConverter = ObjectConverterUtils.createObjectConverter(annotatedClasses);
        
        assertEquals(TextBean.class, objectConverter.getAnnotatedClassFor("test:textdocument"));
        assertEquals(CommentBean.class, objectConverter.getAnnotatedClassFor("test:comment"));
        
        assertEquals("test:textdocument", objectConverter.getPrimaryNodeTypeNameFor(TextBean.class));
        assertEquals("test:comment", objectConverter.getPrimaryNodeTypeNameFor(CommentBean.class));
    }
    
    @Node(jcrType="test:textdocument")
    public static class TextBean extends HippoDocument {
    }
    
    @Node(jcrType="test:comment")
    public static class CommentBean extends HippoDocument {
    }
}

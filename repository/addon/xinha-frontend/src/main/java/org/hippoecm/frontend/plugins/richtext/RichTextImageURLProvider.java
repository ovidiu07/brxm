/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.frontend.plugins.richtext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RichTextImageURLProvider implements IImageURLProvider {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(RichTextImageURLProvider.class);

    private final IRichTextImageFactory imageFactory;
    private final IRichTextLinkFactory linkFactory;

    public RichTextImageURLProvider(IRichTextImageFactory factory, IRichTextLinkFactory linkFactory) {
        this.imageFactory = factory;
        this.linkFactory = linkFactory;
    }

    public String getURL(String link) {
        String facetName = link;
        if (link.indexOf('/') > 0) {
            facetName = link.substring(0, link.indexOf('/'));
        }
        if (linkFactory.getLinks().contains(facetName)) {
            try {
                RichTextImage rti = imageFactory.loadImageItem(link);
                return rti.getUrl();
            } catch (RichTextException ex) {
                log.error("Could not load link as image", ex);
            }
        }
        return link;
    }
}
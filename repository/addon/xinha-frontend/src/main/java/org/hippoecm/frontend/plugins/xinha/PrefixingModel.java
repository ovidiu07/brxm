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
package org.hippoecm.frontend.plugins.xinha;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.plugins.richtext.IImageDecorator;
import org.hippoecm.frontend.plugins.richtext.PrefixingImageDecorator;
import org.hippoecm.frontend.plugins.richtext.RichTextProcessor;

public class PrefixingModel implements IModel<String> {
    private static final long serialVersionUID = 1L;

    private IImageDecorator decorator;
    private IModel<String> bare;

    public PrefixingModel(IModel<String> bare, String prefix) {
        this.bare = bare;
        this.decorator = new PrefixingImageDecorator(prefix);
    }

    public PrefixingModel(IModel<String> bare, IImageDecorator decorator) {
        this.bare = bare;
        this.decorator = decorator;
    }
    
    public String getObject() {
        String text = bare.getObject();
        if (text != null) {
            return RichTextProcessor.prefixImageLinks(text, decorator);
        }
        return null;
    }

    public void setObject(String object) {
        bare.setObject(RichTextProcessor.restoreFacets(object));
    }
    
    public void detach() {
        bare.detach();
    }

}

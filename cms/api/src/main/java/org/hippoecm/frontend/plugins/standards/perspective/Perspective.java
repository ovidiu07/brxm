/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.perspective;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.ITitleDecorator;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.service.render.RenderPlugin;

public abstract class Perspective extends RenderPlugin<Void> implements ITitleDecorator {

    private static final long serialVersionUID = 1L;

    public static final String TITLE = "perspective.title";

    private IModel<String> title = new Model<String>("title");

    public Perspective(IPluginContext context, IPluginConfig config) {
        super(context, config);

        if (config.getString(TITLE) != null) {
            title = new StringResourceModel(config.getString(TITLE), this, null);
        }
    }

    // ITitleDecorator

    public IModel<String> getTitle() {
        return title;
    }

    @Override
    public void onComponentTag(final ComponentTag tag) {
        super.onComponentTag(tag);
        tag.append("class", "perspective", " ");
    }

    public ResourceReference getIcon(IconSize size) {
        return new ResourceReference(Perspective.class, "perspective-" + size.getSize() + ".png");
    }

    protected void setTitle(String title) {
        this.title.setObject(title);
    }

}

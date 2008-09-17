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
package org.hippoecm.frontend.editor.plugins.resource;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.NonCachingImage;
import org.apache.wicket.markup.html.link.ResourceLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.hippoecm.frontend.model.IJcrNodeModelListener;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.resource.JcrResource;
import org.hippoecm.frontend.resource.JcrResourceStream;
import org.hippoecm.frontend.service.IJcrService;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageDisplayPlugin extends RenderPlugin implements IJcrNodeModelListener {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: ImageDisplayPlugin.java 12039 2008-06-13 09:27:05Z bvanhalderen $";

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ImageDisplayPlugin.class);

    private JcrResourceStream resource;

    public ImageDisplayPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        context.registerService(this, IJcrService.class.getName());

        resource = new JcrResourceStream(((JcrNodeModel) getModel()).getNode());
        Fragment fragment = new Fragment("fragment", "unknown", this);
        try {
            Node node = ((JcrNodeModel) getModel()).getNode();
            String mimeType = node.getProperty("jcr:mimeType").getString();
            if (mimeType.indexOf('/') > 0) {
                String category = mimeType.substring(0, mimeType.indexOf('/'));
                if ("image".equals(category)) {
                    fragment = new Fragment("fragment", "image", this);
                    fragment.add(new NonCachingImage("image", new JcrResource(resource)));
                } else {
                    ResourceLink link = new ResourceLink("link", new JcrResource(resource));
                    link.add(new Label("name", "download"));
                    fragment = new Fragment("fragment", "embed", this);
                    fragment.add(link);
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        add(fragment);
    }

    @Override
    public void onDetach() {
        resource.detach();
        super.onDetach();
    }

    public void onFlush(JcrNodeModel newModel) {
        String path = newModel.getItemModel().getPath();
        String target = ((JcrNodeModel) getModel()).getItemModel().getPath();
        if (target.length() >= path.length() && target.substring(0, path.length()).equals(path)) {
            try {
                resource.close();
            } catch (IOException ex) {
                log.error(ex.getMessage());
            }
        }
    }

}

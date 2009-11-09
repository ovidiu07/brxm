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
package org.hippoecm.frontend.plugins.standards.browse;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.wicket.IClusterable;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.util.MaxLengthStringFormatter;
import org.hippoecm.repository.api.NodeNameCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BreadcrumbPlugin extends RenderPlugin {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(BreadcrumbPlugin.class);

    private final Set<String> roots;
    private final AjaxButton up;

    private MaxLengthStringFormatter format;
    final IModelReference folderReference;

    private List<NodeItem> nodeitems;

    public BreadcrumbPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        if (config.getString("model.folder") == null) {
            throw new IllegalArgumentException("Expected model.folder configuration key");
        }

        roots = new HashSet<String>();
        String[] paths = config.getStringArray("root.paths");
        if (paths != null) {
            for (String path : paths) {
                roots.add(path);
            }
        } else {
            roots.add("/");
        }

        folderReference = context.getService(config.getString("model.folder"), IModelReference.class);
        if (folderReference != null) {
            context.registerService(new IObserver() {
                private static final long serialVersionUID = 1L;

                public IObservable getObservable() {
                    return folderReference;
                }

                public void onEvent(Iterator<? extends IEvent> event) {
                    update((JcrNodeModel) folderReference.getModel());
                }

            }, IObserver.class.getName());
        }

        JcrNodeModel nodeModel = (JcrNodeModel) folderReference.getModel();

        add(getListView(nodeModel));

        up = new AjaxButton("up") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                JcrNodeModel model = (JcrNodeModel) folderReference.getModel();
                model = model.getParentModel();
                if (model != null) {
                    folderReference.setModel(model);
                }
            }
        };
        up.setModel(new StringResourceModel("dialog-breadcrumb-up", this, null));
        if (nodeModel == null || roots.contains(nodeModel.getItemModel().getPath())) {
            up.setEnabled(false);
        }
        add(up);

        format = new MaxLengthStringFormatter(config.getInt("crumb.max.length", 10), config.getString("crumb.splitter",
                ".."), 0);
    }

    @Override
    protected void onDetach() {
        if (nodeitems != null) {
            for (NodeItem nodeItem : nodeitems) {
                nodeItem.detach();
            }
        }
        super.onDetach();
    }

    protected void update(JcrNodeModel model) {
        replace(getListView(model));
        setModel(model);

        JcrNodeModel parentModel = model.getParentModel();
        if (parentModel == null || roots.contains(model.getItemModel().getPath())) {
            up.setEnabled(false);
        } else {
            up.setEnabled(true);
        }
        AjaxRequestTarget.get().addComponent(this);
    }

    private ListView getListView(JcrNodeModel model) {
        nodeitems = new LinkedList<NodeItem>();
        if (model != null) {
            //add current folder as disabled
            nodeitems.add(new NodeItem(model, false));
            if (!roots.contains(model.getItemModel().getPath())) {
                model = model.getParentModel();
                while (model != null) {
                    nodeitems.add(new NodeItem(model, true));
                    if (roots.contains(model.getItemModel().getPath())) {
                        model = null;
                    } else {
                        model = model.getParentModel();
                    }
                }
            }
        }
        Collections.reverse(nodeitems);
        ListView listview = new ListView("crumbs", nodeitems) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem item) {
                final NodeItem nodeItem = (NodeItem) item.getModelObject();
                AjaxLink link = new AjaxLink("link") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        folderReference.setModel(nodeItem.model);
                    }

                };

                link.add(new Label("name", new AbstractReadOnlyModel() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object getObject() {
                        return (nodeItem.name != null ? format.parse(nodeItem.name) : null);
                    }

                }));
                link.add(new AttributeAppender("title", true, new Model(nodeItem.getDecodedName()), " "));

                link.setEnabled(nodeItem.enabled);
                item.add(link);

                IModel css = new Model() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public String getObject() {
                        String css = nodeItem.enabled ? "enabled" : "disabled";

                        if (nodeitems.size() == 1) {
                            css += " firstlast";
                        } else if (item.getIndex() == 0) {
                            css += " first";
                        } else if (item.getIndex() == (nodeitems.size() - 1)) {
                            css += " last";
                        }
                        return css;
                    }
                };
                item.add(new AttributeAppender("class", css, " "));
            }
        };
        return listview;
    }

    private static class NodeItem implements IDetachable {
        private static final long serialVersionUID = 1L;

        boolean enabled;
        JcrNodeModel model;
        String name;

        public NodeItem(JcrNodeModel model, boolean enabled) {
            try {
                if (model != null && model.getNode() != null) {
                    this.name = model.getNode().getName();
                }
            } catch (RepositoryException e) {
                String path = model.getItemModel().getPath();
                this.name = path.substring(path.lastIndexOf('/'));
                log.warn("Error retrieving name from node[" + path + "]");
            }
            this.model = model;
            this.enabled = enabled;
        }

        public String getDecodedName() {
            return (name != null ? NodeNameCodec.decode(name) : null);
        }

        public void detach() {
            model.detach();
        }

    }

}

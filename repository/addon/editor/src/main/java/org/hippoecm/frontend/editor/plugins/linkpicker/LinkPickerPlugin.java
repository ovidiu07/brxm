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
package org.hippoecm.frontend.editor.plugins.linkpicker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.wicket.Session;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IChainingModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.dialog.AbstractDialog;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.properties.JcrPropertyValueModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkPickerPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id: LinkPickerPlugin.java 12039 2008-06-13 09:27:05Z bvanhalderen $";

    private static final long serialVersionUID = 1L;

    private JcrPropertyValueModel valueModel;

    private List<String> nodetypes = new ArrayList<String>();

    static final Logger log = LoggerFactory.getLogger(LinkPickerPlugin.class);

    public LinkPickerPlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);

        IDialogService dialogService = getDialogService();
        valueModel = (JcrPropertyValueModel) getModel();

        IModel displayModel = new Model() {
            private static final long serialVersionUID = 1L;

            public Object getObject() {
                String docbaseUUID = (String) valueModel.getObject();
                if (docbaseUUID == null || docbaseUUID.equals("")) {
                    return "[...]";
                }
                try {
                    return ((UserSession) Session.get()).getJcrSession().getNodeByUUID(docbaseUUID).getPath();
                } catch (ValueFormatException e) {
                    log.warn("Invalid value format for docbase " + e.getMessage());
                    log.debug("Invalid value format for docbase ", e);
                } catch (PathNotFoundException e) {
                    log.warn("Docbase not found " + e.getMessage());
                    log.debug("Docbase not found ", e);
                } catch (RepositoryException e) {
                    log.error("Invalid docbase" + e.getMessage(), e);
                }
                return "[...]";
            }
        };

        if ("edit".equals(config.getString("mode", "view"))) {
            if (config.getStringArray("nodetypes") != null) {
                String[] nodeTypes = config.getStringArray("nodetypes");
                nodetypes.addAll(Arrays.asList(nodeTypes));
            }
            if (nodetypes.size() == 0) {
                log.debug("No configuration specified for filtering on nodetypes. No filtering will take place.");
            }

            IDialogFactory dialogFactory = new IDialogFactory() {
                private static final long serialVersionUID = 1L;

                public AbstractDialog createDialog() {
                    return new LinkPickerDialog(context, getPluginConfig(), new IChainingModel() {
                        private static final long serialVersionUID = 1L;

                        public Object getObject() {
                            return valueModel.getObject();
                        }

                        public void setObject(Object object) {
                            valueModel.setObject(object);
                            redraw();
                        }

                        public IModel getChainedModel() {
                            return valueModel;
                        }

                        public void setChainedModel(IModel model) {
                            throw new UnsupportedOperationException("Value model cannot be changed");
                        }

                        public void detach() {
                            valueModel.detach();
                        }

                    }, nodetypes);
                }
            };
            add(new DialogLink("value", displayModel, dialogFactory, dialogService));
        } else {
            add(new Label("value", valueModel));
        }
        setOutputMarkupId(true);
    }

    @Override
    public void onModelChanged() {
        redraw();
    }

}

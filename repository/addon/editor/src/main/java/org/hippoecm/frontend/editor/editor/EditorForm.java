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
package org.hippoecm.frontend.editor.editor;

import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.util.lang.Bytes;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.editor.ITemplateEngine;
import org.hippoecm.frontend.editor.impl.TemplateEngine;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.plugin.IClusterControl;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.frontend.service.render.RenderService;
import org.hippoecm.frontend.types.ITypeDescriptor;
import org.hippoecm.frontend.types.ITypeStore;
import org.hippoecm.frontend.types.JcrTypeStore;

public class EditorForm extends Form {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private IPluginContext context;

    private IClusterControl cluster;
    private ModelReference modelService;
    private ServiceTracker<IRenderService> fieldTracker;
    private List<IRenderService> fields;
    private ITypeStore typeStore;
    private TemplateEngine engine;
    private String engineId;

    public EditorForm(String wicketId, JcrNodeModel model, final IRenderService parent, IPluginContext context,
            IPluginConfig config) {
        super(wicketId, model);

        this.context = context;

        add(new EmptyPanel("template"));

        setMultiPart(true);
        // FIXME: make this configurable
        setMaxSize(Bytes.megabytes(5));

        typeStore = new JcrTypeStore();
        engine = new TemplateEngine(context, typeStore);
        context.registerService(engine, ITemplateEngine.class.getName());
        engineId = context.getReference(engine).getServiceId();

        fields = new LinkedList<IRenderService>();
        fieldTracker = new ServiceTracker<IRenderService>(IRenderService.class) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onRemoveService(IRenderService service, String name) {
                replace(new EmptyPanel("template"));
                service.unbind();
                fields.remove(service);
            }

            @Override
            public void onServiceAdded(IRenderService service, String name) {
                service.bind(parent, "template");
                replace(service.getComponent());
                fields.add(service);
            }
        };
        context.registerTracker(fieldTracker, engineId + ".wicket.root");

        createTemplate();
    }

    public void destroy() {
        context.unregisterService(engine, ITemplateEngine.class.getName());
        context.unregisterTracker(fieldTracker, engineId + ".wicket.root");
        if (cluster != null) {
            cluster.stop();
            modelService.destroy();
        }
    }

    @Override
    public void validate() {
        super.validate();
    }

    @Override
    public void onDetach() {
        typeStore.detach();
        super.onDetach();
    }

    @Override
    public void onModelChanged() {
        super.onModelChanged();
        if (cluster != null) {
            cluster.stop();
            modelService.destroy();
        }
        createTemplate();
    }

    public void render(PluginRequestTarget target) {
        for (IRenderService child : fields) {
            child.render(target);
        }
    }

    protected void createTemplate() {
        JcrNodeModel model = (JcrNodeModel) getModel();
        if (model != null && model.getNode() != null) {
            ITypeDescriptor type = engine.getType(model);

            if (type != null) {
                IClusterConfig template = engine.getTemplate(type, ITemplateEngine.EDIT_MODE);
                if (template != null) {
                    IPluginConfig parameters = new JavaPluginConfig();
                    parameters.put(RenderService.WICKET_ID, engineId + ".wicket.root");
                    parameters.put(ITemplateEngine.ENGINE, engineId);
                    cluster = context.newCluster(template, parameters);

                    String modelId = cluster.getClusterConfig().getString(RenderService.MODEL_ID);
                    modelService = new ModelReference(modelId, model);
                    modelService.init(context);

                    cluster.start();
                }
            }
        }
    }

}

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
package org.hippoecm.frontend.plugins.reviewedactions;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.addon.workflow.CompatibilityWorkflowPlugin.WorkflowAction;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.i18n.types.TypeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.nodetypes.JcrNodeTypeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.DeleteDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.DepublishDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.HistoryDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.ScheduleDepublishDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.SchedulePublishDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.UnpublishedReferencesDialog;
import org.hippoecm.frontend.plugins.reviewedactions.dialogs.WhereUsedDialog;
import org.hippoecm.frontend.plugins.reviewedactions.model.ReferenceProvider;
import org.hippoecm.frontend.plugins.reviewedactions.model.UnpublishedReferenceProvider;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.IEditorManager;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowException;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.reviewedactions.BasicReviewedActionsWorkflow;

public class BasicReviewedActionsWorkflowPlugin extends CompatibilityWorkflowPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    private static Logger log = LoggerFactory.getLogger(BasicReviewedActionsWorkflowPlugin.class);

    public String stateSummary = "UNKNOWN";

    public String inUseBy = "";

    StdWorkflow infoAction;
    StdWorkflow infoEditAction;
    WorkflowAction editAction;
    WorkflowAction publishAction;
    WorkflowAction depublishAction;
    WorkflowAction deleteAction;
    WorkflowAction schedulePublishAction;
    WorkflowAction scheduleDepublishAction;
    WorkflowAction whereUsedAction;
    WorkflowAction historyAction;

    public BasicReviewedActionsWorkflowPlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);

        final TypeTranslator translator = new TypeTranslator(new JcrNodeTypeModel("hippostd:publishableSummary"));
        add(infoAction = new StdWorkflow("info", "info") {
            @Override
            protected IModel getTitle() {
                return translator.getValueName("hippostd:stateSummary", new PropertyModel(BasicReviewedActionsWorkflowPlugin.this, "stateSummary"));
            }
            @Override
            protected void invoke() {
            }
        });

        add(infoEditAction = new StdWorkflow("infoEdit", "infoEdit") {
            @Override
            protected IModel getTitle() {
                return new StringResourceModel("in-use-by", this, null, new Object[] { new PropertyModel(BasicReviewedActionsWorkflowPlugin.this, "inUseBy") });
            }
            @Override
            protected void invoke() {
            }
        });

        add(editAction = new WorkflowAction("edit", new StringResourceModel("edit", this, null).getString(), null) {
            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "edit-16.png");
            }
            @Override
            protected String execute(Workflow wf) throws Exception {
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                Document docRef = workflow.obtainEditableInstance();
                ((UserSession) getSession()).getJcrSession().refresh(true);
                Node docNode = ((UserSession) getSession()).getJcrSession().getNodeByUUID(docRef.getIdentity());
                IEditorManager editorMgr = getPluginContext().getService(
                        getPluginConfig().getString(IEditorManager.EDITOR_ID), IEditorManager.class);
                if (editorMgr != null) {
                    JcrNodeModel docModel = new JcrNodeModel(docNode);
                    IEditor editor = editorMgr.getEditor(docModel);
                    if (editor == null) {
                        editorMgr.openEditor(docModel);
                    }
                } else {
                    log.warn("No editor found to edit {}", docNode.getPath());
                }
                return null;
            }
        });

        add(publishAction = new WorkflowAction("requestPublication", new StringResourceModel("request-publication", this, null).getString(), null) {
            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "workflow-requestpublish-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                HippoNode node;
                try {
                    node = (HippoNode) ((WorkflowDescriptorModel) getDefaultModel()).getNode();
                    final UnpublishedReferenceProvider referenced = new UnpublishedReferenceProvider(new ReferenceProvider(
                            new JcrNodeModel(node)));
                    if (referenced.size() > 0) {
                        return new UnpublishedReferencesDialog(publishAction, new UnpublishedReferenceNodeProvider(referenced), getEditorManager());
                    }
                } catch (RepositoryException e) {
                    log.error(e.getMessage());
                }
                return null;
            }
            
            @Override
            protected String execute(Workflow wf) throws Exception {
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                workflow.requestPublication();
                return null;
            }
        });

        add(depublishAction = new WorkflowAction("requestDepublication", new StringResourceModel("request-depublication", this, null).getString(), null) {

            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "workflow-requestunpublish-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                final IModel docName = getDocumentName();
                IModel title = new StringResourceModel("depublish-title", BasicReviewedActionsWorkflowPlugin.this, null,
                        new Object[] { docName });
                IModel message = new StringResourceModel("depublish-message", BasicReviewedActionsWorkflowPlugin.this,
                        null, new Object[] { docName });
                return new DepublishDialog(title, message, this, getEditorManager());
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                workflow.requestDepublication();
                return null;
            }
        });

        add(deleteAction = new WorkflowAction("requestDeletion", new StringResourceModel("request-delete", this, null).getString(), null) {
            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "workflow-requestdelete-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                IModel<String> message = new StringResourceModel("delete-message",
                        BasicReviewedActionsWorkflowPlugin.this, null, new Object[] { getDocumentName() });
                IModel<String> title = new StringResourceModel("delete-title", BasicReviewedActionsWorkflowPlugin.this,
                        null, new Object[] { getDocumentName() });
                return new DeleteDialog(title, message, this, getEditorManager());
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow) wf;
                workflow.requestDeletion();
                return null;
            }
        });

        add(schedulePublishAction = new WorkflowAction("schedulePublish", new StringResourceModel("schedule-publish-label", this, null).getString(), null) {
            public Date date = new Date();
            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "publish-schedule-16.png");
            }
            @Override
            protected Dialog createRequestDialog() {
                WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) getDefaultModel();
                try {
                    return new SchedulePublishDialog(this, new JcrNodeModel(wdm.getNode()), new PropertyModel(this, "date"), getEditorManager());
                } catch (RepositoryException ex) {
                    log.warn("could not retrieve node for scheduling publish", ex);
                }
                return null;
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow)wf;
                if (date != null) {
                    workflow.requestPublication(date);
                } else {
                    workflow.requestPublication();
                }
                return null;
            }
        });

        add(scheduleDepublishAction = new WorkflowAction("scheduleDepublish", new StringResourceModel("schedule-depublish-label", this, null).getString(), null) {
            public Date date = new Date();
            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "unpublish-scheduled-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) getDefaultModel();
                try {
                    return new ScheduleDepublishDialog(this, new JcrNodeModel(wdm.getNode()), new PropertyModel(this, "date"), getEditorManager());
                } catch (RepositoryException e) {
                    log.warn("could not retrieve node for scheduling depublish", e);
                }
                return null;
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                BasicReviewedActionsWorkflow workflow = (BasicReviewedActionsWorkflow)wf;
                if (date != null) {
                    workflow.requestDepublication(date);
                } else {
                    workflow.requestDepublication();
                }
                return null;
            }
        });


        add(whereUsedAction = new WorkflowAction("where-used", new StringResourceModel("where-used-label", this, null)
                .getString(), null) {
            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "where-used-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) getDefaultModel();
                return new WhereUsedDialog(wdm, getEditorManager());
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                return null;
            }
        });

        add(historyAction = new WorkflowAction("history", new StringResourceModel("history-label", this, null)
                .getString(), null) {
            @Override
            protected ResourceReference getIcon() {
                return new ResourceReference(getClass(), "revision-16.png");
            }

            @Override
            protected Dialog createRequestDialog() {
                WorkflowDescriptorModel wdm = (WorkflowDescriptorModel) getDefaultModel();
                return new HistoryDialog(wdm, getEditorManager());
            }

            @Override
            protected String execute(Workflow wf) throws Exception {
                return null;
            }
        });
    }

    private IEditorManager getEditorManager() {
        return getPluginContext().getService(getPluginConfig().getString("editor.id"), IEditorManager.class);
    }

    IModel<String> getDocumentName() {
        try {
            return (new NodeTranslator(new JcrNodeModel(((WorkflowDescriptorModel) getDefaultModel()).getNode())))
                    .getNodeName();
        } catch (RepositoryException ex) {
            try {
                return new Model<String>(((WorkflowDescriptorModel) getDefaultModel()).getNode().getName());
            } catch (RepositoryException e) {
                return new StringResourceModel("unknown", this, null);
            }
        }
    }

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
        try {
            WorkflowManager manager = ((UserSession)org.apache.wicket.Session.get()).getWorkflowManager();
            WorkflowDescriptorModel workflowDescriptorModel = (WorkflowDescriptorModel)getDefaultModel();
            WorkflowDescriptor workflowDescriptor = (WorkflowDescriptor)getDefaultModelObject();
            if(workflowDescriptor != null) {
                Node documentNode = workflowDescriptorModel.getNode();
                if(documentNode != null && documentNode.hasProperty("hippostd:stateSummary")) {
                    stateSummary = documentNode.getProperty("hippostd:stateSummary").getString();
                }
                Workflow workflow = manager.getWorkflow(workflowDescriptor);
                Map<String, Serializable> info = workflow.hints();
                if (info.containsKey("obtainEditableInstance") && info.get("obtainEditableInstance") instanceof Boolean && !((Boolean)info.get("obtainEditableInstance")).booleanValue()) {
                     editAction.setVisible(false);
                }
                if (info.containsKey("publish") && info.get("publish") instanceof Boolean && !((Boolean)info.get("publish")).booleanValue()) {
                   publishAction.setVisible(false);
                    schedulePublishAction.setVisible(false);
                }
                if (info.containsKey("depublish") && info.get("depublish") instanceof Boolean && !((Boolean)info.get("depublish")).booleanValue()) {
                    depublishAction.setVisible(false);
                    scheduleDepublishAction.setVisible(false);
                }
                if (info.containsKey("delete") && info.get("delete") instanceof Boolean && !((Boolean)info.get("delete")).booleanValue()) {
                    deleteAction.setVisible(false);
                }
                if (info.containsKey("status") && info.get("status") instanceof Boolean && !((Boolean)info.get("status")).booleanValue()) {
                    infoAction.setVisible(false);
                    whereUsedAction.setVisible(false);
                    historyAction.setVisible(false);
                }
                if (info.containsKey("inUseBy") && info.get("inUseBy") instanceof String) {
                    inUseBy = (String) info.get("inUseBy");
                    infoEditAction.setVisible(true);
                } else {
                    infoEditAction.setVisible(false);
                }
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage(), ex);
        } catch (WorkflowException ex) {
            log.error(ex.getMessage(), ex);
        } catch (RemoteException ex) {
            log.error(ex.getMessage(), ex);
        }
    }
}

/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.onehippo.cms7.essentials.plugins.indexexporter;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

import javax.inject.Inject;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.instructions.Instruction;
import org.onehippo.cms7.essentials.dashboard.instructions.InstructionStatus;
import org.onehippo.cms7.essentials.dashboard.model.TargetPom;
import org.onehippo.cms7.essentials.dashboard.packaging.MessageGroup;
import org.onehippo.cms7.essentials.dashboard.service.WebXmlService;

public class LuceneIndexExporterInstruction implements Instruction {
    private static final String SERVLET_NAME = "RepositoryJaxrsServlet";
    private static final String SERVLET_FQCN = "org.onehippo.repository.jaxrs.RepositoryJaxrsServlet";
    private static final List<String> URL_PATTERNS = Collections.singletonList("/ws/*");

    @Inject
    private WebXmlService webXmlService;

    @Override
    public InstructionStatus execute(PluginContext context) {
        return webXmlService.addServlet(TargetPom.CMS, SERVLET_NAME, SERVLET_FQCN, 6)
                && webXmlService.addServletMapping(TargetPom.CMS, SERVLET_NAME, URL_PATTERNS)
                ? InstructionStatus.SUCCESS : InstructionStatus.FAILED;
    }

    @Override
    public void populateChangeMessages(final BiConsumer<MessageGroup, String> changeMessageQueue) {
        changeMessageQueue.accept(MessageGroup.EXECUTE,
                "Ensure availability of '" + SERVLET_NAME + "' through cms web.xml");
    }
}

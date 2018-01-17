/*
 * Copyright 2014-2018 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.plugin.sdk.instruction.executors;

import java.util.Set;

import com.google.common.collect.Multimap;

import org.onehippo.cms7.essentials.sdk.api.ctx.PluginContext;
import org.onehippo.cms7.essentials.sdk.api.install.Instruction;
import org.onehippo.cms7.essentials.plugin.sdk.instruction.PluginInstructionSet;
import org.onehippo.cms7.essentials.plugin.sdk.rest.MessageRestful;
import org.springframework.stereotype.Component;

/**
 * @version "$Id$"
 */
@Component
public class PluginInstructionExecutor {

    public Instruction.Status execute(final PluginInstructionSet set, PluginContext context) {
        Instruction.Status status = Instruction.Status.SUCCESS;
        final Set<Instruction> instructions = set.getInstructions();
        for (Instruction instruction : instructions) {
            status = instruction.execute(context);
        }
        return status;
    }

    @SuppressWarnings("unchecked")
    public Multimap<Instruction.Type, MessageRestful> getInstructionsMessages(final PluginInstructionSet instruction, final PluginContext context) {
        MessageInstructionExecutor executor = new MessageInstructionExecutor();
        return executor.execute(instruction, context);
    }

}

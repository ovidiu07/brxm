/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

import { ViewAsDirective } from './viewAs.directive';
import { ViewAsCtrl } from './viewAs.controller';
import { ExperimentLabelDirective } from './experimentLabel.directive';
import { ExperimentLabelCtrl } from './experimentLabel.controller';

export const channelRelevanceModule = angular
  .module('hippo-cm.channel.relevance', [
    'hippo-cm-api',
  ])
  .controller('ViewAsCtrl', ViewAsCtrl)
  .directive('relevanceViewAs', ViewAsDirective)
  .controller('ExperimentLabelCtrl', ExperimentLabelCtrl)
  .directive('experimentLabel', ExperimentLabelDirective);

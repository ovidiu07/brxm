/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Directive, Type } from '@angular/core';
import { Container } from '@bloomreach/spa-sdk';
import { BrNodeComponentDirective } from './br-node-component.directive';
import { BrProps } from './br-props.model';

@Directive({
  selector: '[brNodeContainer]',
  inputs: [ 'component:brNodeContainer' ], // tslint:disable-line: no-inputs-metadata-property
})
export class BrNodeContainerDirective extends BrNodeComponentDirective<Container> {
  protected getMapping(): Type<BrProps> | undefined {
    const type = this.component.getType();

    return type && this.page.mapping[type];
  }
}

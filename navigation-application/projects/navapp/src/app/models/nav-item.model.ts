/*
 * Copyright 2019 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

import { NavItem as NavItemDto } from '@bloomreach/navapp-communication';
import { Observable } from 'rxjs';

export class NavItem implements NavItemDto {
  get id(): string {
    return this.dto.id;
  }

  get appIframeUrl(): string {
    return this.dto.appIframeUrl;
  }

  get appPath(): string {
    return this.dto.appPath;
  }

  get displayName(): string {
    return this.dto.displayName;
  }

  get active$(): Observable<boolean> {
    return this.activation$;
  }

  constructor(
    private readonly dto: NavItemDto,
    private readonly activation$: Observable<boolean>,
  ) {}
}

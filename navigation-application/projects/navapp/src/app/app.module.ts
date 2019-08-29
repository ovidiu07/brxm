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

import { APP_BASE_HREF, Location, LocationStrategy, PathLocationStrategy } from '@angular/common';
import { HttpClientModule } from '@angular/common/http';
import { APP_INITIALIZER, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppComponent } from './app.component';
import { ClientAppModule } from './client-app/client-app.module';
import { MainMenuModule } from './main-menu/main-menu.module';
import { NavConfigService } from './services/nav-config.service';
import { SharedModule } from './shared/shared.module';
import { TopPanelModule } from './top-panel/top-panel.module';

const loadNavItems = (navConfigService: NavConfigService) => () => navConfigService.init();

@NgModule({
  imports: [
    BrowserModule,
    SharedModule,
    BrowserModule,
    ClientAppModule,
    HttpClientModule,
    MainMenuModule,
    TopPanelModule,
  ],
  providers: [
    Location,
    { provide: LocationStrategy, useClass: PathLocationStrategy },
    { provide: APP_BASE_HREF, useValue: window.location.origin },
    {
      provide: APP_INITIALIZER,
      useFactory: loadNavItems,
      deps: [NavConfigService],
      multi: true,
    },
  ],
  declarations: [AppComponent],
  bootstrap: [AppComponent],
})
export class AppModule {}

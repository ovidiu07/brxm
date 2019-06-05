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

import { Component, OnInit } from '@angular/core';
import { connectToParent, NavLocation, ParentConnectConfig } from '@bloomreach/navapp-communication';

import { mockSites, navigationConfiguration } from './mocks';

@Component({
  selector: 'app-root',
  template: `
    <h1>Number of times navigated {{ navigateCount }}</h1>
    <h2>It was navigated to "{{ navigatedTo }}"</h2>
    <h3>The button was clicked {{ buttonClicked }} times.</h3>
    <button (click)="onButtonClicked()">Button</button>
  `,
})
export class AppComponent implements OnInit {
  navigateCount = 0;
  navigatedTo: string;
  buttonClicked = 0;

  ngOnInit(): void {
    if (window.parent === window) {
      console.log('Iframe app was not loaded inside iframe');
      return;
    }
    const config: ParentConnectConfig = {
      parentOrigin: '*',
      methods: {
        navigate: (location: NavLocation) => {
          this.navigateCount += 1;
          this.navigatedTo = location.path;
        },
        getNavItems: () => {
          return navigationConfiguration;
        },
        getSites: () => {
          return mockSites;
        },
      },
    };

    connectToParent(config);
  }

  onButtonClicked(): void {
    this.buttonClicked++;
  }
}

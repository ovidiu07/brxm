/*
 * Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
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

/**
 * @module ui-extension
 * Main entry point of the ui-extension library. Implements the public API defined in the
 * api module and communicates with the parent frame using the parent module.
 *
 * @see module:api
 * @see module:parent
 */

/**
 * Disable TSLint for imports that start with an uppercase letter
 * @see https://github.com/Microsoft/tslint-microsoft-contrib/issues/387
 */
import Emittery = require('emittery'); // tslint:disable-line:import-name
import {
  ChannelScope,
  ChannelScopeEvents,
  DocumentScope,
  Emitter,
  EventHandler,
  FieldScope,
  PageProperties,
  PageScope,
  PageScopeEvents,
  UiProperties,
  UiScope,
} from './api';
import { Parent, ParentConnection, ParentMethod } from './parent';

const SCOPE_CHANNEL = 'channel';
const SCOPE_PAGE = `${SCOPE_CHANNEL}.page`;

abstract class Scope<T extends Parent = Parent> {
  protected constructor(protected _parent: ParentConnection<T>) {
  }
}

abstract class ScopeEmitter<Events, T extends Parent> extends Scope<T> implements Emitter<Events> {
  constructor(parent: ParentConnection, private _eventEmitter: Emittery, private _eventScope: string) {
    super(parent);
  }

  on(eventName: keyof Events, callback: EventHandler<Events>) {
    const scopedEventName = `${this._eventScope}.${eventName}`;
    return this._eventEmitter.on(scopedEventName, callback);
  }
}

interface ChannelParent extends Parent {
  getPage: ParentMethod<PageProperties>;
  getProperties: ParentMethod<UiProperties>;
  refreshChannel: ParentMethod;
  refreshPage: ParentMethod;
}

interface DocumentParent extends Parent {
  getFieldValue: ParentMethod<string>;
  setFieldValue: ParentMethod<void, [string]>;
}

class Page extends ScopeEmitter<PageScopeEvents, ChannelParent> implements PageScope {
  get() {
    return this._parent.call('getPage');
  }

  refresh() {
    return this._parent.call('refreshPage');
  }
}

class Channel extends ScopeEmitter<ChannelScopeEvents, ChannelParent> implements ChannelScope {
  page: Page;

  constructor(parent: ParentConnection, eventEmitter: Emittery, eventScope: string) {
    super(parent, eventEmitter, eventScope);
    this.page = new Page(parent, eventEmitter, SCOPE_PAGE);
  }

  refresh() {
    return this._parent.call('refreshChannel');
  }
}

class Document extends Scope<DocumentParent> implements DocumentScope {
  field = new Field(this._parent);
}

class Field extends Scope<DocumentParent> implements FieldScope {
  getValue() {
    return this._parent.call('getFieldValue');
  }

  setValue(value: string) {
    return this._parent.call('setFieldValue', value);
  }
}

export class Ui extends Scope implements UiScope {
  baseUrl: string;
  channel: ChannelScope;
  document: DocumentScope;
  extension: {
    config: string,
  };
  locale: string;
  timeZone: string;
  user: {
    id: string,
    firstName: string,
    lastName: string,
    displayName: string,
  };
  version: string;

  constructor(parent: ParentConnection, eventEmitter: Emittery) {
    super(parent);
    this.channel = new Channel(parent, eventEmitter, SCOPE_CHANNEL);
    this.document = new Document(parent);
  }

  init(): Promise<Ui> {
    return this._parent.call('getProperties')
      .then(properties => Object.assign(this, properties));
  }
}

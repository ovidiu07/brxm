/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

describe('iframeExtension', () => {
  let $componentController;
  let $ctrl;
  let $element;
  let $q;
  let $window;
  let context;
  let extension;
  let iframe;
  let ChannelService;
  let ConfigService;
  let DomService;
  let ExtensionService;
  let HippoIframeService;
  let Penpal;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$componentController_, _$q_) => {
      $componentController = _$componentController_;
      $q = _$q_;
    });

    context = {
      foo: 1,
    };

    extension = {
      id: 'test',
      displayName: 'Test',
      extensionPoint: 'testExtensionPoint',
      url: '/testUrl',
      config: 'testConfig',
    };

    ChannelService = jasmine.createSpyObj('ChannelService', ['reload']);
    ConfigService = jasmine.createSpyObj('ConfigService', ['getCmsContextPath']);
    DomService = jasmine.createSpyObj('DomService', ['getIframeWindow']);
    ExtensionService = jasmine.createSpyObj('ExtensionService', ['getExtension']);
    HippoIframeService = jasmine.createSpyObj('HippoIframeService', ['reload']);
    Penpal = jasmine.createSpyObj('Penpal', ['connectToChild']);
    $window = {
      location: {
        origin: 'https://www.example.com:443',
      },
    };

    $element = angular.element('<div></div>');
    iframe = angular.element('<iframe src="about:blank"></iframe>');
    $ctrl = $componentController('iframeExtension', {
      $element,
      $window,
      ChannelService,
      ConfigService,
      DomService,
      ExtensionService,
      HippoIframeService,
      Penpal,
    }, {
      extensionId: extension.id,
      context,
    });
  });

  describe('$onInit', () => {
    beforeEach(() => {
      Penpal.connectToChild.and.returnValue({
        promise: $q.resolve(),
        iframe,
      });
      ExtensionService.getExtension.and.returnValue(extension);
    });

    it('initializes the extension', () => {
      $ctrl.$onInit();

      expect(ExtensionService.getExtension).toHaveBeenCalledWith('test');
      expect($ctrl.extension).toEqual(extension);
    });

    it('connects to the child', () => {
      ConfigService.antiCache = 42;
      ConfigService.getCmsContextPath.and.returnValue('/cms/');

      $ctrl.$onInit();

      expect(Penpal.connectToChild).toHaveBeenCalledWith({
        url: '/cms/testUrl?br.antiCache=42&br.parentOrigin=https%3A%2F%2Fwww.example.com%3A443',
        appendTo: $element[0],
        methods: jasmine.any(Object),
      });
    });
  });

  describe('_getExtensionUrl', () => {
    beforeEach(() => {
      ConfigService.antiCache = 42;
      $ctrl.extension = extension;
    });

    describe('for extensions from the same origin', () => {
      it('works when the CMS location has a context path', () => {
        ConfigService.getCmsContextPath.and.returnValue('/cms/');
        expect($ctrl._getExtensionUrl()).toEqual('/cms/testUrl?br.antiCache=42&br.parentOrigin=https%3A%2F%2Fwww.example.com%3A443');
      });

      it('works when the CMS location has no context path', () => {
        ConfigService.getCmsContextPath.and.returnValue('/');
        expect($ctrl._getExtensionUrl()).toEqual('/testUrl?br.antiCache=42&br.parentOrigin=https%3A%2F%2Fwww.example.com%3A443');
      });

      it('works when the extension URL path contains search parameters', () => {
        ConfigService.getCmsContextPath.and.returnValue('/cms/');
        extension.url = '/testUrl?customParam=X';
        expect($ctrl._getExtensionUrl()).toEqual('/cms/testUrl?customParam=X&br.antiCache=42&br.parentOrigin=https%3A%2F%2Fwww.example.com%3A443');
      });

      it('works when the extension URL path does not start with a slash', () => {
        ConfigService.getCmsContextPath.and.returnValue('/cms/');
        extension.url = 'testUrl';
        expect($ctrl._getExtensionUrl()).toEqual('/cms/testUrl?br.antiCache=42&br.parentOrigin=https%3A%2F%2Fwww.example.com%3A443');
      });

      it('works when the extension URL path contains dots', () => {
        ConfigService.getCmsContextPath.and.returnValue('/cms/');
        extension.url = '../testUrl';
        expect($ctrl._getExtensionUrl()).toEqual('/testUrl?br.antiCache=42&br.parentOrigin=https%3A%2F%2Fwww.example.com%3A443');
      });
    });

    describe('for extensions from a different origin', () => {
      it('works for URLs without parameters', () => {
        extension.url = 'http://www.bloomreach.com';
        expect($ctrl._getExtensionUrl().$$unwrapTrustedValue()).toEqual('http://www.bloomreach.com/?br.antiCache=42&br.parentOrigin=https%3A%2F%2Fwww.example.com%3A443');
      });

      it('works for URLs with parameters', () => {
        extension.url = 'http://www.bloomreach.com?customParam=X';
        expect($ctrl._getExtensionUrl().$$unwrapTrustedValue()).toEqual('http://www.bloomreach.com/?customParam=X&br.antiCache=42&br.parentOrigin=https%3A%2F%2Fwww.example.com%3A443');
      });

      it('works for HTTPS URLs', () => {
        extension.url = 'https://www.bloomreach.com';
        expect($ctrl._getExtensionUrl().$$unwrapTrustedValue()).toEqual('https://www.bloomreach.com/?br.antiCache=42&br.parentOrigin=https%3A%2F%2Fwww.example.com%3A443');
      });
    });
  });
});

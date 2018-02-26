/*
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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

class ChannelCtrl {
  constructor(
    $log,
    $rootScope,
    $stateParams,
    $timeout,
    $translate,
    OverlayService,
    ChannelActionsService,
    ChannelService,
    ConfigService,
    FeedbackService,
    HippoIframeService,
    ProjectService,
    PageActionsService,
    PageMetaDataService,
    SidePanelService,
  ) {
    'ngInject';

    this.$log = $log;
    this.$rootScope = $rootScope;
    this.$timeout = $timeout;
    this.$translate = $translate;
    this.OverlayService = OverlayService;
    this.ChannelActionsService = ChannelActionsService;
    this.ChannelService = ChannelService;
    this.ConfigService = ConfigService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;
    this.PageActionsService = PageActionsService;
    this.PageMetaDataService = PageMetaDataService;
    this.ProjectService = ProjectService;
    this.SidePanelService = SidePanelService;

    this.projectsEnabled = ConfigService.projectsEnabled;

    this.HippoIframeService.load($stateParams.initialRenderPath);

    this.menus = [
      ChannelActionsService.getMenu(subPage => this.showSubpage(subPage)),
      PageActionsService.getMenu(subPage => this.showSubpage(subPage)),
    ];
  }

  get isContentOverlayDisplayed() {
    this.OverlayService.showContentOverlay(this.OverlayService.isContentOverlayDisplayed && (!this.ProjectService || (this.ProjectService && this.ProjectService.actionFlags.contentOverlay.allowed)));
    return this.OverlayService.isContentOverlayDisplayed;
  }

  set isContentOverlayDisplayed(value) {
    this.OverlayService.showContentOverlay(value);
  }

  get isComponentsOverlayDisplayed() {
    this.OverlayService.showComponentsOverlay(this.OverlayService.isComponentsOverlayDisplayed && (!this.ProjectService || (this.ProjectService && this.ProjectService.actionFlags.componentsOverlay.allowed)));
    return this.OverlayService.isComponentsOverlayDisplayed;
  }

  set isComponentsOverlayDisplayed(value) {
    this.OverlayService.showComponentsOverlay(value);
  }

  isControlsDisabled() {
    return !this.isChannelLoaded() || !this.isPageLoaded();
  }

  isConfigurationLocked() {
    return this.ChannelService.isConfigurationLocked();
  }

  isChannelLoaded() {
    return this.ChannelService.hasChannel();
  }

  isPageLoaded() {
    return this.HippoIframeService.isPageLoaded();
  }

  projectsEnabled() {
    return this.ConfigService.projectsEnabled;
  }

  isEditable() {
    return this.ChannelService.isEditable();
  }

  editMenu(menuUuid) {
    this.menuUuid = menuUuid;
    this.showSubpage('menu-editor');
  }

  getRenderVariant() {
    return this.PageMetaDataService.getRenderVariant();
  }

  isSubpageOpen() {
    return !!this.currentSubpage;
  }

  showSubpage(subpage) {
    this.currentSubpage = subpage;
  }

  hideSubpage() {
    delete this.currentSubpage;
  }

  onSubpageSuccess(key, params) {
    this.hideSubpage();
    if (key) {
      // TODO show a toast message notify this change
      this.$log.info(this.$translate.instant(key, params));
    }
  }

  onSubpageError(key, params) {
    this.hideSubpage();
    if (key) {
      this.FeedbackService.showError(key, params);
    }
  }

  isToolbarDisplayed() {
    return this.ChannelService.isToolbarDisplayed;
  }
}

export default ChannelCtrl;

/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

(function () {
    "use strict";
    angular.module('hippo.essentials')
        .controller('ecmTaggingPluginCtrl', function ($scope, $filter, $sce, $log, $rootScope, $http) {
            $scope.pluginId = "ecmTaggingPlugin";
            var endpoint = $rootScope.REST.documents;
            var endpointEcmTagging = $scope.endpoint = $rootScope.REST.dynamic + 'ecmtaggingyplugin/';

            $scope.locales = [
                {name: "en"},
                {name: "fr"},
                {name: "de"},
                {name: "es"},
                {name: "it"},
                {name: "nl"}
            ];
            $scope.addDocuments = function () {

                var selectedDocumentNames = [];
                var selectedDocumentLocations = [];

                var documents = $filter('filter')($scope.documentTypes, {checked: true});
                angular.forEach(documents, function (value) {
                    selectedDocumentNames.push(value.name);
                    selectedDocumentLocations.push(value.selectedField ? value.selectedField : "${cluster.id}.field");

                });
                var payload = Essentials.addPayloadData("documents", selectedDocumentNames.join(','), null);
                Essentials.addPayloadData("locations", selectedDocumentLocations.join(','), payload);

            };

            $scope.addTagging = function () {

                var payload = Essentials.addPayloadData("locales", extractLocales($scope.locales), null);
                $http.post(endpointEcmTagging, payload).success(function (data) {


                });
            };
            //############################################
            // INITIALIZE APP:
            //############################################
            $http.get(endpoint).success(function (data) {
                $scope.documentTypes = data;
            });
            //
            $http.get($rootScope.REST.root + "/plugins/plugins/" + $scope.pluginId).success(function (plugin) {
                $scope.pluginDescription = $sce.trustAsHtml(plugin.description);
            });
            //############################################
            // HELPERS
            //############################################
            function extractLocales(l) {
                var loc = $filter('filter')(l, {checked: true});
                var locales = [];
                if (loc.length == 0) {
                    locales.push('en');
                } else {
                    angular.forEach(l, function (value) {
                        locales.push(value.name);
                    });
                }
                return locales.join(',');
            }

        })
})();

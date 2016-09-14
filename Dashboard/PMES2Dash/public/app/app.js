/**
 * Created by bscuser on 9/14/16.
 */
'use strict';

angular.module('pmes2', ['ui.router'])
    .config(function($urlRouterProvider, $stateProvider) {
        $urlRouterProvider
            .otherwise('login');

        $stateProvider
            .state('login', {
                url: '/login',
                templateUrl: 'views/login.html'
            })
            .state('dash', {
                url: '/dash',
                templateUrl: 'views/dash.html',
                controller: 'dashController',
                controllerAs: 'dashCtrl'
            });
    })
    .run(function($rootScope, $state, $stateParams) {
        $rootScope.$state = $state;
        $rootScope.$stateParams = $stateParams;
    });
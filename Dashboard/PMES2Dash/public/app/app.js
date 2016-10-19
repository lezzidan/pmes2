/**
 * Created by bscuser on 9/14/16.
 */
'use strict';

angular.module('pmes2', ['ui.router'])
    .config(function($urlRouterProvider, $stateProvider) {
        var authenticate = function($q, $http, $state) {
            $http.get('/user')
                .then(function(data){
                return $q.when();
            }, function (data) {
                    $state.go('login');
                });
        };

        $urlRouterProvider
            .otherwise('login');
        $stateProvider
            .state('login', {
                url: '/login',
                templateUrl: 'views/login.html',
                controller: 'authController',
                controllerAs: 'authCtrl'
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
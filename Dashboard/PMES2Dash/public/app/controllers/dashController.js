/**
 * Created by bscuser on 9/14/16.
 */
'use strict';

angular.module('pmes2')
    .controller('dashController', function($http) {
        var store = this;
        this.userName = "test";
        this.newApp = {args:[], user:"test"};
        this.newJob = {};
        this.newStorage = {};
        this.jobsList = [];
        this.appsList = [];
        this.error = null;

        this.saveNewApp = function() {
            $http({
                method: 'POST',
                url: 'dash/app',
                data: this.newApp
            }).then(
                function(data) {
                    $('#newApp').modal('hide');
                    //store.jobsList.push({id: 4, app: "cosa", time: 11, status: "running"});
                    store.appsList.push(store.newApp);
                    store.error = null;
                },
                function(error) {
                    store.error = 'HA FALLADO: '+error.data.error;
                }
            );
            console.log('Hola que pasa');
        };

        this.saveNewJob = function() {
            $http({
                method: 'POST',
                url: 'dash/job',
                data: this.newJob
            }).then(
                function(data) {
                    $('#newJob').modal('hide');
                    store.jobsList.push(store.newJob); //{id: 4, app: "cosa", time: 11, status: "running"});
                    store.error = null;
                },
                function(error) {
                    store.error = 'HA FALLADO: '+error.data.error;
                }
            );
            console.log('Hola que pasa');
        };

        this.saveNewStorage = function() {
            $http({
                method: 'POST',
                url: 'dash/storage',
                data: this.newStorage
            }).then(
                function(data) {
                    $('#newStorage').modal('hide');
                    //store.jobsList.push({id: 4, app: "cosa", time: 11, status: "running"});
                    store.error = null;
                },
                function(error) {
                    store.error = 'HA FALLADO: '+error.data.error;
                }
            );
            console.log('Hola que pasa');
        };

        this.removeArg = function (index) {
            var index = this.newApp.args.indexOf(index);
            if (index > -1){
                this.newApp.args.splice(index, 1);
            }
        };

        this.addArg = function (arg) {
            var newArg = {name:arg.name, defaultV:arg.defaultV, prefix:arg.prefix, file:arg.file, optional:arg.optional}
            this.newApp.args.push(newArg);
        };

        this.getJobsList = function() {
            $http.get('dash/jobs')
                .then(
                    function(data) {
                        store.jobsList = data.data;
                    },
                    function(error) {
                        console.log('OH NO, SOOMETHING HAS FAILED! AND NOBODY CARES');
                    });
        };

        this.getAppsList = function() {
            $http.get('dash/apps')
                .then(
                    function(data) {
                        store.appsList = data.data;
                    },
                    function(error) {
                        console.log('OH NO, SOOMETHING HAS FAILED! AND NOBODY CARES');
                    });
        };

        //Init values
        this.getJobsList();
        this.getAppsList();

    });
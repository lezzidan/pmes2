/**
 * Created by bscuser on 9/14/16.
 */
'use strict';

angular.module('pmes2')
    .controller('dashController', function($http) {
        var store = this;
        this.user = {
            name: "test",
            key: "test.key",
            pub: "test.pub"
        };

        this.newApp = {args:[], user:"test"};
        this.infoApp = {};

        this.newJob = {};
        this.newJob.app = {};

        this.newStorage = {};
        this.infoStorage = {};

        this.jobsList = [];
        this.appsList = [];
        this.storagesList = [];

        this.error = null;

        this.saveNewApp = function() {
            $http({
                method: 'POST',
                url: 'dash/app',
                data: this.newApp
            }).then(
                function(data) {
                    $('#newApp').modal('hide');
                    store.appsList.push(store.newApp);
                    store.error = null;
                },
                function(error) {
                    store.error = 'ERROR: '+error.data.error;
                }
            );
        };

        this.saveNewJob = function() {
            this.newJob.appName = this.newJob.appVal.name;
            this.newJob.imageName = this.newJob.appVal.image;
            this.newJob.args = this.newJob.appVal.args;
            this.newJob.user = store.userName;
            $http({
                method: 'POST',
                url: 'dash/job',
                data: this.newJob
            }).then(
                function(data) {
                    $('#newJob').modal('hide');
                    store.jobsList.push(store.newJob);
                    store.error = null;
                },
                function(error) {
                    store.error = 'Error: '+error.data.error;
                }
            );
        };

        this.runJob = function(job){
            //TODO
        };

        this.stopJob = function(job){
            //TODO
        };

        this.removeJob = function(job) {
            var index = this.jobsList.indexOf(job);
            if (index > -1){
                this.jobsList.splice(index, 1);
            }
        };

        this.removeStorage = function(storage) {
            var index = store.storagesList.indexOf(storage);
            if (index > -1){
                store.storagesList.splice(index, 1);
            }
        };

        this.removeApp = function(app) {
            var index = store.appsList.indexOf(app);
            if (index > -1){
                store.appsList.splice(index, 1);
            }
        };


        this.saveNewJobAndRun = function() {
            this.newJob.app.name = this.newJob.appVal.name;
            this.newJob.app.executable = this.newJob.appVal.executable;
            this.newJob.app.pathV = this.newJob.appVal.path;
            this.newJob.args = this.newJob.appVal.args;

            this.newJob.user = store.user;

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
            store.newJob.jobName = this.newJob.appVal.name;
            var resultArgs = store.newJob.args.reduce(function(map, obj){
                map[obj.name] = obj.value;
                return map;
            }, {});
            var jobToSend = {
                "jobName":store.newJob.jobName,
                "wallTime": store.newJob.wallTime,
                "minimumVMs": store.newJob.minimumVMs,
                "maximumVMs": store.newJob.maximumVMs,
                "limitVMs": store.newJob.maximumVMs,
                "initialVMs": store.newJob.minimumVMs,
                "memory": store.newJob.memory,
                "cores": store.newJob.cores,
                "inputPath": "/home/",
                "outputPath": "/home/",
                "numNodes": "1",
                "user": {
                    "username":store.newJob.user.name,
                    "credentials": {
                        "key": store.newJob.user.key,
                        "pub": store.newJob.user.pub
                    }
                },
                "img": {
                    "imageName":store.newJob.imageName,
                    "imageType": "small"
                },
                "app": {
                    "name":store.newJob.app.name,
                    "target":store.newJob.app.pathV,
                    "source":store.newJob.app.executable,
                    "args": resultArgs
                }
            };
            console.log(jobToSend);

            $http({
                method: 'POST',
                url: 'api/createActivity',
                data: jobToSend
            }).then(
                function(data) {
                    console.log(data);
                    store.error = null;
                }, function(error) {
                    store.error = 'Activity creation error'+error.data.error;
                }
            );
        };

        this.saveNewStorage = function() {
            $http({
                method: 'POST',
                url: 'dash/storage',
                data: this.newStorage
            }).then(
                function(data) {
                    $('#newStorage').modal('hide');
                    store.storagesList.push(store.newStorage);
                    store.error = null;
                },
                function(error) {
                    store.error = 'HA FALLADO: '+error.data.error;
                }
            );
            console.log('Hola que pasa');
        };

        this.infoStorage = function(storage) {
            var index = this.storagesList.indexOf(storage);
            store.infoStorage = store.storagesList[index];
        };

        this.infoApp = function(app) {
            var index = this.appsList.indexOf(app);
            store.infoApp = store.appsList[index];
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

        this.getStorageList = function() {
            $http.get('dash/storages')
                .then(
                  function(data) {
                      store.storagesList = data.data;
                  }, function(error){
                        console.log('OH NO, SOOMETHING HAS FAILED! AND NOBODY CARES');
                    }
                );
        };

        //Init values
        this.getJobsList();
        this.getAppsList();
        this.getStorageList();

    });
/**
 * Created by bscuser on 9/22/16.
 */
'use strict';

angular.module('pmes2')
    .controller('authController', ['$state', '$http', function($state, $http) {
        var storeAuth = this;
        this.title = "PMES2";
        this.newUser = {};
        this.user = {};
        this.user.email = "";
        this.user.password = "";
        this.error = "";
        storeAuth.userS = {
            username: "scorella",
            credentials: {
                key: "/home/pmes/certs/scorella_test.key",
                pem: "/home/pmes/certs/scorella_test.pem"
            }
        };

        this.login = function() {
            $http({
                method: 'POST',
                url: 'auth/login',
                data: this.user
            }).then(
                function(data) {
                    console.log(data);
                    $state.go('dash', {}, { reload: true});
                    storeAuth.error = null;
                },
                function(error) {
                    alert("User not authorized");
                    storeAuth.error = 'ERROR: '+error.data.error;
                }
            );
        };

        /*this.loginGoogle = function() {
            $http({
                method: 'GET',
                url: 'auth/google',
                dataType: 'jsonp'
            }).then(
                function(data) {
                    $state.go('dash', {}, { reload: true});
                    storeAuth.error = null;
                },
                function(error) {
                    alert("User not authorized");
                    storeAuth.error = 'ERROR: '+error.data.error;
                }
            );
        };*/
        this.loginGoogle = function() {
            $http({
                method: 'GET',
                url: 'auth/google',
                dataType: 'jsonp'
            }).success(function(data, status){
                console.log(status);
                console.log("SUCCESS");
                $state.go('dash', {}, { reload: true});
            }).error(function (data) {
                storeAuth.error = 'ERROR: '+error.data.error;
            })
        };

        this.saveNewUser = function() {
            console.log(storeAuth.newUser);
            $http({
                method: 'POST',
                url: 'auth/signup',
                data: storeAuth.newUser
            }).then(
                function(data) {
                    $('#signup').modal('hide');
                    storeAuth.error = null;
                    //storeAuth.userS._id = data.data;
                    //storeAuth.user = data.data;
                },
                function(error) {
                    storeAuth.error = 'ERROR: '+error.data.error;
                }
            );
            /*$http({
                method: 'POST',
                url: 'dash/user',
                data: storeAuth.newUser
            }).then(
                function(data) {
                    storeAuth.error = null;
                    storeAuth.userS._id = data.data;
                },
                function(error) {
                    storeAuth.error = 'ERROR: '+error.data.error;
                }
            );*/
        };
        //Init calls
        //this.saveNewUser();
    }]);
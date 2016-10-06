/**
 * Created by bscuser on 9/22/16.
 */
'use strict';

angular.module('pmes2')
    .controller('authController', ['$state', '$http', function($state, $http) {
        var storeAuth = this;
        this.title = "PMES2";
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
                    //$location.path('/dash');
                    storeAuth.error = null;
                },
                function(error) {
                    storeAuth.error = 'ERROR: '+error.data.error;
                }
            );
        };

        this.saveNewUser = function() {
            $http({
                method: 'POST',
                url: 'dash/user',
                data: this.userS
            }).then(
                function(data) {
                    storeAuth.error = null;
                    storeAuth.userS._id = data.data;
                },
                function(error) {
                    storeAuth.error = 'ERROR: '+error.data.error;
                }
            );
        };

        //Init calls
        //this.saveNewUser();
    }]);
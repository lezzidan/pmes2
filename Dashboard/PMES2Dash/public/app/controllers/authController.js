/**
 * Created by bscuser on 9/22/16.
 */
'use strict';

angular.module('pmes2')
    .controller('authController', function($http, $location) {
        var store = this;
        this.title = "PMES2";
        this.user = {};
        this.user.email = "";
        this.user.password = "";
        this.error = "";

        this.login = function() {
            $http({
                method: 'POST',
                url: 'auth/login',
                data: this.user
            }).then(
                function(data) {
                    console.log(data);
                    $location.path('/dash');
                    store.error = null;
                },
                function(error) {
                    store.error = 'ERROR: '+error.data.error;
                }
            );
        };
    });
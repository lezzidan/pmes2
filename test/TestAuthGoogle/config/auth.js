/**
 * Created by bscuser on 5/10/16.
 */
// config/auth.js

// expose our config directly to our application using module.exports
module.exports = {

    'facebookAuth' : {
        'clientID'      : 'your-secret-clientID-here', // your App ID
        'clientSecret'  : 'your-client-secret-here', // your App Secret
        'callbackURL'   : 'http://localhost:8080/auth/facebook/callback'
    },

    'twitterAuth' : {
        'consumerKey'       : 'your-consumer-key-here',
        'consumerSecret'    : 'your-client-secret-here',
        'callbackURL'       : 'http://localhost:8080/auth/twitter/callback'
    },

    'googleAuth' : {
        'clientID'      : '664048954502-uioicpgi0dahu2589fm3c83ebrrl3sse.apps.googleusercontent.com',
        'clientSecret'  : 'Mo40oT7bdMu-SRledQGdomKi',
        'callbackURL'   : 'http://localhost:8080/auth/google/callback'
    }

};

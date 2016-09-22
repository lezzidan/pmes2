/**
 * Created by bscuser on 9/21/16.
 */

var LocalStrategy   = require('passport-local').Strategy;
var GoogleStrategy = require('passport-google-oauth').OAuth2Strategy;

var User = require('../App/models/user');

var configAuth = require('./auth');

module.exports = function(passport) {
    // =========================================================================
    // passport session setup ==================================================
    // =========================================================================
    // required for persistent login sessions
    // passport needs ability to serialize and unserialize users out of session

    // used to serialize the user for the session
    passport.serializeUser(function(user, done) {
        done(null, user.id);
    });

    // used to deserialize the user
    passport.deserializeUser(function(id, done) {
        User.findById(id, function(err, user) {
            done(err, user);
        });
    });

    // =========================================================================
    // LOCAL STRATEGY ==========================================================
    // =========================================================================
    /* signup */
    passport.use('local-signup', new LocalStrategy({
            usernameField : 'email',
            passwordField : 'password',
            passReqToCallback : true
        },
        function(req, email, password, done){
            process.nextTick(function() {
                User.findOne({'login.local.email': email}, function (err, user) {
                    if (err)
                        return done(err);
                    if (user) {
                        return done(null, false, req.flash('signupMessage', 'the email is already taken'));
                    } else {
                        var newUser = new User();
                        newUser.login.local.email = email;
                        newUser.login.local.password = newUser.generateHash(password);

                        newUser.save(function (err) {
                            if (err)
                                throw err;
                            return done(null, newUser);
                        });
                    }
                });
            });
    }));

    /* login */
    passport.use('local-login', new LocalStrategy({
        usernameField: 'email',
        passwordField: 'password',
        passReqToCallback: true
    },
    function(req, email, password, done){
        var newUser = new User();
        newUser.login.local.email = email;
        newUser.login.local.password = newUser.generateHash(password);
        return done(null, newUser);
        /*User.findOne({'login.local.email': email}, function(err, user){
            if (err)
                return done(err);
            if (!user) {
                return done(null, false);
            }
            if (user) {
                if (!user.validPassword(password)) {
                    return done(null, false);
                }
                return done(null, user);
            }
            return done(null, user);
        });*/
    }));

};
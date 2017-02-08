/**
 * Created by bscuser on 9/21/16.
 */

var LocalStrategy   = require('passport-local').Strategy;
var GoogleStrategy = require('passport-google-oauth').OAuth2Strategy;

var User = require('../App/models/user');
var Group = require('../App/models/group');

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
                        console.log("Creating new user");
                        var newUser = new User();
                        newUser.username = email.split("@")[0];
                        /*var gid = null;
                        Group.find({name: "admin"},function(err, group){
                            if(err){
                                console.log("Group not found");
                                newUser.group = [];
                            } else {
                                console.log(group);
                                console.log(group._id);
                                gid = group._id;
                                console.log(gid);
                            }
                        });*/

                        newUser.group = [];
                        newUser.credentials.key = "";
                        newUser.credentials.pem = "";
                        newUser.credentials.uid = "";
                        newUser.credentials.gid = "";
                        newUser.login.local.email = email;
                        newUser.login.local.password = newUser.generateHash(password);
                        newUser.authorized = false;

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
        User.findOne({'login.local.email': email}, function(err, user){
            if (err)
                return done(err);
            if (!user) {
                return done(null, false);
            }
            if (user) {
                if (!user.validPassword(password) || !user.authorized) {
                    console.log("Password not valid or user not authorized");
                    return done(null, false);
                }
                console.log("valid password and user authorized");
                return done(null, user);
            }
            return done(null, user);
        });
    }));

    /*google*/
    passport.use('google', new GoogleStrategy({
            clientID        : configAuth.googleAuth.clientID,
            clientSecret    : configAuth.googleAuth.clientSecret,
            callbackURL     : configAuth.googleAuth.callbackURL
        },
        function(token, refreshToken, profile, done) {
            // make the code asynchronous
            // User.findOne won't fire until we have all our data back from Google
            process.nextTick(function() {

                // try to find the user based on their google id
                User.findOne({ 'login.google.id' : profile.id }, function(err, user) {
                    if (err)
                        return done(err);
                    if (!user){
                        // if the user isnt in our database, create a new user
                        var newUser          = new User();

                        // set all of the relevant information
                        newUser.login.google.id    = profile.id;
                        newUser.login.google.token = token;
                        newUser.login.google.name  = profile.displayName;
                        newUser.login.google.email = profile.emails[0].value; // pull the first email
                        newUser.username = profile.emails[0].value.split("@")[0];
                        newUser.group = [];
                        newUser.credentials.key = "";
                        newUser.credentials.pem = "";
                        newUser.credentials.uid = "";
                        newUser.credentials.gid = "";
                        newUser.authorized = false;
                        // save the user
                        newUser.save(function(err) {
                            if (err)
                                throw err;
                            return done(null, false, {message: 'User created but not authorized yet'});
                        });
                    }
                     else {
                        if (!user.authorized) {
                            // if a user is found, log them in
                            return done(null, false, {message: 'Not Authorized yet'});
                        }
                        return done(null, user);
                    }
                });
            });

        }));
};
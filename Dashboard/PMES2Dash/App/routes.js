var Application = require('./models/application');
var Job = require('./models/job');
var Storage = require('./models/storage');
var User = require('./models/user');
var fs = require('fs');
var async = require('async');

var formatJob = function (job) {
    console.log("Format Job "+job);
    var resultArgs = job.app.args.reduce(function(map, obj){
        map[obj.name] = obj.value;
        return map;
    }, {});
    User.findOne({_id: job.user}, function(err, usr){
        if(err){
            console.log(err);
        }
        console.log("USER: "+usr);
        var jobToSend = {
            "jobName":job.jobName,
            "wallTime": job.wallTime,
            "minimumVMs": job.minimumVMs,
            "maximumVMs": job.maximumVMs,
            "limitVMs": job.maximumVMs,
            "initialVMs": job.minimumVMs,
            "memory": job.memory,
            "cores": job.cores,
            "inputPath": job.inputPath,
            "outputPath": job.outputPath,
            "numNodes": job.numNodes,
            "user": usr,
            "img": job.img,
            "app": {
                "name":job.app.name,
                "target":job.app.target,
                "source":job.app.source,
                "args": resultArgs
            }
        };
        console.log("jobToSend: "+jobToSend);
        return jobToSend;
    });
};

module.exports = function(app, passport) {
    /* Create new application */
    app.post('/dash/app', isLoggedIn, function(req, res, next) {
        console.log("---- new app ----");
        if(!req.body.name || req.body.name.length < 5) {
            res.status(404).send({ error: 'NameApp too short'});
        } else {
            //var auxApp = Object.create(req.body);
            //auxApp.user = req.user._id;
            //var newApp = new Application(auxApp);
            var newApp = new Application(req.body);
            newApp.save(function(err, app){
                if(err) console.log(err);
                console.log(app);
            });
            res.send('OK');
        }
    });

    /* Create new job */
    app.post('/dash/job', isLoggedIn, function(req, res, next) {
        console.log("---- new job ----");
        console.log(req.body);
        if(!req.body.app) {
            res.status(404).send({ error: 'App name'});
        } else {
            var newJob = new Job(req.body);
            newJob.jobName = newJob.jobName + '_' + newJob._id;
            // Find app id
            Application.findOne({name: req.body.appVal.name}, function(err, app){
                if(err){
                    console.log(err);
                }
                newJob.app = app._id;
                newJob.save(function(err, job){
                    if(err) console.log(err);
                    console.log("new job created");
                });
                res.send(newJob.jobName);
            });
            /*newJob.save(function(err, job){
               if(err) console.log(err);
                console.log("new job created");
            });
            res.send(newJob.jobName);*/
        }
    });

    /* Create new storage */
    app.post('/dash/storage', isLoggedIn, function(req, res, next) {
        console.log("---- new Storage ----");
        console.log(req.body);
        if(!req.body.name) {
            res.status(404).send({ error: 'PATH'});
        } else {
            var newStorage = new Storage(req.body);
            newStorage.password = newStorage.encrypt(newStorage.password);
            newStorage.save(function(err, storage){
                if(err) console.log(err);
                console.log(storage);
            });
            console.log(newStorage);
            res.send('OK');
        }
    });

    /* update storage */
    app.put('/dash/storage', isLoggedIn, function(req, res, next){
        console.log("---- updating Storage ----");
        console.log(req.body);
        if(!req.body.name) {
            res.status(404).send({ error: 'PATH'});
        } else {
            Storage.findOne({name: req.body.name}, function(err, st){
                if(err){
                    console.log(err);
                }
                st.path = req.body.path;
                st.user = req.body.user;
                st.password = st.encrypt(req.body.password);
                st.save();
            });
            res.send('OK');
        }
    });

    /* update app */
    app.put('/dash/app', isLoggedIn, function(req, res, next){
        console.log("---- updating App ----");
        console.log(req.body);
        if(!req.body.name) {
            res.status(404).send({ error: 'PATH'});
        } else {
            Application.findOne({name: req.body.name}, function(err, ap){
                if(err){
                    console.log(err);
                }
                ap.image = req.body.image;
                ap.location = req.body.location;
                ap.target = req.body.target;
                ap.source = req.body.source;
                ap.description = req.body.description;
                ap.compss = req.body.compss;
                ap.publicApp = req.body.publicApp;
                ap.args = req.body.args;
                ap.save();
            });
            res.send('OK');
        }
    });

    /* update job */
    app.put('/dash/job', isLoggedIn, function(req, res, next){
        console.log("---- updating Job ----");
        console.log(req.body);
        if(!req.body.app.name) {
            res.status(404).send({ error: 'PATH'});
        } else {
            Job.findOne({name: req.body.jobName}, function(err, jb){
                if(err){
                    console.log(err);
                }
                jb.status = req.body.status;
                jb.finished = req.body.finished;
                jb.duration = req.body.duration;
                jb.save();
            });
            res.send('OK');
        }
    });

    /* update User */
    app.put('/dash/user', isLoggedIn, function(req, res, next){
        console.log("---- updating user ----");
        console.log(req.body);
        if(!req.body.username) {
            res.status(404).send({ error: 'username'});
        } else {
            User.findOne({username: req.body.username}, function(err, usr){
                if(err){
                    console.log(err);
                }
                usr.authorized = req.body.authorized;
                usr.credentials.key = req.body.credentials.key;
                usr.credentials.pem = req.body.credentials.pem;
                usr.save();
            });
            res.send('OK');
        }
    });

    /* delete job */
    app.delete('/dash/job', isLoggedIn, function(req, res, next){
        console.log("---- deleting Job ----");
        console.log(req.body);
        Job.findOneAndRemove({jobName: req.body.jobName}, function(err){
            if (err) {
                console.log("Job not found");
            }
            console.log("deleted");
            res.send('OK');
        });
    });

    /* delete app */
    app.delete('/dash/app', isLoggedIn, function(req, res, next){
        console.log("---- deleting App ----");
        console.log(req.body);
        Application.findOneAndRemove({name: req.body.name}, function(err){
            if (err) {
                console.log("App not found");
            }
            console.log("deleted");
            res.send('OK');
        });
    });

    /* delete storage */
    app.delete('/dash/storage', isLoggedIn, function(req, res, next){
        console.log("---- deleting Storage ----");
        Storage.findOneAndRemove(req.body, function(err){
            if (err) {
                console.log("storage not found");
            }
            console.log("deleted");
            res.send('OK');
        });
    });

    /* delete user */
    app.delete('/dash/user', isLoggedIn, function(req, res, next){
        console.log("---- deleting User ----");
        User.findOneAndRemove({username: req.body.username}, function(err){
            console.log(req.body.username);
            if (err) {
                console.log("user not found");
                console.log(err);
            }
            console.log("deleted");
            res.send('OK');
        });
    });

    /* Create new user */
    app.post('/dash/user', isLoggedIn, function(req, res, next){
        console.log("---- New User ----");
        console.log(req.body);
        if(!req.body.username || !req.body.credentials){
            res.status(404).send({error: 'name or credentials'});
        } else {
            var newUser = new User(req.body);
            newUser.save(function(err, user){
                if(err) console.log(err);
                console.log("user created"+user);
            });
            console.log(newUser);
            res.send(newUser._id);
        }
    });

    /* Get user */
    app.get('/dash/user', isLoggedIn, function(req, res, next) {
        console.log("REQ user"+req.user);
        User.find({username:'scorella'},function(err, user){
            if(err){
                console.log(err);
                res.send([]);
            } else {
                console.log(user);
                res.send(user);
            }
        });

    });

    app.get('/user', isLoggedIn, function(req, res) {
        res.send(req.user);
    });

    /* Get list of users */
    app.get('/dash/users', isLoggedIn, function(req, res, next) {
        User.find(function(err, users){
            if(err){
                console.log(err);
                res.send([]);
            } else {
                res.send(users);
            }
        });
    });

    /* Get list of jobs */
    app.get('/dash/jobs', isLoggedIn, function(req, res, next) {
        Job.find(function(err, jobs){
            if(err){
                console.log(err);
                res.send([]);
            } else {
                res.send(jobs);
            }
        });
    });

    /* Get list of applications */
    app.get('/dash/apps', isLoggedIn, function(req, res, next) {
        Application.find(function(err, apps){
            if(err){
                console.log(err);
                res.send([]);
            } else {
                res.send(apps);
            }
        });
    });

    /* Get list of storages */
    app.get('/dash/storages', isLoggedIn, function(req, res, next) {
        Storage.find(function(err, storages){
            if(err){
                console.log(err);
                res.send([]);
            } else {
                console.log("list storages: "+storages);
                console.log("len storages: "+storages.length);
                res.send(storages);
            }
        });
    });

    app.post('/dash/log', isLoggedIn, function(req, res, next){
        var path = req.body;
        console.log(path);

        fs.readFile(path.file, 'utf8', function(err, data){
           if(err){
               console.log(err);
           }
            res.send(data);
        });
    });

    // =====================================
    // AUTH       ==========================
    // =====================================
    app.post('/auth/login',
        passport.authenticate('local-login'),
        function(req, res) {
            console.log(req);
            console.log(req.user);
            res.send(req.user);
        });

    app.post('/auth/signup',
        passport.authenticate('local-signup'),
        function(req, res) {
            console.log(req.user);
            res.send(req.user);
            //res.redirect('/dash');
        });

    app.get('/auth/google', passport.authenticate('google', { scope : ['profile', 'email'] }), function (req,res) {
        console.log("estoy en google");
    });


    // the callback after google has authenticated the user
    app.get('/auth/google/callback',
        passport.authenticate('google', {
            successRedirect:'/#/dash',
            failureRedirect:'/#/login'
        }));

    app.get('/auth/logout', isLoggedIn, function(req, res){
        req.logout();
        res.send(200);
    });


    app.post('/dash/images', isLoggedIn, function(req, res){
        var execSync = require('child_process').execSync;
        var endpoint = " --endpoint https://rocci-server.bsc.es:11443";
        var auth = " --auth x509";
        var caPath = " --ca-path /etc/grid-security/certificates/";
        var userCred = " --user-cred "+req.body.credentials.pem;
        var userPass = " --password "+req.body.credentials.key;
        var action = " --action list --resource os_tpl";
        var command = "occi "+endpoint+auth+caPath+userCred+userPass+action;
        try {
            var ex = execSync(command);
            var listOfImages = ex.toString().split("\n");
            var listImagesToSend = [];
            for (var i=0; i < listOfImages.length; i++){
                var str = listOfImages[i];
                var ind = str.search("#");
                var im = str.substr(ind+1);
                listImagesToSend.push(im);
            }

            res.send(listImagesToSend);
        } catch(err) {
            console.log("Error getting list of images "+err);
            res.send(["test"]);
        }
    });

    // =====================================
    // API - PMES ==========================
    // =====================================
    app.post('/api/createActivity', isLoggedIn, function(req, res){
        console.log("creating activity");
        console.log(req.body);
        console.log(req.body.user);
        var listOfJobs = [req.body];
        var request = require('request');
        var options = {
            uri: 'http://localhost:8080/trunk_war_exploded/pmes/createActivity',
            method: 'POST',
            json: listOfJobs
        };
        request.post(options, function(error, response, body){
            if (!error && response.statusCode == 200){
                console.log(body);
                res.send('ok');
            } else {
                console.log(error);
                res.status(404).send({ error: error});
            }
        });
    });

    app.post('/api/getActivityStatus', isLoggedIn, function(req, res){
        console.log("asking for status");
        console.log(req.body);
        var listOfIds = [req.body];
        var request = require('request');
        var options = {
            uri: 'http://localhost:8080/trunk_war_exploded/pmes/getActivityStatus',
            method: 'POST',
            json: listOfIds
        };
        request.post(options, function(error, response, body){
            if (!error && response.statusCode == 200){
                console.log(body);
                //TODO: actualizar estado jobs.
                res.send('ok');
            } else {
                console.log(error);
                res.status(404).send({ error: error});
            }
        });
    });

};

// route middleware to make sure a user is logged in
function isLoggedIn(req, res, next) {

    // if user is authenticated in the session, carry on
    if (req.isAuthenticated())
        return next();

    // if they aren't redirect them to the home page
    res.redirect('/');
}
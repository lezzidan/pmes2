var Application = require('./models/application');
var Job = require('./models/job');
var Storage = require('./models/storage');
var User = require('./models/user');
var fs = require('fs');

module.exports = function(app, passport) {
    /* Create new application */
    app.post('/dash/app', function(req, res, next) {
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
    app.post('/dash/job', function(req, res, next) {
        console.log("---- new job ----");
        console.log(req.body);
        if(!req.body.app) {
            res.status(404).send({ error: 'App name'});
        } else {
            var newJob = new Job(req.body);
            newJob.jobName = newJob.jobName + '_' + newJob._id;
            newJob.save(function(err, job){
               if(err) console.log(err);
                console.log(job);
            });
            res.send(newJob.jobName);
        }
    });

    /* Create new storage */
    app.post('/dash/storage', function(req, res, next) {
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
    app.put('/dash/storage', function(req, res, next){
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
    app.put('/dash/app', function(req, res, next){
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
    app.put('/dash/job', function(req, res, next){
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

    /* delete job */
    app.delete('/dash/job', function(req, res, next){
        console.log("---- deleting Job ----");
        console.log(req.body);
        Job.findOneAndRemove({name: req.body.jobName}, function(err){
            if (err) {
                console.log("Job not found");
            }
            console.log("deleted");
            res.send('OK');
        });
    });

    /* delete app */
    app.delete('/dash/app', function(req, res, next){
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
    app.delete('/dash/storage', function(req, res, next){
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
    app.delete('/dash/user', function(req, res, next){
        console.log("---- deleting Storage ----");
        Storage.findOneAndRemove(req.body.username, function(err){
            if (err) {
                console.log("user not found");
            }
            console.log("deleted");
            res.send('OK');
        });
    });

    /* Create new user */
    app.post('/dash/user', function(req, res, next){
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

    /* Get list of jobs */
    app.get('/dash/user', function(req, res, next) {
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

    /* Get list of jobs */
    app.get('/dash/jobs', function(req, res, next) {
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
    app.get('/dash/apps', function(req, res, next) {
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
    app.get('/dash/storages', function(req, res, next) {
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

    app.post('/dash/log', function(req, res, next){
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
        passport.authenticate('local-login', {
            failureRedirect: '/login' }),
        function(req, res) {
            console.log(req.user);
            res.send(req.user);
        });

    app.post('/auth/signup',
        passport.authenticate('local-signup', {
            failureRedirect: '/login' }),
        function(req, res) {
            console.log(req.user);
            res.send(req.user);
            //res.redirect('/dash');
        });

    app.get('/logout', function(req, res){
        req.logout();
    });


    app.post('/dash/images', function(req, res){
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
    app.post('/api/createActivity', function(req, res){
        console.log("creating activity");
        console.log(req.body);
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
};
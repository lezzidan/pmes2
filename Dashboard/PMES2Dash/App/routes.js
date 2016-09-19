var Application = require('./models/application');
var Job = require('./models/job');
var Storage = require('./models/storage');

module.exports = function(app, passport) {

    /* Create new application */
    app.post('/dash/app', function(req, res, next) {
        console.log(req.body);
        if(!req.body.name || req.body.name.length < 5) {
            res.status(404).send({ error: 'NameApp too short'});
        } else {
            var a = new Application(req.body);
            console.log(a);
            res.send('OK');
        }
    });

    /* Create new job */
    app.post('/dash/job', function(req, res, next) {
        console.log("---- new job ----");
        console.log(req.body);
        if(!req.body.app.name) {
            res.status(404).send({ error: 'App name'});
        } else {
            var j = new Job(req.body);
            j.jobName = j.appName + '_' + j._id;
            console.log(j);
            res.send('OK');
        }
    });

    /* Create new storage */
    app.post('/dash/storage', function(req, res, next) {
        console.log("---- new Storage ----");
        console.log(req.body);
        if(!req.body.name) {
            res.status(404).send({ error: 'PATH'});
        } else {
            var storage = new Storage(req.body);
            storage.password = storage.encrypt(storage.password);
            console.log(storage);
            res.send('OK');
        }
    });

    /* Create new user */
    app.post('/dash/user', function(req, res, next){
        console.log("New User ------------");
        console.log(req.body);
        if(!req.body.name || !req.body.credentials){
            res.status(404).send({error: 'name or credentials'});
        } else {
            var user = new User(req.body);
            console.log(user);
            res.send('OK');
        }
    });

    /* Get list of jobs */
    app.get('/dash/jobs', function(req, res, next) {
        res.send([
            {
                id: 1,
                app: "date",
                time: 10,
                status: "finished"
            },
            {
                id: 2,
                app: "date",
                time: 10,
                status: "finished"
            },
            {
                id: 3,
                app: "date",
                time: 10,
                status: "running"
            }
        ]);
    });

    /* Get list of applications */
    app.get('/dash/apps', function(req, res, next) {
        res.send([{
            name: 'HelloWorld',
            image: 'Image1',
            location: 'local',
            path: '/home/Hello/',
            executable: 'hello.py',
            args: [{
                name: 'name',
                defaultV: 'test',
                prefix: '',
                file: false,
                optional: false
            }]
        }
        ]);
    });

    /* Get list of storages */
    app.get('/dash/storages', function(req, res, next) {
        res.send([]);
    });


    // =====================================
    // API - PMES ==========================
    // =====================================
    app.post('/api/createActivity', function(req, res){
        console.log("creating activity");
        console.log(req.body);
        var listOfJobs = [req.body];
        //var listOfJobs = [{"jobName":"test"}];
        var request = require('request');
        var options = {
            uri: 'http://localhost:8081/trunk_war_exploded/pmes/createActivity',
            method: 'POST',
            json: listOfJobs
            //json: [{"jobName":"test"},{"jobName":"test2"}]
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
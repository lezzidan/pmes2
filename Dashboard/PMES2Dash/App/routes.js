var Application = require('./models/application');
var Job = require('./models/job');

module.exports = function(app, passport) {

    app.get('/dash', function(req, res, next) {
        res.render('dash', { title: 'PMES2 Dashboard', message: '' });
    });

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
    app.post('/dash/job', function(req, res, next) {
        console.log("new job ----------------------");
        console.log(req.body);
        if(!req.body.appName.name) {
            res.status(404).send({ error: 'App name'});
        } else {
            var j = new Job(req.body);
            console.log(j);
            res.send('OK');
        }
    });
    app.post('/dash/storage', function(req, res, next) {
        console.log(req.body);
        /*if(!req.body.nameApp || req.body.nameApp.length < 5) {
            res.status(404).send({ error: 'NameApp too short'});
        } else {
            res.send('OK');
        }*/
        res.send('OK');
    });
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

    app.get('/dash/apps', function(req, res, next) {
        res.send([{
            name: 'test',
            args: [{
                name: 'arg1',
                defaultV: 'test',
                prefix: '',
                file: false,
                optional: false
            }]
        }
        ]);
    });

    // =====================================
    // API - PMES ==========================
    // =====================================
    app.post('/api/createActivity', function(req, res){
        console.log("creating activity");
        console.log(req.body);
        var request = require('request');
        var options = {
            uri: 'http://localhost:8081/trunk_war_exploded/pmes/createActivity',
            method: 'POST',
            json: [{"jobName":"test"},{"jobName":"test2"}]
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
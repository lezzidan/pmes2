var Application = require('./models/application');

module.exports = function(app, passport) {

    /* GET home page.
    app.get('/', function (req, res, next) {
        res.render('index', {title: 'PMES2 Dashboard', message: ''});
    });*/

    /* GET users listing.
    app.get('/users', function (req, res, next) {
        res.send('respond with a resource');
    }); */

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
        console.log(req.body);
        /*if(!req.body.nameApp || req.body.nameApp.length < 5) {
            res.status(404).send({ error: 'NameApp too short'});
        } else {
            res.send('OK');
        }*/
        res.send('OK');
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
        res.send([
        ]);
    });

};
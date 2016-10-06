var mongoose = require('mongoose');
var User = require('./user');
var Application = require('./application');

var jobSchema = mongoose.Schema({
    jobName : String,
    /*app: {
        name: String,
        source: String,
        target: String,
        compss: String,
        args: [{name: String, value: String, defaultV: String, prefix: String, file: Boolean, optional: Boolean}]
    },*/
    app: {type:mongoose.Schema.Types.ObjectId, ref: 'Application'},
    description: String,
    /*user: {
        username: String,
        credentials: {
            key: String,
            pub: String
        }
    },*/
    user: {type:mongoose.Schema.Types.ObjectId, ref: 'User'},
    img: {
        imageName: String,
        imageType: String
    },
    status: String,
    cores: Number,
    disk: Number,
    memory: Number,
    minimumVMs: Number,
    maximumVMs: Number,
    wallTime: Number,

    submitted: Date,
    finished: Date,
    duration: Number,
    errorMessage: String,
    numNodes: Number,
    initialVMs: Number,
    limitVMs: Number,
    log: String,
    output: String,
    error: String,
    inputPath: String,
    outputPath: String
});

//private HashMap<String, String> compss_flags;



module.exports = mongoose.model('Job', jobSchema);
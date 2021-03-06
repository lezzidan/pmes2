var mongoose = require('mongoose');
var User = require('./user');
var Application = require('./application');

var jobSchema = mongoose.Schema({
    jobName : String,
    pmesID : String,
    app: {type:mongoose.Schema.Types.ObjectId, ref: 'Application'},
    description: String,
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
    inputPaths: [String],
    outputPaths: [String],
    mountPath: String,
    compss_flags: {
        lang: String,
        path: String,
        library_path: String,
        graph: Boolean,
        tracing: Boolean,
        monitoring: Boolean,
        debug: Boolean,
        comm: String,
        conn: String,
        scheduler: String
    }
});

var autoPopulate = function (next) {
    this.populate('app');
    this.populate('user');
    next();
};

jobSchema
    .pre('findOne', autoPopulate)
    .pre('find', autoPopulate)


module.exports = mongoose.model('Job', jobSchema);
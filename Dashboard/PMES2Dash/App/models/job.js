/**
 * Created by bscuser on 9/14/16.
 */
/**
 * Created by bscuser on 9/13/16.
 */
var mongoose = require('mongoose');

var jobSchema = mongoose.Schema({
    name : String,
    appName: String,
    description: String,
    user: String,
    args: [{name: String, defaultV: String, prefix: String, file: Boolean, optional: Boolean}],
    submitted: Date,
    finished: Date,
    duration: Number,
    status: String,
    errorMessage: String,
    wallClock: Number,
    cores: Number,
    disk: Number,
    memory: Number,
    log: String,
    output: String,
    error: String
});

module.exports = mongoose.model('Job', jobSchema);
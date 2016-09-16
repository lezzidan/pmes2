/**
 * Created by bscuser on 9/14/16.
 */
/**
 * Created by bscuser on 9/13/16.
 */
var mongoose = require('mongoose');

var jobSchema = mongoose.Schema({
    //name : String,
    jobName : String,
    appName: String,
    description: String,
    user: String,
    args: [{name: String, value: String, defaultV: String, prefix: String, file: Boolean, optional: Boolean}],
    submitted: Date,
    finished: Date,
    duration: Number,
    status: String,
    errorMessage: String,
    wallTime: Number,
    cores: Number,
    numNodes: Number,
    disk: Number,
    memory: Number,
    initialVMs: Number,
    minimumVMs: Number,
    maximumVMs: Number,
    limitVMs: Number,
    log: String,
    output: String,
    error: String

});

//private App app;
//private Image img;
//private User user;
//private String inputPath;
//private String outputPath;
//private HashMap<String, String> compss_flags;



module.exports = mongoose.model('Job', jobSchema);
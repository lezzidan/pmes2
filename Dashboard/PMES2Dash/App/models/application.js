/**
 * Created by bscuser on 9/13/16.
 */
var mongoose = require('mongoose');
var User = require('./user');

var appSchema = mongoose.Schema({
    name : String,
    image: String,
    location: {type:mongoose.Schema.Types.ObjectId, ref: 'Storage'},
    target: String,
    source: String,
    description: String,
    compss: Boolean,
    publicApp: Boolean,
    user: {type:mongoose.Schema.Types.ObjectId, ref: 'User'},
    args: [{name: String, defaultV: String, prefix: String, file: Boolean, optional: Boolean}]

});

module.exports = mongoose.model('Application', appSchema);
/**
 * Created by bscuser on 9/13/16.
 */
var mongoose = require('mongoose');

var appSchema = mongoose.Schema({
    name : String,
    image: String,
    location: String,
    target: String,
    source: String,
    description: String,
    compss: Boolean,
    publicApp: Boolean,
    user: {
        username: String,
        credentials: {
            key: String,
            pem: String
        }
    },
    args: [{name: String, defaultV: String, prefix: String, file: Boolean, optional: Boolean}]

});

module.exports = mongoose.model('Application', appSchema);
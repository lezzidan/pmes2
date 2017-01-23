/**
 * Created by bscuser on 12/13/16.
 */
var mongoose = require('mongoose');
var bcrypt   = require('bcrypt-nodejs');

var groupSchema = mongoose.Schema({
    name : {
        type: String,
        unique: true
    },
    permission: {
        admin: Boolean,
        users: Boolean,
        groups: Boolean,
        storages: Boolean,
        apps: Boolean,
        jobs: Boolean
    }
});


module.exports = mongoose.model('Group', groupSchema);

/* Create Admin and Test Group */
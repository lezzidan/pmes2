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
        users: Boolean,
        storage: String
    }
});
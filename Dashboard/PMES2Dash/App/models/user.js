/**
 * Created by bscuser on 9/16/16.
 */
var mongoose = require('mongoose');
var bcrypt   = require('bcrypt-nodejs');
var Group = require('./group');

var userSchema = mongoose.Schema({
    username : {
        type: String,
        unique: true
    },
    credentials: {
        key: String,
        pem: String,
        uid: String,
        gid: String,
    },
    login : {
        local: {
            id: String,
            email: String,
            password: String
        },
        google: {
            id: String,
            email: String,
            token: String,
            name: String
        }
    },
    //group: [String],
    group: [{type:mongoose.Schema.Types.ObjectId, ref: 'Group'}],
    authorized: Boolean
});

// methods ======================
// generating a hash
userSchema.methods.generateHash = function(password) {
    return bcrypt.hashSync(password, bcrypt.genSaltSync(8), null);
};

// checking if password is valid
userSchema.methods.validPassword = function(password) {
 return bcrypt.compareSync(password, this.login.local.password);
};

var autoPopulate = function (next) {
    this.populate('group');
    next();
};

userSchema
    .pre('findOne', autoPopulate)
    .pre('find', autoPopulate);


module.exports = mongoose.model('User', userSchema);
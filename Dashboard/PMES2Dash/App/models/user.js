/**
 * Created by bscuser on 9/16/16.
 */
var mongoose = require('mongoose');
var bcrypt   = require('bcrypt-nodejs');

var userSchema = mongoose.Schema({
    username : String,
    credentials: {
        key: String,
        pem: String
    },
    password: String
});

// methods ======================
// generating a hash
userSchema.methods.generateHash = function(password) {
    return bcrypt.hashSync(password, bcrypt.genSaltSync(8), null);
};

// checking if password is valid
userSchema.methods.validPassword = function(password) {
 return bcrypt.compareSync(password, this.local.password);
};

module.exports = mongoose.model('User', userSchema);
var mongoose = require('mongoose');
var bcrypt   = require('bcrypt-nodejs');
var crypto = require('crypto'),
    algorithm = 'aes-256-ctr',
    password = 'd6F3Efeq';

var User = require('./user');

var storageSchema = mongoose.Schema({
    name : {
        type: String,
        unique: true
    },
    path: String,
    user: String,
    password: String,
    username: {type:mongoose.Schema.Types.ObjectId, ref: 'User'}
});

// methods ======================
// generating a hash
storageSchema.methods.generateHash = function(password) {
    return bcrypt.hashSync(password, bcrypt.genSaltSync(8), null);
};

storageSchema.methods.encrypt = function (pass) {
    var cipher = crypto.createCipher(algorithm,password);
    var crypted = cipher.update(pass,'utf8','hex');
    crypted += cipher.final('hex');
    return crypted;
};

storageSchema.methods.decrypt = function(pass){
    var decipher = crypto.createDecipher(algorithm,password);
    var dec = decipher.update(pass,'hex','utf8');
    dec += decipher.final('utf8');
    return dec;
};

module.exports = mongoose.model('Storage', storageSchema);
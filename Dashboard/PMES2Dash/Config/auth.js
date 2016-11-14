
// expose our config directly to our application using module.exports
// config google console https://console.cloud.google.com/apis/credentials?project=pmes2-146414
module.exports = {
    'googleAuth' : {
        'clientID'      : '546873064953-d9a8ejighk6vp7bsfjns89dugo371uqo.apps.googleusercontent.com',
        'clientSecret'  : '1FOIuW-Cu5IjGoT9gD81_o-B',
        'callbackURL'   : 'http://localhost:3000/auth/google/callback'
    }

};



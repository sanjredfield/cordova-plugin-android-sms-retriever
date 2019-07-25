var exec = require('cordova/exec');

exports.onSmsReceived = function(success, error, notifyWhenStarted) {
    exec(success, error, "CordovaPluginAndroidSmsRetriever", "onSmsReceived", [ notifyWhenStarted ]);
};

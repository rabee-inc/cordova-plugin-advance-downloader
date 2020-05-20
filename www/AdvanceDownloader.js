'use strict';

var exec = require('cordova/exec');

// cordova exec
var _AdvanceDownloader = {
    start: (onSuccess, onFail, param) => {
        return exec(onSuccess, onFail, 'AdvanceDownloader', 'start', [param]);
    },
};
  
// Promise wrapper
var AdvanceDownloader = {
    start: (params) => {
        return new Promise((resolve, reject) => {
        _AdvanceDownloader.start((res) => {
            resolve(res);
        }, (err) => {
            reject(err);
        }, params);
        });
    },
}

module.exports = CordovaTools;

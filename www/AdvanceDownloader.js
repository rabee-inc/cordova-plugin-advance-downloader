'use strict';

var exec = require('cordova/exec');

// cordova exec
var _AdvanceDownloader = {
    // param -> path, url, header, filename
    add: (onSuccess, onFail, param) => {
        return exec(onSuccess, onFail, 'AdvanceDownloader', 'add', [param]);
    },
    // param -> id
    get: (onSuccess, onFail, param) => {

    },

    


};
  
// Promise wrapper
var AdvanceDownloader = {
    add: (params) => {
        return new Promise((resolve, reject) => {
            _AdvanceDownloader.add((res) => {
                resolve(res);
            }, (err) => {
                reject(err);
            }, params);
        });
    },
}

module.exports = AdvanceDownloader;

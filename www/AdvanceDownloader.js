'use strict';

var exec = require('cordova/exec');

// cordova の実行ファイルを登録する
const registerCordovaExecuter = (action, onSuccess, onFail, param) => {
    return exec(onSuccess, onFail, 'AdvanceDownloader', action, [param]);
};

// promise で返す。 cordova の excuter の wrapper
const createAction = (action, params) => {
    return new Promise((resolve, reject) => {
        // actionが定義されているかを判定したい
        if (true) {
            // cordova 実行ファイルを登録
            registerCordovaExecuter(action, resolve, reject, params);
        }
        else {
            // TODO: error handling
        }
    });
};

// 本体 -> これを利用したもう一つ大きなクラスを作って 体裁を整える
const AdvanceDownloader = {
    // 新規ダウンロードの追加
    add: (params) => createAction('add', params),
    // ダウンロードの取得
    getTasks: (params) => createAction('getTasks', params),
    // 特定の id のダウンロードの開始
    start: (params) => createAction('start', params),
    // 特定の id のダウンロードの一時停止
    pause: (params) => createAction('pause', params),
    // 特定の id のダウンロードの再開
    resume: (params) => createAction('resume', params),
    // 特定の id のダウンロードの中断
    stop: (params) => createAction('stop', params),
    // イベントの登録
    on: (id, action, callback) => {
        // action === start | pause | resume | stop | complete | fail;
        exec(
            (data) => {
                // 成功
                callback(data)
            },
            () => {
                // 失敗
                // TODO: error handling
            },'AdvanceDownloader', action, [id, action]
        );
    },
    // イベントの削除
    off: (params) => createAction('off', params),
}


module.exports = AdvanceDownloader;

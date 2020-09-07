'use strict';

var exec = require('cordova/exec'); // cordova の実行ファイルを登録する


var registerCordovaExecuter = function registerCordovaExecuter(action, onSuccess, onFail, param) {
  return exec(onSuccess, onFail, 'AdvanceDownloader', action, [param]);
}; // promise で返す。 cordova の excuter の wrapper


var createAction = function createAction(action, params) {
  return new Promise(function (resolve, reject) {
    // actionが定義されているかを判定したい
    if (true) {
      // cordova 実行ファイルを登録
      registerCordovaExecuter(action, resolve, reject, params);
    } else {// TODO: error handling
    }
  });
}; // 本体 -> これを利用したもう一つ大きなクラスを作って 体裁を整える


var AdvanceDownloader = {
  // 新規ダウンロードの追加
  add: function add(params) {
    return createAction('add', params);
  },
  // ダウンロードの取得
  getTasks: function getTasks(params) {
    return createAction('getTasks', params);
  },
  // 特定の id のダウンロードの開始
  start: function start(params) {
    return createAction('start', params);
  },
  // 特定の id のダウンロードの一時停止
  pause: function pause(params) {
    return createAction('pause', params);
  },
  // 特定の id のダウンロードの再開
  resume: function resume(params) {
    return createAction('resume', params);
  },
  // 特定の id のダウンロードの中断
  stop: function stop(params) {
    return createAction('stop', params);
  },
  // イベントの登録
  on: function on(type, callback, id) {
    // type === progress | complete | failed;
    var actionType = '';

    switch (type) {
      case 'progress':
        actionType = 'setOnProgress';
        break;

      case 'complete':
        actionType = 'setOnComplete';
        break;

      case 'failed':
        actionType = 'setOnFailed';
        break;

      case 'changedStatus':
        actionType = 'setOnChangedStatus';
        break;

      default:
        break;
    }

    if (!actionType) return console.warn('please set action type');
    exec(function (data) {
      // 成功
      if (typeof callback === 'function') {
        callback(data);
      }
    }, function (error) {
      // 失敗
      // TODO: error handling
      console.log(error, 'error');
    }, 'AdvanceDownloader', actionType, [{
      id: id
    }]);
  },
  // イベントの削除
  off: function off(type, id) {
    // type === progress | complete | failed;
    var actionType = '';

    switch (type) {
      case 'progress':
        actionType = 'removeOnProgress';
        break;

      case 'complete':
        actionType = 'removeOnComplete';
        break;

      case 'failed':
        actionType = 'removeOnFailed';
        break;

      case 'changedStatus':
        actionType = 'removeOnChangedStatus';
        break;

      default:
        break;
    }

    if (!actionType) return console.error('please set action type');
    return createAction(actionType, {
      id: id
    });
  }
};
module.exports = AdvanceDownloader;
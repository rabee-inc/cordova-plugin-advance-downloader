# cordova-plugin-advance-downloader

## 環境セットアップ
### Cordova のインストール

```bash
$ npm install -g cordova@10.0.0-nightly.2020.5.20.7b8e8678
$ cordova -v 
```


### iOS の場合 cocoapod のインストール

- cocoapods 使ってるので、 cocoapods の方のインストールをお願いします
- インストール方法は割愛します。自分は rbenv 使って ruby install して gem 入れてます
- 留意点 ... pod の version を 1.8.0 以上にしておく


## ビルド

- 開発したプラグインを cordova で使ってみるために demo フォルダーがあります。
- demo フォルダーでビルドして確認してくだい
- 以下コマンドで platform を追加後、xcode or android studio で開いて、そこから run すれば動きます


```bash
$ cd demo;
$ yarn add:ios
$ yarn build:ios
$ open ./platforms/ios # このフォルダ以下が ios の開発環境になります xcworkspace を xcode で開いてください
$ open ./platforms/android # このフォルダ以下が android の開発環境になります android studio　から指定してください
```

**シンボリックリンク貼ってるので、基本的に、IDE から該当ファイルいじっても、plugin の file の方も同期されます**


なお、対象のファイルは xcode だと


![image](https://user-images.githubusercontent.com/13277036/82432757-79292680-9acb-11ea-8716-cac2396be7c9.png)

の二つかなと思います。別途ファイルを追加する場合は、plugin.xml に追加するファイルを既述する必要があるので、ご注意ください。


## 画面をいじる

- cordova なので HTML ファイルをいじってください
- `Demo/www/**` がビルド時にバンドルされる HTML/CSS/JS になります
- とりあえずテストボタンを追加しております


## 開発の留意点

### フォルダ-構成

```
- demo
    - package.json
    - www // cordova 上で表示する HTML/CSS/JS
        + css
        + img
        + js 
            - index.js <- ここに画面に関するロジックを書く
        + index.html　<- こいつをいじると画面が反映される

- src <-  各プラットフォームのソースファイルが入っている
    + ios
    + android

- www 
 + AdvanceDownloader.js <- ブリッジとなる js ファイル (js inteface)

```

### 何かおかしくなったら、
```bash
$ yarn clean
$ yarn add:ios
```


### プッシュするもの
基本的には `./src/**/*` と `www/src/` だけで大丈夫です


## ブリッジ interface


```js
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
```

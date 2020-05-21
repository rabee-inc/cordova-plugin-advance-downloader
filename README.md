# cordova-plugin-advance-downloader

## 環境セットアップ
### Cordova のインストール

```bash
$ npm install -g cordova@10.0.0-nightly.2020.5.20.7b8e8678
$ cordova -v 
```


### ios の場合、 cocoapod 使うので pod　のインストールをお願いします

留意点 ... pod の version を 1.8.0 以上にしておく


## ビルド
以下コマンドで platform を追加後、xcode or android studio で開いちゃってください
普通にビルドすれば動きます


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


## 開発の留意点

### 何かおかしくなったら、
```bash
$ yarn clean
$ yarn add:ios
```


### プッシュするもの
基本的には `./src/**/*` と `www/src/`

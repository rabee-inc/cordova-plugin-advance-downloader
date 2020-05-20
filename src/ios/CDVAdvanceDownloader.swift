@objc(CDVAdvanceDownloader) class CDVAdvanceDownloader:CDVPlugin {

    var progressCallbackId: String?
    
    override func pluginInitialize() {
        // アプリ起動時にここが呼ばれる
        print("hello this is CDVAdvanceDownloader")
    };
    
    @objc func add(_ command: CDVInvokedUrlCommand) {
        // TODO: something

        // 結果を生成して返す 
        let result = CDVPluginResult(status: CDVCommandStatus_OK)
        
        // result にいろいろなデータを渡して返してあげることができる
        // let result = CDVPluginResult(status: CDVCommandStatus_OK,  messageAs: "hi")

        // 辞書式配列で返すことが可能 (その場合勝手に js 側では json になる)
        // let result = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: ["hoge": "foo"])
        
        commandDelegate.send(result, callbackId: command.callbackId)
        
        // callback id に関しては保存しておいて後から何度も呼び出すことが可能
        // その場合 result の　keepCallback を true にする

        // if progressCallbackId != nil {
        //     let result = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: "10.000")
        //     result?.keepCallback = true
        //     self.commandDelegate.send(result, callbackId: progressCallbackId)
        // }

        // 非同期処理や時間がかかるものに関して commandDelegate の run(inBackground: {}) メソッドを利用してやる
        // commandDelegate.run(inBackground: { 
        //     // 画像アップロード処理とか...
        // })
    }
}

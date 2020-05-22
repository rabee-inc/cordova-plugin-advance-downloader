@objc(CDVAdvanceDownloader) class CDVAdvanceDownloader:CDVPlugin {

    var progressCallbackId: String?
    
    override func pluginInitialize() {
        // アプリ起動時にここが呼ばれる
        print("hello this is CDVAdvanceDownloader")
    };

    // id と file 名一覧 とステータスが取得できる
    @objc func getTasks(_ command: CDVInvokedUrlCommand) {}
    
    // 特定のダウンロード
    @objc func add(_ command: CDVInvokedUrlCommand) {
        // TODO: something

        // parameter として渡したい
        //{
        //     url: 'https://hgoehgoe.com'
        //     path: 'documents/hoehoge',
        //     fileName: 'hoge.mp4',
        // }
            
        // js 側で params に渡した第一引数が取れてくる。
        let value = command.argument(at: 0) as! String
        
        // 結果を生成して返す 
        // let result = CDVPluginResult(status: CDVCommandStatus_OK)
        // result にいろいろなデータを渡して返してあげることができる
         let result = CDVPluginResult(status: CDVCommandStatus_OK,  messageAs: value)

        // 辞書式配列で返すことが可能 (その場合勝手に js 側では json になる)
        // let result = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: ["hoge": "foo"])
        
        commandDelegate.send(result, callbackId: command.callbackId)
        
        // callback id に関しては保存しておいて後から何度も呼び出すことが可能
        // その場合 result の　keepCallback を true にする

        // if progressCallbackId != nil {
        //     let result = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: "10.000")
        //     result?.keepCallback = true
        // }

        // 非同期処理や時間がかかるものに関して commandDelegate の run(inBackground: {}) メソッドを利用してやる
        // commandDelegate.run(inBackground: { 
        //     // 画像アップロード処理とか...
        // })
    }


    // 特定のidを指定してダウンロードをスタートする
    @objc func start(_ command: CDVInvokedUrlCommand) {
        let id = command.argument(at: 0) as! String // guard した方がいい？
        print("start ==========> id: \(id)")
        let result = CDVPluginResult(status: CDVCommandStatus_OK)
        commandDelegate.send(result, callbackId: command.callbackId)
    }

    // 特定のidを指定してダウンロードを中断する
    @objc func pause(_ command: CDVInvokedUrlCommand) {
        let id = command.argument(at: 0) as! String // guard した方がいい？
        print("pause ==========> id: \(id)")
        let result = CDVPluginResult(status: CDVCommandStatus_OK)
        commandDelegate.send(result, callbackId: command.callbackId)
    }

    // 特定のidを指定してダウンロードを再開する
    @objc func resume(_ command: CDVInvokedUrlCommand) {
        let id = command.argument(at: 0) as! String // guard した方がいい？
        print("resume ==========> id: \(id)")
        let result = CDVPluginResult(status: CDVCommandStatus_OK)
        commandDelegate.send(result, callbackId: command.callbackId)
    }

    // 特定のidを指定してダウンロードを注視する
    @objc func stop(_ command: CDVInvokedUrlCommand) {
        let id = command.argument(at: 0) as! String // guard した方がいい？
        print("stop ==========> id: \(id)")
        let result = CDVPluginResult(status: CDVCommandStatus_OK)
        commandDelegate.send(result, callbackId: command.callbackId)
    }
    
    // 特定のidのアクションのコールバックの登録
    // 欲しいイベント [start, pause, resume, stop, complete, fail]
    @objc func on(_ command: CDVInvokedUrlCommand) {
        let id = command.argument(at: 0) as! String
        let event = command.argument(at: 1) as! String
        let callbackId = command.callbackId
        // event が callbackId を複数もつイメージ
        // ここで登録した callbackId を各イベントごとに発火してあげるイメージ
        // できれば特定の id に対して複数の on ができると嬉しい
        // { complete: [id1: [callbackId1a, callbackId1b], id2: callbackId2, ...etc ]}
    }

    // 特定のidのコールバックidを削除する
    @objc func off(_ command: CDVInvokedUrlCommand) {
        let id = command.argument(at: 0) as! String
        let action = command.argument(at: 0) as! String
    }
}

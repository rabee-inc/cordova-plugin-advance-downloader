@objc(CDVAdvanceDownloader) class CDVAdvanceDownloader: CDVPlugin {

    var client = CDVADDownload()
    
    var onChangedStatusCallbackIDs: [String:[String]] = [:]
    var onProgressCallbackIDs: [String:[String]] = [:]
    var onCompleteCallbackIDs: [String:[String]] = [:]
    var onFailedCallbackIDs: [String:[String]] = [:]
    
    override func pluginInitialize() {
        CDVADNotification.register()
        client.clean()
    };

    // 現在のダウンロード状況を全て取得
    @objc func getTasks(_ command: CDVInvokedUrlCommand) {
        let tasks = client.list()
        var results: [[String: Any]] = []
        for task in tasks {
            results.append([
                "id": task.id,
                "url": task.url,
                "headers": task.headers,
                "size": task.size,
                "file_path": task.filePath,
                "file_name": task.fileName,
                "progress": task.progress,
                "status": task.status
            ])
        }
        commandDelegate.send(
            CDVPluginResult(status: CDVCommandStatus_OK,  messageAs: results),
            callbackId: command.callbackId
        )
    }
    
    // ダウンロードを追加
    @objc func add(_ command: CDVInvokedUrlCommand) {
        guard
            let id = command.argument(at: 0) as? String,
            let urlStr = command.argument(at: 1) as? String,
            let url = URL(string: urlStr),
            let headers = command.argument(at: 2) as? [String:String],
            let size = command.argument(at: 3) as? Int,
            let filePath = command.argument(at: 4) as? String,
            let fileName = command.argument(at: 5) as? String else {
            let result = ["message": "invalid argument"]
            commandDelegate.send(
                CDVPluginResult(status: CDVCommandStatus_ERROR,  messageAs: result),
                callbackId: command.callbackId
            )
            return
        }
        
        var task = CDVADTask(id: id, url: url, headers: headers, size: size, filePath: filePath, fileName: fileName)
        task.onChangeStatus = { [weak self] status in
            guard let self = self, let cIDs = self.onChangedStatusCallbackIDs[id] else { return }
            let result: [String:Any] = [
                "id": id,
                "status": status,
            ]
            for cID in cIDs {
                self.commandDelegate.send(
                    CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result),
                    callbackId: cID
                )
            }
        }
        task.onProgress = { [weak self] progress in
            guard let self = self, let cIDs = self.onProgressCallbackIDs[id] else { return }
            let result: [String:Any] = [
               "id": id,
               "progress": progress,
            ]
            for cID in cIDs {
                self.commandDelegate.send(
                    CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result),
                    callbackId: cID
                )
            }
        }
        task.onComplete = { [weak self] fileURL in
            guard let self = self, let cIDs = self.onCompleteCallbackIDs[id] else { return }
            let result: [String:Any] = [
               "id": id,
               "file_url": fileURL.absoluteString,
            ]
            for cID in cIDs {
                self.commandDelegate.send(
                    CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result),
                    callbackId: cID
                )
            }
            CDVADNotification.send(title: "", body: "ダウンロードが完了しました", badge: 0)
        }
        task.onFailed = { [weak self] message in
            guard let self = self, let cIDs = self.onFailedCallbackIDs[id] else { return }
            let result: [String:Any] = [
               "id": id,
               "message": message,
            ]
            for cID in cIDs {
                self.commandDelegate.send(
                    CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result),
                    callbackId: cID
                )
            }
        }
        client.add(task: task)
        
        let result = ["id": id]
        commandDelegate.send(
            CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result),
            callbackId: command.callbackId
        )
    }


    // ダウンロードを開始
    @objc func start(_ command: CDVInvokedUrlCommand) {
        guard let id = command.argument(at: 0) as? String else {
            let result = ["message": "invalid argument"]
            commandDelegate.send(
                CDVPluginResult(status: CDVCommandStatus_ERROR,  messageAs: result),
                callbackId: command.callbackId
            )
            return
        }
        
        client.start(id: id)
        
        let result = ["id": id]
        commandDelegate.send(
            CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result),
            callbackId: command.callbackId
        )
    }

    // ダウンロードを一時停止
    @objc func pause(_ command: CDVInvokedUrlCommand) {
        guard let id = command.argument(at: 0) as? String else {
            let result = ["message": "invalid argument"]
            commandDelegate.send(
                CDVPluginResult(status: CDVCommandStatus_ERROR,  messageAs: result),
                callbackId: command.callbackId
            )
            return
        }
        
        client.pause(id: id)
        
        let result = ["id": id]
        commandDelegate.send(
            CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result),
            callbackId: command.callbackId
        )
    }

    // ダウンロードを再開
    @objc func resume(_ command: CDVInvokedUrlCommand) {
        guard let id = command.argument(at: 0) as? String else {
            let result = ["message": "invalid argument"]
            commandDelegate.send(
                CDVPluginResult(status: CDVCommandStatus_ERROR,  messageAs: result),
                callbackId: command.callbackId
            )
            return
        }
        
        client.resume(id: id)
        
        let result = ["id": id]
        commandDelegate.send(
            CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result),
            callbackId: command.callbackId
        )
    }

    // ダウンロードをキャンセル
    @objc func stop(_ command: CDVInvokedUrlCommand) {
        guard let id = command.argument(at: 0) as? String else {
            let result = ["message": "invalid argument"]
            commandDelegate.send(
                CDVPluginResult(status: CDVCommandStatus_ERROR,  messageAs: result),
                callbackId: command.callbackId
            )
            return
        }
        
        client.cancel(id: id)
        
        let result = ["id": id]
        commandDelegate.send(
            CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result),
            callbackId: command.callbackId
        )
    }
    
    // ステータス変更のコールバックを設定
    @objc func setOnChangedStatus(_ command: CDVInvokedUrlCommand) {
        guard let id = command.argument(at: 0) as? String else {
            let result = ["message": "invalid argument"]
            commandDelegate.send(
                CDVPluginResult(status: CDVCommandStatus_ERROR,  messageAs: result),
                callbackId: command.callbackId
            )
            return
        }
        if var ids = onChangedStatusCallbackIDs[id] {
            ids.append(command.callbackId)
            onChangedStatusCallbackIDs[id] = ids
        } else {
            onChangedStatusCallbackIDs[id] = [command.callbackId]
        }
        
        let result = ["id": id]
        commandDelegate.send(
            CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result),
            callbackId: command.callbackId
        )
    }
    
    // ステータス変更のコールバックを解除
    @objc func removeOnChangedStatus(_ command: CDVInvokedUrlCommand) {
        guard let id = command.argument(at: 0) as? String else {
            let result = ["message": "invalid argument"]
            commandDelegate.send(
                CDVPluginResult(status: CDVCommandStatus_ERROR,  messageAs: result),
                callbackId: command.callbackId
            )
            return
        }
        if var ids = onChangedStatusCallbackIDs[id] {
            for i in 0 ..< ids.count {
                if ids[i] == id {
                    ids.remove(at: i)
                }
            }
            onChangedStatusCallbackIDs[id] = ids
        }
        
        let result = ["id": id]
        commandDelegate.send(
            CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result),
            callbackId: command.callbackId
        )
    }
    
    // プログレスのコールバックを設定
    @objc func setOnProgress(_ command: CDVInvokedUrlCommand) {
        guard let id = command.argument(at: 0) as? String else {
            let result = ["message": "invalid argument"]
            commandDelegate.send(
                CDVPluginResult(status: CDVCommandStatus_ERROR,  messageAs: result),
                callbackId: command.callbackId
            )
            return
        }
        if var ids = onProgressCallbackIDs[id] {
            ids.append(command.callbackId)
            onProgressCallbackIDs[id] = ids
        } else {
            onProgressCallbackIDs[id] = [command.callbackId]
        }
        
        let result = ["id": id]
        commandDelegate.send(
            CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result),
            callbackId: command.callbackId
        )
    }
    
    // プログレスのコールバックを解除
    @objc func removeOnProgressStatus(_ command: CDVInvokedUrlCommand) {
        guard let id = command.argument(at: 0) as? String else {
            let result = ["message": "invalid argument"]
            commandDelegate.send(
                CDVPluginResult(status: CDVCommandStatus_ERROR,  messageAs: result),
                callbackId: command.callbackId
            )
            return
        }
        if var ids = onProgressCallbackIDs[id] {
            for i in 0 ..< ids.count {
                if ids[i] == id {
                    ids.remove(at: i)
                }
            }
            onProgressCallbackIDs[id] = ids
        }
        
        let result = ["id": id]
        commandDelegate.send(
            CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result),
            callbackId: command.callbackId
        )
    }
    
    // ダウンロード成功のコールバックを設定
    @objc func setOnComplete(_ command: CDVInvokedUrlCommand) {
        guard let id = command.argument(at: 0) as? String else {
            let result = ["message": "invalid argument"]
            commandDelegate.send(
                CDVPluginResult(status: CDVCommandStatus_ERROR,  messageAs: result),
                callbackId: command.callbackId
            )
            return
        }
        if var ids = onCompleteCallbackIDs[id] {
            ids.append(command.callbackId)
            onCompleteCallbackIDs[id] = ids
        } else {
            onCompleteCallbackIDs[id] = [command.callbackId]
        }
        
        let result = ["id": id]
        commandDelegate.send(
            CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result),
            callbackId: command.callbackId
        )
    }
    
    // ダウンロード成功のコールバックを解除
    @objc func removeOnComplete(_ command: CDVInvokedUrlCommand) {
        guard let id = command.argument(at: 0) as? String else {
            let result = ["message": "invalid argument"]
            commandDelegate.send(
                CDVPluginResult(status: CDVCommandStatus_ERROR,  messageAs: result),
                callbackId: command.callbackId
            )
            return
        }
        if var ids = onCompleteCallbackIDs[id] {
            for i in 0 ..< ids.count {
                if ids[i] == id {
                    ids.remove(at: i)
                }
            }
            onCompleteCallbackIDs[id] = ids
        }
        
        let result = ["id": id]
        commandDelegate.send(
            CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result),
            callbackId: command.callbackId
        )
    }
    
    // ダウンロード失敗のコールバックを設定
    @objc func setOnFailed(_ command: CDVInvokedUrlCommand) {
        guard let id = command.argument(at: 0) as? String else {
           let result = ["message": "invalid argument"]
           commandDelegate.send(
               CDVPluginResult(status: CDVCommandStatus_ERROR,  messageAs: result),
               callbackId: command.callbackId
           )
           return
        }
        if var ids = onFailedCallbackIDs[id] {
           ids.append(command.callbackId)
           onFailedCallbackIDs[id] = ids
        } else {
           onFailedCallbackIDs[id] = [command.callbackId]
        }
        
        let result = ["id": id]
        commandDelegate.send(
            CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result),
            callbackId: command.callbackId
        )
    }
    
    // ダウンロード失敗のコールバックを解除
    @objc func removeOnFailed(_ command: CDVInvokedUrlCommand) {
        guard let id = command.argument(at: 0) as? String else {
            let result = ["message": "invalid argument"]
            commandDelegate.send(
                CDVPluginResult(status: CDVCommandStatus_ERROR,  messageAs: result),
                callbackId: command.callbackId
            )
            return
        }
        if var ids = onFailedCallbackIDs[id] {
            for i in 0 ..< ids.count {
                if ids[i] == id {
                    ids.remove(at: i)
                }
            }
            onFailedCallbackIDs[id] = ids
        }
        
        let result = ["id": id]
        commandDelegate.send(
            CDVPluginResult(status: CDVCommandStatus_OK, messageAs: result),
            callbackId: command.callbackId
        )
    }
}

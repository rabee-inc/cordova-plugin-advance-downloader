//
//  CDVADDownload.swift
//  TAD
//
//  Created by hirose.yuuki on 2020/05/24.
//

import Foundation
import Alamofire
import UserNotifications

enum CDVADStatus: String {
    case Waiting = "waiting"
    case Processing = "processing"
    case Paused = "paused"
    case Canceled = "canceled"
    case Complete = "complete"
    case Failed = "failed"
}

typealias CDVADTaskOnChangedStatus = (CDVADStatus) -> Void
typealias CDVADTaskOnProgress = (Float) -> Void
typealias CDVADTaskOnComplete = (URL, String) -> Void
typealias CDVADTaskOnFailed = (String) -> Void

struct CDVADTask {
    var id: String
    var url: URL
    var headers: [String: String]
    var size: Int
    var filePath: String
    var fileName: String
    var request: DownloadRequest?
    var progress: Float = 0.0
    var status: CDVADStatus = .Waiting
    var onChangeStatus: CDVADTaskOnChangedStatus?
    var onProgress: CDVADTaskOnProgress?
    var onComplete: CDVADTaskOnComplete?
    var onFailed: CDVADTaskOnFailed?
    
    init(id: String, url: URL, headers: [String:String], size: Int, filePath: String, fileName: String) {
        self.id = id
        self.url = url
        self.headers = headers
        self.size = size
        self.filePath = filePath
        self.fileName = fileName
    }
}

class CDVADDownload {
    private var tasks: Dictionary<String, CDVADTask>
    private var manager: Session
    private var backgroundTaskID: UIBackgroundTaskIdentifier
    
    private let stopBackgroundTaskID = UIBackgroundTaskIdentifier(0)
    private let mutex = NSLock()
    
    private let msgDownloadFailed = "データのダウンロードに失敗しました"
    private let msgCapacityLack = "端末の容量が不足しているため、ダウンロードできませんでした"
    
    init() {
        tasks = Dictionary<String, CDVADTask>()
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 30;
        manager = Alamofire.Session(configuration: config)
        backgroundTaskID = stopBackgroundTaskID
    }
}

// Action
extension CDVADDownload {
    
    func clean() {
        mutex.lock()
        let downloadingFileURLs = CDVADUserDefaults.downloadingFileURLs
        for downloadingFileURL in downloadingFileURLs {
            guard let url = URL(string: downloadingFileURL) else { continue }
            do {
                try FileManager.default.removeItem(at: url)
            } catch {
                print("file remove error: \(url)")
            }
        }
        CDVADUserDefaults.downloadingFileURLs = []
        mutex.unlock()
    }
    
    func add(task: CDVADTask) {
        mutex.lock()
        guard tasks[task.id] == nil else {
            mutex.unlock()
            return
        }
        tasks[task.id] = task
        mutex.unlock()
        
        task.onChangeStatus?(.Waiting)
    }
    
    func get(id: String) -> CDVADTask? {
        return tasks[id]
    }
    
    func list() -> [CDVADTask] {
        return tasks.map({ $0.value })
    }
    
    func start(id: String) {
        mutex.lock()
        guard var task = tasks[id] else {
            mutex.unlock()
            return
        }
        run(task: &task)
        mutex.unlock()
    }
    
    func pause(id: String) {
        mutex.lock()
        guard var task = tasks[id], let request = task.request, task.status == .Processing else {
            mutex.unlock()
            return
        }
        request.suspend()
        task.status = .Paused
        tasks[id] = task
        mutex.unlock()
        
        task.onChangeStatus?(.Paused)
    }
    
    func resume(id: String) {
        mutex.lock()
        guard var task = tasks[id], let request = task.request, task.status == .Paused else {
            mutex.unlock()
            return
        }
        request.resume()
        task.status = .Processing
        tasks[id] = task
        mutex.unlock()
        
        task.onChangeStatus?(.Processing)
    }
    
    func cancel(id: String) {
        mutex.lock()
        guard var task = tasks[id], let request = task.request,
            task.status == .Waiting || task.status == .Processing || task.status == .Paused else {
            mutex.unlock()
            return
        }
        
        request.cancel()
        task.status = .Canceled
        tasks[id] = task
        mutex.unlock()
        
        task.onChangeStatus?(.Canceled)
    }
}

// Private
extension CDVADDownload {
    
    private func run(task: inout CDVADTask) {
        let id = task.id
        
        guard let freeSize = CDVADSystem.freeSize(), freeSize > task.size else {
            self.setFailed(id: id, message: self.msgCapacityLack, err: nil)
            return
        }
        
        task.status = .Processing
        task.onChangeStatus?(.Processing)
        
        let fileURL = generateFileURL(filePath: task.filePath, fileName: task.fileName)
        var downloadingFileURLs = CDVADUserDefaults.downloadingFileURLs
        downloadingFileURLs.append(fileURL.absoluteString)
        CDVADUserDefaults.downloadingFileURLs = downloadingFileURLs
        
        // バックグラウンドタスク開始
        if backgroundTaskID == stopBackgroundTaskID {
            backgroundTaskID = UIApplication.shared.beginBackgroundTask(expirationHandler: nil)
        }
        
        // 保存先の指定
        let destination: DownloadRequest.Destination = { _, _ in
            return (fileURL, [.removePreviousFile, .createIntermediateDirectories])
        }
        
        // ダウンロード開始
        var req = URLRequest(url: task.url)
        for (field, value) in task.headers {
            req.setValue(value, forHTTPHeaderField: field)
        }
        let request = manager.download(req, to: destination)
            .downloadProgress(closure: { [weak self] progress in
                guard let self = self else { return }
                self.mutex.lock()
                self.setProgress(id: id, progress: Float(progress.fractionCompleted))
                self.mutex.unlock()
            })
            .response(completionHandler: { [weak self] response in
                guard let self = self else { return }
                if let err = response.error {
                    self.mutex.lock()
                    if (err.isExplicitlyCancelledError) {
                        // TODO: handle cancel
                    }
                    else {
                        self.setFailed(id: id, message: self.msgDownloadFailed, err: err)
                    }
                    self.mutex.unlock()
                } else {
                    self.mutex.lock()
                    self.setComplete(id: id, fileURL: fileURL)
                    self.setDownloaded(fileURL: fileURL.absoluteString)
                    self.mutex.unlock()
                }
                
                // 未ダウンロードのタスクがあったら再帰処理
                if var waitTask = self.tasks.filter({ $0.value.status == .Waiting }).map({ $0.value }).first {
                    self.run(task: &waitTask)
                } else {
                    // バックグラウンドタスク終了
                    self.mutex.lock()
                    if !self.tasks.contains(where:{
                        switch $0.value.status {
                        case .Waiting, .Canceled, .Complete, .Failed:
                            return false
                        case .Processing, .Paused:
                            return true
                        }
                    }) {
                        UIApplication.shared.endBackgroundTask(self.backgroundTaskID)
                        self.backgroundTaskID = self.stopBackgroundTaskID
                    }
                    self.mutex.unlock()
                }
            })
        task.request = request
        tasks[id] = task
    }
    
    private func setDownloaded(fileURL: String) {
        var downloadingFileURLs = CDVADUserDefaults.downloadingFileURLs
        for i in 0 ..< downloadingFileURLs.count {
            let downloadingFileURL = downloadingFileURLs[i]
            if downloadingFileURL == fileURL {
                downloadingFileURLs.remove(at: i)
                break
            }
        }
        CDVADUserDefaults.downloadingFileURLs = downloadingFileURLs
    }
    
    private func setProgress(id: String, progress: Float) {
        guard let task = tasks[id] else {
            return
        }
        
        task.onProgress?(progress)
    }
    
    private func setComplete(id: String, fileURL: URL) {
        guard var task = tasks[id] else {
            return
        }
        task.status = .Complete
        tasks[id] = task
        
        task.onChangeStatus?(.Complete)
        task.onComplete?(fileURL, task.fileName)
    }
    
    private func setFailed(id: String, message: String, err: Error?) {
        if let err = err {
            print(err.localizedDescription)
            print(err)
        }
        
        guard var task = tasks[id] else {
            return
        }
        task.status = .Failed
        tasks[id] = task
        
        task.onChangeStatus?(.Failed)
        task.onFailed?(message)
    }

    private func generateFileURL(filePath: String, fileName: String) -> URL {
        let fileURL = URL(fileURLWithPath: filePath.replacingOccurrences(of: "file://", with: ""))
        return fileURL
    }
}


struct CDVADSystem {
    static func freeSize() -> Int? {
        guard
            let path = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true).last,
            let systemAttrs = try? FileManager.default.attributesOfFileSystem(forPath: path),
            let freeSize = systemAttrs[FileAttributeKey.systemFreeSize] as? NSNumber else { return nil }
        return freeSize.intValue
    }
}


//
//  CDVADNotification.swift
//  downloads
//
//  Created by hirose.yuuki on 2020/05/25.
//  Copyright © 2020 aikizoku. All rights reserved.



struct CDVADNotification {
    
    static func register() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.badge, .sound, .alert], completionHandler: { (granted, err) in
            guard err == nil else {
                print(err as Any)
                return
            }
            guard !granted else {
                print("通知拒否")
                return
            }
        })
    }
    
    static func send(title: String, body: String, badge: Int) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.badge = NSNumber(value: badge)
        let request = UNNotificationRequest(
            identifier: "CDVADNotification",
            content: content,
            trigger:UNTimeIntervalNotificationTrigger(timeInterval: 1, repeats: false)
        )
        UNUserNotificationCenter.current().add(request, withCompletionHandler: nil)
    }
}



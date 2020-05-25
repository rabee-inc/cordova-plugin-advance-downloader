//
//  CDVADDownload.swift
//  TAD
//
//  Created by hirose.yuuki on 2020/05/24.
//

import Foundation
import Alamofire

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
typealias CDVADTaskOnComplete = (URL) -> Void
typealias CDVADTaskOnFailed = (Error) -> Void

struct CDVADTask {
    var id: String
    var url: URL
    var filePath: String
    var fileName: String
    var request: DownloadRequest?
    var progress: Float = 0.0
    var status: CDVADStatus = .Waiting
    var onChangeStatus: CDVADTaskOnChangedStatus?
    var onProgress: CDVADTaskOnProgress?
    var onComplete: CDVADTaskOnComplete?
    var onFailed: CDVADTaskOnFailed?
    
    init(id: String, url: URL, filePath: String, fileName: String) {
        self.id = id
        self.url = url
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
    
    init() {
        tasks = Dictionary<String, CDVADTask>()
        manager = Alamofire.Session(configuration: URLSessionConfiguration.default)
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
        let id = task.id
        let req = URLRequest(url: task.url)
        let request = manager.download(req, to: destination)
            .downloadProgress(closure: { [weak self] progress in
                guard let self = self else { return }
                self.setProgress(id: id, progress: Float(progress.fractionCompleted))
            })
            .response(completionHandler: { [weak self] response in
                guard let self = self else { return }
                if let err = response.error {
                    self.setFailed(id: id, err: err)
                } else {
                    self.setComplete(id: id, fileURL: fileURL)
                    
                    self.mutex.lock()
                    var downloadingFileURLs = CDVADUserDefaults.downloadingFileURLs
                    for i in 0 ..< downloadingFileURLs.count {
                        let downloadingFileURL = downloadingFileURLs[i]
                        if downloadingFileURL == fileURL.absoluteString {
                            downloadingFileURLs.remove(at: i)
                        }
                    }
                    CDVADUserDefaults.downloadingFileURLs = downloadingFileURLs
                    self.mutex.unlock()
                }
                
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
            })
        task.request = request
        tasks[id] = task
    }
    
    private func setProgress(id: String, progress: Float) {
        mutex.lock()
        guard let task = tasks[id] else {
            mutex.unlock()
            return
        }
        mutex.unlock()
        
        task.onProgress?(progress)
    }
    
    private func setComplete(id: String, fileURL: URL) {
        mutex.lock()
        guard var task = tasks[id] else {
            mutex.unlock()
            return
        }
        task.status = .Complete
        tasks[id] = task
        mutex.unlock()
        
        task.onChangeStatus?(.Complete)
        task.onComplete?(fileURL)
    }
    
    private func setFailed(id: String, err: Error) {
        mutex.lock()
        guard var task = tasks[id] else {
            mutex.unlock()
            return
        }
        task.status = .Failed
        tasks[id] = task
        mutex.unlock()
        
        task.onChangeStatus?(.Failed)
        task.onFailed?(err)
    }
    
    private func generateFileURL(filePath: String, fileName: String) -> URL {
        let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        return documentsURL.appendingPathComponent("\(filePath)/\(fileName)")
    }
}

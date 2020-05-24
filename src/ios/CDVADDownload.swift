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
    }
    
    func add(task: CDVADTask) {
        mutex.lock()
        
        guard tasks[task.id] == nil else { return }
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
        guard let task = tasks[id] else { return }
        run(task: task)
    }
    
    func pause(id: String) {
        guard var task = tasks[id], let request = task.request, task.status == .Processing else { return }
        
        request.suspend()
        
        task.status = .Paused
        task.onChangeStatus?(.Paused)
    }
    
    func resume(id: String) {
        guard var task = tasks[id], let request = task.request, task.status == .Paused else { return }
        
        request.resume()
        
        task.status = .Processing
        task.onChangeStatus?(.Processing)
    }
    
    func cancel(id: String) {
        guard var task = tasks[id], let request = task.request,
            task.status == .Waiting || task.status == .Processing || task.status == .Paused else { return }
        
        request.cancel()
        
        task.status = .Canceled
        task.onChangeStatus?(.Canceled)
    }
}

// Private
extension CDVADDownload {
    
    private func run(task: CDVADTask) {
        var task = task
        
        task.status = .Processing
        task.onChangeStatus?(.Processing)
        
        let fileURL = generateFileURL(filePath: task.filePath, fileName: task.fileName)
        var downloadingFileURLs = CDVADUserDefaults.downloadingFileURLs
        downloadingFileURLs.append(fileURL.absoluteString)
        CDVADUserDefaults.downloadingFileURLs = downloadingFileURLs
        
        // バックグラウンドタスク開始
        mutex.lock()
        if backgroundTaskID == stopBackgroundTaskID {
            backgroundTaskID = UIApplication.shared.beginBackgroundTask(expirationHandler: nil)
        }
        mutex.unlock()
        
        // 保存先の指定
        let destination: DownloadRequest.Destination = { _, _ in
            return (fileURL, [.removePreviousFile, .createIntermediateDirectories])
        }
        
        // ダウンロード開始
        let req = URLRequest(url: task.url)
        let request = manager.download(req, to: destination)
            .downloadProgress(closure: { progress in
                task.onProgress?(Float(progress.fractionCompleted))
            })
            .response(completionHandler: { [weak self] response in
                guard let self = self else { return }
                if let err = response.error {
                    task.status = .Failed
                    task.onChangeStatus?(.Failed)
                    task.onFailed?(err)
                } else {
                    task.status = .Complete
                    task.onChangeStatus?(.Complete)
                    task.onComplete?(fileURL)
                    
                    var downloadingFileURLs = CDVADUserDefaults.downloadingFileURLs
                    for i in 0 ..< downloadingFileURLs.count {
                        let downloadingFileURL = downloadingFileURLs[i]
                        if downloadingFileURL == fileURL.absoluteString {
                            downloadingFileURLs.remove(at: i)
                        }
                    }
                    CDVADUserDefaults.downloadingFileURLs = downloadingFileURLs
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
    }
    
    private func generateFileURL(filePath: String, fileName: String) -> URL {
        let documentsURL = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        return documentsURL.appendingPathComponent("\(filePath)/\(fileName)")
    }
}

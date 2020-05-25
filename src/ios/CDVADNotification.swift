//
//  CDVADNotification.swift
//  downloads
//
//  Created by hirose.yuuki on 2020/05/25.
//  Copyright © 2020 aikizoku. All rights reserved.
//

import Foundation
import UserNotifications

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

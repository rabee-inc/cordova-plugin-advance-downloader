//
//  CDVADSystem.swift
//  downloads
//
//  Created by hirose.yuuki on 2020/05/25.
//  Copyright © 2020 aikizoku. All rights reserved.
//

import Foundation

struct CDVADSystem {
    
    static func freeSize() -> Int? {
        guard
            let path = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true).last,
            let systemAttrs = try? FileManager.default.attributesOfFileSystem(forPath: path),
            let freeSize = systemAttrs[FileAttributeKey.systemFreeSize] as? NSNumber else { return nil }
        return freeSize.intValue
    }
}

import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        KoinBridge.shared.start()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
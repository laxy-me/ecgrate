import SwiftUI

struct ContentView: View {
    @StateObject private var viewModel = RateViewModel()

    var body: some View {
        MainView(viewModel: viewModel)
    }
}

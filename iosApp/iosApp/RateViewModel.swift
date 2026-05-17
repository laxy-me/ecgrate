import Foundation
import shared
import WidgetKit

private let appGroupID = "group.com.laxy.ecgrate"

@MainActor
class RateViewModel: ObservableObject {
    @Published var rates: [CurrencyRate.Body] = []
    @Published var isLoading: Bool = false
    @Published var lastUpdated: String = ""
    @Published var selectedCurrency: String = "美元"
    @Published var errorMessage: String? = nil
    @Published var editMode: Bool = false
    @Published var interval: Int = 30

    private let iosClient = IOSRateClient()
    private var timer: Timer?

    init() {
        let saved = UserDefaults(suiteName: appGroupID)?.integer(forKey: "widget_refresh_interval") ?? 0
        if saved > 0 { interval = saved }

        iosClient.startObserving { [weak self] state in
            guard let self else { return }
            self.rates = state.rates as? [CurrencyRate.Body] ?? []
            self.isLoading = state.isLoading
            self.lastUpdated = state.lastUpdated
            self.selectedCurrency = state.selectedCurrency
            self.errorMessage = state.error
            self.persistToWidget(state: state)
        }
        iosClient.refresh()
        startPolling()
    }

    func refresh() { iosClient.refresh() }

    func selectCurrency(_ currency: String) {
        iosClient.setSelectedCurrency(currency: currency)
        iosClient.refresh()
    }

    func setInterval(_ seconds: Int) {
        interval = seconds
        if let d = UserDefaults(suiteName: appGroupID) {
            d.set(seconds, forKey: "widget_refresh_interval")
            d.synchronize()
        }
        WidgetCenter.shared.reloadAllTimelines()
        restartPolling()
    }

    var selectedBody: CurrencyRate.Body? {
        rates.first { $0.ccyNbr == selectedCurrency }
    }

    // MARK: - Widget 数据同步

    private func persistToWidget(state: RateState) {
        guard let rateList = state.rates as? [CurrencyRate.Body],
              let body = rateList.first(where: { $0.ccyNbr == state.selectedCurrency })
        else { return }

        guard let d = UserDefaults(suiteName: appGroupID) else { return }
        d.set(body.ccyNbr,   forKey: "widget_currency_name")
        d.set(body.rthBid,   forKey: "widget_rth_bid")
        d.set(body.rthOfr,   forKey: "widget_rth_ofr")
        d.set(body.rtcBid,   forKey: "widget_rtc_bid")
        d.set(body.rtcOfr,   forKey: "widget_rtc_ofr")
        d.set(body.ratTim,   forKey: "widget_last_updated")
        d.synchronize()
        WidgetCenter.shared.reloadAllTimelines()
    }

    // MARK: - Polling

    private func startPolling() {
        timer = Timer.scheduledTimer(withTimeInterval: TimeInterval(interval), repeats: true) { [weak self] _ in
            Task { @MainActor [weak self] in self?.iosClient.refresh() }
        }
    }

    private func restartPolling() {
        timer?.invalidate()
        startPolling()
    }

    deinit {
        timer?.invalidate()
        iosClient.dispose()
    }
}

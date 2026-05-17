import WidgetKit
import SwiftUI

private let appGroupID = "group.com.laxy.ecgrate"
private let apiURL     = URL(string: "https://fx.cmbchina.com/api/v1/fx/rate")!

// MARK: - API Models (widget 自用，不依赖 shared 模块)

private struct RateResponse: Decodable {
    let returnCode: String
    let body: [RateBody]
}

private struct RateBody: Decodable {
    let ccyNbr: String
    let ccyNbrEng: String
    let ratDat: String
    let ratTim: String
    let rthBid: String
    let rthOfr: String
    let rtcBid: String
    let rtcOfr: String
}

// MARK: - Timeline Entry

struct RateEntry: TimelineEntry {
    let date: Date
    let currencyName: String
    let rthBid: String
    let rthOfr: String
    let rtcBid: String
    let rtcOfr: String
    let lastUpdated: String

    static let placeholder = RateEntry(
        date: .now, currencyName: "USD",
        rthBid: "726.53", rthOfr: "729.37",
        rtcBid: "721.05", rtcOfr: "729.37",
        lastUpdated: ""
    )
}

// MARK: - Timeline Provider

struct RateProvider: TimelineProvider {
    func placeholder(in context: Context) -> RateEntry { .placeholder }

    func getSnapshot(in context: Context, completion: @escaping (RateEntry) -> Void) {
        if context.isPreview {
            completion(.placeholder)
        } else {
            Task { completion(await loadEntry()) }
        }
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<RateEntry>) -> Void) {
        Task {
            let entry = await loadEntry()
            let saved = UserDefaults(suiteName: appGroupID)?.integer(forKey: "widget_refresh_interval") ?? 0
            let interval = saved > 0 ? TimeInterval(saved) : 1800
            let next = Date(timeIntervalSinceNow: interval)
            completion(Timeline(entries: [entry], policy: .after(next)))
        }
    }

    // MARK: - Data Loading

    private func loadEntry() async -> RateEntry {
        if let fresh = await fetchFromNetwork() { return fresh }
        return readFromUserDefaults()
    }

    private func fetchFromNetwork() async -> RateEntry? {
        guard let (data, response) = try? await URLSession.shared.data(from: apiURL),
              (response as? HTTPURLResponse)?.statusCode == 200,
              let decoded = try? JSONDecoder().decode(RateResponse.self, from: data),
              decoded.returnCode == "SUC0000"
        else { return nil }

        // 优先读取主 App 选定的货币（以 ccyNbrEng 为键）
        let saved = UserDefaults(suiteName: appGroupID)?.string(forKey: "widget_currency_name")
        guard let body = decoded.body.first(where: { saved != nil && $0.ccyNbr == saved })
                      ?? decoded.body.first(where: { $0.ccyNbr == "美元" })
                      ?? decoded.body.first
        else { return nil }

        return RateEntry(
            date: .now,
            currencyName: body.ccyNbr,
            rthBid: body.rthBid,
            rthOfr: body.rthOfr,
            rtcBid: body.rtcBid,
            rtcOfr: body.rtcOfr,
            lastUpdated: body.ratTim
        )
    }

    private func readFromUserDefaults() -> RateEntry {
        let d = UserDefaults(suiteName: appGroupID)
        return RateEntry(
            date: .now,
            currencyName: d?.string(forKey: "widget_currency_name") ?? "--",
            rthBid:       d?.string(forKey: "widget_rth_bid")       ?? "--",
            rthOfr:       d?.string(forKey: "widget_rth_ofr")       ?? "--",
            rtcBid:       d?.string(forKey: "widget_rtc_bid")       ?? "--",
            rtcOfr:       d?.string(forKey: "widget_rtc_ofr")       ?? "--",
            lastUpdated:  d?.string(forKey: "widget_last_updated")  ?? ""
        )
    }
}

// Android 同款渐变：135° 右下深蓝 → 左上浅蓝
private let widgetGradient = LinearGradient(
    colors: [
        Color(red: 1/255,  green: 87/255,  blue: 155/255),  // #01579B
        Color(red: 3/255,  green: 155/255, blue: 229/255),  // #039BE5
    ],
    startPoint: .bottomTrailing,
    endPoint: .topLeading
)

// MARK: - Small View

struct SmallWidgetView: View {
    let entry: RateEntry

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("100 \(entry.currencyName)")
                .font(.system(size: 11))
                .foregroundStyle(.white.opacity(0.7))
                .tracking(0.4)
                .lineLimit(1)
            Spacer(minLength: 2)
            Text(entry.rthOfr)
                .font(.system(size: 30, weight: .bold))
                .foregroundStyle(.white)
                .minimumScaleFactor(0.6)
                .lineLimit(1)
            Spacer(minLength: 2)
            if !entry.lastUpdated.isEmpty {
                Text("↻  \(entry.lastUpdated)")
                    .font(.system(size: 10))
                    .foregroundStyle(.white.opacity(0.55))
                    .lineLimit(1)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
    }
}

// MARK: - Medium View

struct MediumWidgetView: View {
    let entry: RateEntry

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("100 \(entry.currencyName)")
                .font(.system(size: 11))
                .foregroundStyle(.white.opacity(0.7))
                .tracking(0.4)
                .lineLimit(1)
            Spacer(minLength: 4)
            HStack(alignment: .bottom, spacing: 0) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(entry.rthOfr)
                        .font(.system(size: 30, weight: .bold))
                        .foregroundStyle(.white)
                        .minimumScaleFactor(0.6)
                        .lineLimit(1)
                    Text("现汇卖出")
                        .font(.system(size: 10))
                        .foregroundStyle(.white.opacity(0.7))
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text(entry.rthBid)
                        .font(.system(size: 30, weight: .bold))
                        .foregroundStyle(.white)
                        .minimumScaleFactor(0.6)
                        .lineLimit(1)
                    Text("现汇买入")
                        .font(.system(size: 10))
                        .foregroundStyle(.white.opacity(0.7))
                }
            }
            Spacer(minLength: 4)
            if !entry.lastUpdated.isEmpty {
                Text("↻  \(entry.lastUpdated)")
                    .font(.system(size: 10))
                    .foregroundStyle(.white.opacity(0.55))
                    .lineLimit(1)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .padding(.horizontal, 14)
        .padding(.vertical, 10)
    }
}

// MARK: - Entry View

struct RateWidgetEntryView: View {
    @Environment(\.widgetFamily) var family
    let entry: RateEntry

    var body: some View {
        Group {
            switch family {
            case .systemMedium: MediumWidgetView(entry: entry)
            default:            SmallWidgetView(entry: entry)
            }
        }
        .containerBackground(for: .widget) { widgetGradient }
    }
}

// MARK: - Widget

struct RateWidget: Widget {
    let kind = "RateWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: RateProvider()) { entry in
            RateWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("外汇牌价")
        .description("显示招商银行实时外汇汇率")
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

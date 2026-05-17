import SwiftUI
import shared

struct MainView: View {
    @ObservedObject var viewModel: RateViewModel
    @State private var tempSelected: String? = nil
    @State private var intervalText: String = ""

    private let columns = [GridItem(.flexible()), GridItem(.flexible())]

    var body: some View {
        VStack(spacing: 0) {
            headerView
            contentView
        }
        .ignoresSafeArea(edges: .top)
    }

    // MARK: - Header

    private var headerView: some View {
        ZStack(alignment: .bottom) {
            LinearGradient(
                colors: [Color(hex: 0x1565C0), Color(hex: 0x42A5F5)],
                startPoint: .top,
                endPoint: .bottom
            )
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text("ECGRate")
                        .font(.title2.bold())
                        .foregroundColor(.white)
                    Spacer()
                    Button(viewModel.editMode ? "完成" : "编辑") {
                        if viewModel.editMode {
                            if let sel = tempSelected { viewModel.selectCurrency(sel) }
                            viewModel.editMode = false
                        } else {
                            tempSelected = nil
                            viewModel.editMode = true
                        }
                    }
                    .foregroundColor(.white)
                }

                Button(action: viewModel.refresh) {
                    Text(viewModel.lastUpdated.isEmpty
                         ? (viewModel.isLoading ? "加载中..." : "点击刷新")
                         : viewModel.lastUpdated)
                        .font(.caption)
                        .foregroundColor(.white.opacity(0.85))
                }

                if viewModel.editMode {
                    HStack {
                        TextField("刷新间隔(秒)", text: $intervalText)
                            .keyboardType(.numberPad)
                            .foregroundColor(.white)
                            .accentColor(.white)
                            .padding(8)
                            .overlay(
                                RoundedRectangle(cornerRadius: 6)
                                    .stroke(Color.white.opacity(0.6), lineWidth: 1)
                            )
                        Button("保存") {
                            if let val = Int(intervalText), val > 0 {
                                viewModel.setInterval(val)
                                intervalText = ""
                            }
                        }
                        .foregroundColor(.white)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(Color.white.opacity(0.2))
                        .cornerRadius(8)
                    }
                    .padding(.top, 4)
                }
            }
            .padding(.horizontal, 16)
            .padding(.top, 56)
            .padding(.bottom, 12)
        }
        .fixedSize(horizontal: false, vertical: true)
    }

    // MARK: - Content

    @ViewBuilder
    private var contentView: some View {
        if viewModel.isLoading && viewModel.rates.isEmpty {
            VStack {
                Spacer()
                ProgressView().progressViewStyle(.circular)
                Spacer()
            }
        } else {
            ScrollView {
                LazyVGrid(columns: columns, spacing: 8) {
                    ForEach(viewModel.rates, id: \.ccyNbr) { body in
                        let isSelected = body.ccyNbr == (tempSelected ?? viewModel.selectedCurrency)
                        RateItemView(
                            rateBody: body,
                            isSelected: isSelected && viewModel.editMode,
                            editMode: viewModel.editMode
                        ) {
                            if viewModel.editMode { tempSelected = body.ccyNbr }
                        }
                    }
                }
                .padding(8)
            }
            .background(Color(.systemGroupedBackground))
        }
    }
}

// MARK: - Rate Item

struct RateItemView: View {
    let rateBody: CurrencyRate.Body
    let isSelected: Bool
    let editMode: Bool
    let onTap: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(rateBody.ccyNbrEng)
                .font(.footnote.weight(.semibold))
                .lineLimit(1)

            Divider()

            rateRow(label: "现汇买入", value: rateBody.rthBid)
            rateRow(label: "现汇卖出", value: rateBody.rthOfr)
            rateRow(label: "现钞买入", value: rateBody.rtcBid)
            rateRow(label: "现钞卖出", value: rateBody.rtcOfr)
        }
        .padding(10)
        .background(isSelected ? Color.blue.opacity(0.1) : Color(.secondarySystemGroupedBackground))
        .cornerRadius(10)
        .overlay(
            RoundedRectangle(cornerRadius: 10)
                .stroke(isSelected ? Color.blue : Color.clear, lineWidth: 2)
        )
        .onTapGesture { if editMode { onTap() } }
    }

    private func rateRow(label: String, value: String) -> some View {
        HStack {
            Text(label)
                .font(.system(size: 10))
                .foregroundColor(.secondary)
            Spacer()
            Text(value)
                .font(.system(size: 10, weight: .medium))
        }
    }
}

// MARK: - Color extension

extension Color {
    init(hex: UInt32) {
        let r = Double((hex >> 16) & 0xFF) / 255
        let g = Double((hex >> 8) & 0xFF) / 255
        let b = Double(hex & 0xFF) / 255
        self.init(red: r, green: g, blue: b)
    }
}

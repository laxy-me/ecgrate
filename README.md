# ECGRate

招商银行外汇牌价查看 App，基于 **Kotlin Multiplatform (KMP)** 构建，Android 使用 Jetpack Compose，iOS 使用 SwiftUI。

---

## 项目结构

```
ecgrate/
├── shared/                          # KMP 共享模块（业务逻辑 + 网络）
│   └── src/
│       ├── commonMain/kotlin/com/laxy/ecgrate/
│       │   ├── entity/CurrencyRate.kt       # 汇率数据模型
│       │   ├── network/RateRepository.kt    # Ktor HTTP 客户端
│       │   └── RateClient.kt               # 核心业务逻辑（StateFlow）
│       ├── androidMain/                     # Android 平台特定代码
│       └── iosMain/kotlin/com/laxy/ecgrate/
│           └── IOSRateClient.kt            # iOS callback 桥接层
│
├── app/                             # Android App
│   └── src/main/java/com/laxy/ecgrate/
│       ├── EcgrateApp.kt           # Application 类，初始化 RateManager
│       ├── RateManager.kt          # 全局调度（AlarmManager、轮询、Widget 通知）
│       ├── MainViewModel.kt        # ViewModel，持有 StateFlow
│       ├── MainActivity.kt         # Compose Activity 入口
│       ├── ui/
│       │   ├── MainScreen.kt       # 主界面 Compose UI
│       │   └── Theme.kt            # Material3 主题
│       ├── widget/RateWidget.kt    # 桌面小部件（RemoteViews）
│       └── receiver/               # BroadcastReceiver
│           ├── BootReceiver.kt     # 开机后重新调度闹钟
│           ├── RefreshBroadcastReceiver.kt  # 闹钟触发刷新
│           └── ScreenReceiver.kt   # 屏幕开关控制轮询
│
└── iosApp/                          # iOS App
    ├── iosApp.xcodeproj/
    └── iosApp/
        ├── iosAppApp.swift          # App 入口（@main）
        ├── ContentView.swift        # 根视图
        ├── RateViewModel.swift      # ObservableObject，桥接 IOSRateClient
        ├── MainView.swift           # 主界面 SwiftUI（含 RateItemView）
        └── Assets.xcassets/
```

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 语言 | Kotlin 2.0.21 / Swift 5.9 |
| 构建 | Gradle 8.9 / AGP 8.5.2 |
| 共享网络 | Ktor 2.3.13（Android: OkHttp 引擎，iOS: Darwin 引擎）|
| 共享序列化 | kotlinx.serialization 1.7.3 |
| Android UI | Jetpack Compose + Material3 |
| iOS UI | SwiftUI |
| 数据流 | Kotlin `StateFlow`（Android）/ Timer + Callback（iOS）|
| 后台刷新 | AlarmManager（Android）/ `Timer`（iOS）|

---

## 开发环境要求

- **Android**: Android Studio Ladybug+ / JDK 11+
- **iOS**: Xcode 15+ / macOS 14+（构建 XCFramework 需要 macOS）
- **共同**: JDK 17+（运行 Gradle）

---

## 构建命令

### Android

```bash
# 构建 Debug APK
./gradlew :app:assembleDebug

# 构建 Release APK
./gradlew :app:assembleRelease

# 只编译检查（不打包）
./gradlew :app:compileDebugKotlin
```

### iOS XCFramework

```bash
# 构建 Debug XCFramework（开发用）
./gradlew :shared:assembleSharedDebugXCFramework

# 构建 Release XCFramework（发布用）
./gradlew :shared:assembleSharedReleaseXCFramework
```

构建产物路径：
```
shared/build/XCFrameworks/
├── debug/shared.xcframework
└── release/shared.xcframework
```

### 打开 iOS 项目

```bash
open iosApp/iosApp.xcodeproj
```

> 首次打开前必须先执行一次 `assembleSharedDebugXCFramework`，否则 Xcode 找不到 framework。

---

## 编辑注意事项

### 修改共享业务逻辑（推荐入口）

所有网络请求、数据模型、核心状态都在 `shared/src/commonMain/`，改完后：

1. Android 侧自动引用最新代码（同一 Gradle 构建）。
2. iOS 侧需重新构建 XCFramework：

```bash
./gradlew :shared:assembleSharedDebugXCFramework
```

然后 Xcode 重新 Build 即可（无需重新 Add framework）。

---

### 添加新的共享依赖

在 `shared/build.gradle.kts` 的对应 sourceSet 中添加：

```kotlin
sourceSets {
    commonMain.dependencies {
        implementation("...") // 两平台都需要
    }
    androidMain.dependencies {
        implementation("...") // 仅 Android
    }
    iosMain.dependencies {
        implementation("...") // 仅 iOS
    }
}
```

**注意**：并非所有库都支持 KMP，添加前确认库有 `commonMain` 或对应平台的 artifact。

---

### iOS Swift 类型名称规则

Kotlin/Native 将 Kotlin 类型暴露给 Swift 时，命名规则如下：

| Kotlin | Swift |
|--------|-------|
| `data class CurrencyRate` | `CurrencyRate` |
| `data class CurrencyRate.Body`（内部类）| `CurrencyRate.Body`（带点，非 `CurrencyRateBody`）|
| `object RateClient` | `RateClient` |
| `fun setSelectedCurrency(currency: String)` | `setSelectedCurrency(currency:)` |

如果不确定 Swift 名称，查看生成的头文件：

```bash
cat shared/build/XCFrameworks/debug/shared.xcframework/ios-arm64_x86_64-simulator/shared.framework/Headers/shared.h
```

---

### iOS 无法直接使用 Kotlin `StateFlow`

Kotlin 的 `StateFlow` 不能直接在 Swift 中收集，项目通过 `IOSRateClient`（`iosMain`）使用 callback 桥接：

```kotlin
// iosMain/IOSRateClient.kt
class IOSRateClient {
    fun startObserving(onState: (RateState) -> Unit) {
        scope.launch { client.state.collect { onState(it) } }
    }
}
```

Swift 侧在 `RateViewModel.swift` 中调用：

```swift
iosClient.startObserving { [weak self] state in
    self?.rates = state.rates as? [CurrencyRate.Body] ?? []
}
```

---

### Android Widget 依赖共享层

`RateWidget` 通过 `EcgrateApp.rateClient.selectedBody()` 读取当前选中货币的汇率数据，因此：

- Widget 收到广播时，`EcgrateApp` 已由 Android 自动创建，`RateManager.init()` 已执行。
- 若 Widget 显示空白，先在 App 内刷新一次让数据填充到 `RateClient.state`。

---

### 选中货币的存储格式

`selectedCurrency` 存储的是 **`ccyNbr` 字段值**（中文货币名，如 `"美元"`），而不是 ISO 数字代码。这与原始 API 返回格式一致，小部件通过以下逻辑提取货币符号：

```kotlin
// ccyNbrEng = "美元 USD"，ccyNbr = "美元"
val code = body.ccyNbrEng.replace(body.ccyNbr, "").trim() // "USD"
val symbol = Currency.getInstance(code).getSymbol(Locale.getDefault()) // "$"
```

---

## 数据来源

API 端点：`GET https://fx.cmbchina.com/api/v1/fx/rate`

返回字段说明：

| 字段 | 含义 |
|------|------|
| `ccyNbr` | 货币中文名（如 `美元`） |
| `ccyNbrEng` | 中英文组合（如 `美元 USD`） |
| `rthBid` | 现汇买入价 |
| `rthOfr` | 现汇卖出价 |
| `rtcBid` | 现钞买入价 |
| `rtcOfr` | 现钞卖出价 |
| `ratDat` / `ratTim` | 汇率日期 / 时间 |

---

## 常见问题

**Q: iOS 构建报 `cannot find type 'xxx' in scope`**  
A: 先执行 `./gradlew :shared:assembleSharedDebugXCFramework` 重新生成 XCFramework，再在 Xcode 中 Clean Build Folder（⇧⌘K）后重新构建。

**Q: Android 构建报 `Unresolved reference`**  
A: 检查导入包名，共享模块的类在 `com.laxy.ecgrate` 包下，不再是 `com.laxy.ecgrate.entity` 等子包（entity 包已移到 shared 模块）。

**Q: iOS 运行时 `RateState.rates` 为空**  
A: `rates` 从 Kotlin 传来是 `NSArray`，Swift 类型转换用 `as? [CurrencyRate.Body] ?? []`，若转换失败检查 XCFramework 版本是否最新。

**Q: 修改了 Kotlin 共享代码但 iOS 没生效**  
A: 重新运行 `./gradlew :shared:assembleSharedDebugXCFramework`，Xcode 项目引用的是文件系统上的 XCFramework，Gradle 构建会直接覆盖它。

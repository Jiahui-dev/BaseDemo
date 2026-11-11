# ble-kit

> 面向“以 GATT 特征读写通信”的 BLE 设备接入库。只管链路，不管协议。

[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)]()
[![minSdk](https://img.shields.io/badge/minSdk-24-green.svg)]()

---

## 为什么还要一个 BLE 库

Android 原生 BLE API 是“能跑通”的级别，不是“能上线”的级别。真实设备接入时会反复撞上同一批问题：

- 扫描被系统限流，`onScanFailed errorCode=2`，重启后依然失败；
- 连接成功后立刻 `discoverServices()` 成功率低，偶发 `status=133`；
- 断线后每个业务页面各写一套重连，最后变成三份不一致的逻辑；
- 数据超过 MTU 直接丢包，或连续写太快导致设备不回包；
- 不同固件脾性不同：有的收到 MTU 请求就不回包了，有的把写和通知拆在两个 Service 里。

这些不是“用法不对”，是**每个 BLE 项目都要重踩一遍的坑**。`ble-kit` 把这些收敛成默认行为，业务层只关心自己的报文。

## 快速开始

```kotlin
val ble = BleKit.create(context)          // 创建一个实例，建议跟随 ViewModel 存活

ble.connect(BpProfile, address)           // 内置：扫描 → 连接 → 发现服务 → 断线重连
ble.state.collect { /* ConnectionState */ }   // 链路状态，变化时回调
ble.packets.collect { bytes -> /* 你的协议解析 */ }
ble.write(cmd)                            // 自动按 MTU 分包
```

四行。没有 `Application` 初始化，没有 `BaseActivity`，没有继承要求。

---

## 写给不熟悉 Kotlin 协程的开发者

如果你之前一直写 Java + 回调，这一节可以让你在 3 分钟内看懂后面的代码。

| 你现在熟悉的写法 | ble-kit 里的写法 | 为什么换 |
|---|---|---|
| `interface XxxListener { void onXxx(T t); }` | `Flow<T>` + `collect { }` | 回调只能有一个监听者、容易漏取消；Flow 天然支持多订阅、跟随作用域自动取消 |
| `LiveData<T>` | `StateFlow<T>` | 同样“有当前值、可观察”，但不依赖主线程，也不需要 `observe(this)` |
| `EventBus.post(event)` | `SharedFlow<ByteArray>` | 语义一致：只推新事件，不保存历史值 |
| `new Thread(...).start()` / `Handler.postDelayed` | `suspend` 函数 + `delay()` | 不占用线程，代码是顺序的，不用写回调嵌套 |
| `abstract class` + 模板方法 | `interface` + `suspend` | Kotlin 没有强制继承的必要，组合更灵活 |
| `enum State { ... }` | `sealed interface` + `data object` | `enum` 不能带不同的参数；`sealed` 可以让 `Ready` 携带 `mtu` 而 `Idle` 不携带 |
| `try { } finally { }` 手动清理 | `lifecycleScope.launch { }` | 页面销毁自动取消，不用手动 removeCallback |

**三个关键字记住就够了：**

- `val` —— 相当于 Java 的 `final` 字段，只能读不能改；
- `suspend` —— “可挂起的函数”，只能在协程里调用，调用时不会卡住线程；
- `collect` —— “订阅”，后面的大括号就是每次数据到达时要执行的代码。

---

## 它解决什么

| 能力 | 说明 |
|---|---|
| 扫描 | 30s 扫描窗口 + 限流保护，拒绝 stop/start 竞态；多实例共享一次物理扫描 |
| 连接 | 连接后延迟发现服务，失败自动重试 |
| 重连 | 5s → 10s → 20s → 30s 指数退避，上限封顶 |
| 保活 | 可配置心跳/握手包，间隔可调 |
| 发送 | 按当前 MTU 自动分包，无响应写 20ms 节流，带响应写 500ms 兜底 |
| 状态 | `StateFlow` 暴露，无需手写回调转发 |
| 诊断 | 结构化事件日志 + 环形缓冲，现场问题可导出复盘 |
| 设备数 | **一个实例管理一台设备**；需要同时连多台时创建多个实例，互不干扰 |

## 它不做什么

这几条是硬边界，写在这里是为了防止范围蔓延：

- ❌ **不解析你的业务报文** —— 字节怎么变血压值，是你的领域知识，库不该知道；
- ❌ **不做 UI** —— 不提供任何 View / Dialog / Compose 组件；
- ❌ **不管权限** —— 权限申请交给调用方（各 App 的权限时机不同）；
- ❌ **不做 DFU / OTA** —— 固件升级是独立领域，后续单开；
- ❌ **不做经典蓝牙（SPP）** —— 只做 BLE GATT。

> 如果你只是想连个手环读心率，官方示例就够了，不用引这个库。

---

## 核心概念

### `DeviceProfile`：描述一台设备

业务方只需要实现这一个接口，把“这台设备长什么样”告诉库。

```kotlin
interface DeviceProfile {

    // ===== 通信通道 =====
    // val：只读属性，实现类用 override val xxx = UUID.fromString(...) 赋值

    /** 通信主服务 UUID */
    val serviceUuid: UUID

    /** 接收通知（Notify）的特征 UUID。设备上报数据走这里 */
    val notifyUuid: UUID

    /** 写入命令的特征 UUID。注意：可以和 notify 不在同一个 Service 里 */
    val writeUuid: UUID

    // ===== 扫描规则 =====

    /**
     * 判断这个广播是不是我要找的设备。
     * 库每扫到一个设备就会调一次，返回 true 才发起连接。
     *
     * 举例：device.name?.startsWith("BP") == true
     * 注意 Kotlin 的 ?. 是"安全调用"：name 为 null 时整个表达式返回 null，不会空指针
     */
    fun isTarget(device: BluetoothDevice, rssi: Int): Boolean

    // ===== 保活 =====

    /**
     * 心跳包（防息屏断连）。不需要心跳就返回 null。
     * 返回 null 时库不会启动心跳循环。
     */
    val handshake: ByteArray?

    /** 心跳间隔（毫秒），默认 2000 */
    val handshakeIntervalMs: Long

    // ===== 固件兼容开关 =====

    /**
     * 是否请求 MTU 协商。
     * 默认 false —— 因为部分固件（如 101Lite 新固件）收到 MTU 请求后会
     * 停止回复握手包，导致链路超时断开。
     * 只有确认你的设备支持时才打开。
     */
    val autoRequestMtu: Boolean

    /** MTU 协商目标值，只在 autoRequestMtu = true 时生效 */
    val targetMtu: Int
}
```

### `ConnectionState`：链路状态

```kotlin
/**
 * sealed interface = "封闭的"接口：所有实现都必须写在同一个文件里。
 * 好处是 when 分支可以穷举，编译器会提醒你漏了哪个状态。
 * 类似于加强版的 enum —— enum 的每个值参数必须一致，
 * sealed 允许 Ready 带 mtu、Idle 不带任何东西。
 */
sealed interface ConnectionState {

    /** data object = 没有参数的单例，等价于 Java 里没有字段的 enum 值 */
    data object Idle : ConnectionState          // 还没开始
    data object Scanning : ConnectionState      // 正在扫描
    data object Connecting : ConnectionState    // 已发起连接，等待连接建立
    data object Discovering : ConnectionState   // 连接建立，正在发现服务

    /** data class = 带参数的不可变数据对象，自动生成 equals/hashCode/toString */
    data class Ready(val mtu: Int) : ConnectionState   // 可以发数据了，mtu 是当前可用载荷

    data class Retrying(val attempt: Int, val delayMs: Long) : ConnectionState  // 等待重连

    data class Failed(val reason: String) : ConnectionState  // 失败且不再自动重试
}
```

### `BleKit`：唯一入口

```kotlin
interface BleKit {

    /**
     * 发起连接。内部会自己完成：扫描 → 找到目标 → 连接 → 发现服务。
     * 断线后自动按指数退避重连，直到你调用 disconnect() 或 close()。
     */
    fun connect(profile: DeviceProfile, address: String)

    /** 主动断开，并停止自动重连 */
    fun disconnect()

    /** 彻底释放（Activity/ViewModel 销毁时调用）。调用后这个实例不可再用 */
    fun close()

    // ===== 两条数据流 =====

    /**
     * 链路状态流。
     * StateFlow 的特点：永远有一个"当前值"，新订阅者会立刻收到最近一次状态。
     * 对应你熟悉的 LiveData，但不需要切主线程。
     */
    val state: StateFlow<ConnectionState>

    /**
     * 设备上报的数据流。
     * SharedFlow 的特点：只推送新数据，不保存历史，新订阅者收不到旧包。
     * 对应你熟悉的 EventBus / 回调，用于业务报文。
     */
    val packets: SharedFlow<ByteArray>

    // ===== 发送 =====

    /**
     * 写入数据。suspend = 挂起函数，只能在协程里调用。
     * 内部会按当前 MTU 自动分包、按间隔节流，写完才返回。
     * 不会卡住主线程。
     */
    suspend fun write(data: ByteArray)

    /**
     * 写入并等待设备响应。
     * 用于"发一条命令、必须等到回包才能发下一条"的协议。
     * 超时抛 BleTimeoutException。
     */
    suspend fun writeAndAwait(data: ByteArray): ByteArray
}
```

---

## 使用示例（带注解）

```kotlin
// AndroidViewModel 会由系统注入 Application；普通 ViewModel 拿不到 Context
class MainViewModel(app: Application) : AndroidViewModel(app) {

    // ViewModel 里创建，跟随 ViewModel 一起销毁，不用你手动 close
    // 注意：这里用 Application 而不是 Activity，避免持有页面引用
    private val ble = BleKit.create(app)

    init {
        // viewModelScope = ViewModel 自带的协程作用域，onCleared 时自动取消
        viewModelScope.launch {
            ble.state.collect { state ->        // collect 后面的大括号 = 每次状态变化都执行
                when (state) {                  // when ≈ Java 的 switch，但支持类型判断
                    is ConnectionState.Ready -> _uiState.value = UiState.Connected(state.mtu)
                    is ConnectionState.Retrying -> _uiState.value = UiState.Retrying(state.attempt)
                    is ConnectionState.Failed -> _uiState.value = UiState.Error(state.reason)
                    else -> _uiState.value = UiState.Connecting
                    // is = "是不是这个类型"，Kotlin 会自动把 state 智能转换成对应类型
                }
            }
        }

        viewModelScope.launch {
            ble.packets.collect { bytes ->
                _bp.value = parseBloodPressure(bytes)   // 解析是你的活，库不管
            }
        }
    }

    fun start(address: String) = ble.connect(BpProfile, address)

    fun sendCommand(cmd: ByteArray) {
        viewModelScope.launch {          // 因为 write 是 suspend，必须放在协程里
            ble.write(cmd)               // 看起来是同步的，实际不会阻塞线程
        }
    }

    override fun onCleared() {
        ble.close()
        super.onCleared()
    }
}
```

---

## 多实例与全局扫描调度

Android 对扫描有硬限制：**30 秒内启动扫描超过 5 次**会返回 `errorCode=2`；Android 14 起还会返回 `SCAN_FAILED_SCANNING_TOO_FREQUENTLY`。如果每个 `BleKit` 实例各自 `startScan` / `stopScan`，多设备场景下必然互相打断。

本库内部有一个进程级的 `ScanArbiter`，按**活跃实例数**自动切换扫描策略：

```
ScanArbiter（internal 单例）
├─ 引用计数：每次 connect() 注册一个 ticket，disconnect() 注销
├─ 1 个 ticket  → 带 ScanFilter 的过滤扫描（省电、回调少）
├─ ≥2 个 ticket → 合并为一次无过滤扫描，进程内按 isTarget() 分发
├─ 物理扫描：0 → 1 时启动；最后一个 ticket 释放时才 stop
└─ 限流保护：切换策略前检查 30s 窗口内的启动次数，超限则等当前窗口结束再切
```

| 方案 | 结果 |
|---|---|
| 每实例各自扫描 | 触发 `errorCode=2`；互相 `stopScan` 打断对方 |
| 串行排队扫描 | 后连的设备要等前面的连上才发现，连接变慢 |
| 一律无过滤合扫 | 单设备场景下白白多收几倍广播，耗电 |
| **按实例数自动切换（本库）** | 单设备省电，多设备不冲突 |

**策略切换的开销**：第二个实例 `connect()` 时会重启一次扫描（过滤 → 合扫）。重启前会先做限流检查，如果 30s 窗口内已经启动 4 次以上，则等当前扫描窗口自然结束后再切，避免撞 `errorCode=2`。

> **对使用者透明**：对外 API 没有任何变化，你不需要知道当前是哪种模式，也不需要为"以后要加设备"预留代码。

## 诊断与日志

蓝牙问题最难排查，因为现场无法调试。`ble-kit` 提供结构化诊断事件，而不是简单的字符串日志。

```kotlin
// 一条诊断记录。结构化而不是字符串，方便上报后做聚合统计
data class BleRecord(
    val timeMs: Long,
    val level: BleLogLevel,     // VERBOSE / DEBUG / INFO / WARN / ERROR
    val event: BleEvent,
    val address: String?,       // 设备地址
)

sealed interface BleEvent {
    data object ScanStarted : BleEvent
    data class ScanFailed(val errorCode: Int) : BleEvent      // 能直接统计 133 / 2 出现频率
    data object Connected : BleEvent
    data class Disconnected(val status: Int) : BleEvent
    data class ServicesDiscovered(val count: Int) : BleEvent
    data class MtuChanged(val mtu: Int) : BleEvent
    data class Tx(val bytes: ByteArray) : BleEvent
    data class Rx(val bytes: ByteArray) : BleEvent
    data class Error(val reason: String) : BleEvent
}

// fun interface = 只有一个方法的接口，Java 里也能用 lambda 传
fun interface BleLogger {
    fun onLog(record: BleRecord)
}
```

配置方式（全局一处，写法和 `OkHttp` 的 interceptor 类似）：

```kotlin
// 全局配置，通常放在 Application 里
BleKit.logger = BleLogger { record ->
    // record.event 已经是结构化对象，可以按类型分流上报
    if (record.level >= BleLogLevel.WARN) {
        CompanyLog.upload(record)
    }
}
BleKit.logLevel = BleLogLevel.WARN   // release 建议 WARN，Debug 用 VERBOSE
```

两个关键机制：

- **环形缓冲**：每个实例常驻内存保留最近 200 条记录。用户反馈"连不上"时，导出的就是真实现场，不需要他复现。
- **原始字节按需附带**：v0.1 默认**不**记录 `Tx` / `Rx` 的字节内容，只在 `Error` / `Disconnected` 等异常事件发生时，把此前最近 N 条的收发字节一并附上。这样平时没有日志压力，出事时又拿得到现场。

```kotlin
// 出事时取现场
val dump = ble.recentRecords(count = 200)
// dump 里异常记录附带的字节内容可直接看：
// Error(reason="GATT 133") → 之前 3 条 Tx/Rx 原始字节
```

如果需要完整抓包排查（比如逆向某台设备的报文时序），可以显式打开：

```kotlin
BleKit.diagnostics = DiagnosticsConfig(
    captureBytes = CaptureBytes.Always,   // 默认 CaptureBytes.OnError
    bufferSize = 500,
)
```

> 日志写入不占用 BLE 回调线程：记录先进入无锁环形队列，再由独立协程消费，避免拖慢 GATT 状态机。

---

## 内置的兼容处理

以下是本库的核心价值，也是与其他 BLE 封装最大的区别：

| 现象 | 根因 | ble-kit 的处理 |
|---|---|---|
| `onScanFailed errorCode=2` | Android 9 及更早的 BT 栈异步注销，同 callback 的 stop+start 竞态 | 不重启未注册的扫描；确实在扫描时先停，延迟 300ms 再启 |
| `status=133` | 连接过快、GATT 未完全释放 | 先 `close()` 旧 GATT 再连；失败走退避重连而非立即重试 |
| 发现服务失败 | 连接建立后立即 discover 部分设备未就绪 | 延迟 300ms，失败重试 2 次，间隔 3s |
| 拿不到通知 | 写特征与通知特征分属两个 Service（如 FFE5/FFE0） | 主服务找不到时遍历全部服务查找 |
| MTU 协商后断链 | 部分固件收到 MTU 请求后停止回包 | `autoRequestMtu` 默认 `false`，需显式开启 |
| 长数据丢包 | 单包超过 `MTU - 3` | 按当前 MTU 自动切分，逐包写入 |
| 连续写无响应 | 设备处理不过来 | 无响应写 20ms 间隔，带响应写等回调，500ms 超时兜底 |
| 息屏后断连 | 长时间无数据交互 | 可配置心跳包，默认 2s |

## 设计原则

1. **不接管你的 App** —— 不提供 BaseActivity / BaseFragment，不要求特定目录结构。
2. **不绑定 UI 框架** —— 纯 Kotlin，View 与 Compose 都能用。
3. **Flow 优先** —— 状态与数据统一用 `Flow`，同时保留回调重载给 Java 项目过渡。
4. **可测试** —— 链路层抽象成接口，协议解析与状态机可用假实现单测。
5. **默认安全** —— 所有已知坑默认已处理；确实需要覆盖时通过 `DeviceProfile` 配置，而不是全局开关。
6. **可观测** —— 每个状态跃迁、每条收发的字节都可追溯到带时间戳的结构化事件，现场问题不靠猜。

## 兼容性

| 项 | 要求 |
|---|---|
| minSdk | 24（Android 7.0） |
| Kotlin | 1.9+ |
| 协程 | `kotlinx-coroutines-core` |
| 权限 | 调用方声明 `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT`（Android 12+） |

## Roadmap

- **v0.1** —— 扫描 / 连接 / 重连 / 分包写，**单实例单设备**；诊断事件 + 环形缓冲（开发库自己也要用它排错）
- **v0.3** —— 多设备并发，`ScanArbiter` 自动切换过滤扫描 / 合扫
- **v0.5** —— 可插拔的重连策略、日志采样与脱敏
- **v1.0** —— API 稳定，发布 Maven Central

## 状态

**草案阶段。** API 尚未冻结，欢迎在 Issue 里描述你遇到的设备型号与固件问题。

## License

Apache-2.0

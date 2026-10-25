# Tieba Native 技术架构与后续优化

## 上游基线

来源：HuanCheng65/TiebaLite 的 4.0-dev 分支。未修改上游的 LICENSE，
保留 UPSTREAM_README.md 中的非商业说明。
这里是基于现有 Kotlin Android 应用逐步改造，而不是另起一套 WebView 壳。

## 当前核心技术

| 层 | 实现 |
| --- | --- |
| 原生 UI | Jetpack Compose，少量传统 Android View |
| 页面与状态 | ViewModel + Intent / PartialChange / UiState 单向数据流 |
| 导航 | Compose Destinations / Navigation Compose |
| 依赖注入 | Hilt |
| 网络 | Retrofit / OkHttp |
| 接口数据 | Wire Protobuf、JSON |
| 本地存储 | LitePal、DataStore |
| 异步 | Coroutines / Flow |

## 新的底部导航

- FloatingBottomNavigation.kt：悬浮胶囊、动画描边、线转面选中态、滑动选择
- NavigationBehavior.kt：不依赖 Android 环境的拖动索引与导航显隐计算
- MainPage.kt：通过 NestedScrollConnection 只观察子页面滚动，不消费滚动距离
- MyScaffold.bottomBar：动态预留空间，胶囊不会遮挡正文或底部按钮
- NavigationBehaviorTest.kt：覆盖拖动跨多个项、越界、渐隐、渐显与非法数值
- 宽屏和折叠屏继续使用上游原有的 NavigationRail / NavigationDrawer

## 数据和可靠性

- FrsPageRepository 改为有限容量的按参数键控缓存，降低请求间数据覆盖风险
- ForumViewModel 成功加载时及时结束 Loading 状态
- PbPageRepository 作者匹配使用哈希索引，缺失数据转为已有 API 异常
- BaseViewModel 移除可能包含私人数据的 Intent 和事件日志
- API 26 起步，移除多余的 legacy multidex release 配置

## 尚需逐步解决的问题

- Google Play 在 2026 年发布所需的 targetSdk 继续升级（目前 35）
- 上游 Microsoft App Center 已停用，现已移除 SDK 与上游统计/更新服务；如需软件内更新和崩溃诊断，须配置本项目自主维护的替代服务
- 清理剩余旧式 Java View / IntentService、弃用 API 和全局状态
- DataStore 的 runBlocking 调用、LitePal 数据迁移和页面性能基准
- 认证协议安全性与模拟器/真机 UI、登录、签到、发帖的集成回归测试

保留上游现有 API、Protobuf 和登录协议实现，以免在第一阶段破坏核心功能。

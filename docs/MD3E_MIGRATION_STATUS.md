# Tieba Native · Material 3 Expressive 迁移盘点

本文件用于区分「源码使用了 M3 组件」与「设备上视觉与交互完成验收」。

## 设计体系

- 采用共享 ColorScheme（主题种子色 + Android 12 及以后动态色），统一深浅主题、容器层级和文本色。
- 采用共享 Shape / Typography token，保留已有底部悬浮胶囊的果冻动画与滚动自动隐藏。
- 通用按钮、开关、标签、工具栏、脚手架、偏好设置容器、帖子卡片、弹窗输入框使用 Jetpack Compose Material 3。
- 各主业务页面文本、图标、图标按钮使用 Material 3。
- 平板/折叠屏的侧边导航栏和抽屉使用 Material 3。
- 贴吧 Protobuf、网络层及上游 GPL-3.0 授权文件不变。

## 页面覆盖范围

页面基础迁移涵盖以下 42 个主要 Page.kt 文件：

- 首页、动态（关注/个性推荐/热门）、消息和个人中心
- 贴吧主页、帖子列表、帖子详情、楼中楼、发帖及回复
- 搜索（综合/贴吧/帖子/用户）、历史、收藏、热榜
- 账户、外观、阅读习惯、屏蔽、签到、更多设置及关于
- 登录 WebView 容器、用户主页和用户内容

上述覆盖指共享样式与页面基础组件，并**不**代表每个页面的全部旧控件都已替换。
登录需要保留百度认证 WebView，原生业务页也可能内嵌视频播放器等 AndroidView。

## 仍需验证或改造

- 旧 Android View/XML 页面还需逐项迁移，特别是旧签到 Activity 和字体大小设置。
- 页面里部分旧 M2 DropdownMenuItem、旧式弹层、选项卡和内容输入组件尚未全部换成 M3。
- 仍使用兼容库（Kotlin 1.9.22、Compose BOM 2024.10、Material3 1.2.1）；
  当前提供 **Material 3 + Expressive 风格设计 token**，尚不是最新 1.5 beta 的所有原生 Expressive API。
- 需在 Android 8 / 12 / 16 上测试浅色、深色、动态色、字体缩放、折叠屏、
  预测性返回和导航按压效果；CI 编译不等于真机视觉验收。
- 对 UI 进行 screenshot/golden 测试、性能基准（Macrobenchmark）和可访问性检查。

## 验收命令

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

构建状态以 GitHub Actions 为准。保留上游 LICENSE、UPSTREAM_README.md 与非商业说明。

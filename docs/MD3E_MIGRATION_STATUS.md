# Tieba Native · Material 3 / Expressive 风格迁移状态

区分源码改造、自动化回归与真机验收，不用 CI 绿色代替所有页面完成验收。

## 共享基础

- 共享 ColorScheme、动态色、Shape 与 Typography；保留悬浮导航的果冻动画、拖动选择和滚动显隐。
- 主页面迁移 M3 文本、图标、工具栏和无抽屉 Scaffold；共享按钮、开关、搜索框、帖子卡片、偏好设置容器及平板侧边导航使用 M3。
- 旧字体大小 Activity 改为原生 Compose M3：可滚动预览、80%—130% 十一个档位、无障碍滑块、增减与复位按钮、显式保存和返回取消。
- 字号草稿不会立即写入。保存等待 DataStore 成功；失败保留草稿并提示。Compose 根部按生命周期收集字号，无需重启应用。
- 设置行改为 M3 ListItem。共享点击/长按菜单改用 M3 DropdownMenu；圆角作用于弹出表面而非仅锚点。
- MaterialInterop 向 M2 与 M3 同时传递有效颜色、透明度和文字样式，修复混合迁移阶段的按钮、菜单、设置行与提示文字。
- 上游 LICENSE、UPSTREAM_README.md、协议定义与业务网络请求不变。

## 自动化验证

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

新增字号步进、无效值保护、预览不落盘、保存和取消、复位、滑块无障碍进度、最小档位、浅色布局、窄屏深色大字体布局及 M2/M3 样式互通测试。

Robolectric 使用独立的普通 Application，不连接用户账户。`Tieba-Native-UI-Regression` 构建产物包含 JUnit/HTML 报告和字号页复核截图。它们用于检查当前渲染结果，**不是黄金图像比对，也不是真机截图**。实际通过/失败以对应提交的 Actions 与报告为准。

## 覆盖与剩余工作

基础迁移涉及约 42 个 Page.kt 文件（首页、动态、消息、个人中心、贴吧、帖子、楼中楼、搜索、历史、收藏、设置、登录容器与用户主页），不代表每页所有旧控件均已替换。

仍需推进：
- 旧签到及半透明主题等 View/XML 流程，旧 M2 菜单项、部分弹层、选项卡和编辑控件。
- 最新 Material 3 Expressive 专用 API 与 Kotlin/Compose 工具链的配套升级。当前仍为 Kotlin 1.9.22、Compose BOM 2024.10、Material3 1.2.1；风格 token 桥接不等于完整官方 Expressive API 迁移。
- Android 8/12/16、动态色、平板/折叠屏、预测性返回、登录和发帖的真机回归。
- 截图基线、辅助功能服务验证和 Macrobenchmark 性能数据。

保留百度认证 WebView 与视频 AndroidView。独立模型审阅尚未接入；源码自查与自动化测试不冒称第二模型复审。

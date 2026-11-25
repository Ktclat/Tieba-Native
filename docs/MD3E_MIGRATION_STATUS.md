# Tieba Native 的 Material 3 Expressive 迁移进度

## 共享基础

- Compose 根主题采用 Google 的 `MaterialExpressiveTheme`，Activity 和系统对话框使用 Material Components 的 Expressive 主题。自选品牌色、动态色、字号与深色模式沿用用户设置。
- 使用 Kotlin 2.0.21、Compose BOM 2025.06.01、Compose UI 1.8.3 与 Material Components 1.14.0。Compose Material 3 显式选用 1.4.0-alpha18，该版本公开了连组按钮和 Expressive 动效 API。
- 主导航点按直接选页，保留已访问标签的列表位置。底部导航支持左右拖动、重选刷新和随内容滚动显隐。
- 帖子页的筛选使用 `TonalToggleButton` 和 `ButtonGroupDefaults`。楼中楼共用浅色容器，作者和接收人同行，时间与属地另列。有效用户 ID、纯文本定向回复和普通提及分别保留各自的交互。
- 设置中的整行开关、复选、多选、滑块和下拉菜单使用 Material 3。
- 主页面迁移 M3 文本、图标、工具栏和无抽屉 Scaffold；共享按钮、开关、搜索框、帖子卡片、偏好设置容器及平板侧边导航使用 M3。
- 旧字体大小 Activity 改为原生 Compose M3：可滚动预览、80%—130% 十一个档位、无障碍滑块、增减与复位按钮、显式保存和返回取消。
- 点保存后才将字号写入 DataStore。写入失败时，错误提示显示在保存按钮上方，并通过无障碍语义通知读屏服务。用户可以用当前草稿重试。保存成功后，已打开的 Compose 页面会自动应用新字号。
- 设置行改为 M3 ListItem。共享点击/长按菜单改用 M3 DropdownMenu；圆角作用于弹出表面而非仅锚点。
- 全局搜索和吧内搜索共用搜索框与 M3 历史列表。可以重新搜索、逐条删除或确认清空。吧内历史只显示和操作当前贴吧的词条。
- 全局搜索联想支持直接搜索或填入后继续编辑。结果页使用 M3 标签切换贴吧、帖子和用户，再点已选的帖子标签可切换排序。
- 帖子搜索结果可以打开命中的主帖、楼层或楼中楼。分页失败时可在结果末尾重试，之前加载的内容仍可阅读。
- MaterialInterop 向 M2 与 M3 同时传递有效颜色、透明度和文字样式，修复混合迁移阶段的按钮、菜单、设置行与提示文字。
- 上游 LICENSE、UPSTREAM_README.md、协议定义与业务网络请求不变。

## 自动化验证

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

字号测试覆盖步进、草稿、保存和取消、复位、滑块无障碍进度、浅色布局、180dp 和 320dp 的深色大字体布局及 M2/M3 样式互通。搜索测试覆盖 LitePal 读写与跨吧隔离、历史管理、联想编辑和取消、结果定位以及分页重试。

Robolectric 使用独立的普通 Application。`Tieba-Native-UI-Regression` 构建产物包含 JUnit/HTML 报告，以及字号、搜索、导航与讨论串的渲染截图，可供人工复核。讨论串测试覆盖主题图、回帖大图、筛选、楼中楼预览与详情，并在 360dp 浅色与 320dp 深色大字号下检查排版和点击。实际结果见对应提交的 Actions 与报告。

## 覆盖与剩余工作

主导航、帖子、楼中楼和设置共用 Expressive 主题。部分历史页面仍通过 Material 2 兼容层读取这套配色与字阶，登录使用百度认证 WebView，视频播放使用 AndroidView。

以下场景仍需验证或继续迁移。

- 旧签到和部分原生编辑弹窗里的自绘控件。
- Android 8、12 和 16 上的动态色、平板与折叠屏、预测性返回、登录和发帖真机流程。
- 使用辅助功能服务操作和在实体设备上采集 Macrobenchmark 帧耗时。

# Tieba Native 的 Material 3 Expressive 界面

## 设计与实现

- 应用使用 Google 的 `MaterialExpressiveTheme`，由官方组件决定字阶、形状、触控反馈和动效。浅色与深色主题使用对应的 Material 色彩角色，Android 12 起可选系统动态色。自选颜色由 Material Color Utilities 生成各级底色及其配对的文字颜色。
- 手机使用 `ShortNavigationBar`，宽屏使用导航轨或固定抽屉。点按立即切页，再次选中可以刷新。离开一页再返回时保留列表位置。向下浏览时导航栏整体移出屏幕。
- 帖子的「全部 / 只看楼主」使用 32dp 高的连接式筛选按钮，保留至少 48dp 的点击区域。楼中楼把作者、接收人和正文放在同一组，右上角显示该条回复的赞。完整楼中楼与回复编辑器可以独立打开并返回，编辑器支持多行文字、表情和撤销。
- 设置使用 `ListItem`、开关、复选框、滑块和对话框。字体大小提供 80% 到 130% 的阅读预览，点保存后应用。Material 3 的搜索框、历史记录和结果标签共用于全局搜索与吧内搜索。
- 代码采用 Kotlin 2.0.21、Compose BOM 2025.06.01、Compose Material 3 1.4.0-alpha18 和原生 Material Components 1.14.0。图标资源、百度登录 WebView、视频播放引擎和图片解码器独立于控件主题。

[Google 的 Compose Material 3 指南](https://developer.android.com/develop/ui/compose/designsystems/material3) · [Material 3 导航栏规范](https://m3.material.io/components/navigation-bar/guidelines)

## 验证

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Android CI 上传 APK、JUnit 报告及生产组件的离线渲染截图。`Tieba-Native-UI-Regression` 包含主导航、搜索、字号设置、讨论串等场景。讨论串覆盖 360dp 浅色与 320dp 深色大字号、定向回复、点赞、复制、筛选和用户跳转。

在 Actions 的 Android CI 页面可以手动运行设备检查。模拟器使用 Android 15、Pixel 9 尺寸（1080 × 2424）、默认字号和 1 倍系统动画。`Tieba-Native-Android-Screens` 包含主要页面、固定示例讨论串，以及从左右边缘拖动、取消和完成预测性返回的分步截图。

`Tieba-Native-Frame-Timings` 保存切换底部导航和滚动 180 条本地浏览记录时的逐帧数据。拉取请求会自动查找基准提交的安装包。手动运行时可以在 `baseline_commit` 填写保有成功 APK 产物的完整提交 SHA。找到基准包后，同一台模拟器依次运行旧版、新版、新版、旧版，结果汇总在 `measurements.json`。

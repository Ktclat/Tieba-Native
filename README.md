# Tieba Native

基于 [HuanCheng65/TiebaLite](https://github.com/HuanCheng65/TiebaLite)
的 Kotlin + Jetpack Compose 原生 Android 贴吧客户端改造版。

> 非官方贴吧客户端。基于上游 4.0-dev 分支改造。
> 继承原项目说明：**本软件及源码仅供学习交流使用，严禁用于商业用途。**

## 已实现的改造

- 手机端悬浮胶囊底部导航，保留平板原有导航栏/抽屉自适应布局
- 选中项描边可左右拖动切换页面；拖动时描边过渡为填充背景（线转面）
- 列表下滑底部导航按滚动距离渐隐，向上滚动恢复
- 底部导航占据 Scaffold 的布局空间，不覆盖列表、按钮或系统手势区域
- 最低 API 26，编译/目标 API 35；AGP 8.6.1、Gradle 8.7、JDK 17
- 修复贴吧加载成功仍显示加载中、缓存串页和缺失作者信息处理
- 删除 ViewModel 中可能含私人数据的日志
- 新增导航计算单元测试与 CI 构建验证

## 构建

Android Studio 打开根目录，配置 JDK 17 和 Android SDK 35。
运行 ./gradlew :app:assembleDebug :app:testDebugUnitTest。

当前是渐进式重构，上游部分旧库仍保留，Android 2026 的发布目标 SDK
与第三方依赖还需要进一步升级。请以 GitHub Actions 的构建状态为准。

## 上游协议、署名及修改说明

- 原项目：[HuanCheng65/TiebaLite](https://github.com/HuanCheng65/TiebaLite)
- 上游基线：4.0-dev，保留完整代码、资源、协议定义和原许可证
- 开源协议：GNU GPL-3.0，见 [LICENSE](LICENSE)，修改部分继续以 GPL-3.0 开源
- [上游原始 README 完整副本](UPSTREAM_README.md)，保留上游非商业用途声明
- 本仓库与百度官方无关联，不得冒充官方产品

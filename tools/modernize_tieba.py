#!/usr/bin/env python3
"""Apply deterministic Tieba Native changes to an imported TiebaLite checkout."""
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

def patch(path: str, before: str, after: str, expected: int = 1) -> None:
    target = ROOT / path
    src = target.read_text(encoding="utf-8")
    count = src.count(before)
    if count != expected:
        raise RuntimeError(f"{path}: expected {expected} matches, found {count}: {before[:90]!r}")
    target.write_text(src.replace(before, after), encoding="utf-8")

MAIN = "app/src/main/java/com/huanchengfly/tieba/post/ui/page/main/MainPage.kt"
BASE_VM = "app/src/main/java/com/huanchengfly/tieba/post/arch/BaseViewModel.kt"
FORUM_VM = "app/src/main/java/com/huanchengfly/tieba/post/ui/page/forum/ForumViewModel.kt"
FRS = "app/src/main/java/com/huanchengfly/tieba/post/repository/FrsPageRepository.kt"
PB = "app/src/main/java/com/huanchengfly/tieba/post/repository/PbPageRepository.kt"

# The floating bar reserves layout space and observes nested scroll without consuming it.
patch(MAIN, "import androidx.compose.foundation.layout.Column\n",
      "import androidx.compose.foundation.layout.Box\nimport androidx.compose.foundation.layout.Column\n"
      "import androidx.compose.foundation.layout.height\n"
      "import androidx.compose.foundation.layout.navigationBarsPadding\n"
      "import androidx.compose.foundation.layout.padding\n"
      "import androidx.compose.foundation.layout.fillMaxWidth\n")
patch(MAIN, "import androidx.compose.runtime.getValue\n",
      "import androidx.compose.runtime.getValue\nimport androidx.compose.runtime.mutableFloatStateOf\n")
patch(MAIN, "import androidx.compose.ui.Alignment\n",
      "import androidx.compose.ui.Alignment\n"
      "import androidx.compose.ui.draw.clipToBounds\n"
      "import androidx.compose.ui.geometry.Offset\n"
      "import androidx.compose.ui.graphics.graphicsLayer\n"
      "import androidx.compose.ui.input.nestedscroll.NestedScrollConnection\n"
      "import androidx.compose.ui.input.nestedscroll.NestedScrollSource\n"
      "import androidx.compose.ui.input.nestedscroll.nestedScroll\n"
      "import androidx.compose.ui.platform.LocalDensity\n"
      "import androidx.compose.ui.unit.dp\n")

marker = '    val onReselected: (Int) -> Unit = {\n'
observer = '''    // Observes child scroll, consuming none of its distance.
    // The Scaffold bottom bar owns layout height, never overlays page content.
    val navHideFraction = remember { mutableFloatStateOf(0f) }
    val navScrollRangePx = with(LocalDensity.current) { 132.dp.toPx() }
    val navScrollObserver = remember(navigationType, navScrollRangePx) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (navigationType == MainNavigationType.BOTTOM_NAVIGATION &&
                    (source == NestedScrollSource.Drag || source == NestedScrollSource.Fling)
                ) {
                    navHideFraction.floatValue = updateNavigationHideFraction(
                        current = navHideFraction.floatValue,
                        consumedDyPx = consumed.y,
                        distancePx = navScrollRangePx,
                    )
                }
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(pagerState.currentPage) {
        navHideFraction.floatValue = 0f
    }

'''
patch(MAIN, marker, observer + marker)
patch(MAIN, '                modifier = Modifier.fillMaxSize(),\n                bottomBar = {\n',
      '                modifier = Modifier.fillMaxSize().nestedScroll(navScrollObserver),\n                bottomBar = {\n')
patch(MAIN, '''                    AnimatedVisibility(visible = navigationType == MainNavigationType.BOTTOM_NAVIGATION) {
                        BottomNavigation(
                            currentPosition = pagerState.currentPage,
                            onChangePosition = {
                                coroutineScope.launch { pagerState.scrollToPage(it) }
                            },
                            onReselected = onReselected,
                            navigationItems = navigationItems,
                            themeColors = themeColors,
                        )
                    }
''', '''                    if (navigationType == MainNavigationType.BOTTOM_NAVIGATION) {
                        val visibleFraction = 1f - navHideFraction.floatValue
                        // Reserve bottom space above the Android gesture inset.
                        // As the capsule collapses, content gets the space back.
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .height(82.dp * visibleFraction)
                                .clipToBounds()
                        ) {
                            FloatingBottomNavigation(
                                currentPosition = pagerState.currentPage,
                                onChangePosition = {
                                    coroutineScope.launch { pagerState.scrollToPage(it) }
                                },
                                onReselected = onReselected,
                                navigationItems = navigationItems,
                                themeColors = themeColors,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(horizontal = 18.dp, vertical = 6.dp)
                                    .graphicsLayer { alpha = visibleFraction },
                            )
                        }
                    }
''')

# Avoid permanently displaying loading UI after a successful forum request.
patch(FORUM_VM, '''            is Success -> oldState.copy(
                isLoading = true,
                isError = false,
''', '''            is Success -> oldState.copy(
                isLoading = false,
                isError = false,
''')

# Replace the global last-response slot with a 16-key, thread-safe LRU.
patch(FRS, '''    var lastHash: String = ""
    var lastResponse: FrsPageResponse? = null
''', '''    private const val MAX_CACHED_PAGES = 16
    private val cacheLock = Any()
    private val cachedPages = object : LinkedHashMap<String, FrsPageResponse>(
        MAX_CACHED_PAGES, 0.75f, true
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, FrsPageResponse>?
        ): Boolean = size > MAX_CACHED_PAGES
    }
''')
patch(FRS, '''        if (!forceNew && lastResponse != null && lastHash == hash) {
            return flowOf(lastResponse!!)
        }
        lastHash = hash
''', '''        if (!forceNew) {
            val cached = synchronized(cacheLock) { cachedPages[hash] }
            if (cached != null) return flowOf(cached)
        }
''')
patch(FRS, '''            .onEach { lastResponse = it }
''', '''            .onEach { result ->
                synchronized(cacheLock) { cachedPages[hash] = result }
            }
''')

# Use a single lookup map and a known API exception for missing authors.
patch(PB, '''                val userList = response.data_.user_list
                val postList = response.data_.post_list.map {
''', '''                val userList = response.data_.user_list
                val userById = userList.associateBy { it.id }
                val postList = response.data_.post_list.map {
''')
patch(PB, 'userList.first { user -> user.id == it.author_id }',
      'userById[it.author_id] ?: throw TiebaUnknownException', expected=2)
patch(PB, 'userList.first { user -> user.id == subPost.author_id }',
      'userById[subPost.author_id] ?: throw TiebaUnknownException', expected=2)

# Intent / response / event details can contain private account information.
patch(BASE_VM, 'import android.util.Log\n', '')
patch(BASE_VM, '            Log.i("ViewModel", "partialChange $it")\n', '')
patch(BASE_VM, '                Log.i("ViewModel", "event $event")\n', '')
patch(BASE_VM, '        Log.i("ViewModel", "send $intent")\n', '')

# Raise minimum OS to Android 8.0 and make a conservative, compatible build upgrade.
GRADLE = "app/build.gradle.kts"
patch(GRADLE, '    buildToolsVersion = "34.0.0"\n    compileSdk = 34\n',
      '    compileSdk = 35\n')
patch(GRADLE, '        minSdk = 21\n', '        minSdk = 26\n')
patch(GRADLE, '        targetSdk = 34\n', '        targetSdk = 35\n')
patch(GRADLE, '        targetCompatibility = JavaVersion.VERSION_11\n',
      '        targetCompatibility = JavaVersion.VERSION_17\n')
patch(GRADLE, '        sourceCompatibility = JavaVersion.VERSION_11\n',
      '        sourceCompatibility = JavaVersion.VERSION_17\n')
patch(GRADLE, '        jvmTarget = "11"\n', '        jvmTarget = "17"\n')
patch("gradle/sweet-dependency/sweet-dependency-config.yaml",
      '  com.android.application:\n    version: 8.2.2\n',
      '  com.android.application:\n    version: 8.6.1\n')
patch("gradle/wrapper/gradle-wrapper.properties",
      "gradle-8.2-all.zip", "gradle-8.7-all.zip")

# Keep upstream license and original README in full.
upstream_readme = (ROOT / "README.md").read_text(encoding="utf-8")
(ROOT / "UPSTREAM_README.md").write_text(upstream_readme, encoding="utf-8")
(ROOT / "README.md").write_text("""# Tieba Native

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
""", encoding="utf-8")
print("Applied Tieba Native modernization patches.")

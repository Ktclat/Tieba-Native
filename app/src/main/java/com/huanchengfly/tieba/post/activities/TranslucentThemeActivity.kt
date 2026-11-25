package com.huanchengfly.tieba.post.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.text.HtmlCompat
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import com.github.panpf.sketch.request.DisplayRequest
import com.github.panpf.sketch.request.DisplayResult
import com.github.panpf.sketch.request.LoadRequest
import com.github.panpf.sketch.request.LoadResult
import com.github.panpf.sketch.request.execute
import com.github.panpf.sketch.resize.Scale
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.slider.Slider
import com.huanchengfly.tieba.post.components.dialogs.showThemeColorPicker
import com.gyf.immersionbar.ImmersionBar
import com.huanchengfly.tieba.post.*
import com.huanchengfly.tieba.post.App.Companion.translucentBackground
import com.huanchengfly.tieba.post.adapters.TranslucentThemeColorAdapter
import com.huanchengfly.tieba.post.adapters.WallpaperAdapter
import com.huanchengfly.tieba.post.api.LiteApi
import com.huanchengfly.tieba.post.api.retrofit.doIfSuccess
import com.huanchengfly.tieba.post.components.MyLinearLayoutManager
import com.huanchengfly.tieba.post.components.dividers.HorizontalSpacesDecoration
import com.huanchengfly.tieba.post.components.transformations.SketchBlurTransformation
import com.huanchengfly.tieba.post.interfaces.OnItemClickListener
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import com.huanchengfly.tieba.post.utils.*
import com.huanchengfly.tieba.post.utils.ThemeUtil.TRANSLUCENT_THEME_DARK
import com.huanchengfly.tieba.post.utils.ThemeUtil.TRANSLUCENT_THEME_LIGHT
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlin.math.roundToInt
import java.io.File

class TranslucentThemeActivity : BaseActivity(), View.OnClickListener {
    private var previewJob: Job? = null
    private val selectedColor: Int get() = appPreferences.translucentPrimaryColor
        ?.let { runCatching { Color.parseColor(it) }.getOrNull() }
        ?: ThemeUtils.getColorById(this, R.color.default_color_primary)
    private var mUri: Uri? = null
    private var alpha = 0
    private var blur = 0
    private var mPalette: Palette? = null

    @BindView(R.id.select_color)
    lateinit var mSelectColor: View

    @BindView(R.id.recommend_wallpapers)
    lateinit var recommendWallpapers: View

    @BindView(R.id.wallpapers_rv)
    lateinit var recommendWallpapersRv: RecyclerView

    @BindView(R.id.progress)
    lateinit var mProgress: View

    @BindView(R.id.button_back)
    lateinit var backBtn: View

    @BindView(R.id.bottom_sheet)
    lateinit var bottomSheet: View

    @BindView(R.id.button_finish)
    lateinit var finishBtn: View

    @BindView(R.id.mask)
    lateinit var maskView: View

    @BindView(R.id.experimental_tip)
    lateinit var experimentalTipView: View

    @BindView(R.id.color_theme)
    lateinit var colorTheme: MaterialButtonToggleGroup

    private val selectImageLauncher = registerPickMediasLauncher { (_, uris) ->
        if (uris.isNotEmpty()) {
            val sourceUri = uris[0]
            launchUCrop(sourceUri)
        }
    }

    var wallpapers: List<String>? = null
        set(value) {
            field = value
            refreshWallpapers()
        }
    private val wallpaperAdapter: WallpaperAdapter by lazy { WallpaperAdapter(this) }

    private val mTranslucentThemeColorAdapter: TranslucentThemeColorAdapter by lazy {
        TranslucentThemeColorAdapter(
            this
        )
    }

    private fun launchUCrop(sourceUri: Uri) {
        mProgress.visibility = View.VISIBLE
        launch {
            val result = LoadRequest(this@TranslucentThemeActivity, sourceUri.toString()).execute()
            if (result is LoadResult.Success) {
                mProgress.visibility = View.GONE
                val file =
                    ImageUtil.bitmapToFile(result.bitmap, File(cacheDir, "origin_background.jpg"))
                val sourceFileUri = Uri.fromFile(file)
                val destUri = Uri.fromFile(File(filesDir, "cropped_background.jpg"))
                val height = App.ScreenInfo.EXACT_SCREEN_HEIGHT.toFloat()
                val width = App.ScreenInfo.EXACT_SCREEN_WIDTH.toFloat()
                UCrop.of(sourceFileUri, destUri)
                    .withAspectRatio(width / height, 1f)
                    .withOptions(UCrop.Options().apply {
                        setShowCropFrame(true)
                        setShowCropGrid(true)
                        setStatusBarColor(
                            ColorUtils.getDarkerColor(
                                ThemeUtils.getColorByAttr(
                                    this@TranslucentThemeActivity,
                                    R.attr.colorPrimary
                                )
                            )
                        )
                        setToolbarColor(
                            ThemeUtils.getColorByAttr(
                                this@TranslucentThemeActivity,
                                R.attr.colorPrimary
                            )
                        )
                        setToolbarWidgetColor(
                            ThemeUtils.getColorByAttr(
                                this@TranslucentThemeActivity,
                                R.attr.colorTextOnPrimary
                            )
                        )
                        setActiveControlsWidgetColor(
                            ThemeUtils.getColorByAttr(
                                this@TranslucentThemeActivity,
                                R.attr.colorAccent
                            )
                        )
                        setLogoColor(
                            ThemeUtils.getColorByAttr(
                                this@TranslucentThemeActivity,
                                R.attr.colorPrimary
                            )
                        )
                        setCompressionFormat(Bitmap.CompressFormat.JPEG)
                    })
                    .start(this@TranslucentThemeActivity)
            } else if (result is LoadResult.Error) {
                mProgress.visibility = View.GONE
                toastShort(R.string.text_load_failed)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            mUri = UCrop.getOutput(data!!)
            invalidateFinishBtn()
            refreshBackground()
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            cropError!!.printStackTrace()
        }
    }

    private fun refreshWallpapers() {
        if (wallpapers.isNullOrEmpty()) {
            recommendWallpapers.visibility = View.GONE
        } else {
            recommendWallpapers.visibility = View.VISIBLE
            wallpaperAdapter.setData(wallpapers)
        }
    }

    private fun refreshBackground() {
        previewJob?.cancel()
        mProgress.visibility = View.VISIBLE
        if (mUri == null) {
            findViewById<View>(R.id.background).setBackgroundColor(
                com.google.android.material.color.MaterialColors.getColor(findViewById(R.id.background), com.google.android.material.R.attr.colorSurface)
            )
            mProgress.visibility = View.GONE
            return
        }
        previewJob = launch {
            val result = DisplayRequest(this@TranslucentThemeActivity, mUri.toString()) {
                resizeScale(Scale.CENTER_CROP)
                if (blur > 0) {
                    transformations(SketchBlurTransformation(blur))
                }
            }.execute()
            if (result is DisplayResult.Success) {
                result.drawable.alpha = alpha
                findViewById<View>(R.id.background).background = result.drawable
                mPalette = Palette.from(ImageUtil.drawableToBitmap(result.drawable)).generate()
                mTranslucentThemeColorAdapter.setPalette(mPalette)
                mSelectColor.visibility = View.VISIBLE
            }
            mProgress.visibility = View.GONE
        }
    }

    override fun refreshStatusBarColor() {
        ImmersionBar.with(this)
            .transparentBar()
            .init()
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_translucent_theme
    }

    @SuppressLint("ApplySharedPref", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        experimentalTipView.setOnClickListener {
            showDialog {
                setTitle(R.string.title_translucent_theme_experimental_feature)
                setMessage(
                    HtmlCompat.fromHtml(
                        getString(R.string.tip_translucent_theme),
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                )
                setNegativeButton(R.string.btn_close, null)
            }
        }
        listOf(
            findViewById(R.id.custom_color),
            findViewById(R.id.select_pic),
            backBtn,
            finishBtn
        ).forEach {
            it.setOnClickListener(this@TranslucentThemeActivity)
        }
        wallpapers =
            CacheUtil.getCache(this, "recommend_wallpapers", List::class.java) as List<String>?
        colorTheme.check(
            if (appPreferences.translucentBackgroundTheme == TRANSLUCENT_THEME_DARK) R.id.dark_color
            else R.id.light_color
        )
        colorTheme.addOnButtonCheckedListener { _, id, checked ->
            if (checked) {
                appPreferences.translucentBackgroundTheme =
                    if (id == R.id.dark_color) TRANSLUCENT_THEME_DARK else TRANSLUCENT_THEME_LIGHT
            }
        }
        wallpaperAdapter.setOnItemClickListener { _, item, _ ->
            launchUCrop(Uri.parse(item))
        }
        recommendWallpapersRv.addItemDecoration(
            HorizontalSpacesDecoration(
                0,
                0,
                16.dpToPx(),
                16.dpToPx(),
                false
            )
        )
        recommendWallpapersRv.adapter = wallpaperAdapter
        recommendWallpapersRv.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        mTranslucentThemeColorAdapter.setSelectedColor(selectedColor)
        mTranslucentThemeColorAdapter.onItemClickListener =
            OnItemClickListener { _: View?, themeColor: Int, _: Int, _: Int ->
                appPreferences.translucentPrimaryColor = toString(themeColor)
                maskView.post { ThemeUtils.refreshUI(this, this) }
            }
        (findViewById<RecyclerView>(R.id.select_color_recycler_view)).apply {
            addItemDecoration(HorizontalSpacesDecoration(0, 0, 12.dpToPx(), 12.dpToPx(), false))
            layoutManager = MyLinearLayoutManager(
                this@TranslucentThemeActivity,
                MyLinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = mTranslucentThemeColorAdapter
        }
        alpha = appPreferences.translucentBackgroundAlpha
        blur = appPreferences.translucentBackgroundBlur
        val updatePreview = Runnable { refreshBackground() }
        val preview = findViewById<View>(R.id.background)
        fun bindSlider(id: Int, initial: Int, update: (Int) -> Unit) {
            findViewById<Slider>(id).apply {
                value = initial.toFloat().coerceIn(valueFrom, valueTo)
                setLabelFormatter { it.roundToInt().toString() }
                addOnChangeListener { _, newValue, fromUser ->
                    if (fromUser) {
                        update(newValue.roundToInt())
                        preview.removeCallbacks(updatePreview)
                        preview.postDelayed(updatePreview, 150)
                    }
                }
            }
        }
        bindSlider(R.id.alpha, alpha) { alpha = it }
        bindSlider(R.id.blur, blur) { blur = it }
        mProgress.setOnTouchListener { _: View?, _: MotionEvent? -> true }
        mProgress.visibility = View.GONE
        val file = File(filesDir, "cropped_background.jpg")
        if (file.exists()) {
            mUri = Uri.fromFile(file)
        }
        invalidateFinishBtn()
        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {}

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                maskView.alpha = 0.32f * slideOffset.coerceIn(0f, 1f)
                maskView.visibility = if (slideOffset < 0.01f) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
            }

        })
        maskView.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }
        refreshBackground()
        fetchWallpapers()
    }

    private fun fetchWallpapers() {
        launch(IO + job) {
            LiteApi.instance
                .wallpapersAsync()
                .doIfSuccess {
                    CacheUtil.putCache(this@TranslucentThemeActivity, "recommend_wallpapers", it)
                    withContext(Main.immediate) { wallpapers = it }
                }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.select_color -> return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun savePic(callback: SavePicCallback<File>) {
        runCatching {
            val oldFilePath = appPreferences.translucentThemeBackgroundPath
            if (oldFilePath != null) {
                val oldFile = File(oldFilePath)
                oldFile.delete()
            }
        }
        mProgress.visibility = View.VISIBLE
        launch {
            val result = DisplayRequest(this@TranslucentThemeActivity, mUri.toString()) {
                resizeScale(Scale.CENTER_CROP)
                if (blur > 0) {
                    transformations(SketchBlurTransformation(blur))
                }
            }.execute()
            if (result is DisplayResult.Success) {
                result.drawable.alpha = alpha
                val bitmap = ImageUtil.drawableToBitmap(result.drawable)
                val file = ImageUtil.compressImage(
                    bitmap,
                    File(filesDir, "background_${System.currentTimeMillis()}.jpg"),
                    maxSizeKb = 512,
                    initialQuality = 97
                )
                mPalette = Palette.from(bitmap).generate()
                appPreferences.translucentThemeBackgroundPath = file.absolutePath
                ThemeUtils.refreshUI(
                    this@TranslucentThemeActivity,
                    this@TranslucentThemeActivity
                )
                callback.onSuccess(file)
            }
        }
    }

    private fun invalidateFinishBtn() {
        if (mUri != null) {
            finishBtn.visibility = View.VISIBLE
        } else {
            finishBtn.visibility = View.GONE
        }
    }

    override fun finish() {
        ImageCacheUtil.clearImageMemoryCache(this)
        super.finish()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.button_finish -> {
                appPreferences.apply {
                    translucentBackgroundAlpha = alpha
                    translucentBackgroundBlur = blur
                }
                savePic(object : SavePicCallback<File> {
                    override fun onSuccess(t: File) {
                        ThemeUtil.switchTheme(ThemeUtil.THEME_TRANSLUCENT, false)
                        toastShort(R.string.toast_save_pic_success)
                        translucentBackground = null
                        mProgress.visibility = View.GONE
                        finish()
                    }
                })
            }
            R.id.button_back -> {
                finish()
            }
            R.id.select_pic -> askPermission {
                selectImageLauncher.launch(PickMediasRequest(mediaType = PickMediasRequest.ImageOnly))
            }
            R.id.custom_color -> {
                showThemeColorPicker(this, selectedColor) { color ->
                    appPreferences.translucentPrimaryColor = toString(color)
                    mTranslucentThemeColorAdapter.setSelectedColor(color)
                    ThemeUtils.refreshUI(this, this)
                }
            }
        }
    }

    private fun askPermission(granted: () -> Unit) {
        if (shouldUsePhotoPicker()) {
            granted()
            return
        }
        requestPermission {
            unchecked = true
            permissions = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                listOf(
                    PermissionUtils.READ_EXTERNAL_STORAGE,
                    PermissionUtils.WRITE_EXTERNAL_STORAGE
                )
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                listOf(
                    PermissionUtils.READ_EXTERNAL_STORAGE
                )
            } else {
                listOf(PermissionUtils.READ_MEDIA_IMAGES)
            }
            description = getString(R.string.tip_permission_storage)
            onGranted = granted
            onDenied = { toastShort(R.string.toast_no_permission_insert_photo) }
        }
    }

    interface SavePicCallback<T> {
        fun onSuccess(t: T)
    }

    companion object {
        val TAG = TranslucentThemeActivity::class.java.simpleName
        const val REQUEST_CODE_CHOOSE = 2
        fun toString(alpha: Int, red: Int, green: Int, blue: Int): String {
            val hr = Integer.toHexString(red)
            val hg = Integer.toHexString(green)
            val hb = Integer.toHexString(blue)
            val ha = Integer.toHexString(alpha)
            return "#" + fixHexString(ha) + fixHexString(hr) + fixHexString(hg) + fixHexString(hb)
        }

        private fun fixHexString(string: String): String {
            var hexStr = string
            if (hexStr.isEmpty()) {
                hexStr = "00"
            }
            if (hexStr.length == 1) {
                hexStr = "0$hexStr"
            }
            if (hexStr.length > 2) {
                hexStr = hexStr.substring(0, 2)
            }
            return hexStr
        }

        fun toString(@ColorInt color: Int): String {
            return toString(
                Color.alpha(color),
                Color.red(color),
                Color.green(color),
                Color.blue(color)
            )
        }
    }
}
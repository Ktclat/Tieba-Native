package com.huanchengfly.tieba.post.utils

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Bitmap.CompressFormat
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Base64
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.widget.ImageView
import androidx.annotation.IntDef
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.github.panpf.sketch.disposeDisplay
import com.github.panpf.sketch.request.Depth
import com.github.panpf.sketch.request.DisplayRequest
import com.github.panpf.sketch.request.DownloadRequest
import com.github.panpf.sketch.request.DownloadResult
import com.github.panpf.sketch.request.enqueue
import com.github.panpf.sketch.request.execute
import com.github.panpf.sketch.transform.CircleCropTransformation
import com.github.panpf.sketch.transform.RoundedCornersTransformation
import com.huanchengfly.tieba.post.App.Companion.INSTANCE
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.dpToPxFloat
import com.huanchengfly.tieba.post.utils.PermissionUtils.PermissionData
import com.huanchengfly.tieba.post.utils.PermissionUtils.askPermission
import com.huanchengfly.tieba.post.utils.ThemeUtil.isNightMode
import com.zhihu.matisse.MimeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.channels.FileChannel
import kotlin.coroutines.resume

object ImageUtil {
    /**
     * 智能省流
     */
    const val SETTINGS_SMART_ORIGIN = 0

    /**
     * 智能无图
     */
    const val SETTINGS_SMART_LOAD = 1

    /**
     * 始终高质量
     */
    const val SETTINGS_ALL_ORIGIN = 2

    /**
     * 始终无图
     */
    const val SETTINGS_ALL_NO = 3
    const val LOAD_TYPE_SMALL_PIC = 0
    const val LOAD_TYPE_AVATAR = 1
    const val LOAD_TYPE_NO_RADIUS = 2
    const val LOAD_TYPE_ALWAYS_ROUND = 3
    const val TAG = "ImageUtil"

    fun compressImage(
        bitmap: Bitmap,
        quality: Int = 100
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        bitmap.compress(CompressFormat.JPEG, quality, baos)
        return baos.use { it.toByteArray() }
    }

    @JvmOverloads
    fun compressImage(
        bitmap: Bitmap,
        output: File,
        maxSizeKb: Int = 100,
        initialQuality: Int = 100
    ): File {
        val baos = ByteArrayOutputStream()
        var quality = initialQuality
        bitmap.compress(CompressFormat.JPEG, quality, baos) //质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        while (baos.toByteArray().size / 1024 > maxSizeKb && quality > 0) {  //循环判断如果压缩后图片是否大于设置的最大值,大于继续压缩
            baos.reset() //重置baos即清空baos
            quality -= 5 //每次都减少5
            bitmap.compress(CompressFormat.JPEG, quality, baos) //这里压缩options%，把压缩后的数据存放到baos中
        }
        try {
            val fos = FileOutputStream(output)
            try {
                fos.write(baos.toByteArray())
                fos.flush()
                fos.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        return output
    }

    @JvmOverloads
    fun bitmapToFile(
        bitmap: Bitmap,
        output: File,
        format: CompressFormat = CompressFormat.JPEG
    ): File {
        val baos = ByteArrayOutputStream()
        bitmap.compress(format, 100, baos)
        try {
            val fos = FileOutputStream(output)
            try {
                fos.write(baos.toByteArray())
                fos.flush()
                fos.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        return output
    }

    fun drawableToBitmap(drawable: Drawable): Bitmap {
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth, drawable.intrinsicHeight,
            if (drawable.opacity != PixelFormat.OPAQUE) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.draw(canvas)
        return bitmap
    }

    fun copyFile(src: FileInputStream?, dest: FileOutputStream?): Boolean {
        if (src == null || dest == null) {
            return false
        }
        val srcChannel: FileChannel?
        val dstChannel: FileChannel?
        try {
            srcChannel = src.channel
            dstChannel = dest.channel
            srcChannel.transferTo(0, srcChannel.size(), dstChannel)
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        try {
            srcChannel.close()
            dstChannel.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return true
    }

    fun copyFile(src: File?, dest: File?): Boolean {
        if (src == null || dest == null) {
            return false
        }
        if (dest.exists()) {
            dest.delete()
        }
        try {
            dest.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val srcChannel: FileChannel?
        val dstChannel: FileChannel?
        try {
            srcChannel = FileInputStream(src).channel
            dstChannel = FileOutputStream(dest).channel
            srcChannel.transferTo(0, srcChannel.size(), dstChannel)
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        try {
            srcChannel.close()
            dstChannel.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return true
    }

    private fun changeBrightness(imageView: ImageView, brightness: Int) {
        val cMatrix = ColorMatrix()
        cMatrix.set(
            floatArrayOf(
                1f, 0f, 0f, 0f, brightness.toFloat(), 0f, 1f, 0f, 0f, brightness.toFloat(),  // 改变亮度
                0f, 0f, 1f, 0f, brightness.toFloat(), 0f, 0f, 0f, 1f, 0f
            )
        )
        imageView.colorFilter = ColorMatrixColorFilter(cMatrix)
    }

    suspend fun preparePhotoShare(context: Context, url: String): Uri =
        withPhoto(context, url) { input, _, fileName ->
            val directory = File(context.cacheDir, ".shareTemp")
            directory.mkdirs()
            val file = File.createTempFile("share_", "." + fileName.substringAfterLast('.'), directory)
            try {
                input.writePhotoTo(file.outputStream())
                FileProvider.getUriForFile(context, "${context.packageName}.share.FileProvider", file)
            } catch (e: Exception) {
                file.delete()
                throw e
            }
        }

    suspend fun savePhoto(context: Context, url: String): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val granted = withContext(Dispatchers.Main.immediate) {
                suspendCancellableCoroutine { continuation ->
                    askPermission(
                        context,
                        PermissionData(
                            listOf(PermissionUtils.WRITE_EXTERNAL_STORAGE),
                            context.getString(R.string.tip_permission_storage)
                        ),
                        R.string.toast_no_permission_save_photo,
                        onGranted = { if (continuation.isActive) continuation.resume(true) },
                        onDenied = { if (continuation.isActive) continuation.resume(false) },
                    )
                }
            }
            if (!granted) return null
        }
        val relativePath = "${Environment.DIRECTORY_PICTURES}/${FileUtil.FILE_FOLDER}"
        return withPhoto(context, url) { input, mimeType, fileName ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IOException("Could not create the photo")
                try {
                    val output = resolver.openOutputStream(uri) ?: throw IOException("Could not open the photo")
                    input.writePhotoTo(output)
                    val published = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                    if (resolver.update(uri, published, null, null) != 1) throw IOException("Could not publish the photo")
                } catch (e: Exception) {
                    resolver.delete(uri, null, null)
                    throw e
                }
                relativePath
            } else {
                val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val directory = File(pictures, FileUtil.FILE_FOLDER)
                directory.mkdirs()
                var file = File(directory, fileName)
                if (!file.createNewFile()) {
                    file = File.createTempFile("tieba_", "." + file.extension, directory)
                }
                try {
                    input.writePhotoTo(file.outputStream())
                    MediaScannerConnection.scanFile(context, arrayOf(file.path), arrayOf(mimeType), null)
                } catch (e: Exception) {
                    file.delete()
                    throw e
                }
                file.path
            }
        }
    }

    private fun InputStream.writePhotoTo(output: OutputStream) {
        output.use { if (copyTo(it) == 0L) throw IOException("Empty image") }
    }

    /** Read the format from the image header. A CDN URL's extension may describe a different encoding. */
    private suspend fun <T> withPhoto(
        context: Context,
        url: String,
        action: (InputStream, mimeType: String, fileName: String) -> T,
    ): T = withContext(Dispatchers.IO) {
        val uri = Uri.parse(url)
        val open: () -> InputStream = when (uri.scheme) {
            ContentResolver.SCHEME_CONTENT, ContentResolver.SCHEME_FILE, ContentResolver.SCHEME_ANDROID_RESOURCE -> {
                { context.contentResolver.openInputStream(uri) ?: throw IOException("Could not open the photo") }
            }
            else -> when (val result = DownloadRequest(context, url).execute()) {
                is DownloadResult.Success -> result.data.data::newInputStream
                is DownloadResult.Error -> throw result.throwable
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open().use { BitmapFactory.decodeStream(it, null, bounds) }
        val mimeType = bounds.outMimeType?.takeIf { it.startsWith("image/") && bounds.outWidth > 0 && bounds.outHeight > 0 }
            ?: throw IOException("Unsupported image")
        val extension = when (mimeType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
                ?: throw IOException("Unknown image format: $mimeType")
        }
        val stem = File(uri.lastPathSegment.orEmpty()).nameWithoutExtension
            .filter { it.isLetterOrDigit() || it == '-' || it == '_' || it == ' ' }
            .take(48).trim().ifEmpty { "tieba" }
        open().use { action(it, mimeType, "$stem.$extension") }
    }

    fun getPicId(picUrl: String?): String {
        val fileName = URLUtil.guessFileName(picUrl, null, MimeType.JPEG.toString())
        return fileName.replace(".jpg", "")
    }

    @JvmStatic
    fun getNonNullString(vararg strings: String?): String? {
        for (url in strings) {
            if (!TextUtils.isEmpty(url)) {
                return url
            }
        }
        return null
    }

    @JvmStatic
    fun getRadiusPx(context: Context): Int {
        return DisplayUtil.dp2px(context, getRadiusDp(context).toFloat())
    }

    private fun getRadiusDp(context: Context): Int {
        return context.appPreferences.radius
    }

    fun clear(imageView: ImageView?) {
        imageView?.disposeDisplay()
    }

    fun getPlaceHolder(context: Context, radius: Int): Drawable {
        val drawable = GradientDrawable()
        val colorResId =
            if (isNightMode()) R.color.color_place_holder_night else R.color.color_place_holder
        val color = ContextCompat.getColor(context, colorResId)
        drawable.setColor(color)
        drawable.cornerRadius =
            DisplayUtil.dp2px(context, radius.toFloat()).toFloat()
        return drawable
    }

    fun getPlaceHolder(context: Context, radius: Float): Drawable {
        val drawable = GradientDrawable()
        val colorResId =
            if (isNightMode()) R.color.color_place_holder_night else R.color.color_place_holder
        val color = ContextCompat.getColor(context, colorResId)
        drawable.setColor(color)
        drawable.cornerRadius = radius.dpToPxFloat()
        return drawable
    }

    @SuppressLint("CheckResult")
    fun load(
        imageView: ImageView,
        @LoadType type: Int,
        url: String?,
        skipNetworkCheck: Boolean,
        noTransition: Boolean,
    ) {
        if (!Util.canLoadGlide(imageView.context)) {
            return
        }
        if (isNightMode()) {
            changeBrightness(imageView, -35)
        } else {
            imageView.clearColorFilter()
        }
        val radius = getRadiusDp(imageView.context).toFloat()
        val requestBuilder =
            if (skipNetworkCheck ||
                type == LOAD_TYPE_AVATAR ||
                imageLoadSettings == SETTINGS_ALL_ORIGIN ||
                imageLoadSettings == SETTINGS_SMART_ORIGIN ||
                (imageLoadSettings == SETTINGS_SMART_LOAD && NetworkUtil.isWifiConnected(imageView.context))
            ) {
                imageView.setTag(R.id.image_load_tag, true)
                DisplayRequest.Builder(imageView.context, url)
            } else {
                imageView.setTag(R.id.image_load_tag, false)
                DisplayRequest.Builder(imageView.context, url).depth(Depth.LOCAL)
            }
        when (type) {
            LOAD_TYPE_SMALL_PIC -> requestBuilder
                .placeholder(getPlaceHolder(imageView.context, radius))
                .transformations(RoundedCornersTransformation(radius.dpToPxFloat()))

            LOAD_TYPE_AVATAR -> requestBuilder
                .placeholder(getPlaceHolder(imageView.context, 6f))
                .transformations(RoundedCornersTransformation(6f.dpToPxFloat()))

            LOAD_TYPE_NO_RADIUS -> requestBuilder
                .placeholder(getPlaceHolder(imageView.context, 0f))

            LOAD_TYPE_ALWAYS_ROUND -> requestBuilder
                .placeholder(getPlaceHolder(imageView.context, 100f))
                .transformations(CircleCropTransformation())
        }
        if (!noTransition) {
            requestBuilder.crossfade()
        }
        requestBuilder.target(imageView).build().enqueue()
    }

    @JvmStatic
    @JvmOverloads
    fun load(
        imageView: ImageView,
        @LoadType type: Int,
        url: String?,
        skipNetworkCheck: Boolean = false
    ) {
        load(imageView, type, url, skipNetworkCheck, false)
    }

    /**
     * 获取要加载的图片 Url
     *
     * @param isSmallPic   加载的是否为缩略图
     * @param originUrl    原图 Url
     * @param smallPicUrls 缩略图 Url，按照画质从好到差排序
     * @return 要加载的图片 Url
     */
    @JvmStatic
    fun getUrl(
        context: Context,
        isSmallPic: Boolean,
        originUrl: String,
        vararg smallPicUrls: String?
    ): String {
        return if (isSmallPic) {
            getThumbnailUrl(context, imageLoadSettings, originUrl, *smallPicUrls)
        } else originUrl
    }

    fun getThumbnailUrl(
        context: Context,
        loadType: Int,
        originUrl: String,
        vararg candidates: String?,
    ): String {
        val highQuality = loadType == SETTINGS_ALL_ORIGIN ||
            (loadType == SETTINGS_SMART_ORIGIN && NetworkUtil.isWifiConnected(context))
        return if (highQuality) candidates.firstOrNull { !it.isNullOrEmpty() } ?: originUrl
        else candidates.lastOrNull { !it.isNullOrEmpty() } ?: originUrl
    }

    @get:ImageLoadSettings
    private val imageLoadSettings: Int
        get() = INSTANCE.appPreferences.imageLoadType!!.toInt()

    fun imageToBase64(inputStream: InputStream?): String? {
        if (inputStream == null) {
            return null
        }
        return runCatching {
            inputStream.use {
                Base64.encodeToString(inputStream.readBytes(), Base64.DEFAULT)
            }
        }.getOrNull()
    }

    fun imageToBase64(file: File?): String? {
        if (file == null) {
            return null
        }
        var result: String? = null
        try {
            val `is`: InputStream = FileInputStream(file)
            result = imageToBase64(`is`)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return result
    }

    @IntDef(LOAD_TYPE_SMALL_PIC, LOAD_TYPE_AVATAR, LOAD_TYPE_NO_RADIUS, LOAD_TYPE_ALWAYS_ROUND)
    @Retention(AnnotationRetention.SOURCE)
    annotation class LoadType

    @IntDef(SETTINGS_SMART_ORIGIN, SETTINGS_SMART_LOAD, SETTINGS_ALL_ORIGIN, SETTINGS_ALL_NO)
    annotation class ImageLoadSettings

}

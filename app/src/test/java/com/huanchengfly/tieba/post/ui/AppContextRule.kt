package com.huanchengfly.tieba.post.ui

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import com.huanchengfly.tieba.post.App
import org.junit.rules.ExternalResource

/** Supplies the context used by the theme without running the app's account and device services. */
class AppContextRule : ExternalResource() {
    private val instance = App::class.java.getDeclaredField("INSTANCE").apply { isAccessible = true }
    private var previous: App? = null

    override fun before() {
        previous = instance.get(null) as App?
        val app = App()
        val context = ApplicationProvider.getApplicationContext<Application>()
        ContextWrapper::class.java.getDeclaredMethod("attachBaseContext", Context::class.java).apply {
            isAccessible = true
            invoke(app, context)
        }
        instance.set(null, app)
    }

    override fun after() {
        instance.set(null, previous)
    }
}

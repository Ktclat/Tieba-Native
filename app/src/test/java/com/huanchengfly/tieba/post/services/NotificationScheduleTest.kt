package com.huanchengfly.tieba.post.services

import android.app.Application
import android.app.job.JobScheduler
import androidx.test.core.app.ApplicationProvider
import com.huanchengfly.tieba.post.utils.JobServiceUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class NotificationScheduleTest {
    @Test fun onlyTheSelectedAccountHasOnePeriodicJobAcrossRestarts() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val scheduler = checkNotNull(app.getSystemService(JobScheduler::class.java))
        val id = JobServiceUtil.getJobId(app)
        NotifyJobService.updateSchedule(app, null)
        assertNull(scheduler.getPendingJob(id))
        NotifyJobService.updateSchedule(app, "42")
        val first = checkNotNull(scheduler.getPendingJob(id))
        NotifyJobService.updateSchedule(app, "42")
        assertSame(first, scheduler.getPendingJob(id))
        NotifyJobService.updateSchedule(app, "83")
        assertNotSame(first, scheduler.getPendingJob(id))
        assertEquals(1, scheduler.allPendingJobs.count { it.id == id })
        NotifyJobService.updateSchedule(app, null)
        assertNull(scheduler.getPendingJob(id))
    }
}

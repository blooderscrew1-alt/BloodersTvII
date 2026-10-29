package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("BloodersTv II", appName)
  }

  @Test
  fun `launch MainActivity`() {
    val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
    org.junit.Assert.assertNotNull(controller.get())
  }

  @Test
  fun `repository starts empty without default channels or playlists`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = com.example.data.repository.IptvRepository(context)
    assertEquals(0, repo.playlists.value.size)
    assertEquals(0, repo.items.value.size)
    assertEquals(0, repo.history.value.size)
  }
}

package org.renpy.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import org.renpy.android.databinding.ActivityWelcomeBinding
import org.renpy.android.tcblog.TcBlogMarkdownParser

class WelcomeActivity : GameWindowActivity() {

    private lateinit var binding: ActivityWelcomeBinding

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setTitle(R.string.title_welcome)

        val isGps = GpsRead.read(this)
        val markdownText = WelcomeContent.getMarkdown(this, isGps)

        val markdownParser = TcBlogMarkdownParser(this)
        markdownParser.renderMarkdown(markdownText, binding.llWelcomeBody)

        if (savedInstanceState == null) {
            checkAndRequestNotificationPermission()
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

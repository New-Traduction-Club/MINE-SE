package org.renpy.android

import android.os.Bundle
import org.renpy.android.databinding.ActivityWelcomeBinding

class WelcomeActivity : GameWindowActivity() {

    private lateinit var binding: ActivityWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setTitle(R.string.title_welcome)
    }
}

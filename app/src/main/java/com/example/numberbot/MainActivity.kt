package com.example.numberbot

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.numberbot.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val projectionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val intent = Intent(this, ScreenCaptureService::class.java).apply {
                    action = ScreenCaptureService.ACTION_START
                    putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                    putExtra(ScreenCaptureService.EXTRA_DATA, result.data)
                }
                startForegroundService(intent)
                binding.statusText.text = "Status: Bot กำลังทำงาน"
            } else {
                binding.statusText.text = "Status: ยกเลิกการแชร์หน้าจอ"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.accessibilityButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        binding.startButton.setOnClickListener {
            if (!isAccessibilityEnabled()) {
                binding.statusText.text = "Status: กรุณาเปิด Accessibility ก่อน"
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                return@setOnClickListener
            }

            NumberAccessibilityService.enabled = true
            NumberAccessibilityService.nextNumber = 1

            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                    as MediaProjectionManager
            projectionLauncher.launch(manager.createScreenCaptureIntent())
        }

        binding.stopButton.setOnClickListener {
            NumberAccessibilityService.enabled = false
            stopService(Intent(this, ScreenCaptureService::class.java))
            binding.statusText.text = "Status: หยุดแล้ว"
        }
    }

    override fun onResume() {
        super.onResume()
        binding.statusText.text =
            if (isAccessibilityEnabled()) "Status: Accessibility พร้อม"
            else "Status: ต้องเปิด Accessibility"
    }

    private fun isAccessibilityEnabled(): Boolean {
        val expected = ComponentName(this, NumberAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        while (splitter.hasNext()) {
            if (splitter.next().equals(expected.flattenToString(), ignoreCase = true)) {
                return true
            }
        }
        return false
    }
}

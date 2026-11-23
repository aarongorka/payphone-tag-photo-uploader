package com.aarongorka.payphonetag

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import com.aarongorka.payphonetag.databinding.ActivityMainBinding
import java.io.File
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var settings: Settings
    private var photo: File? = null

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        val file = photo
        if (taken && file != null) showPreview(file) else photo = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settings = Settings(this)

        binding.username.setText(settings.username)
        binding.password.setText(settings.password)
        savedInstanceState?.getString(STATE_PHOTO)?.let {
            File(it).takeIf(File::exists)?.let { restored ->
                photo = restored
                showPreview(restored)
            }
        }

        binding.takePhoto.setOnClickListener { launchCamera() }
        binding.upload.setOnClickListener { upload() }
        binding.saveCredentials.setOnClickListener {
            settings.username = binding.username.text.toString().trim()
            settings.password = binding.password.text.toString().trim()
            toast(getString(R.string.saved))
        }
        binding.photoType.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            PhotoType.entries.map { it.label },
        )
        if (savedInstanceState == null) handleIntent(intent)
        if (!settings.hasCredentials) binding.status.setText(R.string.need_credentials)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        photo?.let { outState.putString(STATE_PHOTO, it.absolutePath) }
    }

    private fun launchCamera() {
        val dir = File(cacheDir, "photos").apply { mkdirs() }
        val file = File.createTempFile("photo", ".jpg", dir)
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        photo = file
        runCatching { takePicture.launch(uri) }.onFailure { toast(getString(R.string.no_camera)) }
    }

    private fun showPreview(file: File) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        val sample = Integer.highestOneBit((bounds.outWidth / PREVIEW_PX).coerceAtLeast(1))
        val bitmap = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
        binding.preview.setImageBitmap(bitmap)
        binding.preview.isVisible = true
        binding.takePhoto.setText(R.string.action_photo)
    }

    private fun handleIntent(intent: Intent?) {
        val source = intent?.dataString ?: intent?.getStringExtra(Intent.EXTRA_TEXT)
        PayphoneIds.parse(source)?.let { binding.payphoneId.setText(it) }
    }

    private fun upload() {
        val file = photo?.takeIf { it.exists() && it.length() > 0 }
        val id = PayphoneIds.parse(binding.payphoneId.text.toString())
        when {
            !settings.hasCredentials -> return toast(getString(R.string.need_credentials))
            file == null -> return toast(getString(R.string.need_photo))
            id == null -> return toast(getString(R.string.need_id))
        }
        val type = PhotoType.entries[binding.photoType.selectedItemPosition]
        val caption = binding.caption.text.toString().trim()

        setBusy(true)
        binding.status.setText(R.string.uploading)
        thread {
            val result = runCatching {
                val client = WikiClient()
                client.login(settings.username, settings.password)
                client.uploadPhoto(id!!, type, caption, file!!)
            }
            runOnUiThread {
                setBusy(false)
                result.onSuccess {
                    binding.status.text = getString(R.string.uploaded, id)
                    file?.delete()
                    photo = null
                    binding.preview.isVisible = false
                    binding.caption.text = null
                }.onFailure { binding.status.text = getString(R.string.failed, it.message) }
            }
        }
    }

    private fun setBusy(busy: Boolean) {
        binding.upload.isEnabled = !busy
        binding.takePhoto.isEnabled = !busy
        binding.status.visibility = View.VISIBLE
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private companion object {
        const val STATE_PHOTO = "photo"
        const val PREVIEW_PX = 1024
    }
}

package com.example.drawit.ui

import android.Manifest
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import com.example.drawit.R
import com.example.drawit.databinding.ActivityMainBinding
import com.example.drawit.databinding.ViewBrushSizePickerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var aibCurrentColor: AppCompatImageButton
    private lateinit var customProgressDialog: Dialog

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        aibCurrentColor = binding.llColorPaletteContainer[0] as AppCompatImageButton
        aibCurrentColor.setImageDrawable(
            ContextCompat.getDrawable(
                this, R.drawable.layer_list_palette_pressed
            )
        )
        initClickListeners()
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun initClickListeners() {
        binding.drawingView.setBrushSize(20.toFloat())
        binding.aibBrushSizePicker.setOnClickListener {
            showBrushSizePickerDialog()
        }
        binding.aibGallery.setOnClickListener {
            requestReadPermission()
        }
        binding.aibUndo.setOnClickListener {
            binding.drawingView.performUndo()
        }
        binding.aibRedo.setOnClickListener {
            binding.drawingView.performRedo()
        }
        binding.aibSave.setOnClickListener {
            requestWritePermission()
        }
    }

    private fun showBrushSizePickerDialog() {
        val viewBrushSizePickerBinding = ViewBrushSizePickerBinding.inflate(layoutInflater)
        val brushSizePickerDialog = Dialog(this)
        brushSizePickerDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        brushSizePickerDialog.setTitle(getString(R.string.pick_your_brush))
        brushSizePickerDialog.setCancelable(true)
        brushSizePickerDialog.setContentView(viewBrushSizePickerBinding.root)
        viewBrushSizePickerBinding.aibSmall.setOnClickListener {
            binding.drawingView.setBrushSize(10.toFloat())
            brushSizePickerDialog.dismiss()
        }
        viewBrushSizePickerBinding.aibMedium.setOnClickListener {
            binding.drawingView.setBrushSize(20.toFloat())
            brushSizePickerDialog.dismiss()
        }
        viewBrushSizePickerBinding.aibLarge.setOnClickListener {
            binding.drawingView.setBrushSize(30.toFloat())
            brushSizePickerDialog.dismiss()
        }
        brushSizePickerDialog.show()
    }

    fun paintClicked(view: View) {
        if (view != aibCurrentColor) {
            val imageButton = view as AppCompatImageButton
            val colorTag = imageButton.tag.toString()
            binding.drawingView.setBrushColor(colorTag)
            imageButton.setImageDrawable(
                ContextCompat.getDrawable(
                    this, R.drawable.layer_list_palette_pressed
                )
            )
            aibCurrentColor.setImageDrawable(
                ContextCompat.getDrawable(
                    this, R.drawable.layer_list_palette_normal
                )
            )
            aibCurrentColor = imageButton
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun requestReadPermission() {
        requestReadPermission.launch(
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
        )
    }

    private val requestReadPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                val permission = it.key
                val isGranted = it.value
                if (!isGranted) {
                    when {
                        permission == Manifest.permission.READ_EXTERNAL_STORAGE && (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.S_V2) -> {
                            displayAlertDialog(getString(R.string.draw_it_requires_to_read_external_storage_permission_for_its_proper_working_please_provide_the_same))
                        }

                        permission == Manifest.permission.READ_MEDIA_IMAGES && (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) -> {
                            displayAlertDialog(getString(R.string.draw_it_requires_to_read_external_storage_permission_for_its_proper_working_please_provide_the_same))
                        }

                        permission == Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED && (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) -> {
                            displayAlertDialog(getString(R.string.draw_it_requires_to_read_external_storage_permission_for_its_proper_working_please_provide_the_same))
                        }

                    }
                } else {
                    val imagePickIntent: Intent =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    galleryLauncher.launch(imagePickIntent)
                }
            }
        }

    private val galleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                binding.aivBackground.setImageURI(result.data?.data)
            }
        }

    private fun requestWritePermission() {
        requestWritePermission.launch(
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            )
        )
    }

    private val requestWritePermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                val permission = it.key
                val isGranted = it.value
                if (!isGranted) {
                    when {
                        permission == Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
                            displayAlertDialog(getString(R.string.draw_it_requires_to_write_external_storage_permission_for_its_proper_working_please_provide_the_same))
                        }
                    }
                } else {
                    showCustomProgressDialog()
                    lifecycleScope.launch {
                        saveBitmap(getBitmapFromView(binding.flMainContainer))
                    }
                }
            }
        }

    private fun displayAlertDialog(message: String) {
        val dialogBuilder = AlertDialog.Builder(this).setCancelable(false).setPositiveButton(
            getString(R.string.ok_caps),
            DialogInterface.OnClickListener { dialog, _ ->
                dialog.dismiss()
                val intent: Intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts(
                        "package", packageName, null
                    )
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)

            })

        val alert = dialogBuilder.create()
        alert.setTitle(getString(R.string.alert))
        alert.setMessage(message)
        alert.show()
    }

    private fun getBitmapFromView(view: View): Bitmap {
        val bitmap: Bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas: Canvas = Canvas(bitmap)
        val viewBackground: Drawable? = view.background
        when {
            viewBackground != null -> {
                viewBackground.draw(canvas)
            }

            else -> {
                canvas.drawColor(Color.WHITE)
            }
        }
        view.draw(canvas)
        return bitmap
    }

    private suspend fun saveBitmap(bitmap: Bitmap?) {
        withContext(Dispatchers.IO) {
            when {
                bitmap != null -> {
                    try {
                        val byteArrayOutputStream = ByteArrayOutputStream()
                        bitmap.compress(CompressFormat.PNG, 90, byteArrayOutputStream)
                        val file =
                            File(externalCacheDir?.absoluteFile.toString() + File.separator + "DrawIt_" + System.currentTimeMillis() / 1000 + ".jpg")
                        val fileOutputStream = FileOutputStream(file)
                        fileOutputStream.write(byteArrayOutputStream.toByteArray())
                        fileOutputStream.close()
                        runOnUiThread {
                            when {
                                file.absoluteFile.toString().isNotEmpty() -> {
                                    Toast.makeText(
                                        this@MainActivity,
                                        getString(R.string.image_saved_successfully) + file.absoluteFile.toString(),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    shareImage(file.absolutePath.toString())
                                }

                                else -> {
                                    Toast.makeText(
                                        this@MainActivity,
                                        getString(R.string.something_went_wrong_while_saving_the_image),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                else -> {}
            }
        }
    }

    private fun shareImage(imagePath: String) {
        MediaScannerConnection.scanFile(this, arrayOf(imagePath), null) { path, uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }

    private fun showCustomProgressDialog() {
        customProgressDialog = Dialog(this)
        customProgressDialog.setContentView(R.layout.dialog_progress)
        customProgressDialog.show()
    }

    private fun dismissCustomProgressDialog() {
        customProgressDialog.dismiss()
    }

}
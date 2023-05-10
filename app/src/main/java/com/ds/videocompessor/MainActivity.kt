package com.ds.videocompessor

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.arthenica.mobileffmpeg.ExecuteCallback
import com.arthenica.mobileffmpeg.FFmpeg
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {

    lateinit var tvFilePath: TextView;
    lateinit var btnOpenFolder: Button;

    val REQUEST_CODE_SELECT_VIDEO = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnSelectVideo = findViewById<Button>(R.id.button)
        btnOpenFolder = findViewById<Button>(R.id.button2)

        tvFilePath = findViewById<TextView>(R.id.textView)
        btnSelectVideo.setOnClickListener {
            pickFile()
        }

        btnOpenFolder.visibility = View.INVISIBLE
    }

    private fun pickFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "video/*"
        startActivityForResult(intent, REQUEST_CODE_SELECT_VIDEO)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SELECT_VIDEO && resultCode == RESULT_OK) {
            val selectedVideoUri = data?.data

            // Get the file path from the URI
            val selectedVideoPath = getFilePathFromUri(selectedVideoUri!!)

            // Compress the video
            val outputVideoPath = getOutputVideoPath()

            val command = arrayOf("-i", selectedVideoPath, "-c:v", "libx264", "-preset", "ultrafast", "-c:a", "copy", outputVideoPath)

            FFmpeg.executeAsync(command, object : ExecuteCallback {
                override fun apply(executionId: Long, returnCode: Int) {
                    if (returnCode == 0) {
                        // Compression successful, do something with the compressed video
                        Log.d("Printer", "Compression successful")

                        Toast.makeText(this@MainActivity, "Video Compression complete!", Toast.LENGTH_LONG).show()

                        tvFilePath.setText("Output Video Path : " + outputVideoPath)

                        btnOpenFolder.visibility = View.VISIBLE

                        btnOpenFolder.setOnClickListener{
                            val path = Environment.getExternalStorageDirectory()
                                .toString() + "/" + "Downloads" + "/"
                            val uri = Uri.parse(outputVideoPath)
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.setDataAndType(uri, "*/*")
                            startActivity(intent)
                        }

                    } else {
                        // Compression failed, handle the error
                        Log.e("Printer", "Compression failed with return code: $returnCode")

                        btnOpenFolder.visibility = View.INVISIBLE

                        Toast.makeText(this@MainActivity, "Compression failed with return code: $returnCode", Toast.LENGTH_LONG).show()
                    }
                }
            })

        }
    }

    fun getFilePathFromUri(uri: Uri): String? {
        var filePath: String? = null
        val inputStream = applicationContext.contentResolver.openInputStream(uri)
        inputStream?.let {
            val file = File(applicationContext.cacheDir, "temp_file")
            FileOutputStream(file).use { output ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while (it.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                }
                output.flush()
            }
            filePath = file.absolutePath
        }
        return filePath
    }

    private fun getOutputVideoPath(): String {
        val outputFolder = File(getExternalFilesDir(null), "MyAppVideos")
        if (!outputFolder.exists()) {
            outputFolder.mkdirs()
        }
        val outputFile = File(outputFolder, "compressed_${System.currentTimeMillis()}.mp4")
        return outputFile.absolutePath
    }

}
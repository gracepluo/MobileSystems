package com.cs407.lab9app

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class MainActivity : AppCompatActivity() {
    private var imageIndex = 1
    private val maxIndex = 6
    private lateinit var imageHolder: ImageView
    private lateinit var textOutput: TextView

    companion object {
        private const val REQUEST_CAMERA_PERMISSIONS = 1
        private const val IMAGE_CAPTURE_CODE = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        imageHolder = findViewById(R.id.imageHolder)
        textOutput = findViewById(R.id.textOutput)
        textOutput.showSoftInputOnFocus = false
        textOutput.isFocusable = false
    }

    fun onText(view: View) {
        // TODO: Implement the Basic Setup For Text Recognition
        val bitmap = (imageHolder.drawable as BitmapDrawable).bitmap
        val image = InputImage.fromBitmap(bitmap, 0)
        val option = TextRecognizerOptions.DEFAULT_OPTIONS
        val recognizer: TextRecognizer = TextRecognition.getClient(option)
        // TODO: Add Listeners for text detection process
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                if(visionText.textBlocks.isEmpty()){
                    toTextBox("Error", getString(R.string.text_recognition_error))
                }else {
                    for (block in visionText.textBlocks) {
                        val text = block.text
                        val boundingBox = block.boundingBox
                        for (line in block.lines) {
                            // Draw bounding box and display text on the app
                            // Use your helper methods `drawBox` and `toTextBox` here
                            drawBox(line.boundingBox, line.text, Color.RED, Color.BLUE)
                        }
                        toTextBox("Text Found", text)
                    }
                }
            }
            .addOnFailureListener{
                //Toast.makeText(context, "Text recognition failed", Toast.LENGTH_SHORT).show()
                toTextBox("Error", getString(R.string.text_recognition_error))
                toTextBox("Error", it)
            }
    }

    fun onFace(view: View) {
        // TODO: Implement the Basic Setup For Face Recognition
        val bitmap = (imageHolder.drawable as BitmapDrawable).bitmap
        val image = InputImage.fromBitmap(bitmap, 0)
            val options = FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .build()
            val detector : FaceDetector = FaceDetection.getClient(options)
        // TODO: Add Listeners for face detection process
            detector.process(image)
                .addOnSuccessListener { faces ->
                    if(faces.isEmpty()){
                        toTextBox("Error", getString(R.string.face_recognition_error))
                    }
                    for (face in faces) {
                        val bounds = face.boundingBox
                        val rotate = face.headEulerAngleY
                        val tilt = face.headEulerAngleZ
                        val smileProb = face.smilingProbability

                        val leftEye = face.getContour(FaceContour.LEFT_EYE)?.points
                        leftEye?.let {
                           drawLine(it, Color.GREEN)
                        }
                        val rightEye = face.getContour(FaceContour.RIGHT_EYE)?.points
                        rightEye?.let {
                            drawLine(it, Color.GREEN)
                        }
                        toTextBox("Bounds ", bounds)

                        if (smileProb == null || smileProb!! < 0.5) {
                            toTextBox("Smiling", "No!")
                        }else{
                            toTextBox("Smiling", "yes!")

                        }

                        toTextBox("Angle Y", rotate)
                        toTextBox("Angle Z", tilt)
                    }
                }
                .addOnFailureListener{
                    //Toast.makeText(context, "Text recognition failed", Toast.LENGTH_SHORT).show()
                    toTextBox("Error", getString(R.string.face_recognition_error))
                    toTextBox("Error", it)

                }

    }

    fun onLabel(view: View) {
        // TODO: Implement the Basic Setup For Label Recognition
        val bitmap = (imageHolder.drawable as BitmapDrawable).bitmap
        val image = InputImage.fromBitmap(bitmap, 0)
        // TODO: Add Listeners for Label detection process
        val options = ImageLabelerOptions.DEFAULT_OPTIONS
        val labeler: ImageLabeler = ImageLabeling.getClient(options)
        labeler.process(image)
            .addOnSuccessListener { labels ->
                for (label in labels) {
                    val item = label.text
                    val index = label.index
                    val confident = label.confidence
                    toTextBox("Item", item)
                    toTextBox("Index", index)
                    toTextBox("Confidence", confident)
                }

            }
            .addOnFailureListener{
                //Toast.makeText(context, "Text recognition failed", Toast.LENGTH_SHORT).show()
                toTextBox("Error", getString(R.string.label_recognition_error))
                toTextBox("Error", it)

            }
    }

    private fun toTextBox(label: String, value: Any) {
        textOutput.append("$label: $value\n")
    }

    private fun drawBox(bounds: Rect?, label: String, boxColor: Int, textColor: Int) {
        bounds?.let {
            val drawingView = DrawingView(applicationContext, it, label, boxColor, textColor)
            val bitmap = (imageHolder.drawable as BitmapDrawable).bitmap
            val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            drawingView.draw(Canvas(mutableBitmap))
            runOnUiThread { imageHolder.setImageBitmap(mutableBitmap) }
        }
    }

    private fun drawLine(points: List<PointF>, lineColor: Int) {
        val drawingView = DrawingLineView(applicationContext, points, lineColor)
        val bitmap = (imageHolder.drawable as BitmapDrawable).bitmap
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        drawingView.draw(Canvas(mutableBitmap))
        runOnUiThread { imageHolder.setImageBitmap(mutableBitmap) }
    }

    private fun addImage(x: Float, y: Float, bounds: Rect, angle: Float, fileName: String) {
        val img = ImageView(this)
        val resID = resources.getIdentifier(fileName, "drawable", packageName)
        img.setImageResource(resID)
        val frame: FrameLayout = findViewById(R.id.frame)
        frame.addView(img)
        img.layoutParams.apply {
            height = bounds.height()
            width = bounds.width()
        }
        img.x = x - (bounds.width() / 2)
        img.y = y - (bounds.height() / 2)
        img.rotation = angle
        img.bringToFront()
    }

    fun launchCamera(view: View) {
        val permission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSIONS)
        } else {
            val cIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cIntent, IMAGE_CAPTURE_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val cIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(cIntent, IMAGE_CAPTURE_CODE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_CAPTURE_CODE && resultCode == Activity.RESULT_OK) {
            val bitmap = data?.extras?.get("data") as? Bitmap
            imageHolder.setImageBitmap(bitmap)
        }
    }

    fun onNext(view: View) {
        imageIndex++
        if (imageIndex > maxIndex) {
            imageIndex = 1
        }
        val resID = resources.getIdentifier("pic$imageIndex", "drawable", packageName)
        imageHolder.setImageResource(resID)
        textOutput.text = ""
        val frame: FrameLayout = findViewById(R.id.frame)
        frame.removeAllViews()
        frame.addView(imageHolder)
    }

    fun onPrev(view: View) {
        imageIndex--
        if (imageIndex <= 0) {
            imageIndex = maxIndex
        }
        val resID = resources.getIdentifier("pic$imageIndex", "drawable", packageName)
        imageHolder.setImageResource(resID)
        textOutput.text = ""
        val frame: FrameLayout = findViewById(R.id.frame)
        frame.removeAllViews()
        frame.addView(imageHolder)
    }
}

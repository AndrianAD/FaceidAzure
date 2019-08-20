package com.improveit.face

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.improveit.face.App.Companion.currentPhotoPath
import com.microsoft.projectoxford.face.FaceServiceRestClient
import com.microsoft.projectoxford.face.contract.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*


fun Context.toast(message: CharSequence) =
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()


class MainActivity : AppCompatActivity() {

    private val apiEndpoint = "https://westcentralus.api.cognitive.microsoft.com/face/v1.0"
    private val subscriptionKey = "fd0771e0ee8046a1b55e8a9b1120413a"
    val personGroupId = "persogroup"
    val personGroupName = "personGroupName2"
    private val STORAGE_PERMISSION_CODE = 100
    protected val REQUEST_CODE_CAMERA = 123
    lateinit var faceServiceClient: FaceServiceRestClient
    var faceDetected: Array<Face>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        faceServiceClient = FaceServiceRestClient(apiEndpoint, subscriptionKey)





        addPerson.setOnClickListener {
            requestPermissionsCamera()
//            GlobalScope.launch {
//                addPersonToGroup(personGroupId, "Andrii", currentPhotoPath)
//            }
        }


        addGroup.setOnClickListener {
                                    val job = GlobalScope.launch {
                createPersonGroup(personGroupId, personGroupName)
            }
        }


        train.setOnClickListener {

            GlobalScope.launch {
                faceServiceClient.trainPersonGroup(personGroupId)
            }

            GlobalScope.launch {
                Log.d("xxx", "waiting for training")
                delay(500)
                var training = faceServiceClient.getPersonGroupTrainingStatus(personGroupId)
                if (training.status != TrainingStatus.Status.Running) {
                    Log.d("xxx", "Status: ${training.status}")

                }
            }
            Log.d("xxx", "Status: training competed")

        }


        indentify.setOnClickListener {
            if (faceDetected!!.isNotEmpty()) {
                val faceIds = arrayOfNulls<UUID>(faceDetected!!.size)
                    IdentifyTask().execute(*faceIds)
            }
        }


        detect.setOnClickListener {

            //            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.foto2)
//            val outputStream = ByteArrayOutputStream()
//            bitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
//            val inputStream = ByteArrayInputStream(outputStream.toByteArray())


            val myFile = File(currentPhotoPath)
            val targetStream = FileInputStream(myFile)
            DetectTask().execute(targetStream)
        }

        getPhoto.setOnClickListener {
            requestPermissionsCamera()
        }


    }


    private fun createPersonGroup(groupId: String, personGroupName: String) {
        try {
            faceServiceClient.createPersonGroup(groupId, personGroupName, null)
            //toast("createPersonGroup -OK")
            Log.d("xxx", "createPersonGroup -OK")

        } catch (error: Exception) {
            //toast(error.toString())
            Log.d("xxx", error.toString())
        }
    }

    private fun addPersonToGroup(groupId: String, personName: String, imagePath: String) {
        try {
            val group = faceServiceClient.getPersonGroup(groupId)
            if (group != null) {
                val personResult = faceServiceClient.createPerson(groupId, personName, null)
                detectFaceAndRegister(groupId, personResult)
                Log.d("xxx", "addPersonToGroup - OK")
            }

        } catch (error: Exception) {
            Log.d("xxx", error.toString())
            //toast(error.toString())
        }
    }

    private fun detectFaceAndRegister(groupId: String, personResult: CreatePersonResult?) {

        val myFile = File(currentPhotoPath)
        val myUrl = myFile.toURI().toURL()
        val stream = myUrl.openStream()
        faceServiceClient.addPersonFace(
            personGroupId,
            personResult?.personId,
            stream,
            null,
            null
        )
        //faceServiceClient.addPersonFace(personGroupId,personResult?.personId,"https://validcognition.com/wp-content/uploads/2014/08/man.jpg",null,null )
        // faceServiceClient.addPersonFace(personGroupId,personResult?.personId,"https://validcognition.com/wp-content/uploads/2014/08/man.jpg",null,null )
        // faceServiceClient.addPersonFace(personGroupId,personResult?.personId,stream,null,null )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {


        if (requestCode == REQUEST_CODE_CAMERA && resultCode == Activity.RESULT_OK) {
            imageView.setImageURI(Uri.fromFile(File(currentPhotoPath)))
            GlobalScope.launch {
                addPersonToGroup(personGroupId, "Andrii", currentPhotoPath)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                toast("permission granted")
                dispatchTakePictureIntent()
            } else {
                toast("permission_deined")
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivityForResult(intent, requestCode)
            }
        }
    }

    fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {

                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.improveit.face.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_CODE_CAMERA)
                }
            }
        }
    }


    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }


    fun requestPermissionsCamera() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {

            AlertDialog.Builder(this)
                .setTitle("Permission REQUIRED")
                .setMessage("Unfortunately, It seems the App don't have permission to access the camera")
                .setPositiveButton("Ok") { _, _ ->
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.CAMERA), STORAGE_PERMISSION_CODE
                    )
                }
                .setNegativeButton(
                    "Cancel"
                ) { dialog, _ -> dialog.dismiss() }
                .create().show()

        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA), STORAGE_PERMISSION_CODE
            )
        }
    }


    inner class DetectTask : AsyncTask<InputStream, String, Array<Face>>() {
        private val mDialog = ProgressDialog(this@MainActivity)

        override fun doInBackground(vararg params: InputStream): Array<Face>? {
            try {
                publishProgress("Detecting...")
                val results = faceServiceClient.detect(params[0], true, false, null)
                if (results == null) {
                    publishProgress("Detection Finished. Nothing detected")
                    return null
                } else {
                    publishProgress(String.format("Detection Finished. %d face(s) detected", results.size))
                    return results
                }
            } catch (ex: Exception) {
                Log.d("test_test", ex.toString())
                return null
            }

        }

        override fun onPreExecute() {
            mDialog.show()
        }

        override fun onPostExecute(faces: Array<Face>) {
            mDialog.dismiss()
            faceDetected = faces
            var options: BitmapFactory.Options = BitmapFactory.Options()
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            val bmp: Bitmap? = BitmapFactory.decodeFile(currentPhotoPath, options)
            imageView.setImageBitmap(drawFaceRectangleOnBitmap(bmp!!, faceDetected, "test"))

        }

        override fun onProgressUpdate(vararg values: String) {
            mDialog.setMessage(values[0])
            Toast.makeText(this@MainActivity, values[0], Toast.LENGTH_SHORT).show()
        }
    }

    private inner class IdentifyTask :
        AsyncTask<UUID, String, Array<IdentifyResult>>() {

        private val mDialog = ProgressDialog(this@MainActivity)

        override fun doInBackground(vararg params: UUID): Array<IdentifyResult>? {

            try {
                publishProgress("Getting person group status...")
                Log.d("test_test", "Getting person group status...")
                val trainingStatus = faceServiceClient.getPersonGroupTrainingStatus(personGroupId)
                if (trainingStatus.status != TrainingStatus.Status.Succeeded) {
                    publishProgress("Person group training status is " + trainingStatus.status)
                    Log.d("test_test", "Person group training status is ")
                    return null
                }
                publishProgress("Identifying...")
                Log.d("test_test", "Identifying...")

                var result = faceServiceClient.identity(
                    personGroupId, // person group id
                    params // face ids
                    , 1
                )
                return result

            } catch (e: Exception) {
                Log.d("test_test", e.toString())
                return null
            }

        }

        override fun onPreExecute() {
            mDialog.show()
        }

        override fun onPostExecute(identifyResults: Array<IdentifyResult>?) {
            mDialog.dismiss()

            if (identifyResults != null && identifyResults.isNotEmpty()) {
                for (identifyResult in identifyResults) {
                    PersonDetectionTask(personGroupId).execute(identifyResult.candidates[0].personId)
                }
                Log.d("test_test", "identifyResults is NOT NULL")
            } else {
                Log.d("test_test", "identifyResults is NULL")
            }

        }

        override fun onProgressUpdate(vararg values: String) {
            mDialog.setMessage(values[0])
        }
    }


    private inner class PersonDetectionTask(private val personGroupId: String) : AsyncTask<UUID, String, Person>() {
        private val mDialog = ProgressDialog(this@MainActivity)

        override fun doInBackground(vararg params: UUID): Person? {
            try {
                publishProgress("Getting person group status...")

                Log.d("test_test", "Getting person group status...")
                return faceServiceClient.getPerson(personGroupId, params[0])
            } catch (e: Exception) {
                Log.d("test_test", e.toString())
                return null
            }

        }

        override fun onPreExecute() {
            mDialog.show()
        }

        override fun onPostExecute(person: Person) {
            mDialog.dismiss()
            var options: BitmapFactory.Options = BitmapFactory.Options()
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            val bmp: Bitmap? = BitmapFactory.decodeFile(currentPhotoPath, options)
            imageView.setImageBitmap(drawFaceRectangleOnBitmap(bmp!!, faceDetected, person.name))
        }

        override fun onProgressUpdate(vararg values: String) {
            mDialog.setMessage(values[0])
        }
    }
}



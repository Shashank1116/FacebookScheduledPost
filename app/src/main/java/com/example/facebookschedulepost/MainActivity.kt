package com.example.facebookschedulepost

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Telephony.Carriers.AUTH_TYPE
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.example.facebookschedulepost.databinding.ActivityMainBinding
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.appevents.UserDataStore.EMAIL
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.facebook.share.Sharer
import com.facebook.share.model.SharePhoto
import com.facebook.share.model.SharePhotoContent
import com.facebook.share.widget.ShareDialog
import io.reactivex.disposables.CompositeDisposable
import java.io.IOException
import java.security.AccessController.getContext
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var imageBitmap:Bitmap?=null
    var shareDialog:ShareDialog?=null
    var callback:CallbackManager?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,R.layout.activity_main)
        attachListeners()
        binding.showDefaultView()
        initializeFacebookCallbacks()
        setUpLogin(callback)
        subscribeToBusEvents()

    }

    private fun initializeFacebookCallbacks() {
        callback = CallbackManager.Factory.create()
        shareDialog = ShareDialog(this)
        shareDialog!!.registerCallback(callback, object : FacebookCallback<Sharer.Result> {
            override fun onSuccess(result: Sharer.Result?) {
                Toast.makeText(this@MainActivity,"Upload successful",Toast.LENGTH_LONG).show()
            }

            override fun onCancel() {

            }

            override fun onError(error: FacebookException?) {
            }
        })
    }

    private fun isLoggedIn() :Boolean{
        val accessToken = AccessToken.getCurrentAccessToken()
        return accessToken != null && !accessToken.isExpired
    }

    private fun attachListeners() {
        binding.addButton.setOnClickListener {
            openMediaChooser(1)
        }
        binding.schedulePostButton.setOnClickListener {
            inputDateTime(binding.dateTime)
        }
        binding.imageView2.setOnClickListener {
            openMediaChooser(1)
        }
    }

    private fun setUpLogin(callBAckManager: CallbackManager?) {
        LoginManager.getInstance().registerCallback(callBAckManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult?) {
                inputDateTime(binding.dateTime)
            }

            override fun onCancel() {
                Toast.makeText(this@MainActivity, "Cancelled", Toast.LENGTH_LONG).show()

            }

            override fun onError(error: FacebookException?) {
                Toast.makeText(this@MainActivity, "Exception", Toast.LENGTH_LONG).show()

            }

        })
    }

    fun openMediaChooser(requestCode: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*"))
        intent.type = "(*/*"
        startActivityForResult(intent, requestCode)
    }


    fun subscribeToBusEvents(){
        val resultReceivedNotification = RxBus.getInstance()._bus.publish()
        val compositeDisposable = CompositeDisposable()
        compositeDisposable //
            .add(
                resultReceivedNotification.subscribe(
                    { event ->
                        if (event is TimeToPostEvent) {
                            startPost()

                        }

                    }, { event -> Toast.makeText(this, "Something went wrong!", Toast.LENGTH_SHORT).show() }
                ))

        compositeDisposable.add(resultReceivedNotification.connect())
    }

    fun startPost(){
        val photo =  SharePhoto.Builder()
            .setBitmap(imageBitmap)
            .build()
        val content =  SharePhotoContent.Builder().addPhoto(photo).build()
        shareDialog!!.show(content)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK && null != data) {
            val uri = data.data


            val selectedImageObject = data.data
            val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)

            val cursor = contentResolver.query(
                selectedImageObject,
                filePathColumn, null, null, null
            )
            cursor.moveToFirst()

            val columnIndex = cursor.getColumnIndex(filePathColumn[0])
            val picturePath = cursor.getString(columnIndex)
            cursor!!.close()
            val bmp: Bitmap? = null

            //                this.imagePath = getPath(selectedImageObject);
            setImage(selectedImageObject!!)


        }else{
            callback!!.onActivityResult(requestCode,resultCode,data)
        }
    }

    private fun setImage(selectedImage: Uri) {
        var bmp: Bitmap? = null
        try {
            bmp = getBitmapFromUri(selectedImage)
            imageBitmap = bmp
        } catch (e: IOException) {
            e.printStackTrace()
        }
        binding.showLoadedPhoto()
        binding.imageView2.setImageBitmap(bmp)
    }

    @Throws(IOException::class)
    private fun getBitmapFromUri(uri: Uri): Bitmap {
        val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
        val fileDescriptor = parcelFileDescriptor.fileDescriptor
        val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
        parcelFileDescriptor!!.close()
        return image
    }

    fun scheduleAsAPostToFacebook(timeInMillis:Long){
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent =  Intent(this,MyPostAlarm::class.java)

        val pendingIntent = PendingIntent.getBroadcast(this,0,intent,0)

        am.set(AlarmManager.RTC,timeInMillis,pendingIntent)



    }

    private fun inputDateTime(dateView: TextView) {
        val myCalendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val datePickerDialog = DatePickerDialog(
            this, { view, year, monthOfYear, dayOfMonth ->
                val newDate = Calendar.getInstance()
                newDate.set(year, monthOfYear, dayOfMonth)
                inputTime(sdf.format(newDate.time), dateView)
            }, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH), myCalendar.get(
                Calendar.DAY_OF_MONTH
            )
        )
        datePickerDialog.setTitle("Select Pause Date")
        datePickerDialog.show()
    }

    private fun inputTime(date: String, view: TextView) {
        val currentTime = Calendar.getInstance()
        val hour = currentTime.get(Calendar.HOUR_OF_DAY)
        val minute = currentTime.get(Calendar.MINUTE)
        val mTimePicker: TimePickerDialog
        mTimePicker = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val time = "$selectedHour:$selectedMinute:00"
            val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
            try {
                val formatTime = dateFormat.format(dateFormat.parse(time))
                view.text = "Post scheduled at $date $formatTime"
                if(isLoggedIn()){
                    LoginManager.getInstance().logInWithReadPermissions(this,Arrays.asList(EMAIL))
                }else {
                    scheduleAsAPostToFacebook(parseDateToMillis("$date $time"))
                }

            } catch (e: ParseException) {
                e.printStackTrace()
            }
        }, hour, minute, true)//Yes 24 hour time
        mTimePicker.setTitle("Select Time")
        mTimePicker.show()
    }


    fun ActivityMainBinding.showLoadedPhoto(){
        this.imageView2.visibility = View.VISIBLE
        this.addButton.visibility = View.INVISIBLE
        this.addTitle.visibility = View.INVISIBLE
    }

    fun ActivityMainBinding.showDefaultView(){
        this.imageView2.visibility = View.INVISIBLE
        this.addTitle.visibility = View.VISIBLE
        this.addButton.visibility = View.VISIBLE
    }

    private fun parseDateToMillis(timeStamp:String):Long{
        var time: Long = 0
        val format =SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.getDefault())

        try {
            val date = format.parse(timeStamp)
            time = date.time
        } catch (e: ParseException) {
            e.printStackTrace()
        }

        return time
    }

}

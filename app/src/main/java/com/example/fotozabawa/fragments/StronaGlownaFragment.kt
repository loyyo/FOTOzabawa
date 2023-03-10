package com.example.fotozabawa.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.example.fotozabawa.Constants
import com.example.fotozabawa.R
import com.example.fotozabawa.database.AppDatabase
import com.example.fotozabawa.databinding.FragmentStronaGlownaBinding
import com.example.fotozabawa.model.Id_folder
import com.example.fotozabawa.model.Ustawienia
import com.example.fotozabawa.upload.MyAPI
import com.example.fotozabawa.upload.UploadRequestBody
import com.example.fotozabawa.upload.UploadResponse
import kotlinx.coroutines.*
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class StronaGlownaFragment : Fragment(), UploadRequestBody.UploadCallback {
    private lateinit var appDatabase: AppDatabase
    private var _binding: FragmentStronaGlownaBinding? = null
    private val binding get() = _binding!!
    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    private var list_paths = arrayListOf<String>()
    private var session = false
    var mediaPlayer: MediaPlayer?=null



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentStronaGlownaBinding.inflate(inflater, container, false)
        appDatabase = AppDatabase.getDatabase(requireContext())
        return binding.root
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        outputDirectory = getOutputDirectory()

        //----------------------JE??LI APKA DOSTA??A WSZYSTKIE POTRZEBNE POZWOLENIA------------------
        if(allPermissionGranted()){
            startCamera()
            runBlocking(Dispatchers.IO) {
                launch {
                    //----------------------USTAWIENIA POCZ??TKOWE------------------
                    if (appDatabase.ustawieniaDao().exists()==false) {
                        runBlocking(Dispatchers.IO) {
                            appDatabase.ustawieniaDao().deleteAll()
                            val ustawienie = Ustawienia(1, 1, 2,1, "space",1)
                            appDatabase.ustawieniaDao().insert(ustawienie)
                        }
                    }
                    //----------------------FOLDER POCZ??TKOWY------------------
                    if(appDatabase.id_folderDao().exists()==false){
                        runBlocking (Dispatchers.IO){
                            appDatabase.id_folderDao().insert(Id_folder(1))
                        }
                    }
                }
            }
            //----------------------JE??LI APKA NIE DOSTA??A WSZYSTKICH POTRZEBNYCH POZWOLE??------------------
        }else{
            Toast.makeText(activity?.applicationContext,"Permissions requested", Toast.LENGTH_SHORT).show()
            activity?.let {
                ActivityCompat.requestPermissions(
                    it,arrayOf(Manifest.permission.CAMERA),123)
            }
        }


        //----------------------PIKANIE------------------
        val toneGen1 = ToneGenerator(AudioManager.STREAM_MUSIC, 10000)

        val buttonStart = view.findViewById<Button>(R.id.button_start)
        buttonStart.setOnClickListener {

            //----------------------JE??LI NIE JEST JU?? ODPALONA SEJSA ZDJ????------------------
            if(session==false){
                session=true
                playAudio()
                list_paths.clear()

                //----------------------PIKANIE PRZY ROBIENIU ZDJ???? ORAZ WYKONYWANIE ZDJ????------------------
                GlobalScope.launch(Dispatchers.IO) {
                    val czas_number = async { appDatabase.ustawieniaDao().getCzas() }
                    val tryb_number = async { appDatabase.ustawieniaDao().getTryb() }
                    launch {
                        for (x in 1..tryb_number.await()) {
                            for (y in 1..czas_number.await()) {
                                toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 150)
                                delay(1000)
                            }
                            toneGen1.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 150) //TONE_CDMA_HIGH_L
                            delay(1000)
                            takePhoto()
                        }
                        toneGen1.startTone(ToneGenerator.TONE_CDMA_LOW_SSL, 500)
                        sendimage()
                    }
                }
            }
        }
        //----------------------PRZEJ??CIE DO MENU------------------
        val myButton = view.findViewById<Button>(R.id.button_menu)
        myButton.setOnClickListener {
            if(session==false) {
                val fragment: Fragment = MenuFragment()
                val fragmentManager = requireActivity().supportFragmentManager
                val fragmentTransaction = fragmentManager.beginTransaction()
                fragmentTransaction.replace(R.id.frameLayout, fragment)
                fragmentTransaction.addToBackStack(null)
                fragmentTransaction.commit()
                requireActivity().title = "Menu"
            }
        }
    }

    //----------------------ZATRZYMANIE MUZYKI ORAZ WYWO??ANIE FUNKCJI WYSY??AJ??CEJ ZDJ??CIA NA SERWER------------------
    suspend fun sendimage() {
            pauseAudio()
            delay(5000)
            uploadImages()
        //----------------------AKTUALIZACJA NUMERU FOLDERU------------------
            runBlocking(Dispatchers.IO) {
                val x = appDatabase.id_folderDao().getiD()
                appDatabase.id_folderDao().update(x + 1)
            }
            session = false
    }

    //----------------------FUNKCJA ODPALAJ??CA MUZYK??------------------
    fun playAudio(){
        var position = 0

        runBlocking (Dispatchers.IO){ position = appDatabase.ustawieniaDao().getPiosenka_position() }

        //----------------------WYBRANIE MUZYKI------------------
        if(position==0){ mediaPlayer = MediaPlayer.create(requireContext(),R.raw.pumpedup)}
        else if(position==1){ mediaPlayer = MediaPlayer.create(requireContext(),R.raw.crab)}
        else if(position==2){ mediaPlayer = MediaPlayer.create(requireContext(),R.raw.gandalf)}
        else if(position==3){mediaPlayer = MediaPlayer.create(requireContext(),R.raw.wham_last_christmas)}
        else if(position==4){mediaPlayer = MediaPlayer.create(requireContext(),R.raw.amogus)}
        else if(position==5){mediaPlayer = MediaPlayer.create(requireContext(),R.raw.nowaera)}
        else if(position==6){mediaPlayer = MediaPlayer.create(requireContext(),R.raw.vii)}
        else if(position==7){mediaPlayer = MediaPlayer.create(requireContext(),R.raw.starwars)}
        else if(position==8){mediaPlayer = MediaPlayer.create(requireContext(),R.raw.wideputin)}
        try{
            mediaPlayer!!.start()
        }catch(e: IOException){
            e.printStackTrace()
        }
    }

    //----------------------FUNKCJA ZATRZYMUJ??CA MUZYK??------------------
    fun pauseAudio(){
        if(mediaPlayer!!.isPlaying){
            mediaPlayer!!.stop()
            mediaPlayer!!.reset()
            mediaPlayer!!.release()
        }
    }

    override fun onProgressUpdate(percentage: Int) {}

    //----------------------FUNKCJA WYSY??AJ??CA ZDJ??CIA NA SERWER------------------
    @SuppressLint("Recycle")
    private fun uploadImages(){
        val image1: File
        val image2: File
        val image3: File
        val image4: File
        val image5: File
        val image6: File
        val size = list_paths.size

        //----------------------JE??LI WYBRANO JEDNO ZDJ??CIE------------------
        if(size==1){
            //<------ INICJOWANIE WARTO??CI POCZ??TKOWYCH---->//
            val name = list_paths[0].subSequence(69, list_paths[0].length)
            val parcelFileDescriptor = requireContext().contentResolver.openFileDescriptor(list_paths[0].toUri(), "r", null) ?: return
            image1= File(requireContext().cacheDir, name.toString())
            val inputStream1 = FileInputStream(parcelFileDescriptor.fileDescriptor)
            val outputStream1 = FileOutputStream(image1)
            //<---- PRZYPISYWANIE ---->//
            image2 = image1;image3=image1;image4=image1;image5=image1;image6=image1;
            val inputStream2=inputStream1;val inputStream3=inputStream1;val inputStream4=inputStream1;val inputStream5=inputStream1;val inputStream6=inputStream1;
            val outputStream2=outputStream1;val outputStream3=outputStream1;val outputStream4=outputStream1;val outputStream5=outputStream1;val outputStream6=outputStream1;
            inputStream1.copyTo(outputStream1);inputStream2.copyTo(outputStream2);inputStream3.copyTo(outputStream3);inputStream4.copyTo(outputStream4);inputStream5.copyTo(outputStream5);inputStream6.copyTo(outputStream6)
        }
        //----------------------JE??LI WYBRANO 2 ZDJ??CIA------------------
        else if(size == 2){
            //<------ INICJOWANIE WARTO??CI POCZ??TKOWYCH---->//

            val name1 = list_paths[0].subSequence(69, list_paths[0].length)
            val parcelFileDescriptor1 = requireContext().contentResolver.openFileDescriptor(list_paths[0].toUri(), "r", null) ?: return
            val name2 = list_paths[1].subSequence(69, list_paths[1].length)
            val parcelFileDescriptor2 = requireContext().contentResolver.openFileDescriptor(list_paths[1].toUri(), "r", null) ?: return

            image1= File(requireContext().cacheDir, name1.toString());image2=image1;image3=image1;
            val inputStream1 = FileInputStream(parcelFileDescriptor1.fileDescriptor)
            val outputStream1 = FileOutputStream(image1)

            image4= File(requireContext().cacheDir, name2.toString());image5=image4;image6=image4;
            val inputStream4 = FileInputStream(parcelFileDescriptor2.fileDescriptor)
            val outputStream4 = FileOutputStream(image4)

                    //<---- PRZYPISYWANIE ---->//

            val inputStream2=inputStream1; val inputStream3=inputStream1;   val inputStream5 = inputStream4; val inputStream6=inputStream4;
            val outputStream2 = outputStream1; val outputStream3 = outputStream1;   val outputStream5 = outputStream4;val outputStream6=outputStream4;
            inputStream1.copyTo(outputStream1);inputStream2.copyTo(outputStream2);inputStream3.copyTo(outputStream3);inputStream4.copyTo(outputStream4); inputStream5.copyTo(outputStream5); inputStream6.copyTo(outputStream6)

        }
        //----------------------JE??LI WYBRANO 3 ZDJ??CIA------------------
        else if(size==3){
            //<------ INICJOWANIE WARTO??CI POCZ??TKOWYCH---->//
            val name1 = list_paths[0].subSequence(69, list_paths[0].length)
            val parcelFileDescriptor1 = requireContext().contentResolver.openFileDescriptor(list_paths[0].toUri(), "r", null) ?: return

            val name2 = list_paths[1].subSequence(69, list_paths[1].length)
            val parcelFileDescriptor2 = requireContext().contentResolver.openFileDescriptor(list_paths[1].toUri(), "r", null) ?: return

            val name3 = list_paths[2].subSequence(69, list_paths[2].length)
            val parcelFileDescriptor3 = requireContext().contentResolver.openFileDescriptor(list_paths[2].toUri(), "r", null) ?: return
            //<------ INICJOWANIE I PRZYPISYWANIE---->//
            image1= File(requireContext().cacheDir, name1.toString());image2=image1;
            val inputStream1 = FileInputStream(parcelFileDescriptor1.fileDescriptor); val inputStream2 = inputStream1;
            val outputStream1 = FileOutputStream(image1); val outputStream2 = outputStream1;

            image3= File(requireContext().cacheDir, name2.toString());image4=image3;
            val inputStream3 = FileInputStream(parcelFileDescriptor2.fileDescriptor); val inputStream4 = inputStream3;
            val outputStream3 = FileOutputStream(image3); val outputStream4 = outputStream3;

            image5= File(requireContext().cacheDir, name3.toString());image6=image5;
            val inputStream5 = FileInputStream(parcelFileDescriptor3.fileDescriptor); val inputStream6 = inputStream5;
            val outputStream5 = FileOutputStream(image5); val outputStream6=outputStream5;
            inputStream1.copyTo(outputStream1);inputStream2.copyTo(outputStream2);inputStream3.copyTo(outputStream3);inputStream4.copyTo(outputStream4); inputStream5.copyTo(outputStream5); inputStream6.copyTo(outputStream6)

        }
        //----------------------JE??LI WYBRANO 6 ZDJ????------------------
        else{

            val name1 = list_paths[0].subSequence(69, list_paths[0].length)
            val parcelFileDescriptor1 = requireContext().contentResolver.openFileDescriptor(list_paths[0].toUri(), "r", null) ?: return

            val name2 = list_paths[1].subSequence(69, list_paths[1].length)
            val parcelFileDescriptor2 = requireContext().contentResolver.openFileDescriptor(list_paths[1].toUri(), "r", null) ?: return

            val name3 = list_paths[2].subSequence(69, list_paths[2].length)
            val parcelFileDescriptor3 = requireContext().contentResolver.openFileDescriptor(list_paths[2].toUri(), "r", null) ?: return

            val name4 = list_paths[3].subSequence(69, list_paths[3].length)
            val parcelFileDescriptor4 = requireContext().contentResolver.openFileDescriptor(list_paths[3].toUri(), "r", null) ?: return

            val name5 = list_paths[4].subSequence(69, list_paths[4].length)
            val parcelFileDescriptor5 = requireContext().contentResolver.openFileDescriptor(list_paths[4].toUri(), "r", null) ?: return

            val name6 = list_paths[5].subSequence(69, list_paths[5].length)
            val parcelFileDescriptor6 = requireContext().contentResolver.openFileDescriptor(list_paths[5].toUri(), "r", null) ?: return

            image1= File(requireContext().cacheDir, name1.toString())
            val inputStream1 = FileInputStream(parcelFileDescriptor1.fileDescriptor)
            val outputStream1 = FileOutputStream(image1)
            inputStream1.copyTo(outputStream1)

            image2= File(requireContext().cacheDir, name2.toString())
            val inputStream2 = FileInputStream(parcelFileDescriptor2.fileDescriptor)
            val outputStream2 = FileOutputStream(image2)
            inputStream2.copyTo(outputStream2)

            image3= File(requireContext().cacheDir, name3.toString())
            val inputStream3 = FileInputStream(parcelFileDescriptor3.fileDescriptor)
            val outputStream3 = FileOutputStream(image3)
            inputStream3.copyTo(outputStream3)

            image4= File(requireContext().cacheDir, name4.toString())
            val inputStream4 = FileInputStream(parcelFileDescriptor4.fileDescriptor)
            val outputStream4 = FileOutputStream(image4)
            inputStream4.copyTo(outputStream4)

            image5= File(requireContext().cacheDir, name5.toString())
            val inputStream5 = FileInputStream(parcelFileDescriptor5.fileDescriptor)
            val outputStream5 = FileOutputStream(image5)
            inputStream5.copyTo(outputStream5)

            image6= File(requireContext().cacheDir, name6.toString())
            val inputStream6 = FileInputStream(parcelFileDescriptor6.fileDescriptor)
            val outputStream6 = FileOutputStream(image6)
            inputStream6.copyTo(outputStream6)
        }


            val body1 = UploadRequestBody(image1, "image", this)
            val body2 = UploadRequestBody(image2, "image", this)
            val body3 = UploadRequestBody(image3, "image", this)
            val body4 = UploadRequestBody(image4, "image", this)
            val body5 = UploadRequestBody(image5, "image", this)
            val body6 = UploadRequestBody(image6, "image", this)

        Log.d("Zdj??cie1---------------",image1.toString())
        Log.d("Zdj??cie2---------------",image2.toString())
        Log.d("Zdj??cie3---------------",image3.toString())
        Log.d("Zdj??cie4---------------",image4.toString())
        Log.d("Zdj??cie5---------------",image5.toString())
        Log.d("Zdj??cie6---------------",image6.toString())

        //----------------------POBRANIE WYBRANEGO BANERU ORAZ NR FOLDERU------------------
        runBlocking(Dispatchers.IO) {
            launch {
                val id = async{appDatabase.id_folderDao().getiD()}
                val banner_selected = async{appDatabase.ustawieniaDao().get_banner()}
                var number2 = ""; var number3 = ""; var number4 = ""; var number5 = ""; var number6 = ""
                when (size) {
                    1 -> {number2 = "1"; number3="1"; number4 = "1"; number5 = "1"; number6 = "1";}
                    2 -> {number2 = "1"; number3="1"; number4 = "2"; number5 = "2"; number6 = "2";}
                    3 -> {number2 = "2"; number3="3"; number4 = "1"; number5 = "2"; number6 = "3";}
                    6 -> {number2 = "2"; number3="3"; number4 = "4"; number5 = "5"; number6 = "6";}
                }
                //----------------------WYSY??ANIE DANYCH------------------
                MyAPI().uploadImage(
                    RequestBody.create(
                        MediaType.parse("multipart/form-data"),
                        "folder" + id.await().toString()
                    ),
                    MultipartBody.Part.createFormData("image1", "image1.jpg", body1),
                    MultipartBody.Part.createFormData("image2", "image${number2}.jpg", body2),
                    MultipartBody.Part.createFormData("image3", "image${number3}.jpg", body3),
                    MultipartBody.Part.createFormData("image4", "image${number4}.jpg", body4),
                    MultipartBody.Part.createFormData("image5", "image${number5}.jpg", body5),
                    MultipartBody.Part.createFormData("image6", "image${number6}.jpg", body6),
                    RequestBody.create(MediaType.parse("multipart/form-data"), banner_selected.await())
                ).enqueue(object : Callback<UploadResponse> {
                    override fun onResponse(
                        call: Call<UploadResponse>,
                        response: Response<UploadResponse>
                    ) {}

                    override fun onFailure(call: Call<UploadResponse>, t: Throwable) {
                        Log.d("B????D---------      ", t.message!!)
                    }
                })
            }
        }
    }


    private fun getOutputDirectory(): File{
        val mediaDir = activity?.externalMediaDirs?.firstOrNull()?.let{ mFile ->
            File(mFile, resources.getString(R.string.app_name)).apply{
                mkdirs()
            }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else activity?.filesDir!!
    }

    //----------------------ROBIENIE ZDJ??CIA------------------
    private fun takePhoto(){
        val imageCapture = imageCapture?:return
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(Constants.FILE_NAME_FORMAT, Locale.getDefault()).format(System.currentTimeMillis()) + ".jpg")

        val outputOption = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOption, ContextCompat.getMainExecutor(requireContext()),
            object :ImageCapture.OnImageSavedCallback{
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    //----------------------ZAPISYWANIE URI DO NASZEJ LISTY------------------
                    list_paths.add(savedUri.toString())

                }
                override fun onError(exception: ImageCaptureException) {
                    Log.d(Constants.TAG, "onError: ${exception.message}",exception)
                }
            }
        )
    }

    //----------------------URUCHOMIENIE KAMERY------------------
    private fun startCamera(){
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { mPreview->
                mPreview.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try{
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            }catch (e:Exception){
                Log.d(Constants.TAG, "startCamera Fail:", e)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    //----------------------POZWOLENIA------------------
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if(requestCode == Constants.REQUEST_CODE_PERMISSIONS){
            if(allPermissionGranted()){
                startCamera()
            }else{
                Toast.makeText(requireContext(),"permissions not granted by the user", Toast.LENGTH_SHORT).show()
            }

        }
    }
    private fun allPermissionGranted()= Constants.REQUIRED_PERMISSIONS.all{
        ContextCompat.checkSelfPermission(requireActivity().baseContext,it)== PackageManager.PERMISSION_GRANTED
    }

}





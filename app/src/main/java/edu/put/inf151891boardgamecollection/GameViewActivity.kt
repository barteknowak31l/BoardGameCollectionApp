package edu.put.inf151891boardgamecollection


import android.content.ContentResolver
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.util.Objects
import java.util.UUID


class GameViewActivity : AppCompatActivity() {

    val dbHandler = DBHandler(this,null,null,1)
    var itemId = 0
    private var rows = 0


    lateinit var gameImageView: ImageView
    lateinit var gameTitleView: TextView
    lateinit var gameYearView: TextView
    lateinit var gameRankView: TextView
    lateinit var gameIdView: TextView

    lateinit var imageTable: TableLayout
    lateinit var addImageButton: Button
    lateinit var cameraButton: Button


    lateinit var imageUri: Uri

    var mGetContent = registerForActivityResult(ActivityResultContracts.GetContent()){ result ->

        try{
            val state = Environment.getExternalStorageState()
            Log.i("err",state)
            if(result != null &&Environment.MEDIA_MOUNTED.equals(state))
            {
                val uid = UUID.randomUUID()
                addImageToList(result)
                val name = itemId.toString() + "_" + uid.toString()
                val bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), result)
                saveExternal(name,bitmap)
                addImageToNamesString(result,uid)
            }
        }
        catch(e:java.lang.Exception)
        {
            Log.i("err",Log.getStackTraceString(e))
        }

    }



    object BitmapScaler{
        fun scaleToFitWidth(b: Bitmap, width: Int): Bitmap{
            val factor = width / b.width.toFloat()
            return  Bitmap.createScaledBitmap(b,width,(b.height*factor).toInt(),true)
        }
        fun scaleToFitHeight(b: Bitmap, height: Int): Bitmap{
            val factor = height / b.height.toFloat()
            return  Bitmap.createScaledBitmap(b,(b.width*factor).toInt(),height,true)
        }
    }

    private fun getCapturedImage(selectedPhotoUri: Uri,width: Int): Bitmap{
        val bitmap = when{
            Build.VERSION.SDK_INT < 28 -> MediaStore.Images.Media.getBitmap(this.contentResolver,selectedPhotoUri)
            else ->{
                val source = ImageDecoder.createSource(this.contentResolver,selectedPhotoUri)
                ImageDecoder.decodeBitmap(source)
            }
        }
        return BitmapScaler.scaleToFitWidth(bitmap,width)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        itemId = intent.getIntExtra("id",0)


        setContentView(R.layout.activity_game_view)


        gameImageView = findViewById(R.id.gameImage)
        gameTitleView = findViewById(R.id.gameTitle)
        gameYearView = findViewById(R.id.gameYear)
        gameRankView = findViewById(R.id.gameRank)
        gameIdView = findViewById(R.id.gameId)

        addImageButton = findViewById(R.id.addImage)
        addImageButton.setOnClickListener{ mGetContent.launch("image/*")}
        cameraButton = findViewById(R.id.cameraButton)
        imageTable = findViewById(R.id.imageTable)


        setTextFieldsAndImage()


        val tempImageUri = initTempUri()
        val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()){

            try{
                val state = Environment.getExternalStorageState()
                if(it && Environment.MEDIA_MOUNTED.equals(state))
                {
                    val uid = UUID.randomUUID()
                    addImageToList(tempImageUri)
                    val name = itemId.toString() + "_" + uid.toString()
                    val bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), tempImageUri)
                    saveExternal(name,bitmap)
                    addImageToNamesString(tempImageUri,uid)
                }
            }
            catch (e:java.lang.Exception)
            {
                Log.i("err",Log.getStackTraceString(e))
            }
        }


        cameraButton.setOnClickListener{
            takePictureLauncher.launch(tempImageUri)}

        buildImageList()


    }



    fun setTextFieldsAndImage()
    {
        val gameData = dbHandler.findGameByID(itemId)
        try {
            if (gameData != null) {
                gameTitleView.text = "Tytuł: "+gameData.title
                gameYearView.text = "Rok wydania: "+gameData.year_pub.toString()
                gameRankView.text = "Ranking: "+gameData.rank_pos.toString()
                gameIdView.text = "ID: "+gameData.id.toString()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val url = URL(gameData.pic)
                        val image = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                        withContext(Dispatchers.Main) {

                            gameImageView.setImageBitmap(BitmapScaler.scaleToFitWidth(image,600))
                        }
                    } catch (e: java.lang.Exception) {
                        withContext(Dispatchers.Main) {
                            gameImageView.setImageDrawable(resources.getDrawable(R.drawable.placeholderbig))
                            Log.i("err", Log.getStackTraceString(e))
                        }
                    }

                }

            }
            else throw java.lang.NullPointerException()
        }
        catch (e: NullPointerException)
        {
            Log.i("err",Log.getStackTraceString(e))
        }
        catch (e: java.lang.Exception)
        {
            Log.i("err",Log.getStackTraceString(e))
        }


    }



    fun buildImageList()
    {
        imageTable.removeAllViews()
        val gameData = dbHandler.findGameByID(itemId)
        val names = gameData?.img_names

        try{
            val names_split = names?.split(",")
            if( names_split != null)
            {
                for(name in names_split)
                {

                    try
                    {
                        val path = Environment.getExternalStorageDirectory().toString() +"/Pictures/"+ name +".jpg"
                        val file = File(path)
                        val urii = Uri.fromFile(file)
                        addImageToList(urii)
                    }
                    catch(e:java.lang.Exception)
                    {
                        Log.i("err",Log.getStackTraceString(e))
                    }
                }
            }
            else throw NullPointerException()


        }
        catch (e: NullPointerException)
        {
            Log.e("err",Log.getStackTraceString(e))
        }
        catch (e: Exception)
        {
            Log.e("err",Log.getStackTraceString(e))
        }

    }

    fun addImageToList(result: Uri?)
    {
        if(result == null) return


        imageUri = result

        val leftRowMargin = 0
        val topRowMargin = 0
        val rightRowMargin = 0
        val bottomRowMargin = 0

        val thumbnail = ImageView(this)
        thumbnail.layoutParams = TableRow.LayoutParams(
            TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.MATCH_PARENT)

        thumbnail.setImageURI(result)
        thumbnail.setImageBitmap(getCapturedImage(result,400))


        thumbnail.setOnClickListener{onImageClick(it)}

        val tr = TableRow(this)
        tr.id = rows
        rows++
        val trParams = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT,
            TableLayout.LayoutParams.MATCH_PARENT)
        trParams.setMargins(leftRowMargin, topRowMargin, rightRowMargin, bottomRowMargin)
        tr.setPadding(10, 0, 10, 0)
        tr.layoutParams = trParams
        tr.addView(thumbnail)
        imageTable.addView(tr, trParams)

    }


    fun deleteImages(v: View)
    {
        imageTable.removeAllViews()
        val gameData = dbHandler.findGameByID(itemId)
        val names = gameData?.img_names

        try{
            val names_split = names?.split(",")
            if( names_split != null)
            {
                for(name in names_split)
                {

                    try
                    {
                        val path = Environment.getExternalStorageDirectory().toString() +"/Pictures/"+ name +".jpg"
                        val file = File(path)
                        if(file.exists()) {
                            file.delete()
                            gameData.img_names = ""
                            dbHandler.updateNamesString(gameData)
                        }
                    }
                    catch(e:java.lang.Exception)
                    {
                        Log.i("err",Log.getStackTraceString(e))
                    }
                }
            }
            else throw NullPointerException()
        }
        catch (e: NullPointerException)
        {
            Log.e("err",Log.getStackTraceString(e))
        }
        catch (e: Exception)
        {
            Log.e("err",Log.getStackTraceString(e))
        }
    }


    fun saveExternal(name:String, bmp: Bitmap):Boolean
    {
        var imageCollection:Uri? = null

        val resolver: ContentResolver = contentResolver


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            imageCollection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
        else
        {
            imageCollection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }


        var content = ContentValues()
        content.put(MediaStore.Images.Media.DISPLAY_NAME,name+".jpg")
        content.put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg")
        val imageUri = resolver.insert(imageCollection,content)

        try{
          val  OutputStream = Objects.requireNonNull(imageUri)
              ?.let { resolver.openOutputStream(it) }
            bmp.compress(Bitmap.CompressFormat.JPEG,100, OutputStream)
            Objects.requireNonNull(OutputStream)
            return true

        }
        catch (e: Exception)
        {
            Toast.makeText(this, "Image not saved", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
        return false



    }

    fun addImageToNamesString(uri: Uri?, uid: UUID)
    {
        var gameData = dbHandler.findGameByID(itemId)
        var name_str = gameData?.img_names

        if(name_str != null){
            var newName = itemId.toString() + "_" + uid.toString()
            if(name_str == "")
            {
                name_str = newName
            }
            else
            {
                name_str = name_str + "," + newName
            }

            if (gameData != null) {
                gameData.img_names = name_str
                dbHandler.updateNamesString(gameData)
            }


        }

    }


    private fun initTempUri():Uri{

        val tempImagesDir = File(applicationContext.filesDir,getString(R.string.temp_images_dir))
        tempImagesDir.mkdir()
        val tempImage = File(tempImagesDir,getString(R.string.temp_image))
        return FileProvider.getUriForFile(applicationContext,getString(R.string.authorities),tempImage)

    }


    fun onImageClick(v:View)
    {
        val i = Intent(this,ImageViewActivity::class.java)
        i.putExtra("uri",imageUri.toString())
        startActivity(i)
    }





}
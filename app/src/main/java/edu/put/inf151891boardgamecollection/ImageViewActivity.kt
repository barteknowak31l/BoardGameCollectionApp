package edu.put.inf151891boardgamecollection

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView


class ImageViewActivity : AppCompatActivity() {

    lateinit var backButton:Button
    lateinit var image:ImageView
    lateinit var uri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_view)

        backButton=findViewById(R.id.backButton)
        image=findViewById(R.id.imageView)
        backButton.setOnClickListener{backOnClick(it)}

        val uriStr = intent.getStringExtra("uri")
        uri = Uri.parse(uriStr)
        setImage()


    }


    fun backOnClick(v: View)
    {
       finish()
    }

    fun setImage()
    {
        val bitmap = when{
            Build.VERSION.SDK_INT < 28 -> MediaStore.Images.Media.getBitmap(this.contentResolver,uri)
            else ->{
                val source = ImageDecoder.createSource(this.contentResolver,uri)
                ImageDecoder.decodeBitmap(source)
            }
        }

        image.setImageBitmap(GameViewActivity.BitmapScaler.scaleToFitWidth(bitmap,800))
    }



}
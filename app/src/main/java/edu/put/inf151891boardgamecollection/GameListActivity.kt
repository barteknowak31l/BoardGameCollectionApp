package edu.put.inf151891boardgamecollection

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.net.URL

class GameListActivity : AppCompatActivity() {

    val dbHandler = DBHandler(this,null,null,1)

    var lGameData = ArrayList<GameData>()

    lateinit var tbl: TableLayout

    lateinit var mode:itemTypes

    var howMuchToShow = 50
    var currentFirstToShow = 0

    lateinit var currentFirstToShowInputField: TextView
    lateinit var sortByNameButton: Button
    lateinit var sortByYearButton: Button

    lateinit var headerText: TextView



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_list)

        val modeStr = intent.getStringExtra("mode")

        headerText = findViewById(R.id.header)

        when(modeStr)
        {
            "games" ->{
                headerText.text = "Lista posiadanych gier"
                mode = itemTypes.GAME
            }
            "expansions" -> {
                headerText.text = "Lista posiadanych dodatków"
                mode = itemTypes.EXPANSION
            }
            else -> {
                headerText.text = "Lista posiadanych gier"
                mode = itemTypes.GAME
            }
        }



        tbl = findViewById(R.id.tblLayout)
        currentFirstToShowInputField = findViewById(R.id.editTextNumberDecimal2)
        sortByNameButton = findViewById(R.id.sortByTitle)
        sortByYearButton = findViewById(R.id.sortByYear)

        selectGames(R.id.sortByTitle)


    }

    fun sortOnClick(v:View)
    {
        currentFirstToShow = currentFirstToShowInputField.text.toString().toIntOrNull()?: 0
        if(currentFirstToShow == null)
        {
            currentFirstToShow= 0
        }
        if(currentFirstToShow -1 >=0)
        {
            currentFirstToShow--
        }

        selectGames(v.id)
    }


    fun selectGames(sortId: Int)
    {
        Toast.makeText(applicationContext, "Rozpoczynam sortowanie, to może chwilę potrwać:", Toast.LENGTH_SHORT).show()
        disableButton(sortByNameButton)
        disableButton(sortByYearButton)
        currentFirstToShowInputField.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            when (sortId) {
                R.id.sortByTitle -> {
                    lGameData = dbHandler?.selectAll(mode, "title")!!
                }

                R.id.sortByYear -> {
                    lGameData = dbHandler?.selectAll(mode, "year_pub")!!
                }

                else -> {
                    lGameData = dbHandler?.selectAll(mode, "lol")!!
                }
            }

            withContext(Dispatchers.Main) {
                displayList()
                Toast.makeText(applicationContext, "Sortowanie zakończone", Toast.LENGTH_SHORT).show()
                enableButton(sortByNameButton)
                enableButton(sortByYearButton)
                currentFirstToShowInputField.isEnabled = true
            }
        }
    }

    fun displayList()
    {

        tbl.removeAllViews()

        val leftRowMargin = 0
        val topRowMargin = 0
        val rightRowMargin = 0
        val bottomRowMargin = 0

        var first = currentFirstToShow
        var howMuch = currentFirstToShow + howMuchToShow
        if (howMuch > lGameData.size -1) {
            howMuch = lGameData.size - 1
            first =  lGameData.size - 1 - howMuchToShow
        }
        if(first <0) first = 0

        //NAGŁOWEK
        val headNO = TextView(this)
        headNO.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT)
        headNO.text = "Pozycja"
        headNO.gravity = Gravity.LEFT
        headNO.setBackgroundColor(Color.parseColor("#D7D7D7"))
        headNO.setPadding(20, 15, 20, 15)

        val headThumbnail = TextView(this)
        headThumbnail.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
            TableRow.LayoutParams.WRAP_CONTENT)
        headThumbnail.text = "Miniaturka"
        headThumbnail.gravity = Gravity.CENTER
        headThumbnail.setBackgroundColor(Color.parseColor("#D7D7D7"))
        headThumbnail.setPadding(20, 15, 20, 15)

        val headDesc = TextView(this)
        headDesc.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
            TableRow.LayoutParams.WRAP_CONTENT)
        headDesc.text = "Opis"
        headDesc.gravity = Gravity.LEFT
        headDesc.setBackgroundColor(Color.parseColor("#D7D7D7"))
        headDesc.setPadding(20, 15, 20, 15)

        val headTr = TableRow(this)
        headTr.id = -1
        val headTrParams = TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT,
            TableLayout.LayoutParams.WRAP_CONTENT)
        headTrParams.setMargins(leftRowMargin, topRowMargin, rightRowMargin, bottomRowMargin)
        headTr.setPadding(10, 0, 10, 0)
        headTr.layoutParams = headTrParams
        headTr.addView(headNO)

        headTr.addView(headThumbnail)
        headTr.addView(headDesc)
        tbl.addView(headTr, headTrParams)

        //TABELA



        for( i in first.. howMuch)
        {

            val no = TextView(this)
            val thumbnail = ImageView(this)
            val desc = TextView(this)

            no.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.MATCH_PARENT)
            no.text = (i+1).toString()
            no.gravity = Gravity.CENTER
            no.gravity = Gravity.CENTER
            no.setBackgroundColor(Color.parseColor("#B2B2B0"))
            no.setPadding(20, 15, 20, 15)
            no.setOnClickListener{ listItemOnClick(no)}
            no.setTag(R.string.tag_id,lGameData[i].id)


            thumbnail.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                         val url = URL(lGameData[i].pic)
                         val image = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                         withContext(Dispatchers.Main) {
                            //thumbnail.setImageBitmap(GameViewActivity.BitmapScaler.scaleToFitHeight(image,200))

                            if(image!= null)
                            {
                                thumbnail.setImageBitmap(image)
                            }
                            else{
                                thumbnail.setImageDrawable(resources.getDrawable(R.drawable.placeholder))
                            }
                        }


                    } catch (e: Exception) {
                        thumbnail.setImageDrawable(resources.getDrawable(R.drawable.placeholder))
                        Log.i("err", Log.getStackTraceString(e))
                        }
                }



            thumbnail.setBackgroundColor(Color.parseColor("#Eeee17"))
            thumbnail.setPadding(20, 15, 20, 15)
            thumbnail.setOnClickListener{ listItemOnClick(thumbnail)}
            thumbnail.setTag(R.string.tag_id,lGameData[i].id)


            val game = lGameData[i]
            val description = "Tytuł: ${game.title}\nRok wydania: ${game.year_pub}"

            desc.layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.MATCH_PARENT)
            desc.text = description
            desc.gravity = Gravity.CENTER_VERTICAL
            desc.setBackgroundColor(Color.parseColor("#B2B2B0"))
            desc.setPadding(20, 15, 20, 15)
            desc.setOnClickListener{ listItemOnClick(desc)}
            desc.setTag(R.string.tag_id,lGameData[i].id)


            val tr = TableRow(this)
            tr.id = i+1
            val trParams = TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT,
                TableLayout.LayoutParams.WRAP_CONTENT)
            trParams.setMargins(leftRowMargin, topRowMargin, rightRowMargin, bottomRowMargin)
            tr.setPadding(10, 0, 10, 0)
            tr.layoutParams = trParams


            tr.addView(no)
            tr.addView(thumbnail)
            tr.addView(desc)


            tbl.addView(tr, trParams)
        }
    }


    fun listItemOnClick(v:View)
    {
        val i = Intent(this,GameViewActivity::class.java)
        i.putExtra("id",v.getTag(R.string.tag_id) as Int)
        startActivity(i)
    }
    fun disableButton(button: Button)
    {
        button.isEnabled = false
        button.isClickable = false
    }
    fun enableButton(button: Button)
    {
        try {
            button.isEnabled = true
            button.isClickable = true
        }
        catch (e:java.lang.Exception){
            Log.i("err",Log.getStackTraceString(e))
        }
    }


}
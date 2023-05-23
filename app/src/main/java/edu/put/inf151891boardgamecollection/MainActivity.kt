package edu.put.inf151891boardgamecollection

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import kotlin.system.exitProcess
import android.Manifest
import android.widget.Button



class GameData{
    var id: Int = 0
    var title: String? = null
    var year_pub: Int = 2001
    var rank_pos: Int = 0
    var pic: String? = null
    var expansion: Int = 0
    var img_names: String = ""

    constructor()

    constructor(
        id: Int,
        title: String?,
        year_pub: Int,
        rank_pos: Int,
        pic: String?,
        exp: Int
    ) {
        this.id = id
        this.title = title
        this.year_pub = year_pub
        this.rank_pos = rank_pos
        this.pic = pic
        this.expansion = exp
    }

    constructor(
        id: Int,
        title: String?,
        year_pub: Int,
        rank_pos: Int,
        pic: String?,
        exp: Int,
        names: String
    ) {
        this.id = id
        this.title = title
        this.year_pub = year_pub
        this.rank_pos = rank_pos
        this.pic = pic
        this.expansion = exp
        this.img_names = names
    }

}

enum class itemTypes{
    GAME,EXPANSION
}

class MainActivity : AppCompatActivity() {

    var prefs: SharedPreferences? = null
    var itemTypeNames = arrayOf<String>("boardgame","boardgameexpansion")

    val dbHandler = DBHandler(this,null,null,1)


    var nickname = ""
    var gamesOwned = 0
    var expansionsOwned = 0
    lateinit var lastSync: Date

    lateinit var nicknameField: TextView
    lateinit var gamesOwnedField: TextView
    lateinit var expansionsOwnedField: TextView
    lateinit var lastSynchField: TextView

    lateinit var gameListButton: Button
    lateinit var expListButton: Button
    lateinit var syncDataButton: Button
    lateinit var clearDataButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // views
        nicknameField = findViewById(R.id.nickDisplay)
        gamesOwnedField = findViewById(R.id.gamesOwned)
        expansionsOwnedField = findViewById(R.id.expansionsOwned)
        lastSynchField = findViewById(R.id.lastSynch)

        //buttons
        gameListButton = findViewById(R.id.gameListButton)
        expListButton = findViewById(R.id.expansionListButton)
        syncDataButton = findViewById(R.id.syncButton)
        clearDataButton = findViewById(R.id.clearDataButton)

        prefs = getSharedPreferences("edu.put.boardgamecollection", MODE_PRIVATE);
        lastSync = Date(prefs!!.getLong("lastSync", 0))



        if (prefs!!.getBoolean("setup", true)) {

            setupDialog()
            prefs!!.edit().putBoolean("setup", false).commit()
        }
        else
        {
            nickname = prefs!!.getString("nickname", "default") ?: "Not set"

            setTextFields()
        }

        expListButton.setOnClickListener{expansionListOnClick(it)}

        setupPermissions()


    }

    private val READ_REQUEST_CODE = 111
    private val WRITE_REQUEST_CODE = 112
    private fun setupPermissions()
    {
        val permission = ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)
        if(permission != PackageManager.PERMISSION_GRANTED)
        {
            makeRequest(READ_REQUEST_CODE)
        }

        val permission2 = ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)


        if(permission2 != PackageManager.PERMISSION_GRANTED)
        {
            makeRequest(WRITE_REQUEST_CODE)
        }



    }
    private fun makeRequest(code: Int)
    {

        when(code)
        {
            READ_REQUEST_CODE ->requestPermissions( arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),code)
            WRITE_REQUEST_CODE -> requestPermissions( arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),code)
        }

    }

    override fun onRequestPermissionsResult( // ta funkcja chyba jest nie potrzebna
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            READ_REQUEST_CODE -> {
                if(grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED){
                }
            }

            WRITE_REQUEST_CODE -> {
                if(grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED){
                }
            }

        }

    }



    fun setupDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        builder.setTitle("Wprowadź nick")
        val dialogLayout = inflater.inflate(R.layout.alert_dialog_with_edittext, null)
        val editText  = dialogLayout.findViewById<EditText>(R.id.editText)
        builder.setView(dialogLayout)
        builder.setPositiveButton("OK") { dialogInterface, i ->

            nickname = editText.text.toString()
            prefs!!.edit().putString("nickname", nickname).commit();
            getGameInfo(nickname,true)

        }
        builder.show()
    }


    fun deleteAppDataDialog(v:View)
    {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Czy na pewno chcesz usunąć dane?")
        builder.setMessage("Konieczna będzie ponowna konfiguracja")

        builder.setPositiveButton("Tak") { _, _ ->
            deleteAppData()
        }

        builder.setNegativeButton("Nie") { dialog, which ->
        }

        builder.show()
    }

    fun deleteAppData()
    {
        dbHandler.createEmptyTable()
        prefs!!.edit().putBoolean("setup", true).commit()
        prefs!!.edit().putString("nickname", "").commit()
        this@MainActivity.finish()
        exitProcess(0)

    }


    fun synchOnClick(v: View)
    {
        val currentTime = Calendar.getInstance().time
        val diff = currentTime.time - lastSync.time
        if (diff <  3600000)
        {
            synchDataDialog()
        }
        else{
            getGameInfo(nickname,false)
        }

    }

    fun synchDataDialog()
    {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Czas od ostatniej synchronizacji jest krótszy niż 1 dzień")
        builder.setMessage("Czy na pewno chcesz ponownie synchronizować dane?")

        builder.setPositiveButton("Tak") { _, _ ->
            getGameInfo(nickname,false)
        }

        builder.setNegativeButton("Nie") { dialog, which ->
        }
        builder.show()
    }

    fun gameListOnClick(v: View)
    {
        val i = Intent(this,GameListActivity::class.java)
        i.putExtra("mode","games")
        startActivity(i)
    }

    fun expansionListOnClick(v: View)
    {
        val i = Intent(this,GameListActivity::class.java)
        i.putExtra("mode","expansions")
        startActivity(i)
    }

    fun setTextFields()
    {
        gamesOwned = dbHandler.selectAll(itemTypes.GAME,"").size
        expansionsOwned = dbHandler.selectAll(itemTypes.EXPANSION,"").size

        nicknameField.text = "Witaj $nickname!"
        gamesOwnedField.text = "Posiadane gry: $gamesOwned"
        expansionsOwnedField.text = "Posiadane dodatki: $expansionsOwned"
        var formatter = DateTimeFormatter.ofPattern("dd-MMMM-yyyy hh:mm:ss")
        val format = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a")
        val formattedDate = format.format(lastSync)
        lastSynchField.text = "Ostatnia synchronizacja:\n$formattedDate"

        if(gamesOwned>0)
            enableButton(gameListButton)
        else
            disableButton(gameListButton)

        if(expansionsOwned > 0)
            enableButton(expListButton)
        else
            disableButton(expListButton)

    }

    // API CALL
    fun getGameInfo(nickname: String, deleteOldTable: Boolean)
    {

        Toast.makeText(applicationContext, "Rozpoczynam synchronizację gier\n Może to chwilę potrwać:", Toast.LENGTH_SHORT).show()
        disableButton(gameListButton)
        disableButton(expListButton)
        disableButton(syncDataButton)
        disableButton(clearDataButton)

        if(deleteOldTable)
            dbHandler.createEmptyTable()

        val urlStringGames = "https://boardgamegeek.com/xmlapi2/collection?username=$nickname&stats=1&excludesubtype=boardgameexpansion"
        val urlStringExpansions = "https://boardgamegeek.com/xmlapi2/collection?username=$nickname&stats=1&subtype=boardgameexpansion"

        val xmlDirectory = File("$filesDir/XML")
        if(!xmlDirectory.exists()) xmlDirectory.mkdir()
        val fileName = "$xmlDirectory/$nickname-gamedata.xml"

        CoroutineScope(Dispatchers.IO).launch{
            try{
                var url = URL(urlStringGames)
                var reader = url.openStream().bufferedReader()
                var downloadFile = File(fileName).also { it.createNewFile()}
                var writer = FileWriter(downloadFile).buffered()
                var line : String

                while(reader.readLine().also { line = it?.toString() ?:""} != null)
                {
                   // Log.i("test",line.toString())
                    writer.write(line)
                }
                reader.close()
                writer.close()

                withContext(Dispatchers.Main){
                    lastSync = Calendar.getInstance().time
                    prefs!!.edit().putLong("lastSync",lastSync.time).commit()
                    loadGameData(nickname)
                    gamesOwned = dbHandler.selectAll(itemTypes.GAME,"").size
                    Toast.makeText(applicationContext, "Synchronizacja gier zakończona\nRozpoczynam synchronizację dodatków $nickname:", Toast.LENGTH_SHORT).show()
                }


                url = URL(urlStringExpansions)
                reader = url.openStream().bufferedReader()
                downloadFile = File(fileName)
                writer = FileWriter(downloadFile).buffered()

                while(reader.readLine().also { line = it?.toString() ?:""} != null)
                {
                    // Log.i("test",line.toString())
                    writer.write(line)
                }
                reader.close()
                writer.close()

                withContext(Dispatchers.Main){

                    lastSync = Calendar.getInstance().time
                    prefs!!.edit().putLong("lastSync",lastSync.time).commit()
                    loadGameData(nickname)
                    expansionsOwned = dbHandler.selectAll(itemTypes.EXPANSION,"").size
                    setTextFields()
                    Toast.makeText(applicationContext, "Synchronizacja dodatków zakończona\nZalogowano jako $nickname:", Toast.LENGTH_SHORT).show()

                }


            }
            catch (e: Exception)
            {
                Log.i("err",e.toString())
                withContext(Dispatchers.Main)
                {

                    Toast.makeText(applicationContext, "Synchronizacja nie powiodłą się\nSpróbuj ponownie później:", Toast.LENGTH_SHORT).show()
                    when(e){
                        is MalformedURLException ->
                            print("Malformed URL")
                        else ->
                            print("error")
                    }
                }
                val incompleteFile = File(fileName)
                if(incompleteFile.exists()) incompleteFile.delete()

            }
            withContext(Dispatchers.Main) {

                if(gamesOwned>0)
                    enableButton(gameListButton)

                if(expansionsOwned > 0)
                    enableButton(expListButton)

                enableButton(syncDataButton)
                enableButton(clearDataButton)
            }
        }
    }

    // PARSE XML
    fun loadGameData(name: String)
    {
        val filename = "$name-gamedata.xml"
        val filePath = "$filesDir/XML/$filename"
        val file = File(filePath)

        if(file.exists())
        {
            val pullParserFactory: XmlPullParserFactory
            try{
                pullParserFactory = XmlPullParserFactory.newInstance()
                val parser = pullParserFactory.newPullParser()
                val inputStream = file.inputStream()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,false)
                parser.setInput(inputStream,null)


                // parsing code here
                var eventType = parser.eventType
                var data: GameData? = null

                while(eventType != XmlPullParser.END_DOCUMENT)
                {
                    var name = ""
                    when(eventType){
                        XmlPullParser.START_TAG ->{
                            name = parser.name
                            if(name == "item"){
                                data = GameData()
                                data.id = parser.getAttributeValue(null,"objectid").toInt()
                                val type = parser.getAttributeValue(null,"subtype")

                                if(type == itemTypeNames[itemTypes.GAME.ordinal])
                                {
                                    data.expansion =0
                                }
                                else if(type == itemTypeNames[itemTypes.EXPANSION.ordinal])
                                {
                                    data.expansion =1
                                }
                                else
                                {
                                    data = null
                                }

                            }
                            if(name == "name" && data != null)
                            {
                                data.title = parser.nextText()
                            }
                            if(name == "yearpublished" && data != null)
                            {
                                data.year_pub = parser.nextText().toInt()
                            }
                            if(name == "thumbnail" && data != null)
                            {

                                data.pic = parser.nextText()

                            }

                            if(name == "rank" && data != null)
                            {
                             if(parser.getAttributeValue(null,"type") == "subtype")
                                 if(parser.getAttributeValue(null,"name") == "boardgame")
                                {
                                    val rank = parser.getAttributeValue(null,"value").toIntOrNull()
                                    if(rank != null)
                                        data.rank_pos = rank
                                }
                            }


                        }


                        XmlPullParser.END_TAG ->
                        {
                            name = parser.name
                            if(name.equals("item",ignoreCase = true) && data != null)
                            {
                                dbHandler.addGame(data)
                                data = GameData()
                            }
                        }
                    }
                    eventType = parser.next()

                }

            }
            catch(e: XmlPullParserException){
                Log.getStackTraceString(e)
                e.printStackTrace()
            }
            catch(e: IOException)
            {            Log.i("err",Log.getStackTraceString(e))
                 e.printStackTrace()
            }
            catch(e:Exception)
            {
                           Log.i("err",Log.getStackTraceString(e))
                    e.printStackTrace()
            }

        }
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



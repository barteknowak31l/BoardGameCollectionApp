package edu.put.inf151891boardgamecollection

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.io.File
import java.io.Serializable


class DBHandler(context: Context, name: String?, factory: SQLiteDatabase.CursorFactory?, version: Int): SQLiteOpenHelper(context, DATABASE_NAME,factory,DATABASE_VERSION), Serializable {

    val _context:Context = context


    companion object{
        private val DATABASE_VERSION =1
        private val DATABASE_NAME = "gamedataDB.db"
        val TABLE = "gamedata"
        val COLUMN_ID = "_id"
        val COLUMN_TITLE = "title"
        val COLUMN_YEAR_PUB = "year_pub"
        val COLUMN_RANK_POS = "rank_pos"
        val COLUMN_PICTURE = "pic"
        val COLUMN_EXPANSION = "exp"
        val COLUMN_IMAGE_NAMES = "img_names"
    }

    override fun onCreate(db: SQLiteDatabase) {

        // first, check if database exists, if no, create one
        val databaseFile: File = File(_context.getDatabasePath(DATABASE_NAME).toString())
        if(databaseFile.exists()) return

        createEmptyTable()

    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE)
        onCreate(db)
    }

    fun createEmptyTable()
    {

        Log.i("sql","TWORZE NOWY TABLE!!")

        this.writableDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE)

        val CREATE_TABLE = ("CREATE TABLE "+ TABLE + "(" + COLUMN_ID + " INTEGER PRIMARY KEY," + COLUMN_TITLE +" TEXT," + COLUMN_YEAR_PUB + " INTEGER," + COLUMN_RANK_POS + " INTEGER," +
                COLUMN_PICTURE + " TEXT," + COLUMN_EXPANSION +" INTEGER," + COLUMN_IMAGE_NAMES+ " TEXT DEFAULT ''" + ")")
        this.writableDatabase.execSQL(CREATE_TABLE)
    }


    fun addGame(gameData: GameData) {
        val values = ContentValues()
        values.put(COLUMN_ID, gameData.id)
        values.put(COLUMN_TITLE, gameData.title)
        values.put(COLUMN_YEAR_PUB, gameData.year_pub)
        values.put(COLUMN_RANK_POS, gameData.rank_pos)
        values.put(COLUMN_PICTURE, gameData.pic)
        values.put(COLUMN_EXPANSION, gameData.expansion)
        values.put(COLUMN_IMAGE_NAMES, "")

        val db = this.writableDatabase
        db.insert(TABLE, null, values)
        db.close()
    }

    fun findGameByTitle(title: String): GameData? {
        val query = "SELECT * FROM $TABLE WHERE $COLUMN_TITLE LIKE \"$title\""
        val db = this.writableDatabase
        val cursor = db.rawQuery(query,null)
        var gameData: GameData? = null

        if(cursor.moveToFirst())
        {
            val id = cursor.getInt(0)
            val t = cursor.getString(1)
            val year = cursor.getInt(2)
            val rank = cursor.getInt(3)
            val pic = cursor.getString(4)
            val exp = cursor.getInt(5)
            val uri = cursor.getString(6)

            gameData = GameData(id,t,year,rank,pic,exp,uri)
            cursor.close()
        }
        db.close()

        return gameData
    }

    fun findGameByID(id: Int): GameData? {
        val query = "SELECT * FROM $TABLE WHERE $COLUMN_ID = $id"
        val db = this.writableDatabase
        val cursor = db.rawQuery(query,null)
        var gameData: GameData? = null

        if(cursor.moveToFirst())
        {
            val id = cursor.getInt(0)
            val t = cursor.getString(1)
            val year = cursor.getInt(2)
            val rank = cursor.getInt(3)
            val pic = cursor.getString(4)
            val exp = cursor.getInt(5)
            val uri = cursor.getString(6)

            gameData = GameData(id,t,year,rank,pic,exp,uri)
            cursor.close()
        }
        db.close()

        return gameData
    }


    fun deleteGame(id: Int): Boolean{
        var result = false
        val query = "SELECT * FROM $TABLE WHERE $COLUMN_ID = $id"
        val db = this.writableDatabase

        val cursor = db.rawQuery(query,null)

        if(cursor.moveToFirst())
        {
            db.delete(TABLE, COLUMN_ID + "= $id",null)
            cursor.close()
            result = true
        }
        db.close()
        return result
    }


    fun selectAll(itemType: itemTypes, orderBy: String): ArrayList<GameData>
    {

        var data = ArrayList<GameData>()
        val expansion = itemType.ordinal
        var query: String

        if(orderBy == "title" || orderBy == "year_pub")
        {
            query = "SELECT * FROM $TABLE WHERE $COLUMN_EXPANSION = $expansion ORDER BY $orderBy"
        }
        else
        {
            query = "SELECT * FROM $TABLE WHERE $COLUMN_EXPANSION = $expansion"
        }

        val db = this.writableDatabase
        val cursor = db.rawQuery(query,null)

        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                var gameData: GameData
                val id = cursor.getInt(0)
                val t = cursor.getString(1)
                val year = cursor.getInt(2)
                val rank = cursor.getInt(3)
                val pic = cursor.getString(4)
                val exp = cursor.getInt(5)
                gameData = GameData(id,t,year,rank,pic,exp)
                data.add(gameData)
                cursor.moveToNext()


            }
        }

        return data

    }

    fun updateNamesString(data: GameData)
    {
        val id = data.id
        val uri_str = data.img_names
        val c = ContentValues()
        c.put(COLUMN_IMAGE_NAMES,uri_str)
        this.writableDatabase.update(TABLE,c, COLUMN_ID+" =$id",null)

    }



}
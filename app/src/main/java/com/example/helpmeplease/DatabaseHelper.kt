package com.example.helpmeplease

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import android.provider.ContactsContract

object FeedReaderContract {
    // Table contents are grouped together in an anonymous object.
    object FeedEntry : BaseColumns {
        const val TABLE_NAME = "helpers"
        const val COLUMN_NAME = "name"
        const val COLUMN_NUMBER = "number"
        const val COLUMN_RANK = "rank"
    }
}


private const val SQL_CREATE_ENTRIES =
    "CREATE TABLE ${FeedReaderContract.FeedEntry.TABLE_NAME} (" +
            "${BaseColumns._ID} INTEGER PRIMARY KEY," +
            "${FeedReaderContract.FeedEntry.COLUMN_NAME} TEXT," +
            "${FeedReaderContract.FeedEntry.COLUMN_NUMBER} TEXT, " +
            "${FeedReaderContract.FeedEntry.COLUMN_RANK} TEXT)"

private const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${FeedReaderContract.FeedEntry.TABLE_NAME}"


class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION){

    override fun onCreate(db: SQLiteDatabase?) {
        db!!.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db!!.execSQL(SQL_DELETE_ENTRIES)
        onCreate(db)
    }

    companion object {
        // If you change the database schema, you must increment the database version.
        const val DATABASE_NAME = "HelpContacts.db"
        const val DATABASE_VERSION = 1
    }

    fun removeContact(rank: String){
//        val query = "DELETE FROM ${FeedReaderContract.FeedEntry.TABLE_NAME}" +
//                " WHERE ${FeedReaderContract.FeedEntry.COLUMN_RANK}" +
//                " = $rank"
//
//        val cursor= writableDatabase.rawQuery(query, null)
//
//        cursor?.apply {
//            close()
//        }

        val selection = "${FeedReaderContract.FeedEntry.COLUMN_RANK} LIKE ?"
         // Specify arguments in placeholder order.
        val selectionArgs = arrayOf(rank)
        // Issue SQL statement.
        val deletedRows = writableDatabase.delete(FeedReaderContract.FeedEntry.TABLE_NAME, selection, selectionArgs)
    }

    //adds a contact to the database
    fun addContact(contact: Contact): Contact? {

        val values = ContentValues().apply {
            put(FeedReaderContract.FeedEntry.COLUMN_NAME, contact.name)
            put(FeedReaderContract.FeedEntry.COLUMN_NUMBER, contact.phoneNumber)
            put(FeedReaderContract.FeedEntry.COLUMN_RANK, contact.rank)
        }

        val id = writableDatabase.insert(FeedReaderContract.FeedEntry.TABLE_NAME, null, values)

        return if(id > 0) {
            refreshContactRanks()
            contact
        }else null
    }

    fun getContactByNumber(number: String): Contact?{

        var contact: Contact? = null

        val projection = arrayOf(BaseColumns._ID, FeedReaderContract.FeedEntry.COLUMN_NAME,
                FeedReaderContract.FeedEntry.COLUMN_NUMBER, FeedReaderContract.FeedEntry.COLUMN_RANK)

        // Filter results WHERE "title" = 'My Title'

        val sortOrder = FeedReaderContract.FeedEntry.COLUMN_RANK
        val selection = "${FeedReaderContract.FeedEntry.COLUMN_NUMBER} = ?"
        val selectionArgs = arrayOf(number)
        val cursor = readableDatabase.query(
                FeedReaderContract.FeedEntry.TABLE_NAME,   // The table to query
                projection,             // The array of columns to return (pass null to get all)
                selection,              // The columns for the WHERE clause
                selectionArgs,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                sortOrder               // The sort order
        )


        cursor?.apply {
            val nameIndex = cursor.getColumnIndex(FeedReaderContract.FeedEntry.COLUMN_NAME)
            val numberIndex = cursor.getColumnIndex(FeedReaderContract.FeedEntry.COLUMN_NUMBER)
            val rankIndex = cursor.getColumnIndex(FeedReaderContract.FeedEntry.COLUMN_RANK)


            if ( cursor.moveToFirst() ) {
                val name = cursor.getString(nameIndex)
                val rank = cursor.getString(rankIndex)

                contact = Contact(number, name, rank)
            }


            close()
        }

        return  contact

    }

    fun deleteAll() {
        writableDatabase.delete(FeedReaderContract.FeedEntry.TABLE_NAME,null, null)
    }

    fun deleteContact(number: String) : Boolean {

        val contactDeleted = getContactByNumber(number)
        var res = false

        if (contactDeleted != null) {
            res = writableDatabase.delete(FeedReaderContract.FeedEntry.TABLE_NAME,FeedReaderContract.FeedEntry.COLUMN_NUMBER + "=$number", null) > 0

            val contacts = getContacts()

            refreshContactRanks()

        }


        return res

    }

    private fun refreshContactRanks() {

        val contacts = getContacts()

        if(contacts.isNotEmpty() && contacts[0].rank.toInt() != 1){
            contacts[0].rank = "1"
            updateContactRank(contacts[0], "1")
        }

        println("Debug: Number of elements in contact list is ${contacts.size}")
        for(i in 1 until contacts.size){

            if(contacts[i - 1].rank.toInt() + 1 != contacts[i].rank.toInt()){
                println("${contacts[i].name} new rank is: ${contacts[i - 1].rank.toInt() + 1} ")
                updateContactRank(contacts[i], (contacts[i - 1].rank.toInt() + 1).toString())
                contacts[i].rank = (contacts[i - 1].rank.toInt() + 1).toString()
            }
        }

    }


    fun updateContactRank(contact: Contact, newRank: String): Boolean {

        var updated: Boolean =  false

        val values = ContentValues().apply {
            put(FeedReaderContract.FeedEntry.COLUMN_RANK, newRank)
        }

        val selection = "${FeedReaderContract.FeedEntry.COLUMN_NUMBER} LIKE ?"
        val selectionArgs = arrayOf(contact.phoneNumber)
        updated = writableDatabase.update(
                FeedReaderContract.FeedEntry.TABLE_NAME,
                values,
                selection,
                selectionArgs) > 0

        return updated
    }


    fun hasContact(name: String): Boolean {

//        val query = "SELECT * FROM ${FeedReaderContract.FeedEntry.TABLE_NAME}" +
//                " WHERE ${FeedReaderContract.FeedEntry.COLUMN_NAME}" +
//                " = $name"
        val projection = arrayOf(BaseColumns._ID, FeedReaderContract.FeedEntry.COLUMN_NAME,
            FeedReaderContract.FeedEntry.COLUMN_NUMBER, FeedReaderContract.FeedEntry.COLUMN_RANK)

        // Filter results WHERE "title" = 'My Title'
        val selection = "${FeedReaderContract.FeedEntry.COLUMN_NAME} = ?"
        val selectionArgs = arrayOf(name)

        val cursor = readableDatabase.query(
            FeedReaderContract.FeedEntry.TABLE_NAME,   // The table to query
            projection,             // The array of columns to return (pass null to get all)
            selection,              // The columns for the WHERE clause
            selectionArgs,          // The values for the WHERE clause
            null,                   // don't group the rows
            null,                   // don't filter by row groups
            null               // The sort order
        )

       //val cursor: Cursor? = readableDatabase.rawQuery(query, null)

       if (cursor != null && cursor.count > 0){
           cursor.close()
           return true
       }
        cursor?.apply {
            close()
        }

        return false
    }

    fun getContacts(): ArrayList<Contact>{
        val contacts = ArrayList<Contact>()

        val projection = arrayOf(BaseColumns._ID, FeedReaderContract.FeedEntry.COLUMN_NAME,
            FeedReaderContract.FeedEntry.COLUMN_NUMBER, FeedReaderContract.FeedEntry.COLUMN_RANK)

        // Filter results WHERE "title" = 'My Title'

        val sortOrder = FeedReaderContract.FeedEntry.COLUMN_RANK

        val cursor = readableDatabase.query(
            FeedReaderContract.FeedEntry.TABLE_NAME,   // The table to query
            projection,             // The array of columns to return (pass null to get all)
            null,              // The columns for the WHERE clause
            null,          // The values for the WHERE clause
            null,                   // don't group the rows
            null,                   // don't filter by row groups
            sortOrder               // The sort order
        )


        cursor?.apply {
            val nameIndex = cursor.getColumnIndex(FeedReaderContract.FeedEntry.COLUMN_NAME)
            val numberIndex = cursor.getColumnIndex(FeedReaderContract.FeedEntry.COLUMN_NUMBER)
            val rankIndex = cursor.getColumnIndex(FeedReaderContract.FeedEntry.COLUMN_RANK)


            while(moveToNext()){
                val name = cursor.getString(nameIndex)
                val number = cursor.getString(numberIndex)
                val rank = cursor.getString(rankIndex)

                contacts.add(Contact(number, name, rank))
            }

            close()
        }

        return  contacts
    }

}
package com.example.helpmeplease

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    private var access: Int = 0
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var contacts: ArrayList<Contact>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, Array(1){ Manifest.permission.READ_CONTACTS}, 111 )
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, Array(1){ Manifest.permission.ACCESS_COARSE_LOCATION}, 222 )
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, Array(1){ Manifest.permission.ACCESS_FINE_LOCATION}, 333 )
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, Array(1){ Manifest.permission.SEND_SMS}, 444 )
        }

        else {access = 1
            databaseHelper = DatabaseHelper(this)
            populateListView()
        }

    }

    fun startLocationService(view: View){
        println("Starting location service")
        var primaryContact: Contact? = null

        if(contacts.isEmpty()) {
            println("No contacts")
            return
        }

        contacts.forEach { contact ->
            if (contact.rank == "1"){
                primaryContact = contact
            }
        }

        if (primaryContact == null) {
            primaryContact = contacts[0]
        }

        val intent = Intent(this, SendLocationService::class.java).apply {
            putExtra("NUMBER", primaryContact!!.phoneNumber)
        }

        startService(intent)
    }

    private fun populateListView() {
        contacts = databaseHelper.getContacts()
        val arrayAdapter: ArrayAdapter<Contact> = ArrayAdapter(this, android.R.layout.simple_expandable_list_item_1, contacts)

        val listView = findViewById<ListView>(R.id.simpleList)
        listView.adapter = arrayAdapter
    }

    fun onClearList(view: View){

        databaseHelper.deleteAll()
        populateListView()

    }

    fun onSafe(view: View){
        val intent = Intent(this, SendLocationService::class.java)
        stopService(intent)
    }

    fun addContact(view: View){
        println("SEARCHING...")

        val textView = findViewById<TextView>(R.id.editTextTextPersonName)
        val textView2 = findViewById<TextView>(R.id.editTextTextPersonName2)

        val name = textView.text.toString()
        var rank = textView2.text.toString()

        if ((rank.toInt() < 1) or (rank.toInt() > 5)){

            println("Rank is out of Bounds")
            return

        }

        if (contacts.size < rank.toInt())
            rank = (contacts.size + 1).toString()

        val number = searchContact(name)


//        contacts.forEach { contact ->
//            if(contact.phoneNumber == number)
//                return
//        }

        if(number.isNotEmpty()){
            val contactDeleted = databaseHelper.deleteContact(number)

            if (contactDeleted) {
                println("Contact Deleted")
                populateListView()

                if (rank.toInt() > contacts.size){
                    rank = (contacts.size + 1).toString()
                }
            }

            val contact = Contact(number, name, rank)

            //databaseHelper.removeContact(rank)
            val res = databaseHelper.addContact(contact)

            if(res != null) {
                populateListView()
            }else {
                println("contact not added")
            }

        }else {
           println("Contact could not be Added")
        }

    }

    private fun searchContact(name: String): String{

        if (access == 1){
            println("Fetching contacts...")

            val hm = getContacts()

            if (hm.containsKey(name)){
                val number = hm[name]
                println("Contact $name has number $number")

                return number!![0]

            }else {
               Toast.makeText(this, "User $name not found :(", Toast.LENGTH_SHORT).show()

            }
            println("DONE")
        }else {
           Toast.makeText(this, "Access Denied no Permission", Toast.LENGTH_SHORT).show()
        }


        return ""
    }

    private fun getContacts(): HashMap<String, ArrayList<String>> {

        val hm = HashMap<String, ArrayList<String>>()

        /**
         * The ContentResolver methods provide the basic "CRUD" (create, retrieve, update, and delete) functions of persistent storage.
         *
         * Cursor: This interface provides random read-write access to the result set returned by a database query.
         */
        val cursor: Cursor? = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null)


        cursor?.apply {
            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)


            while(moveToNext()){
                val name = cursor.getString(nameIndex)
                val number = cursor.getString(numberIndex)

                if (hm.containsKey(name)){
                    hm[name]?.add(number)
                }else {
                    hm[name] = arrayListOf(number)
                }

            }

            close()
        }

        return  hm

    }



}
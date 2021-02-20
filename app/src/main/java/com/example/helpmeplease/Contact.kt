package com.example.helpmeplease

class Contact(number: String, contactName: String, priority: String) {
    var phoneNumber: String = number
    var name: String = contactName
    var rank: String = priority

    override fun toString(): String {
        return "$name | $phoneNumber | $rank"
    }
}
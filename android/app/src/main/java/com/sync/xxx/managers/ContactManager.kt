package com.sync.xxx.managers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.ContactsContract
import android.util.Log
import androidx.core.app.ActivityCompat
import org.json.JSONArray
import org.json.JSONObject

/**
 * ContactManager.kt
 * Access and manage device contacts
 * Read contact list with names, numbers, emails
 */
class ContactManager(private val context: Context) {

    private val TAG = "ContactManager"

    /**
     * Check if contacts permission is granted
     */
    fun hasPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get all contacts
     */
    fun getAllContacts(): List<Contact> {
        if (!hasPermission()) {
            Log.e(TAG, "Contacts permission not granted")
            return emptyList()
        }

        val contacts = mutableListOf<Contact>()
        val contentResolver = context.contentResolver

        val cursor: Cursor? = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null,
            null,
            null,
            ContactsContract.Contacts.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val hasPhoneIndex = it.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

            while (it.moveToNext()) {
                val id = it.getString(idIndex)
                val name = it.getString(nameIndex) ?: "Unknown"
                val hasPhone = it.getInt(hasPhoneIndex) > 0

                val phoneNumbers = if (hasPhone) getPhoneNumbers(id) else emptyList()
                val emails = getEmails(id)

                contacts.add(
                    Contact(
                        id = id,
                        name = name,
                        phoneNumbers = phoneNumbers,
                        emails = emails
                    )
                )
            }
        }

        Log.d(TAG, "Retrieved ${contacts.size} contacts")
        return contacts
    }

    /**
     * Get phone numbers for contact
     */
    private fun getPhoneNumbers(contactId: String): List<String> {
        val phoneNumbers = mutableListOf<String>()
        val contentResolver = context.contentResolver

        val cursor: Cursor? = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
            arrayOf(contactId),
            null
        )

        cursor?.use {
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val number = it.getString(numberIndex)
                if (number != null) {
                    phoneNumbers.add(number)
                }
            }
        }

        return phoneNumbers
    }

    /**
     * Get emails for contact
     */
    private fun getEmails(contactId: String): List<String> {
        val emails = mutableListOf<String>()
        val contentResolver = context.contentResolver

        val cursor: Cursor? = contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            null,
            ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
            arrayOf(contactId),
            null
        )

        cursor?.use {
            val emailIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
            while (it.moveToNext()) {
                val email = it.getString(emailIndex)
                if (email != null) {
                    emails.add(email)
                }
            }
        }

        return emails
    }

    /**
     * Search contacts by name
     */
    fun searchContacts(query: String): List<Contact> {
        return getAllContacts().filter { 
            it.name.contains(query, ignoreCase = true) 
        }
    }

    /**
     * Get contact by ID
     */
    fun getContactById(contactId: String): Contact? {
        return getAllContacts().find { it.id == contactId }
    }

    /**
     * Get contacts with phone numbers
     */
    fun getContactsWithPhones(): List<Contact> {
        return getAllContacts().filter { it.phoneNumbers.isNotEmpty() }
    }

    /**
     * Get contacts with emails
     */
    fun getContactsWithEmails(): List<Contact> {
        return getAllContacts().filter { it.emails.isNotEmpty() }
    }

    /**
     * Get total contact count
     */
    fun getContactCount(): Int {
        if (!hasPermission()) return 0

        val contentResolver = context.contentResolver
        val cursor: Cursor? = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts._ID),
            null,
            null,
            null
        )

        val count = cursor?.count ?: 0
        cursor?.close()

        return count
    }

    /**
     * Export contacts as JSON
     */
    fun getContactsAsJson(): JSONArray {
        val contacts = getAllContacts()
        val jsonArray = JSONArray()

        contacts.forEach { contact ->
            jsonArray.put(contact.toJson())
        }

        return jsonArray
    }

    /**
     * Export contacts as VCF (vCard) format
     */
    fun exportToVcf(): String {
        val contacts = getAllContacts()
        val vcfBuilder = StringBuilder()

        contacts.forEach { contact ->
            vcfBuilder.append("BEGIN:VCARD\n")
            vcfBuilder.append("VERSION:3.0\n")
            vcfBuilder.append("FN:${contact.name}\n")
            
            contact.phoneNumbers.forEach { phone ->
                vcfBuilder.append("TEL:$phone\n")
            }
            
            contact.emails.forEach { email ->
                vcfBuilder.append("EMAIL:$email\n")
            }
            
            vcfBuilder.append("END:VCARD\n\n")
        }

        return vcfBuilder.toString()
    }

    /**
     * Export contacts as CSV
     */
    fun exportToCsv(): String {
        val contacts = getAllContacts()
        val csvBuilder = StringBuilder()

        csvBuilder.append("Name,Phone Numbers,Emails\n")

        contacts.forEach { contact ->
            csvBuilder.append("\"${contact.name}\",")
            csvBuilder.append("\"${contact.phoneNumbers.joinToString("; ")}\",")
            csvBuilder.append("\"${contact.emails.joinToString("; ")}\"\n")
        }

        return csvBuilder.toString()
    }

    companion object {
        /**
         * Check if contacts permission is granted
         */
        fun hasPermission(context: Context): Boolean {
            return ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Data class for contact
     */
    data class Contact(
        val id: String,
        val name: String,
        val phoneNumbers: List<String>,
        val emails: List<String>
    ) {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("id", id)
                put("name", name)
                put("phoneNumbers", JSONArray(phoneNumbers))
                put("emails", JSONArray(emails))
            }
        }
    }
}

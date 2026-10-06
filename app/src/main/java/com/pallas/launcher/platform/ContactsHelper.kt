package com.pallas.launcher.platform

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.pallas.launcher.domain.model.ContactItem

object ContactsHelper {

    fun hasContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getAllContacts(context: Context): List<ContactItem> {
        if (!hasContactsPermission(context)) return emptyList()

        val contactList = mutableListOf<ContactItem>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"

        try {
            val cursor: Cursor? = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                sortOrder
            )

            cursor?.use {
                val idCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberCol = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                val seenNumbers = mutableSetOf<String>()
                while (it.moveToNext()) {
                    val id = if (idCol >= 0) it.getString(idCol) ?: "" else ""
                    val name = if (nameCol >= 0) it.getString(nameCol) ?: "" else ""
                    val number = if (numberCol >= 0) it.getString(numberCol) ?: "" else ""
                    val cleanNumber = number.replace("[\\s\\-\\(\\)]".toRegex(), "")

                    if (name.isNotBlank() && cleanNumber.isNotBlank() && seenNumbers.add(cleanNumber)) {
                        contactList.add(ContactItem(id = id, name = name, phoneNumber = number))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return contactList
    }
}

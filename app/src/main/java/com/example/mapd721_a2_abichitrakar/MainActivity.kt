package com.example.mapd721_a2_abichitrakar

import android.annotation.SuppressLint
import android.content.ContentResolver
import androidx.activity.compose.setContent
import android.content.ContentValues
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mapd721_a2_abichitrakar.ui.theme.MAPD721A2AbiChitrakarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MAPD721A2AbiChitrakarTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ContactsList(this)
                }
            }
        }
    }
}

@Composable
fun ContactsList(context: ComponentActivity) {
    var contactName by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }
    var contacts by remember { mutableStateOf(emptyList<Contact>()) }
    var showDialog by remember { mutableStateOf(false) }
    var contactToDelete by remember { mutableStateOf<Contact?>(null) }

    // LaunchedEffect to perform data loading
    LaunchedEffect(Unit) {
        // Load contacts
        contacts = loadContacts(context)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                value = contactName,
                onValueChange = { contactName = it },
                label = { Text(text = "Contact Name") }
            )

            OutlinedTextField(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
                value = contactNumber,
                onValueChange = { newInput ->
                    // Check if the input is a number
                    if (newInput.all { it.isDigit() }) {
                        contactNumber = newInput
                    }
                },
                label = { Text(text = "Contact Number") }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Button(onClick = {
                    if (contactName.isNotBlank() && contactNumber.isNotBlank()) {
                        val newContact = Contact("$contactName", "$contactNumber")
                        contacts = contacts + newContact
                        addContact(context, newContact)
                        contactName = ""
                        contactNumber = ""
                        contacts = loadContacts(context)
                    } else {
                        // Show validation message
                        showDialog = true
                    }
                }) {
                    Text("Add")
                }
            }

            Divider(modifier = Modifier.padding(vertical = 2.dp))

            if (contacts.isEmpty()) {
                Text(
                    text = "No Contacts Yet",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn() {
                    items(contacts) { contact ->
                        ContactItem(contact) {
                            // Set contact to delete and show confirmation dialog
                            contactToDelete = contact
                            showDialog = true
                            contacts = loadContacts(context)

                        }
                    }
                }
            }
        }

        // AboutSection always sticky at the bottom
        AboutSection()
    }


    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                // Dismiss the dialog without any action
                showDialog = false
            },
            title = { Text("Confirmation") },
            text = {
                if (contactToDelete != null) {
                    Text("Are you sure you want to delete ${contactToDelete!!.displayName}?")
                } else {
                    Text("Please provide both contact name and number.")
                }
            },
            confirmButton = {
                if (contactToDelete != null) {
                    Button(
                        onClick = {
                            contacts = contacts - contactToDelete!!
                            deleteContact(context.contentResolver, contactToDelete!!.id)
                            showDialog = false
                            contactToDelete = null
                        }
                    ) {
                        Text("Confirm")
                    }
                }
            },
            dismissButton = {
                if (contactToDelete != null) {
                    Button(
                        onClick = {
                            // Dismiss the dialog without any action
                            showDialog = false
                            contactToDelete = null
                        }
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        )
    }
}


@Composable
fun ContactItem(contact: Contact, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = contact.displayName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = contact.phoneNumber,
                fontSize = 16.sp,
                fontStyle = FontStyle.Italic)


        }
        Button(onClick = onDelete) {
            Text("Delete")
        }
    }
}

@Composable
fun AboutSection() {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)

    ) {
        Text("About Section:" )
        Text("Student Name: Abi Chitrakar", fontSize = 16.sp)
        Text("Student ID: 301369773", fontSize = 16.sp)
    }
}
data class Contact(val displayName: String, val phoneNumber: String, val id : Long?= null)


@SuppressLint("Range")

fun loadContacts(context: ComponentActivity): List<Contact> {
    val contacts = mutableListOf<Contact>()
    context.contentResolver.query(
        ContactsContract.Contacts.CONTENT_URI,
        arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY, ContactsContract.Contacts._ID),
        null,
        null,
    )?.use { cursor ->

        if (cursor.moveToFirst()) {
            do {
                val displayName =
                    cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY))
                val contactId = cursor.getLong(cursor.getColumnIndex(ContactsContract.Contacts._ID))

                // Query phone numbers associated with this contact
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(contactId.toString()),
                    null
                )?.use { phoneCursor ->
                    if (phoneCursor.moveToFirst()) {
                        val phoneNumber =
                            phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                        contacts.add(Contact(displayName, phoneNumber, contactId))
                    }
                }
            } while (cursor.moveToNext())
        }
        }
        return contacts
    }

fun addContact(context: ComponentActivity, contact: Contact) {
    try {
        val values = ContentValues().apply {
            put(ContactsContract.RawContacts.ACCOUNT_TYPE, "")
            put(ContactsContract.RawContacts.ACCOUNT_NAME, "")
        }

        val rawContactUri = context.contentResolver.insert(ContactsContract.RawContacts.CONTENT_URI, values)
        val rawContactId = rawContactUri?.lastPathSegment?.toLongOrNull()

        // Insert display name
        val displayNameValues = ContentValues().apply {
            put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
            put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
            put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.displayName)
        }
        context.contentResolver.insert(ContactsContract.Data.CONTENT_URI, displayNameValues)

        // Insert phone number
        val phoneNumberValues = ContentValues().apply {
            put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
            put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
            put(ContactsContract.CommonDataKinds.Phone.NUMBER, contact.phoneNumber)
            put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
        }
        context.contentResolver.insert(ContactsContract.Data.CONTENT_URI, phoneNumberValues)
    } catch (e: Exception) {
        // Handle the exception appropriately (e.g., log the error, display a message to the user)
        e.printStackTrace()
    }
}

fun deleteContact(contentResolver: ContentResolver, contactId: Long? = null) {
    val whereClause = "${ContactsContract.CommonDataKinds.Phone._ID} = ?"
    val whereArgs = arrayOf(contactId.toString())
    contentResolver.delete(
        ContactsContract.RawContacts.CONTENT_URI,
        whereClause,
        whereArgs
    )
}

@Preview(showBackground = true)
@Composable
fun ContactManagerAppPreview() {
    MAPD721A2AbiChitrakarTheme {
        ContactsList(ComponentActivity())
    }
}




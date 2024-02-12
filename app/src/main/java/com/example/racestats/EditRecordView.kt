package com.example.racestats

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class EditRecordView : AppCompatActivity() {
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_recored_view_ui)

        // Set the activity to full-screen mode
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)

        dbHelper = DatabaseHelper(this)

        // Get the data passed from the previous activity
        val serviceText = intent.getStringExtra("serviceText")
        val mileageText = intent.getStringExtra("mileageText")
        val dateText = intent.getStringExtra("dateText")
        val recordId = intent.getLongExtra("recordId", -1)

        // Find the UI values
        val serviceEditText = findViewById<EditText>(R.id.service_edittext)
        val mileageEditText = findViewById<EditText>(R.id.mileage_edittext)
        val dateEditText = findViewById<EditText>(R.id.date_edittext)
        // UI buttons
        val cancelButton = findViewById<Button>(R.id.cancel_edit_button)
        val saveButton = findViewById<Button>(R.id.save_changes_button)

        // Now you can use these values to display the details in the UI
        serviceEditText.text = Editable.Factory.getInstance().newEditable(serviceText ?: "")
        mileageEditText.text = Editable.Factory.getInstance().newEditable(mileageText ?: "")
        dateEditText.text = Editable.Factory.getInstance().newEditable(dateText ?: "")

        // Will return us back to maintenance records view
        cancelButton.setOnClickListener {
            finish()
        }

        // Will update DB and UI with new values and return back to Maintenance Records
        saveButton.setOnClickListener {
            val service = serviceEditText.text.toString()
            val mileage = mileageEditText.text.toString()
            val date = dateEditText.text.toString()

            // Check if any changes were made
            if (service == serviceText && mileage == mileageText && date == dateText) {
                // No changes were made, show toast message
                Toast.makeText(this, "No changes were made. Record not saved.", Toast.LENGTH_LONG).show()
            } else {
                // Changes were made, update the record
                updateRecordInDatabase(recordId, service, mileage, date)

                // Prepare the Intent to send the updated data back to ServiceRecords
                val resultIntent = Intent().apply {
                    putExtra("serviceText", service)
                    putExtra("mileageText", mileage)
                    putExtra("dateText", date)
                    putExtra("recordId", recordId)
                }

                // Set the result with the updated data and finish the activity
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    // Function to update the record in the database when the user saves changes
    private fun updateRecordInDatabase(recordId: Long, newService: String, newMileage: String, newDate: String) {
        val rowsAffected = dbHelper.updateRecord(recordId, newService, newMileage, newDate)
        if (rowsAffected > 0) {
            // Record updated successfully
            Toast.makeText(this, "Record Updated Successfully", Toast.LENGTH_LONG).show()
        } else {
            // Failed to update record
            Toast.makeText(this, "Failed to Update Record", Toast.LENGTH_LONG).show()
        }
    }
}

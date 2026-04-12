package com.example.geometka

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import com.example.geometka.data.Mark
import com.example.geometka.data.MarkConstants
import com.example.geometka.data.MarkDatabase

class EditMarkActivity : Activity() {

    private lateinit var database: MarkDatabase
    private var markId: Long = -1
    private var mark: Mark? = null

    private lateinit var nameInput: EditText
    private lateinit var objectTypeSpinner: Spinner
    private lateinit var fireHazardSpinner: Spinner
    private lateinit var waterAvailabilitySpinner: Spinner
    private lateinit var vehiclePassabilitySpinner: Spinner
    private lateinit var notesInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = MarkDatabase(this)
        markId = intent.getLongExtra("MARK_ID", -1)

        if (markId == -1L) {
            Toast.makeText(this, "Ошибка загрузки метки", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        mark = database.getMarkById(markId)
        if (mark == null) {
            Toast.makeText(this, "Метка не найдена", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1E1E1E"))
        }

        // Шапка
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 20, 20, 20)
            setBackgroundColor(Color.parseColor("#2A2A2A"))
            gravity = Gravity.CENTER_VERTICAL
        }

        val backButton = Button(this).apply {
            text = "← Назад"
            textSize = 14f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { finish() }
        }

        val titleText = TextView(this).apply {
            text = "✏️ Редактирование"
            textSize = 20f
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            gravity = Gravity.CENTER
        }

        headerLayout.addView(backButton)
        headerLayout.addView(titleText)
        headerLayout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(80, 0)
        })

        // Форма
        val formLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 30, 30, 30)
        }

        formLayout.addView(createLabel("Название метки:"))
        nameInput = createEditText(mark!!.name)
        formLayout.addView(nameInput)

        formLayout.addView(createLabel("Координаты (только для просмотра):"))
        val coordsText = TextView(this).apply {
            text = mark!!.getFormattedCoordinates()
            textSize = 14f
            setTextColor(Color.parseColor("#888888"))
            setPadding(20, 10, 20, 20)
        }
        formLayout.addView(coordsText)

        formLayout.addView(createLabel("Тип объекта:"))
        objectTypeSpinner = createSpinner(MarkConstants.OBJECT_TYPES, mark!!.objectType)
        formLayout.addView(objectTypeSpinner)

        formLayout.addView(createLabel("Класс пожарной опасности:"))
        fireHazardSpinner = createSpinner(MarkConstants.FIRE_HAZARD_CLASSES, mark!!.fireHazardClass)
        formLayout.addView(fireHazardSpinner)

        formLayout.addView(createLabel("Доступность воды:"))
        waterAvailabilitySpinner = createSpinner(MarkConstants.WATER_AVAILABILITY, mark!!.waterAvailability)
        formLayout.addView(waterAvailabilitySpinner)

        formLayout.addView(createLabel("Проходимость техники:"))
        vehiclePassabilitySpinner = createSpinner(MarkConstants.VEHICLE_PASSABILITY, mark!!.vehiclePassability)
        formLayout.addView(vehiclePassabilitySpinner)

        formLayout.addView(createLabel("Дополнительные заметки:"))
        notesInput = createEditText(mark!!.notes, multiline = true)
        formLayout.addView(notesInput)

        val saveButton = createStyledButton("💾 Сохранить изменения", "#4CAF50")
        saveButton.setOnClickListener { saveChanges() }
        formLayout.addView(saveButton)

        val scrollView = ScrollView(this).apply {
            addView(formLayout)
        }

        mainLayout.addView(headerLayout)
        mainLayout.addView(scrollView)

        setContentView(mainLayout)
    }

    private fun createLabel(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(Color.parseColor("#CCCCCC"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 10
                topMargin = 10
            }
        }
    }

    private fun createEditText(value: String, multiline: Boolean = false): EditText {
        return EditText(this).apply {
            setText(value)
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#333333"))
            setPadding(20, 20, 20, 20)
            if (multiline) {
                minLines = 3
                maxLines = 5
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
        }
    }

    private fun createSpinner(items: List<String>, selectedValue: String): Spinner {
        val spinner = Spinner(this)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        val position = items.indexOf(selectedValue)
        if (position >= 0) spinner.setSelection(position)

        spinner.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = 20
        }
        return spinner
    }

    private fun createStyledButton(text: String, color: String): Button {
        val buttonBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor(color))
            cornerRadius = 30f
        }

        return Button(this).apply {
            this.text = text
            textSize = 16f
            setTextColor(Color.WHITE)
            background = buttonBackground
            setPadding(60, 30, 60, 30)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 20
            }
        }
    }

    private fun saveChanges() {
        val name = nameInput.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Введите название метки!", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedMark = mark!!.copy(
            name = name,
            objectType = objectTypeSpinner.selectedItem.toString(),
            fireHazardClass = fireHazardSpinner.selectedItem.toString(),
            waterAvailability = waterAvailabilitySpinner.selectedItem.toString(),
            vehiclePassability = vehiclePassabilitySpinner.selectedItem.toString(),
            notes = notesInput.text.toString().trim()
        )

        val result = database.updateMark(updatedMark)
        if (result > 0) {
            Toast.makeText(this, "✓ Изменения сохранены!", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "✗ Ошибка сохранения", Toast.LENGTH_SHORT).show()
        }
    }
}
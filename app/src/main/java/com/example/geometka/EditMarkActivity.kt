package com.example.geometka

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.*
import com.example.geometka.data.*
import com.example.geometka.ui.UIHelper

class EditMarkActivity : Activity() {

    private lateinit var database: MarkDatabase
    private var mark: Mark? = null

    private lateinit var nameInput: EditText
    private lateinit var pointTypeSpinner: Spinner
    private lateinit var intensitySpinner: Spinner
    private lateinit var typeOfFireSpinner: Spinner
    private lateinit var notesInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = MarkDatabase(this)
        val markId = intent.getLongExtra("MARK_ID", -1)

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

        setContentView(createLayout())
    }

    private fun createLayout(): View {
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.parseColor(UIHelper.Colors.BACKGROUND))
        }

        rootLayout.addView(UIHelper.createHeader(this, "✏️ Редактирование") { finish() })

        val formLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 30, 30, 30)
        }

        val m = mark!!

        formLayout.addView(UIHelper.createLabel(this, "Название метки:"))
        nameInput = UIHelper.createEditText(this, value = m.name)
        formLayout.addView(nameInput)

        formLayout.addView(UIHelper.createLabel(this, "Координаты (только для просмотра):"))
        formLayout.addView(TextView(this).apply {
            text = m.getFormattedCoordinates()
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor(UIHelper.Colors.TEXT_DISABLED))
            setPadding(20, 10, 20, 20)
        })

        formLayout.addView(UIHelper.createLabel(this, "Часть пожара:"))
        val pointTypes = PointType.values().map { it.label }
        pointTypeSpinner = createSpinnerWithSelection(pointTypes, m.pointType.label)
        formLayout.addView(pointTypeSpinner)

        formLayout.addView(UIHelper.createLabel(this, "Интенсивность:"))
        val intensities = FireIntensity.values().map { it.label }
        intensitySpinner = createSpinnerWithSelection(intensities, m.intensity.label)
        formLayout.addView(intensitySpinner)

        formLayout.addView(UIHelper.createLabel(this, "Вид пожара:"))
        val fireTypes = FireType.values().map { it.label }
        typeOfFireSpinner = createSpinnerWithSelection(fireTypes, m.typeOfFire.label)
        formLayout.addView(typeOfFireSpinner)

        formLayout.addView(UIHelper.createLabel(this, "Дополнительные заметки:"))
        notesInput = UIHelper.createEditText(this, value = m.notes ?: "", multiline = true)
        formLayout.addView(notesInput)

        formLayout.addView(UIHelper.createButton(this, "💾 Сохранить изменения", UIHelper.Colors.SUCCESS) {
            saveChanges()
        })

        val scrollView = ScrollView(this).apply {
            addView(formLayout)
        }

        rootLayout.addView(scrollView)
        return rootLayout
    }

    private fun createSpinnerWithSelection(items: List<String>, selectedValue: String): Spinner {
        val spinner = UIHelper.createSpinner(this, items)
        val position = items.indexOf(selectedValue)
        if (position >= 0) spinner.setSelection(position)
        return spinner
    }

    private fun saveChanges() {
        val name = nameInput.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Введите название метки!", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedMark = mark!!.copy(
            name = name,
            pointType = PointType.values().find { it.label == pointTypeSpinner.selectedItem.toString() } ?: PointType.FRONT,
            intensity = FireIntensity.values().find { it.label == intensitySpinner.selectedItem.toString() } ?: FireIntensity.LOW,
            typeOfFire = FireType.values().find { it.label == typeOfFireSpinner.selectedItem.toString() } ?: FireType.GROUND,
            notes = notesInput.text.toString().trim()
        )

        if (database.updateMark(updatedMark) > 0) {
            Toast.makeText(this, "✓ Изменения сохранены!", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "✗ Ошибка сохранения", Toast.LENGTH_SHORT).show()
        }
    }
}

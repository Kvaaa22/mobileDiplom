package com.example.geometka

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.example.geometka.data.Mark
import com.example.geometka.data.MarkDatabase

class MarkListActivity : Activity() {

    private lateinit var database: MarkDatabase
    private lateinit var marksContainer: LinearLayout
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = MarkDatabase(this)

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
            text = "📋 Мои метки"
            textSize = 20f
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            gravity = Gravity.CENTER
        }

        headerLayout.addView(backButton)
        headerLayout.addView(titleText)
        headerLayout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(80, 0)
        })

        // Текст для пустого списка
        emptyText = TextView(this).apply {
            text = "Пока нет сохраненных меток\n\n📍 Создайте первую метку на главном экране"
            textSize = 16f
            setTextColor(Color.parseColor("#888888"))
            gravity = Gravity.CENTER
            visibility = View.GONE
            setPadding(40, 100, 40, 40)
        }

        // Контейнер для меток
        marksContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }

        val scrollView = ScrollView(this).apply {
            addView(marksContainer)
        }

        mainLayout.addView(headerLayout)
        mainLayout.addView(emptyText)
        mainLayout.addView(scrollView)

        setContentView(mainLayout)

        loadMarks()
    }

    private fun loadMarks() {
        marksContainer.removeAllViews()
        val marks = database.getAllMarks()

        if (marks.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            return
        }

        emptyText.visibility = View.GONE

        marks.forEach { mark ->
            marksContainer.addView(createMarkCard(mark))
        }
    }

    private fun createMarkCard(mark: Mark): View {
        val cardBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor("#2A2A2A"))
            cornerRadius = 20f
            setStroke(2, Color.parseColor("#444444"))
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground
            setPadding(20, 20, 20, 20)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 15
            }
        }

        // Название
        val nameText = TextView(this).apply {
            text = mark.name
            textSize = 18f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        // Дата
        val dateText = TextView(this).apply {
            text = "📅 ${mark.getFormattedDate()}"
            textSize = 12f
            setTextColor(Color.parseColor("#888888"))
            setPadding(0, 5, 0, 10)
        }

        // Координаты
        val coordsText = TextView(this).apply {
            text = "📍 ${mark.getFormattedCoordinates()}"
            textSize = 13f
            setTextColor(Color.parseColor("#AAAAAA"))
            setPadding(0, 0, 0, 10)
        }

        // Параметры
        val paramsText = TextView(this).apply {
            text = """
                ${mark.objectType}
                ${mark.fireHazardClass}
                ${mark.waterAvailability}
                ${mark.vehiclePassability}
            """.trimIndent()
            textSize = 13f
            setTextColor(Color.parseColor("#CCCCCC"))
            setPadding(0, 0, 0, 10)
        }

        // Заметки
        if (mark.notes.isNotEmpty()) {
            val notesText = TextView(this).apply {
                text = "💬 ${mark.notes}"
                textSize = 12f
                setTextColor(Color.parseColor("#999999"))
                setPadding(0, 0, 0, 10)
            }
            card.addView(notesText)
        }

        // Кнопки
        val buttonsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        val editButton = createSmallButton("✏️ Редактировать", "#2196F3")
        editButton.setOnClickListener {
            val intent = Intent(this, EditMarkActivity::class.java)
            intent.putExtra("MARK_ID", mark.id)
            startActivity(intent)
        }

        val deleteButton = createSmallButton("🗑️ Удалить", "#F44336")
        deleteButton.setOnClickListener {
            showDeleteDialog(mark)
        }

        buttonsLayout.addView(editButton)
        buttonsLayout.addView(deleteButton)

        card.addView(nameText)
        card.addView(dateText)
        card.addView(coordsText)
        card.addView(paramsText)
        card.addView(buttonsLayout)

        return card
    }

    private fun createSmallButton(text: String, color: String): Button {
        val buttonBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor(color))
            cornerRadius = 15f
        }

        return Button(this).apply {
            this.text = text
            textSize = 12f
            setTextColor(Color.WHITE)
            background = buttonBackground
            setPadding(20, 15, 20, 15)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = 10
            }
        }
    }

    private fun showDeleteDialog(mark: Mark) {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Удалить метку?")
        builder.setMessage("Вы уверены, что хотите удалить метку \"${mark.name}\"?")
        builder.setPositiveButton("Да") { _, _ ->
            database.deleteMark(mark.id)
            Toast.makeText(this, "Метка удалена", Toast.LENGTH_SHORT).show()
            loadMarks()
        }
        builder.setNegativeButton("Отмена", null)
        builder.show()
    }

    override fun onResume() {
        super.onResume()
        loadMarks()
    }
}
package com.example.geometka

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import com.example.geometka.data.Mark
import com.example.geometka.data.MarkDatabase
import com.example.geometka.ui.UIHelper

class MarkListActivity : Activity() {

    private lateinit var database: MarkDatabase
    private lateinit var marksContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        database = MarkDatabase(this)
        setContentView(createLayout())
        loadMarks()
    }

    private fun createLayout(): View {
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(UIHelper.Colors.BACKGROUND))
        }

        // Шапка
        rootLayout.addView(UIHelper.createHeader(this, "📋 Мои метки") { finish() })

        // Контейнер для меток
        marksContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }

        val scrollView = ScrollView(this).apply {
            addView(marksContainer)
        }

        rootLayout.addView(scrollView)
        return rootLayout
    }

    private fun loadMarks() {
        marksContainer.removeAllViews()
        val marks = database.getAllMarks()

        if (marks.isEmpty()) {
            marksContainer.addView(createEmptyView())
        } else {
            marks.forEach { mark ->
                marksContainer.addView(createMarkCard(mark))
            }
        }
    }

    private fun createEmptyView() = TextView(this).apply {
        text = "Пока нет сохраненных меток\n\n📍 Создайте первую метку на главном экране"
        textSize = 16f
        setTextColor(Color.parseColor(UIHelper.Colors.TEXT_DISABLED))
        gravity = Gravity.CENTER
        setPadding(40, 100, 40, 40)
    }

    private fun createMarkCard(mark: Mark): LinearLayout {
        val cardBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor(UIHelper.Colors.CARD_BACKGROUND))
            cornerRadius = 20f
            setStroke(2, Color.parseColor("#444444"))
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBg
            setPadding(20, 20, 20, 20)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 15 }

            // Название
            addView(TextView(this@MarkListActivity).apply {
                text = mark.name
                textSize = 18f
                setTextColor(Color.WHITE)
                setTypeface(null, android.graphics.Typeface.BOLD)
            })

            // Дата
            addView(TextView(this@MarkListActivity).apply {
                text = "📅 ${mark.getFormattedDate()}"
                textSize = 12f
                setTextColor(Color.parseColor(UIHelper.Colors.TEXT_DISABLED))
                setPadding(0, 5, 0, 10)
            })

            // Координаты
            addView(TextView(this@MarkListActivity).apply {
                text = "📍 ${mark.getFormattedCoordinates()}"
                textSize = 13f
                setTextColor(Color.parseColor(UIHelper.Colors.TEXT_SECONDARY))
                setPadding(0, 0, 0, 10)
            })

            // Параметры
            addView(TextView(this@MarkListActivity).apply {
                text = "${mark.objectType}\n${mark.fireHazardClass}\n${mark.waterAvailability}\n${mark.vehiclePassability}"
                textSize = 13f
                setTextColor(Color.parseColor("#CCCCCC"))
                setPadding(0, 0, 0, 10)
            })

            // Заметки
            if (mark.notes.isNotEmpty()) {
                addView(TextView(this@MarkListActivity).apply {
                    text = "💬 ${mark.notes}"
                    textSize = 12f
                    setTextColor(Color.parseColor("#999999"))
                    setPadding(0, 0, 0, 10)
                })
            }

            // Кнопки
            addView(createButtonsLayout(mark))
        }
    }

    private fun createButtonsLayout(mark: Mark): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END

            addView(createSmallButton("✏️ Редактировать", UIHelper.Colors.PRIMARY) {
                val intent = Intent(this@MarkListActivity, EditMarkActivity::class.java)
                intent.putExtra("MARK_ID", mark.id)
                startActivity(intent)
            })

            addView(createSmallButton("🗑️ Удалить", UIHelper.Colors.DANGER) {
                showDeleteDialog(mark)
            })
        }
    }

    private fun createSmallButton(text: String, color: String, onClick: () -> Unit): Button {
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor(color))
            cornerRadius = 15f
        }

        return Button(this).apply {
            this.text = text
            textSize = 12f
            setTextColor(Color.WHITE)
            background = bg
            setPadding(20, 15, 20, 15)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { leftMargin = 10 }
            setOnClickListener { onClick() }
        }
    }

    private fun showDeleteDialog(mark: Mark) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Удалить метку?")
            .setMessage("Вы уверены, что хотите удалить метку \"${mark.name}\"?")
            .setPositiveButton("Да") { _, _ ->
                database.deleteMark(mark.id)
                Toast.makeText(this, "Метка удалена", Toast.LENGTH_SHORT).show()
                loadMarks()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadMarks()
    }
}
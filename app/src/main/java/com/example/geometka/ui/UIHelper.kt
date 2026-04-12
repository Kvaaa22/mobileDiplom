package com.example.geometka.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.*

object UIHelper {

    // Цвета
    object Colors {
        const val BACKGROUND = "#1E1E1E"
        const val CARD_BACKGROUND = "#2A2A2A"
        const val INPUT_BACKGROUND = "#333333"
        const val PRIMARY = "#2196F3"
        const val SUCCESS = "#4CAF50"
        const val WARNING = "#FFA500"
        const val DANGER = "#F44336"
        const val TEXT_PRIMARY = "#FFFFFF"
        const val TEXT_SECONDARY = "#AAAAAA"
        const val TEXT_DISABLED = "#666666"
    }

    // Создание кнопки
    fun createButton(
        context: Context,
        text: String,
        color: String,
        onClick: () -> Unit
    ): Button {
        var background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor(color))
            cornerRadius = 30f
        }

        return Button(context).apply {
            this.text = text
            textSize = 16f
            setTextColor(Color.WHITE)
            background = background
            setPadding(60, 30, 60, 30)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 10
                bottomMargin = 10
            }
            setOnClickListener { onClick() }
        }
    }

    // Создание главной кнопки (большой)
    fun createMainButton(context: Context, text: String, onClick: () -> Unit): Button {
        var background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor(Colors.PRIMARY))
            cornerRadius = 50f
        }

        return Button(context).apply {
            this.text = text
            textSize = 18f
            setTextColor(Color.WHITE)
            background = background
            setPadding(80, 40, 80, 40)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 20
                bottomMargin = 20
            }
            setOnClickListener { onClick() }
        }
    }

    // Создание метки (label)
    fun createLabel(context: Context, text: String): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 14f
            setTextColor(Color.parseColor(Colors.TEXT_SECONDARY))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8
                topMargin = 10
            }
        }
    }

    // Создание поля ввода
    fun createEditText(
        context: Context,
        value: String = "",
        hint: String = "",
        multiline: Boolean = false
    ): EditText {
        return EditText(context).apply {
            setText(value)
            this.hint = hint
            setHintTextColor(Color.parseColor(Colors.TEXT_DISABLED))
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor(Colors.INPUT_BACKGROUND))
            setPadding(20, 20, 20, 20)
            if (multiline) {
                minLines = 3
                maxLines = 5
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 15
            }
        }
    }

    // Создание Spinner
    fun createSpinner(context: Context, items: List<String>): Spinner {
        val spinner = Spinner(context)
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = 15
        }
        return spinner
    }

    // Создание шапки экрана
    fun createHeader(context: Context, title: String, onBack: () -> Unit): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(20, 20, 20, 20)
            setBackgroundColor(Color.parseColor(Colors.CARD_BACKGROUND))
            gravity = Gravity.CENTER_VERTICAL

            val backButton = Button(context).apply {
                text = "← Назад"
                textSize = 14f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener { onBack() }
            }

            val titleText = TextView(context).apply {
                text = title
                textSize = 20f
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                gravity = Gravity.CENTER
            }

            addView(backButton)
            addView(titleText)
            addView(android.view.View(context).apply {
                layoutParams = LinearLayout.LayoutParams(80, 0)
            })
        }
    }

    // Создание элемента нижнего меню
    fun createMenuButton(
        context: Context,
        icon: String,
        label: String,
        selected: Boolean
    ): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(20, 10, 20, 10)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            isClickable = true
            isFocusable = true

            addView(TextView(context).apply {
                text = icon
                textSize = 24f
                gravity = Gravity.CENTER
            })

            addView(TextView(context).apply {
                text = label
                textSize = 11f
                setTextColor(if (selected) Color.parseColor(Colors.PRIMARY) else Color.parseColor(Colors.TEXT_DISABLED))
                gravity = Gravity.CENTER
            })
        }
    }

    // Создание разделителя
    fun createDivider(context: Context): android.view.View {
        return android.view.View(context).apply {
            setBackgroundColor(Color.parseColor("#333333"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2
            ).apply {
                topMargin = 20
                bottomMargin = 20
            }
        }
    }
}
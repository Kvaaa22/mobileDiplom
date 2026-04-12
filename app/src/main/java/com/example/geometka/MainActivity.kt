package com.example.geometka

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            gravity = Gravity.CENTER
        }

        val buttonBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor("#2196F3"))
            cornerRadius = 60f
        }

        val button = Button(this).apply {
            text = "Отставить метку"
            textSize = 18f
            setTextColor(Color.WHITE)
            background = buttonBackground

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 1300
            }

            setPadding(80, 40, 80, 40)

            setOnClickListener {
                Toast.makeText(
                    this@MainActivity,
                    "Метка отставлена!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        layout.addView(button)
        setContentView(layout)
    }
}
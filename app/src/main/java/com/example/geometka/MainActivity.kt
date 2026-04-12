package com.example.geometka

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }

        val button = Button(this).apply {
            text = "gg"
            textSize = 18f

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            setOnClickListener {
                Toast.makeText(
                    this@MainActivity,
                    "Geometka работает!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        layout.addView(button)
        setContentView(layout)
    }
}
package com.example.geometka

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var locationHelper: LocationHelper
    private lateinit var coordinatesText: TextView
    private lateinit var statusText: TextView
    private lateinit var methodText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationHelper = LocationHelper(this)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            gravity = Gravity.CENTER
        }

        // Информация о методе
        methodText = TextView(this).apply {
            text = "Доступно: ${locationHelper.getAvailableMethods()}"
            textSize = 12f
            setTextColor(Color.parseColor("#888888"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 10
            }
        }

        // Статус текст
        statusText = TextView(this).apply {
            text = if (locationHelper.hasInternetConnection())
                "✓ Готов (с интернетом)"
            else
                "✓ Готов (без интернета - только GPS)"
            textSize = 14f
            setTextColor(Color.parseColor("#4CAF50"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
        }

        // Индикатор загрузки
        progressBar = ProgressBar(this).apply {
            visibility = ProgressBar.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
        }

        // Текстовое поле для координат
        coordinatesText = TextView(this).apply {
            text = "Нажмите кнопку для получения координат"
            textSize = 16f
            setTextColor(Color.parseColor("#888888"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 40
            }
        }

        val buttonBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor("#2196F3"))
            cornerRadius = 60f
        }

        button = Button(this).apply {
            text = "Получить координаты"
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
                onMarkButtonClick()
            }
        }

        layout.addView(methodText)
        layout.addView(statusText)
        layout.addView(progressBar)
        layout.addView(coordinatesText)
        layout.addView(button)
        setContentView(layout)

        // Настройка callbacks
        locationHelper.onLocationReceived = { lat, lon, provider, isCached ->
            hideLoading()
            updateCoordinates(lat, lon, provider, isCached)
        }

        locationHelper.onLocationError = { message ->
            hideLoading()
            showError(message)
        }

        locationHelper.onStatusUpdate = { message ->
            statusText.text = message
            statusText.setTextColor(Color.parseColor("#FFA500"))
        }

        checkPermissions()
        updateMethodInfo()
    }

    private fun updateMethodInfo() {
        methodText.text = "Доступно: ${locationHelper.getAvailableMethods()}"

        val hasInternet = locationHelper.hasInternetConnection()
        val hasGps = locationHelper.isGpsEnabled()

        statusText.text = when {
            hasGps && hasInternet -> "✓ Готов (быстрый режим)"
            hasGps && !hasInternet -> "✓ Готов (медленный режим без сети)"
            else -> "⚠ GPS отключен"
        }

        statusText.setTextColor(when {
            hasGps && hasInternet -> Color.parseColor("#4CAF50")
            hasGps && !hasInternet -> Color.parseColor("#FFA500")
            else -> Color.parseColor("#F44336")
        })
    }

    private fun checkPermissions() {
        if (!locationHelper.hasLocationPermission()) {
            locationHelper.requestLocationPermission(this)
        }
    }

    private fun onMarkButtonClick() {
        if (!locationHelper.hasLocationPermission()) {
            Toast.makeText(this, "Нет разрешения на геолокацию", Toast.LENGTH_SHORT).show()
            locationHelper.requestLocationPermission(this)
            return
        }

        if (!locationHelper.isGpsEnabled() && !locationHelper.isNetworkEnabled()) {
            showGpsDisabledDialog()
            return
        }

        showLoading()
        // ВАЖНО: allowCached = false - только НОВЫЕ координаты!
        locationHelper.getCurrentLocation(allowCached = false)
    }

    private fun showLoading() {
        progressBar.visibility = ProgressBar.VISIBLE
        button.isEnabled = false
        coordinatesText.setTextColor(Color.parseColor("#888888"))
        coordinatesText.text = "Получение координат..."
    }

    private fun hideLoading() {
        progressBar.visibility = ProgressBar.GONE
        button.isEnabled = true
    }

    private fun updateCoordinates(latitude: Double, longitude: Double, provider: String, isCached: Boolean) {
        val providerName = when(provider) {
            "gps" -> "GPS"
            "network" -> "Network (Wi-Fi/Сеть)"
            else -> provider
        }

        val cacheIndicator = if (isCached) " [КЭШ]" else " [НОВЫЕ]"

        coordinatesText.text = """
            Широта: %.6f
            Долгота: %.6f
            Источник: $providerName$cacheIndicator
        """.trimIndent().format(latitude, longitude)

        coordinatesText.setTextColor(Color.WHITE)

        statusText.text = if (isCached) {
            "⚠ Показаны кэшированные данные"
        } else {
            "✓ Получены новые координаты!"
        }

        statusText.setTextColor(if (isCached) {
            Color.parseColor("#FFA500")
        } else {
            Color.parseColor("#4CAF50")
        })

        Toast.makeText(
            this,
            if (isCached) "Показаны старые данные" else "Получены новые координаты!",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showError(message: String) {
        coordinatesText.text = "Не удалось получить координаты"
        coordinatesText.setTextColor(Color.parseColor("#F44336"))
        statusText.text = "✗ $message"
        statusText.setTextColor(Color.parseColor("#F44336"))
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showGpsDisabledDialog() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Геолокация отключена")
        builder.setMessage("Для работы приложения необходимо включить GPS или Network. Открыть настройки?")
        builder.setPositiveButton("Да") { _, _ ->
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
        builder.setNegativeButton("Нет", null)
        builder.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LocationHelper.LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                updateMethodInfo()
                Toast.makeText(this, "Разрешение получено!", Toast.LENGTH_SHORT).show()
            } else {
                statusText.text = "✗ Разрешение отклонено"
                statusText.setTextColor(Color.parseColor("#F44336"))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateMethodInfo()
    }

    override fun onDestroy() {
        super.onDestroy()
        locationHelper.stopLocationUpdates()
    }
}
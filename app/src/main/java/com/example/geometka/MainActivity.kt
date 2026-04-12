package com.example.geometka

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import com.example.geometka.data.Mark
import com.example.geometka.data.MarkConstants
import com.example.geometka.data.MarkDatabase
import com.example.geometka.helpers.LocationHelper

class MainActivity : Activity() {

    private lateinit var locationHelper: LocationHelper
    private lateinit var database: MarkDatabase

    private lateinit var statusText: TextView
    private lateinit var methodText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var getLocationButton: Button
    private lateinit var viewMarksButton: Button

    // Поля для ввода
    private lateinit var formLayout: LinearLayout
    private lateinit var nameInput: EditText
    private lateinit var objectTypeSpinner: Spinner
    private lateinit var fireHazardSpinner: Spinner
    private lateinit var waterAvailabilitySpinner: Spinner
    private lateinit var vehiclePassabilitySpinner: Spinner
    private lateinit var notesInput: EditText
    private lateinit var saveButton: Button

    // Данные координат
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private var currentProvider: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationHelper = LocationHelper(this)
        database = MarkDatabase(this)

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(30, 30, 30, 30)
            setBackgroundColor(Color.parseColor("#1E1E1E"))
        }

        // Заголовок
        val titleText = TextView(this).apply {
            text = "🗺️ Geometka"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
        }

        // Информация о методе
        methodText = TextView(this).apply {
            text = "Доступно: ${locationHelper.getAvailableMethods()}"
            textSize = 12f
            setTextColor(Color.parseColor("#888888"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 10
            }
        }

        // Статус
        statusText = TextView(this).apply {
            text = getReadyStatus()
            textSize = 14f
            setTextColor(Color.parseColor("#4CAF50"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
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
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = 20
            }
        }

        // Кнопка получения координат
        getLocationButton = createStyledButton("📍 Получить координаты", "#2196F3")
        getLocationButton.setOnClickListener {
            onGetLocationClick()
        }

        // Форма (скрыта по умолчанию)
        formLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(20, 20, 20, 20)
            setBackgroundColor(Color.parseColor("#2A2A2A"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 30
            }
        }

        // Название метки
        formLayout.addView(createLabel("Название метки:"))
        nameInput = EditText(this).apply {
            hint = "Например: Старый дуб"
            setHintTextColor(Color.parseColor("#666666"))
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#333333"))
            setPadding(20, 20, 20, 20)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
        }
        formLayout.addView(nameInput)

        // Тип объекта
        formLayout.addView(createLabel("Тип объекта:"))
        objectTypeSpinner = createSpinner(MarkConstants.OBJECT_TYPES)
        formLayout.addView(objectTypeSpinner)

        // Класс пожарной опасности
        formLayout.addView(createLabel("Класс пожарной опасности:"))
        fireHazardSpinner = createSpinner(MarkConstants.FIRE_HAZARD_CLASSES)
        formLayout.addView(fireHazardSpinner)

        // Доступность воды
        formLayout.addView(createLabel("Доступность воды:"))
        waterAvailabilitySpinner = createSpinner(MarkConstants.WATER_AVAILABILITY)
        formLayout.addView(waterAvailabilitySpinner)

        // Проходимость техники
        formLayout.addView(createLabel("Проходимость техники:"))
        vehiclePassabilitySpinner = createSpinner(MarkConstants.VEHICLE_PASSABILITY)
        formLayout.addView(vehiclePassabilitySpinner)

        // Дополнительные заметки
        formLayout.addView(createLabel("Дополнительные заметки:"))
        notesInput = EditText(this).apply {
            hint = "Любая дополнительная информация"
            setHintTextColor(Color.parseColor("#666666"))
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#333333"))
            setPadding(20, 20, 20, 20)
            minLines = 3
            maxLines = 5
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
        }
        formLayout.addView(notesInput)

        // Кнопка сохранения
        saveButton = createStyledButton("💾 Сохранить метку", "#4CAF50")
        saveButton.setOnClickListener {
            onSaveMarkClick()
        }
        formLayout.addView(saveButton)

        // Кнопка просмотра меток
        viewMarksButton = createStyledButton("📋 Мои метки (${database.getMarksCount()})", "#FF9800")
        viewMarksButton.setOnClickListener {
            val intent = Intent(this, MarkListActivity::class.java)
            startActivity(intent)
        }

        // Добавляем все в главный layout
        mainLayout.addView(titleText)
        mainLayout.addView(methodText)
        mainLayout.addView(statusText)
        mainLayout.addView(progressBar)
        mainLayout.addView(getLocationButton)
        mainLayout.addView(formLayout)

        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        mainLayout.addView(spacer)

        mainLayout.addView(viewMarksButton)

        // ScrollView для прокрутки
        val scrollView = ScrollView(this).apply {
            addView(mainLayout)
        }

        setContentView(scrollView)

        // Callbacks для LocationHelper
        locationHelper.onLocationReceived = { lat, lon, provider, isCached ->
            hideLoading()
            onLocationReceived(lat, lon, provider, isCached)
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
            }
        }
    }

    private fun createSpinner(items: List<String>): Spinner {
        val spinner = Spinner(this)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
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
                topMargin = 10
                bottomMargin = 10
            }
        }
    }

    private fun getReadyStatus(): String {
        return if (locationHelper.hasInternetConnection()) {
            "✓ Готов (с интернетом)"
        } else {
            "✓ Готов (без интернета - только GPS)"
        }
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

    private fun onGetLocationClick() {
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
        locationHelper.getCurrentLocation(allowCached = false)
    }

    private fun showLoading() {
        progressBar.visibility = ProgressBar.VISIBLE
        getLocationButton.isEnabled = false
    }

    private fun hideLoading() {
        progressBar.visibility = ProgressBar.GONE
        getLocationButton.isEnabled = true
    }

    private fun onLocationReceived(latitude: Double, longitude: Double, provider: String, isCached: Boolean) {
        currentLatitude = latitude
        currentLongitude = longitude
        currentProvider = provider

        if (isCached) {
            Toast.makeText(this, "⚠ Используются кэшированные координаты", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "✓ Координаты получены!", Toast.LENGTH_SHORT).show()
        }

        statusText.text = "✓ Координаты получены! Заполните форму ниже"
        statusText.setTextColor(Color.parseColor("#4CAF50"))

        // Показываем форму
        formLayout.visibility = View.VISIBLE

        // Автоматическое название с датой
        val currentDate = java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date())
        nameInput.setText("Метка $currentDate")
    }

    private fun showError(message: String) {
        statusText.text = "✗ $message"
        statusText.setTextColor(Color.parseColor("#F44336"))
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun onSaveMarkClick() {
        val lat = currentLatitude
        val lon = currentLongitude

        if (lat == null || lon == null) {
            Toast.makeText(this, "Сначала получите координаты!", Toast.LENGTH_SHORT).show()
            return
        }

        val name = nameInput.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Введите название метки!", Toast.LENGTH_SHORT).show()
            return
        }

        val mark = Mark(
            name = name,
            latitude = lat,
            longitude = lon,
            objectType = objectTypeSpinner.selectedItem.toString(),
            fireHazardClass = fireHazardSpinner.selectedItem.toString(),
            waterAvailability = waterAvailabilitySpinner.selectedItem.toString(),
            vehiclePassability = vehiclePassabilitySpinner.selectedItem.toString(),
            notes = notesInput.text.toString().trim(),
            provider = currentProvider ?: "unknown"
        )

        val id = database.insertMark(mark)

        if (id > 0) {
            Toast.makeText(this, "✓ Метка сохранена!", Toast.LENGTH_SHORT).show()

            // Обновляем счетчик
            viewMarksButton.text = "📋 Мои метки (${database.getMarksCount()})"

            // Очищаем форму
            resetForm()
        } else {
            Toast.makeText(this, "✗ Ошибка сохранения", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetForm() {
        formLayout.visibility = View.GONE
        nameInput.setText("")
        notesInput.setText("")
        objectTypeSpinner.setSelection(0)
        fireHazardSpinner.setSelection(0)
        waterAvailabilitySpinner.setSelection(0)
        vehiclePassabilitySpinner.setSelection(0)
        currentLatitude = null
        currentLongitude = null
        currentProvider = null
        statusText.text = getReadyStatus()
        statusText.setTextColor(Color.parseColor("#4CAF50"))
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
        viewMarksButton.text = "📋 Мои метки (${database.getMarksCount()})"
    }

    override fun onDestroy() {
        super.onDestroy()
        locationHelper.stopLocationUpdates()
    }
}
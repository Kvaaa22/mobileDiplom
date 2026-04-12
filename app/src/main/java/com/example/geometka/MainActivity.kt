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

    // UI элементы главного экрана
    private lateinit var mainContentLayout: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var methodText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var getLocationButton: Button

    // UI элементы формы
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

    // Нижнее меню
    private lateinit var bottomMenu: LinearLayout
    private lateinit var homeMenuButton: LinearLayout
    private lateinit var marksMenuButton: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationHelper = LocationHelper(this)
        database = MarkDatabase(this)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1E1E1E"))
        }

        // Главный контент (с прокруткой)
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f // занимает всё пространство кроме меню
            )
        }

        mainContentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 40)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        scrollView.addView(mainContentLayout)

        // === КОНТЕНТ ГЛАВНОГО ЭКРАНА ===

        // Логотип
        val logoText = TextView(this).apply {
            text = "🗺️"
            textSize = 64f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
        }

        val titleText = TextView(this).apply {
            text = "Geometka"
            textSize = 32f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 40
            }
        }

        // Информация о методе
        methodText = TextView(this).apply {
            text = locationHelper.getAvailableMethods()
            textSize = 12f
            setTextColor(Color.parseColor("#666666"))
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
                bottomMargin = 30
            }
        }

        // Индикатор загрузки
        progressBar = ProgressBar(this).apply {
            visibility = ProgressBar.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 30
            }
        }

        // Главная кнопка
        getLocationButton = createMainButton("📍 Получить координаты")
        getLocationButton.setOnClickListener {
            onGetLocationClick()
        }

        // === ФОРМА (скрыта по умолчанию) ===
        formLayout = createFormLayout()

        // Добавляем всё в контент
        mainContentLayout.addView(logoText)
        mainContentLayout.addView(titleText)
        mainContentLayout.addView(methodText)
        mainContentLayout.addView(statusText)
        mainContentLayout.addView(progressBar)
        mainContentLayout.addView(getLocationButton)
        mainContentLayout.addView(formLayout)

        // === НИЖНЕЕ МЕНЮ ===
        bottomMenu = createBottomMenu()

        rootLayout.addView(scrollView)
        rootLayout.addView(bottomMenu)

        setContentView(rootLayout)

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
        updateMenuSelection(true)
    }

    private fun createMainButton(text: String): Button {
        val buttonBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor("#2196F3"))
            cornerRadius = 50f
        }

        return Button(this).apply {
            this.text = text
            textSize = 18f
            setTextColor(Color.WHITE)
            background = buttonBackground
            setPadding(80, 40, 80, 40)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 20
                bottomMargin = 20
            }
        }
    }

    private fun createFormLayout(): LinearLayout {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(0, 30, 0, 30)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val separator = View(this).apply {
            setBackgroundColor(Color.parseColor("#333333"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                2
            ).apply {
                bottomMargin = 30
            }
        }
        layout.addView(separator)

        val formTitle = TextView(this).apply {
            text = "✏️ Заполните данные метки"
            textSize = 18f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 20
            }
        }
        layout.addView(formTitle)

        layout.addView(createLabel("Название метки:"))
        nameInput = createEditText("", "Например: Старый дуб")
        layout.addView(nameInput)

        layout.addView(createLabel("Тип объекта:"))
        objectTypeSpinner = createSpinner(MarkConstants.OBJECT_TYPES)
        layout.addView(objectTypeSpinner)

        layout.addView(createLabel("Класс пожарной опасности:"))
        fireHazardSpinner = createSpinner(MarkConstants.FIRE_HAZARD_CLASSES)
        layout.addView(fireHazardSpinner)

        layout.addView(createLabel("Доступность воды:"))
        waterAvailabilitySpinner = createSpinner(MarkConstants.WATER_AVAILABILITY)
        layout.addView(waterAvailabilitySpinner)

        layout.addView(createLabel("Проходимость техники:"))
        vehiclePassabilitySpinner = createSpinner(MarkConstants.VEHICLE_PASSABILITY)
        layout.addView(vehiclePassabilitySpinner)

        layout.addView(createLabel("Дополнительные заметки:"))
        notesInput = createEditText("", "Любая дополнительная информация", true)
        layout.addView(notesInput)

        saveButton = createSaveButton()
        layout.addView(saveButton)

        return layout
    }

    private fun createLabel(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(Color.parseColor("#AAAAAA"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 8
                topMargin = 10
            }
        }
    }

    private fun createEditText(value: String, hint: String, multiline: Boolean = false): EditText {
        return EditText(this).apply {
            setText(value)
            this.hint = hint
            setHintTextColor(Color.parseColor("#555555"))
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#2A2A2A"))
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

    private fun createSpinner(items: List<String>): Spinner {
        val spinner = Spinner(this)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
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

    private fun createSaveButton(): Button {
        val buttonBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor("#4CAF50"))
            cornerRadius = 30f
        }

        return Button(this).apply {
            text = "💾 Сохранить метку"
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
            setOnClickListener {
                onSaveMarkClick()
            }
        }
    }

    private fun createBottomMenu(): LinearLayout {
        val menuLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#2A2A2A"))
            setPadding(0, 15, 0, 15)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        homeMenuButton = createMenuButton("🏠", "Главная", true)
        homeMenuButton.setOnClickListener {
            // Уже на главной
        }

        marksMenuButton = createMenuButton("📋", "Метки", false)
        marksMenuButton.setOnClickListener {
            val intent = Intent(this, MarkListActivity::class.java)
            startActivity(intent)
        }

        menuLayout.addView(homeMenuButton)
        menuLayout.addView(marksMenuButton)

        return menuLayout
    }

    private fun createMenuButton(icon: String, label: String, selected: Boolean): LinearLayout {
        val button = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(20, 10, 20, 10)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            isClickable = true
            isFocusable = true
        }

        val iconText = TextView(this).apply {
            text = icon
            textSize = 24f
            gravity = Gravity.CENTER
        }

        val labelText = TextView(this).apply {
            text = label
            textSize = 11f
            setTextColor(if (selected) Color.parseColor("#2196F3") else Color.parseColor("#888888"))
            gravity = Gravity.CENTER
        }

        button.addView(iconText)
        button.addView(labelText)

        return button
    }

    private fun updateMenuSelection(isHome: Boolean) {
        val homeLabel = homeMenuButton.getChildAt(1) as TextView
        val marksLabel = marksMenuButton.getChildAt(1) as TextView

        if (isHome) {
            homeLabel.setTextColor(Color.parseColor("#2196F3"))
            marksLabel.setTextColor(Color.parseColor("#888888"))
        } else {
            homeLabel.setTextColor(Color.parseColor("#888888"))
            marksLabel.setTextColor(Color.parseColor("#2196F3"))
        }
    }

    private fun getReadyStatus(): String {
        return if (locationHelper.hasInternetConnection()) {
            "✓ Готов (быстрый режим)"
        } else {
            "✓ Готов (GPS без сети)"
        }
    }

    private fun updateMethodInfo() {
        methodText.text = locationHelper.getAvailableMethods()

        val hasInternet = locationHelper.hasInternetConnection()
        val hasGps = locationHelper.isGpsEnabled()

        statusText.text = when {
            hasGps && hasInternet -> "✓ Готов (быстрый режим)"
            hasGps && !hasInternet -> "✓ Готов (GPS без сети)"
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
        getLocationButton.alpha = 0.5f
    }

    private fun hideLoading() {
        progressBar.visibility = ProgressBar.GONE
        getLocationButton.isEnabled = true
        getLocationButton.alpha = 1f
    }

    private fun onLocationReceived(latitude: Double, longitude: Double, provider: String, isCached: Boolean) {
        currentLatitude = latitude
        currentLongitude = longitude
        currentProvider = provider

        statusText.text = "✓ Координаты получены!"
        statusText.setTextColor(Color.parseColor("#4CAF50"))

        Toast.makeText(this, "✓ Координаты получены!", Toast.LENGTH_SHORT).show()

        // Показываем форму
        formLayout.visibility = View.VISIBLE

        // Автоматическое название
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
            Toast.makeText(this, "✓ Метка сохранена!", Toast.LENGTH_LONG).show()
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
        updateMethodInfo()
    }

    private fun showGpsDisabledDialog() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Геолокация отключена")
        builder.setMessage("Включить GPS?")
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
                Toast.makeText(this, "✓ Разрешение получено!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateMethodInfo()
        updateMenuSelection(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        locationHelper.stopLocationUpdates()
    }
}
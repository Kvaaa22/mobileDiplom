package com.example.geometka

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import com.example.geometka.data.Mark
import com.example.geometka.data.MarkConstants
import com.example.geometka.data.MarkDatabase
import com.example.geometka.helpers.LocationHelper
import com.example.geometka.ui.UIHelper

class MainActivity : Activity() {

    private lateinit var locationHelper: LocationHelper
    private lateinit var database: MarkDatabase

    private lateinit var statusText: TextView
    private lateinit var methodText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var getLocationButton: Button
    private lateinit var formLayout: LinearLayout

    private lateinit var nameInput: EditText
    private lateinit var objectTypeSpinner: Spinner
    private lateinit var fireHazardSpinner: Spinner
    private lateinit var waterAvailabilitySpinner: Spinner
    private lateinit var vehiclePassabilitySpinner: Spinner
    private lateinit var notesInput: EditText

    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private var currentProvider: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationHelper = LocationHelper(this)
        database = MarkDatabase(this)

        setContentView(createMainLayout())
        setupCallbacks()
        checkPermissions()
        updateMethodInfo()
    }

    private fun createMainLayout(): View {
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(UIHelper.Colors.BACKGROUND))
        }

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 60, 40, 40)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // Логотип и заголовок
        contentLayout.addView(createLogo())
        contentLayout.addView(createTitle())

        // Статус
        methodText = createMethodText()
        statusText = createStatusText()
        progressBar = createProgressBar()

        contentLayout.addView(methodText)
        contentLayout.addView(statusText)
        contentLayout.addView(progressBar)

        // Главная кнопка
        getLocationButton = UIHelper.createMainButton(this, "📍 Получить координаты") {
            onGetLocationClick()
        }
        contentLayout.addView(getLocationButton)

        // Форма
        formLayout = createForm()
        contentLayout.addView(formLayout)

        scrollView.addView(contentLayout)
        rootLayout.addView(scrollView)
        rootLayout.addView(createBottomMenu())

        return rootLayout
    }

    private fun createLogo() = TextView(this).apply {
        text = "🗺️"
        textSize = 64f
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 20 }
    }

    private fun createTitle() = TextView(this).apply {
        text = "Geometka"
        textSize = 32f
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        setTypeface(null, android.graphics.Typeface.BOLD)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 40 }
    }

    private fun createMethodText() = TextView(this).apply {
        textSize = 12f
        setTextColor(Color.parseColor(UIHelper.Colors.TEXT_DISABLED))
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 10 }
    }

    private fun createStatusText() = TextView(this).apply {
        textSize = 14f
        setTextColor(Color.parseColor(UIHelper.Colors.SUCCESS))
        gravity = Gravity.CENTER
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 30 }
    }

    private fun createProgressBar() = ProgressBar(this).apply {
        visibility = ProgressBar.GONE
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = 30 }
    }

    private fun createForm(): LinearLayout {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(0, 30, 0, 30)
        }

        layout.addView(UIHelper.createDivider(this))

        layout.addView(TextView(this).apply {
            text = "✏️ Заполните данные метки"
            textSize = 18f
            setTextColor(Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 20 }
        })

        layout.addView(UIHelper.createLabel(this, "Название метки:"))
        nameInput = UIHelper.createEditText(this, hint = "Например: Старый дуб")
        layout.addView(nameInput)

        layout.addView(UIHelper.createLabel(this, "Тип объекта:"))
        objectTypeSpinner = UIHelper.createSpinner(this, MarkConstants.OBJECT_TYPES)
        layout.addView(objectTypeSpinner)

        layout.addView(UIHelper.createLabel(this, "Класс пожарной опасности:"))
        fireHazardSpinner = UIHelper.createSpinner(this, MarkConstants.FIRE_HAZARD_CLASSES)
        layout.addView(fireHazardSpinner)

        layout.addView(UIHelper.createLabel(this, "Доступность воды:"))
        waterAvailabilitySpinner = UIHelper.createSpinner(this, MarkConstants.WATER_AVAILABILITY)
        layout.addView(waterAvailabilitySpinner)

        layout.addView(UIHelper.createLabel(this, "Проходимость техники:"))
        vehiclePassabilitySpinner = UIHelper.createSpinner(this, MarkConstants.VEHICLE_PASSABILITY)
        layout.addView(vehiclePassabilitySpinner)

        layout.addView(UIHelper.createLabel(this, "Дополнительные заметки:"))
        notesInput = UIHelper.createEditText(this, hint = "Любая дополнительная информация", multiline = true)
        layout.addView(notesInput)

        layout.addView(UIHelper.createButton(this, "💾 Сохранить метку", UIHelper.Colors.SUCCESS) {
            onSaveMarkClick()
        })

        return layout
    }

    private fun createBottomMenu(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor(UIHelper.Colors.CARD_BACKGROUND))
            setPadding(0, 15, 0, 15)

            val homeBtn = UIHelper.createMenuButton(this@MainActivity, "🏠", "Главная", true)
            val marksBtn = UIHelper.createMenuButton(this@MainActivity, "📋", "Метки", false)

            marksBtn.setOnClickListener {
                startActivity(Intent(this@MainActivity, MarkListActivity::class.java))
            }

            addView(homeBtn)
            addView(marksBtn)
        }
    }

    private fun setupCallbacks() {
        locationHelper.onLocationReceived = { lat, lon, provider, _ ->
            hideLoading()
            onLocationReceived(lat, lon, provider)
        }

        locationHelper.onLocationError = { message ->
            hideLoading()
            showError(message)
        }

        locationHelper.onStatusUpdate = { message ->
            statusText.text = message
            statusText.setTextColor(Color.parseColor(UIHelper.Colors.WARNING))
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

        statusText.setTextColor(Color.parseColor(when {
            hasGps && hasInternet -> UIHelper.Colors.SUCCESS
            hasGps && !hasInternet -> UIHelper.Colors.WARNING
            else -> UIHelper.Colors.DANGER
        }))
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
            showGpsDialog()
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

    private fun onLocationReceived(lat: Double, lon: Double, provider: String) {
        currentLatitude = lat
        currentLongitude = lon
        currentProvider = provider

        statusText.text = "✓ Координаты получены!"
        statusText.setTextColor(Color.parseColor(UIHelper.Colors.SUCCESS))
        Toast.makeText(this, "✓ Координаты получены!", Toast.LENGTH_SHORT).show()

        formLayout.visibility = View.VISIBLE

        val date = java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date())
        nameInput.setText("Метка $date")
    }

    private fun showError(message: String) {
        statusText.text = "✗ $message"
        statusText.setTextColor(Color.parseColor(UIHelper.Colors.DANGER))
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun onSaveMarkClick() {
        val lat = currentLatitude ?: run {
            Toast.makeText(this, "Сначала получите координаты!", Toast.LENGTH_SHORT).show()
            return
        }
        val lon = currentLongitude ?: return

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

        if (database.insertMark(mark) > 0) {
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
        updateMethodInfo()
    }

    private fun showGpsDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Геолокация отключена")
            .setMessage("Включить GPS?")
            .setPositiveButton("Да") { _, _ ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
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
    }

    override fun onDestroy() {
        super.onDestroy()
        locationHelper.stopLocationUpdates()
    }
}
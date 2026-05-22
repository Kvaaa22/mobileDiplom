package com.example.geometka

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import com.example.geometka.data.FireIntensity
import com.example.geometka.data.FireType
import com.example.geometka.data.Mark
import com.example.geometka.data.MarkDatabase
import com.example.geometka.data.PointType
import com.example.geometka.helpers.LocationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var locationHelper: LocationHelper
    private lateinit var database: MarkDatabase

    private lateinit var statusText: TextView
    private lateinit var methodText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var getLocationButton: Button
    private lateinit var formLayout: LinearLayout
    private lateinit var accuracyWarningText: TextView

    private lateinit var notesInput: EditText

    private val pointTypeChipViews = mutableMapOf<PointType, TextView>()
    private val intensityChipViews = mutableMapOf<FireIntensity, TextView>()
    private val fireTypeChipViews = mutableMapOf<FireType, TextView>()

    private var selectedPointType: PointType = PointType.FRONT
    private var selectedIntensity: FireIntensity = FireIntensity.MEDIUM
    private var selectedFireType: FireType = FireType.GROUND

    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private var currentHorizontalAccuracyMeters: Float? = null
    private var currentAutoName: String = ""

    private object Colors {
        const val GREEN_DARK = "#0B2A18"
        const val GREEN = "#155E32"
        const val GREEN_LIGHT = "#EAF3EB"
        const val BACKGROUND = "#F8FBF7"
        const val CARD = "#FFFFFF"
        const val BORDER = "#C9DDCC"
        const val TEXT_PRIMARY = "#1F2A22"
        const val TEXT_SECONDARY = "#6F7D73"
        const val TEXT_MUTED = "#9AA69E"
        const val WARNING_BG = "#FFF0D2"
        const val WARNING_TEXT = "#D94324"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.parseColor(Colors.GREEN_DARK)
        window.navigationBarColor = Color.WHITE
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

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
            setBackgroundColor(Color.parseColor(Colors.BACKGROUND))
        }

        rootLayout.addView(createHeader())

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(14))
        }

        contentLayout.addView(createLocationCard())

        getLocationButton = createPrimaryButton("Определить координаты") {
            onGetLocationClick()
        }
        contentLayout.addView(getLocationButton)

        formLayout = createForm()
        contentLayout.addView(formLayout)

        scrollView.addView(contentLayout)

        rootLayout.addView(scrollView)
        rootLayout.addView(createBottomMenu())

        return rootLayout
    }

    private fun createHeader(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(Colors.GREEN))
            setPadding(dp(20), dp(20), dp(20), dp(18))

            addView(TextView(this@MainActivity).apply {
                text = "Новая точка"
                textSize = 21f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
            })

            addView(TextView(this@MainActivity).apply {
                text = "Фиксация параметров кромки"
                textSize = 13f
                setTextColor(Color.parseColor("#C8DDCE"))
                setPadding(0, dp(4), 0, 0)
            })
        }
    }

    private fun createLocationCard(): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedDrawable(
                color = Colors.CARD,
                radiusDp = 14,
                strokeColor = Colors.BORDER,
                strokeWidthDp = 1
            )
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
            }
        }

        statusText = TextView(this).apply {
            text = "Координаты не определены"
            textSize = 16f
            setTextColor(Color.parseColor(Colors.TEXT_PRIMARY))
            typeface = Typeface.DEFAULT_BOLD
        }

        methodText = TextView(this).apply {
            text = "Нажмите кнопку ниже, чтобы получить координаты"
            textSize = 12f
            setTextColor(Color.parseColor(Colors.TEXT_SECONDARY))
            setPadding(0, dp(7), 0, 0)
        }

        progressBar = ProgressBar(this).apply {
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
            }
        }

        card.addView(statusText)
        card.addView(methodText)
        card.addView(progressBar)

        return card
    }

    private fun createForm(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(0, dp(4), 0, 0)

            addView(createLabel("Тип точки"))
            addView(createPointTypeChips())

            addView(createLabel("Интенсивность горения"))
            addView(createIntensityChips())

            addView(createLabel("Тип пожара"))
            addView(createFireTypeChips())

            addView(createLabel("Комментарий"))
            notesInput = createStyledEditText(
                hint = "Видимость снижена, сильный дым, рядом просека",
                multiline = true
            )
            addView(notesInput)

            accuracyWarningText = TextView(this@MainActivity).apply {
                text = "Точность более 25 м!"
                textSize = 13f
                setTextColor(Color.parseColor(Colors.WARNING_TEXT))
                gravity = Gravity.CENTER_VERTICAL
                visibility = View.GONE
                background = roundedDrawable(
                    color = Colors.WARNING_BG,
                    radiusDp = 11
                )
                setPadding(dp(16), 0, dp(16), 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(40)
                ).apply {
                    topMargin = dp(16)
                    bottomMargin = dp(18)
                }
            }
            addView(accuracyWarningText)

            addView(createPrimaryButton("Сохранить точку") {
                onSaveMarkClick()
            })
        }
    }

    private fun createLabel(textValue: String): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = 14f
            setTextColor(Color.parseColor(Colors.TEXT_PRIMARY))
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(14)
                bottomMargin = dp(8)
            }
        }
    }

    private fun createStyledEditText(
        hint: String,
        multiline: Boolean
    ): EditText {
        return EditText(this).apply {
            this.hint = hint
            textSize = 13f
            setTextColor(Color.parseColor(Colors.TEXT_PRIMARY))
            setHintTextColor(Color.parseColor(Colors.TEXT_MUTED))
            background = roundedDrawable(
                color = Colors.CARD,
                radiusDp = 10,
                strokeColor = Colors.BORDER,
                strokeWidthDp = 1
            )
            setPadding(dp(16), 0, dp(16), 0)

            if (multiline) {
                minLines = 3
                maxLines = 5
                gravity = Gravity.TOP or Gravity.START
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                setPadding(dp(16), dp(14), dp(16), dp(14))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(92)
                )
            } else {
                setSingleLine(true)
                inputType = InputType.TYPE_CLASS_TEXT
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(46)
                )
            }
        }
    }

    private fun createPointTypeChips(): LinearLayout {
        pointTypeChipViews.clear()

        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            PointType.entries.forEach { type ->
                val chip = createChoiceChip(
                    textValue = type.label,
                    selected = type == selectedPointType
                ) {
                    selectedPointType = type
                    updatePointTypeChips()
                }

                pointTypeChipViews[type] = chip
                addView(chip)
            }
        }
    }

    private fun createIntensityChips(): LinearLayout {
        intensityChipViews.clear()

        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            FireIntensity.entries.forEach { intensity ->
                val chip = createChoiceChip(
                    textValue = intensity.label,
                    selected = intensity == selectedIntensity
                ) {
                    selectedIntensity = intensity
                    updateIntensityChips()
                }

                intensityChipViews[intensity] = chip
                addView(chip)
            }
        }
    }

    private fun createFireTypeChips(): LinearLayout {
        fireTypeChipViews.clear()

        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            FireType.entries.forEach { fireType ->
                val chip = createChoiceChip(
                    textValue = fireType.label,
                    selected = fireType == selectedFireType
                ) {
                    selectedFireType = fireType
                    updateFireTypeChips()
                }

                fireTypeChipViews[fireType] = chip
                addView(chip)
            }
        }
    }

    private fun createChoiceChip(
        textValue: String,
        selected: Boolean,
        onClick: () -> Unit
    ): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = 12f
            gravity = Gravity.CENTER
            typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            setTextColor(
                Color.parseColor(
                    if (selected) Color.WHITE.toHexString() else Colors.TEXT_PRIMARY
                )
            )
            background = choiceChipDrawable(selected)
            setPadding(dp(18), 0, dp(18), 0)
            layoutParams = LinearLayout.LayoutParams(
                0,
                dp(32),
                1f
            ).apply {
                rightMargin = dp(8)
            }
            setOnClickListener {
                onClick()
            }
        }
    }

    private fun updatePointTypeChips() {
        pointTypeChipViews.forEach { (type, chip) ->
            val selected = type == selectedPointType
            chip.background = choiceChipDrawable(selected)
            chip.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            chip.setTextColor(
                Color.parseColor(
                    if (selected) Color.WHITE.toHexString() else Colors.TEXT_PRIMARY
                )
            )
        }
    }

    private fun updateIntensityChips() {
        intensityChipViews.forEach { (intensity, chip) ->
            val selected = intensity == selectedIntensity
            chip.background = choiceChipDrawable(selected)
            chip.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            chip.setTextColor(
                Color.parseColor(
                    if (selected) Color.WHITE.toHexString() else Colors.TEXT_PRIMARY
                )
            )
        }
    }

    private fun updateFireTypeChips() {
        fireTypeChipViews.forEach { (fireType, chip) ->
            val selected = fireType == selectedFireType
            chip.background = choiceChipDrawable(selected)
            chip.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            chip.setTextColor(
                Color.parseColor(
                    if (selected) Color.WHITE.toHexString() else Colors.TEXT_PRIMARY
                )
            )
        }
    }

    private fun createPrimaryButton(
        textValue: String,
        onClick: () -> Unit
    ): Button {
        return Button(this).apply {
            text = textValue
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            isAllCaps = false
            background = roundedDrawable(
                color = Colors.GREEN,
                radiusDp = 9
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
            ).apply {
                topMargin = dp(8)
                bottomMargin = dp(8)
            }
            setOnClickListener {
                onClick()
            }
        }
    }

    private fun createBottomMenu(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            setPadding(0, dp(7), 0, dp(8))

            addView(createMenuItem("⌖", "Карта", selected = true) {
                // Уже на экране карты/создания точки.
            })

            addView(createMenuItem("●", "Точки", selected = false) {
                startActivity(Intent(this@MainActivity, MarkListActivity::class.java))
            })

            addView(createMenuItem("⇄", "Синхр.", selected = false) {
                startActivity(Intent(this@MainActivity, SyncActivity::class.java))
            })
        }
    }

    private fun createMenuItem(
        icon: String,
        label: String,
        selected: Boolean,
        onClick: () -> Unit
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            setOnClickListener {
                onClick()
            }

            addView(TextView(this@MainActivity).apply {
                text = icon
                textSize = 18f
                gravity = Gravity.CENTER
                setTextColor(
                    Color.parseColor(
                        if (selected) Colors.GREEN else Colors.TEXT_MUTED
                    )
                )
            })

            addView(TextView(this@MainActivity).apply {
                text = label
                textSize = 10f
                gravity = Gravity.CENTER
                setTextColor(
                    Color.parseColor(
                        if (selected) Colors.GREEN else Colors.TEXT_MUTED
                    )
                )
                if (selected) {
                    typeface = Typeface.DEFAULT_BOLD
                }
            })
        }
    }

    private fun setupCallbacks() {
        locationHelper.onLocationReceived = { lat, lon, _, accuracy ->
            hideLoading()
            onLocationReceived(lat, lon, accuracy)
        }

        locationHelper.onLocationError = { message ->
            hideLoading()
            showError(message)
        }

        locationHelper.onStatusUpdate = { message ->
            if (currentLatitude == null || currentLongitude == null) {
                methodText.text = message
            }
        }
    }

    private fun updateMethodInfo() {
        if (currentLatitude != null && currentLongitude != null) {
            renderCoordinateInfo()
            return
        }

        val hasInternet = locationHelper.hasInternetConnection()
        val hasGps = locationHelper.isGpsEnabled()

        statusText.text = when {
            hasGps && hasInternet -> "Готово к определению координат"
            hasGps && !hasInternet -> "GPS доступен без сети"
            else -> "GPS отключен"
        }

        methodText.text = when {
            hasGps && hasInternet -> "Можно получить текущие координаты"
            hasGps && !hasInternet -> "Можно работать по GPS без интернета"
            else -> "Включите геолокацию на устройстве"
        }

        statusText.setTextColor(
            Color.parseColor(
                when {
                    hasGps -> Colors.TEXT_PRIMARY
                    else -> Colors.WARNING_TEXT
                }
            )
        )
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
        progressBar.visibility = View.VISIBLE
        getLocationButton.isEnabled = false
        getLocationButton.alpha = 0.55f
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
        getLocationButton.isEnabled = true
        getLocationButton.alpha = 1f
    }

    private fun onLocationReceived(
        lat: Double,
        lon: Double,
        horizontalAccuracyMeters: Float?
    ) {
        currentLatitude = lat
        currentLongitude = lon
        currentHorizontalAccuracyMeters = horizontalAccuracyMeters

        renderCoordinateInfo()

        Toast.makeText(this, "Координаты получены", Toast.LENGTH_SHORT).show()

        getLocationButton.visibility = View.GONE
        formLayout.visibility = View.VISIBLE

        val date = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
            .format(Date())

        currentAutoName = "Фиксация $date"
    }

    private fun renderCoordinateInfo() {
        val lat = currentLatitude
        val lon = currentLongitude

        if (lat == null || lon == null) {
            return
        }

        val accuracyText = currentHorizontalAccuracyMeters?.let {
            "точность %.0f м".format(it)
        } ?: "точность не указана"

        statusText.text = "Координаты определены"
        methodText.text = "%.6f, %.6f · %s".format(lat, lon, accuracyText)
        statusText.setTextColor(Color.parseColor(Colors.TEXT_PRIMARY))

        updateAccuracyWarning()
    }

    private fun updateAccuracyWarning() {
        if (!this::accuracyWarningText.isInitialized) {
            return
        }

        val accuracy = currentHorizontalAccuracyMeters

        accuracyWarningText.visibility = if (accuracy != null && accuracy > 25f) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun showError(message: String) {
        statusText.text = "Ошибка определения координат"
        methodText.text = message
        statusText.setTextColor(Color.parseColor(Colors.WARNING_TEXT))
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun onSaveMarkClick() {
        val lat = currentLatitude ?: run {
            Toast.makeText(this, "Сначала получите координаты!", Toast.LENGTH_SHORT).show()
            return
        }

        val lon = currentLongitude ?: run {
            Toast.makeText(this, "Сначала получите координаты!", Toast.LENGTH_SHORT).show()
            return
        }

        val mark = Mark(
            name = currentAutoName,
            latitude = lat,
            longitude = lon,
            pointType = selectedPointType,
            intensity = selectedIntensity,
            typeOfFire = selectedFireType,
            notes = notesInput.text.toString().trim().ifEmpty { null },
            horizontalAccuracyMeters = currentHorizontalAccuracyMeters
        )

        if (database.insertMark(mark) > 0) {
            Toast.makeText(this, "Точка сохранена", Toast.LENGTH_LONG).show()
            resetForm()
        } else {
            Toast.makeText(this, "Ошибка сохранения", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetForm() {
        formLayout.visibility = View.GONE
        getLocationButton.visibility = View.VISIBLE

        notesInput.setText("")

        selectedPointType = PointType.FRONT
        selectedIntensity = FireIntensity.MEDIUM
        selectedFireType = FireType.GROUND

        updatePointTypeChips()
        updateIntensityChips()
        updateFireTypeChips()

        currentLatitude = null
        currentLongitude = null
        currentHorizontalAccuracyMeters = null
        currentAutoName = ""

        updateAccuracyWarning()
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LocationHelper.LOCATION_PERMISSION_REQUEST_CODE) {
            if (
                grantResults.isNotEmpty() &&
                grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                updateMethodInfo()
                Toast.makeText(this, "Разрешение получено", Toast.LENGTH_SHORT).show()
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

    private fun roundedDrawable(
        color: String,
        radiusDp: Int,
        strokeColor: String? = null,
        strokeWidthDp: Int = 0
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor(color))
            cornerRadius = dp(radiusDp).toFloat()

            if (strokeColor != null && strokeWidthDp > 0) {
                setStroke(dp(strokeWidthDp), Color.parseColor(strokeColor))
            }
        }
    }

    private fun choiceChipDrawable(selected: Boolean): GradientDrawable {
        return roundedDrawable(
            color = if (selected) Colors.GREEN else Colors.GREEN_LIGHT,
            radiusDp = 18
        )
    }

    private fun Int.toHexString(): String {
        return String.format("#%06X", 0xFFFFFF and this)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
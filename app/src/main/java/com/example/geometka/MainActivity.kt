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
import com.example.geometka.maps.MapAvailability
import com.example.geometka.maps.MapDownloadScheduler
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.MapTileProviderArray
import org.osmdroid.tileprovider.modules.ArchiveFileFactory
import org.osmdroid.tileprovider.modules.IArchiveFile
import org.osmdroid.tileprovider.modules.MapTileFileArchiveProvider
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var locationHelper: LocationHelper
    private lateinit var database: MarkDatabase

    private lateinit var subtitleText: TextView
    private lateinit var addCardSubtitle: TextView
    private lateinit var mapContainer: FrameLayout
    private lateinit var progressBar: ProgressBar

    private var mapView: MapView? = null

    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private var currentHorizontalAccuracyMeters: Float? = null

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
        const val RED = "#D73620"
        const val MAP_PLACEHOLDER = "#DCEAD7"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.parseColor(Colors.GREEN_DARK)
        window.navigationBarColor = Color.WHITE
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        Configuration.getInstance().load(
            this,
            getSharedPreferences("osmdroid", MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = packageName

        locationHelper = LocationHelper(this)
        database = MarkDatabase(this)

        MapDownloadScheduler.startAutomaticDownloads(this)

        setContentView(createLayout())
        setupCallbacks()
        checkPermissions()
        refreshMapScreen()
    }

    private fun createLayout(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(Colors.BACKGROUND))
        }

        root.addView(createHeader())

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        content.addView(createMapBlock())
        content.addView(createAddPointBlock())

        root.addView(content)
        root.addView(createBottomMenu())

        return root
    }

    private fun createHeader(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(Colors.GREEN))
            setPadding(dp(18), dp(20), dp(18), dp(16))

            addView(TextView(this@MainActivity).apply {
                text = "Карта пожара"
                textSize = 20f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
            })

            subtitleText = TextView(this@MainActivity).apply {
                text = "Проверка офлайн-карты"
                textSize = 12f
                setTextColor(Color.parseColor("#C8DDCE"))
                setPadding(0, dp(4), 0, 0)
            }

            addView(subtitleText)
        }
    }

    private fun createMapBlock(): FrameLayout {
        mapContainer = FrameLayout(this).apply {
            background = roundedDrawable(
                color = Colors.CARD,
                radiusDp = 15,
                strokeColor = Colors.BORDER,
                strokeWidthDp = 1
            )
            clipToOutline = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                bottomMargin = dp(12)
            }
        }

        return mapContainer
    }

    private fun refreshMapScreen() {
        val mapPath = MapAvailability.getDownloadedMapPath(this)

        subtitleText.text = if (mapPath != null) {
            "Офлайн-карта загружена"
        } else {
            "Карта не загружена · точки фиксируются по GPS"
        }

        mapContainer.removeAllViews()
        mapView = null

        if (mapPath != null) {
            val mapResult = runCatching<MapView?> {
                createOfflineMapView(mapPath)
            }.getOrNull()

            if (mapResult != null) {
                mapView = mapResult
                mapContainer.addView(
                    mapResult,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )
                addMarksToMap()
            } else {
                mapContainer.addView(createMapPlaceholder("Офлайн-карта пока не загружена"))
            }
        } else {
            mapContainer.addView(createMapPlaceholder("Офлайн-карта пока не загружена"))
        }
    }

    private fun createOfflineMapView(mapPath: String): MapView {
        val mapFile = File(mapPath)

        val tileSource = XYTileSource(
            "offline",
            0,
            20,
            256,
            ".png",
            arrayOf<String>()
        )

        val archive = ArchiveFileFactory.getArchiveFile(mapFile)
        val archives = arrayOf<IArchiveFile>(archive)

        val registerReceiver = SimpleRegisterReceiver(this)

        val archiveProvider = MapTileFileArchiveProvider(
            registerReceiver,
            tileSource,
            archives
        )

        val tileProvider = MapTileProviderArray(
            tileSource,
            registerReceiver,
            arrayOf(archiveProvider)
        )

        return MapView(this, tileProvider).apply {
            setMultiTouchControls(true)
            minZoomLevel = 10.0
            maxZoomLevel = 20.0

            val firstMark = database.getAllMarks().firstOrNull()
            val center = if (firstMark != null) {
                GeoPoint(firstMark.latitude, firstMark.longitude)
            } else {
                GeoPoint(56.0106, 92.8526)
            }

            controller.setZoom(15.0)
            controller.setCenter(center)
        }
    }

    private fun addMarksToMap() {
        val map = mapView ?: return

        map.overlays.removeAll { it is Marker }

        database.getAllMarks().forEach { mark ->
            val marker = Marker(map).apply {
                position = GeoPoint(mark.latitude, mark.longitude)
                title = mark.name
                setSubDescription("${mark.pointType.label} · ${mark.intensity.label}")
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = circleDrawable(Colors.RED)
            }

            map.overlays.add(marker)
        }

        map.invalidate()
    }

    private fun createMapPlaceholder(message: String): FrameLayout {
        return FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor(Colors.MAP_PLACEHOLDER))

            addView(TextView(this@MainActivity).apply {
                text = message
                textSize = 14f
                setTextColor(Color.parseColor(Colors.TEXT_SECONDARY))
                gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            })

            addFakeMapDots(this)
        }
    }

    private fun addFakeMapDots(parent: FrameLayout) {
        val positions = listOf(
            Pair(70, 270),
            Pair(130, 295),
            Pair(170, 292),
            Pair(218, 268)
        )

        positions.forEach { (left, top) ->
            parent.addView(View(this).apply {
                background = circleDrawable(Colors.RED)
                layoutParams = FrameLayout.LayoutParams(dp(13), dp(13)).apply {
                    leftMargin = dp(left)
                    topMargin = dp(top)
                }
            })
        }
    }

    private fun createAddPointBlock(): LinearLayout {
        val block = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = roundedDrawable(
                color = Colors.CARD,
                radiusDp = 13
            )
            setPadding(dp(16), dp(12), dp(12), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(74)
            ).apply {
                bottomMargin = dp(10)
            }

            setOnClickListener {
                onAddPointClick()
            }
        }

        val textBlock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        textBlock.addView(TextView(this).apply {
            text = "Добавить новую точку →"
            textSize = 14f
            setTextColor(Color.parseColor(Colors.TEXT_PRIMARY))
            typeface = Typeface.DEFAULT_BOLD
        })

        addCardSubtitle = TextView(this).apply {
            text = "Текущая точность: неизвестна"
            textSize = 11f
            setTextColor(Color.parseColor(Colors.TEXT_SECONDARY))
            setPadding(0, dp(5), 0, 0)
        }

        textBlock.addView(addCardSubtitle)

        val plusButton = TextView(this).apply {
            text = "+"
            textSize = 34f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = circleDrawable(Colors.RED)
            layoutParams = LinearLayout.LayoutParams(dp(56), dp(56))
            setOnClickListener {
                onAddPointClick()
            }
        }

        block.addView(
            textBlock,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )
        block.addView(plusButton)

        return block
    }

    private fun onAddPointClick() {
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

    private fun setupCallbacks() {
        locationHelper.onLocationReceived = { lat, lon, _, accuracy ->
            hideLoading()
            currentLatitude = lat
            currentLongitude = lon
            currentHorizontalAccuracyMeters = accuracy

            updateAccuracyInfo()
            showAddPointDialog()
        }

        locationHelper.onLocationError = { message ->
            hideLoading()
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }

        locationHelper.onStatusUpdate = { message ->
            subtitleText.text = message
        }
    }

    private fun updateAccuracyInfo() {
        val accuracy = currentHorizontalAccuracyMeters

        if (this::addCardSubtitle.isInitialized) {
            addCardSubtitle.text = if (accuracy != null) {
                "Текущая точность: %.0f м".format(accuracy)
            } else {
                "Текущая точность: неизвестна"
            }
        }
    }

    private fun showLoading() {
        if (!this::progressBar.isInitialized) {
            progressBar = ProgressBar(this)
        }

        Toast.makeText(this, "Определение координат…", Toast.LENGTH_SHORT).show()
    }

    private fun hideLoading() {
        // Отдельный progressBar на макете не нужен, оставлено для совместимости логики.
    }

    private fun showAddPointDialog() {
        val lat = currentLatitude ?: return
        val lon = currentLongitude ?: return

        var selectedPointType = PointType.FRONT
        var selectedIntensity = FireIntensity.MEDIUM
        var selectedFireType = FireType.GROUND

        val pointTypeChips = mutableMapOf<PointType, TextView>()
        val intensityChips = mutableMapOf<FireIntensity, TextView>()
        val fireTypeChips = mutableMapOf<FireType, TextView>()

        val notesInput = EditText(this).apply {
            hint = "Комментарий"
            textSize = 13f
            setTextColor(Color.parseColor(Colors.TEXT_PRIMARY))
            setHintTextColor(Color.parseColor(Colors.TEXT_MUTED))
            background = roundedDrawable(
                color = Colors.CARD,
                radiusDp = 10,
                strokeColor = Colors.BORDER,
                strokeWidthDp = 1
            )
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 3
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }

        lateinit var updatePointTypeChips: () -> Unit
        lateinit var updateIntensityChips: () -> Unit
        lateinit var updateFireTypeChips: () -> Unit

        updatePointTypeChips = {
            pointTypeChips.forEach { (type, chip) ->
                updateChipStyle(chip, type == selectedPointType)
            }
        }

        updateIntensityChips = {
            intensityChips.forEach { (type, chip) ->
                updateChipStyle(chip, type == selectedIntensity)
            }
        }

        updateFireTypeChips = {
            fireTypeChips.forEach { (type, chip) ->
                updateChipStyle(chip, type == selectedFireType)
            }
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(14), dp(20), dp(4))

            addView(TextView(this@MainActivity).apply {
                text = "Новая точка"
                textSize = 19f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor(Colors.TEXT_PRIMARY))
            })

            addView(TextView(this@MainActivity).apply {
                val accuracy = currentHorizontalAccuracyMeters?.let {
                    " · точность %.0f м".format(it)
                } ?: ""
                text = "%.6f, %.6f%s".format(lat, lon, accuracy)
                textSize = 12f
                setTextColor(Color.parseColor(Colors.TEXT_SECONDARY))
                setPadding(0, dp(6), 0, dp(14))
            })

            addView(createDialogLabel("Тип точки"))
            addView(createChipRow(PointType.entries.toList(), selectedPointType, pointTypeChips) { type ->
                selectedPointType = type
                updatePointTypeChips()
            })

            addView(createDialogLabel("Интенсивность"))
            addView(createChipRow(FireIntensity.entries.toList(), selectedIntensity, intensityChips) { type ->
                selectedIntensity = type
                updateIntensityChips()
            })

            addView(createDialogLabel("Тип пожара"))
            addView(createChipRow(FireType.entries.toList(), selectedFireType, fireTypeChips) { type ->
                selectedFireType = type
                updateFireTypeChips()
            })

            addView(createDialogLabel("Комментарий"))
            addView(notesInput)
        }

        android.app.AlertDialog.Builder(this)
            .setView(layout)
            .setPositiveButton("Сохранить") { _, _ ->
                savePoint(
                    latitude = lat,
                    longitude = lon,
                    pointType = selectedPointType,
                    intensity = selectedIntensity,
                    fireType = selectedFireType,
                    notes = notesInput.text.toString().trim().ifEmpty { null }
                )
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun <T> createChipRow(
        values: List<T>,
        selectedValue: T,
        chipMap: MutableMap<T, TextView>,
        onSelect: (T) -> Unit
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            values.forEach { value ->
                val label = when (value) {
                    is PointType -> value.label
                    is FireIntensity -> value.label
                    is FireType -> value.label
                    else -> value.toString()
                }

                val chip = createChoiceChip(
                    textValue = label,
                    selected = value == selectedValue
                ) {
                    onSelect(value)
                }

                chipMap[value] = chip
                addView(chip)
            }
        }
    }

    private fun createDialogLabel(textValue: String): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor(Colors.TEXT_PRIMARY))
            setPadding(0, dp(10), 0, dp(7))
        }
    }

    private fun createChoiceChip(
        textValue: String,
        selected: Boolean,
        onClick: () -> Unit
    ): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = 11f
            gravity = Gravity.CENTER
            setPadding(dp(12), 0, dp(12), 0)
            updateChipStyle(this, selected)
            layoutParams = LinearLayout.LayoutParams(
                0,
                dp(32),
                1f
            ).apply {
                rightMargin = dp(7)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun updateChipStyle(chip: TextView, selected: Boolean) {
        chip.background = roundedDrawable(
            color = if (selected) Colors.GREEN else Colors.GREEN_LIGHT,
            radiusDp = 18
        )
        chip.setTextColor(Color.parseColor(if (selected) "#FFFFFF" else Colors.TEXT_PRIMARY))
        chip.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    private fun savePoint(
        latitude: Double,
        longitude: Double,
        pointType: PointType,
        intensity: FireIntensity,
        fireType: FireType,
        notes: String?
    ) {
        val mark = Mark(
            name = buildPointName(pointType),
            latitude = latitude,
            longitude = longitude,
            pointType = pointType,
            intensity = intensity,
            typeOfFire = fireType,
            notes = notes,
            horizontalAccuracyMeters = currentHorizontalAccuracyMeters
        )

        if (database.insertMark(mark) > 0) {
            Toast.makeText(this, "Точка сохранена", Toast.LENGTH_SHORT).show()
            refreshMapScreen()
        } else {
            Toast.makeText(this, "Ошибка сохранения точки", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildPointName(pointType: PointType): String {
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        val baseName = when (pointType) {
            PointType.FRONT -> "Фронт пожара"
            PointType.FLANK -> "Фланг пожара"
            PointType.REAR -> "Тыл пожара"
        }

        return "$baseName $time"
    }

    private fun createBottomMenu(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            setPadding(0, dp(7), 0, dp(8))

            addView(createMenuItem("⌖", "Карта", selected = false) {
                startActivity(Intent(this@MainActivity, OfflineMapActivity::class.java))
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
            setOnClickListener { onClick() }

            addView(TextView(this@MainActivity).apply {
                text = icon
                textSize = 18f
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor(if (selected) Colors.GREEN else Colors.TEXT_MUTED))
            })

            addView(TextView(this@MainActivity).apply {
                text = label
                textSize = 10f
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor(if (selected) Colors.GREEN else Colors.TEXT_MUTED))
                if (selected) {
                    typeface = Typeface.DEFAULT_BOLD
                }
            })
        }
    }

    private fun checkPermissions() {
        if (!locationHelper.hasLocationPermission()) {
            locationHelper.requestLocationPermission(this)
        }
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

    override fun onResume() {
        super.onResume()
        MapDownloadScheduler.startAutomaticDownloads(this)
        mapView?.onResume()
        refreshMapScreen()
    }

    override fun onPause() {
        mapView?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        locationHelper.stopLocationUpdates()
        mapView?.onDetach()
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
                Toast.makeText(this, "Разрешение получено", Toast.LENGTH_SHORT).show()
            }
        }
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

    private fun circleDrawable(color: String): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(color))
            setStroke(dp(2), Color.WHITE)
            setSize(dp(18), dp(18))
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}

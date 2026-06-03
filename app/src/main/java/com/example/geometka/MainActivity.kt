package com.example.geometka

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.geometka.data.FireIntensity
import com.example.geometka.data.FireType
import com.example.geometka.data.Mark
import com.example.geometka.data.MarkDatabase
import com.example.geometka.data.OfflineMapConfig
import com.example.geometka.data.PointType
import com.example.geometka.auth.AppSession
import com.example.geometka.helpers.LocationHelper
import com.example.geometka.maps.MapAvailability
import com.example.geometka.maps.MapDownloadRunner
import com.example.geometka.maps.MapStorage
import com.example.geometka.ui.ScreenChrome
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.datastore.MapDataStore
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.overlay.Marker
import org.mapsforge.map.android.graphics.AndroidBitmap
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.InternalRenderTheme
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
    private lateinit var pointsInfoText: TextView
    private lateinit var accuracyChip: TextView

    private var mapView: MapView? = null
    private var tileCache: TileCache? = null
    private var mapDataStore: MapDataStore? = null
    private var tileRendererLayer: TileRendererLayer? = null

    private val markLayers = mutableListOf<Marker>()
    private var currentLocationMarker: Marker? = null
    private var markInfoBubble: TextView? = null

    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private var currentHorizontalAccuracyMeters: Float? = null
    private var directMapDownloadRunning = false

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
        const val ORANGE = "#F28C28"
        const val YELLOW = "#F2C94C"
        const val BLUE = "#2F80ED"
        const val BORDER_WHITE = "#FFFFFF"
        const val GRAY = "#8A8A8A"
        const val BROWN = "#8B5A2B"
        const val LEGEND_BACKGROUND = "#E9EFE7"
        const val MAP_PLACEHOLDER = "#DCEAD7"
    }

    companion object {
        private const val TAG = "MainMap"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AndroidGraphicFactory.createInstance(application)

        window.statusBarColor = Color.parseColor(Colors.GREEN_DARK)
        window.navigationBarColor = Color.WHITE
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        ScreenChrome.apply(this)

        locationHelper = LocationHelper(this)
        database = MarkDatabase(this)

        setContentView(createLayout())

        setupCallbacks()
        checkPermissions()
        refreshMapScreen()
        requestCurrentLocationIfPossible()
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

            val topRow = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            topRow.addView(TextView(this@MainActivity).apply {
                text = OfflineMapConfig.MAP_TITLE
                textSize = 20f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
            }, LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ))

            topRow.addView(TextView(this@MainActivity).apply {
                text = "Выйти"
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                background = roundedDrawable(
                    color = Colors.GREEN_DARK,
                    radiusDp = 14
                )
                setPadding(dp(12), 0, dp(12), 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    dp(30)
                )
                setOnClickListener {
                    logout()
                }
            })

            addView(topRow)

            subtitleText = TextView(this@MainActivity).apply {
                text = "Проверка карты"
                textSize = 12f
                setTextColor(Color.parseColor("#C8DDCE"))
                setPadding(0, dp(4), 0, 0)
            }

            addView(subtitleText)
        }
    }

    private fun logout() {
        AppSession.lock(this)
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    private fun createMapBlock(): FrameLayout {
        mapContainer = FrameLayout(this).apply {
            background = roundedDrawable(
                color = Colors.CARD,
                radiusDp = 15,
                strokeColor = Colors.BORDER,
                strokeWidthDp = 1
            )

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
                dp(84)
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

        pointsInfoText = TextView(this).apply {
            text = "Сохраненных точек: ${database.getAllMarks().size}"
            textSize = 11f
            setTextColor(Color.parseColor(Colors.TEXT_SECONDARY))
            setPadding(0, dp(3), 0, 0)
        }

        textBlock.addView(addCardSubtitle)
        textBlock.addView(pointsInfoText)

        val plusButton = TextView(this).apply {
            text = "+"
            textSize = 34f
            gravity = Gravity.CENTER
            includeFontPadding = false
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

    private fun createBottomMenu(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            setPadding(0, dp(10), 0, dp(22))

            addView(createMenuItem("⌖", "Карта", selected = true) {
                refreshMapScreen()
            })

            addView(createMenuItem("●", "Точки", selected = false) {
                ScreenChrome.navigateWithoutJump(
                    this@MainActivity,
                    Intent(this@MainActivity, MarkListActivity::class.java)
                )
            })

            addView(createMenuItem("⇄", "Синхр.", selected = false) {
                ScreenChrome.navigateWithoutJump(
                    this@MainActivity,
                    Intent(this@MainActivity, SyncActivity::class.java)
                )
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

    private fun refreshMapScreen() {
        cleanupMapView()

        mapContainer.removeAllViews()
        markInfoBubble = null

        val mapPath = MapAvailability.getDownloadedMapPath(this)
        val mapFile = mapPath?.let { File(it) }
        val hasInternet = locationHelper.hasInternetConnection()

        if (mapFile != null && mapFile.exists() && mapFile.length() > 0L) {
            val created = runCatching {
                createMapsforgeMapView(mapFile)
            }.onFailure { error ->
                Log.e(TAG, "Map loading failed", error)
            }.getOrNull()

            if (created != null) {
                mapView = created

                mapContainer.addView(
                    created,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                )

                addAccuracyChip()
                addMapLegend()

                subtitleText.text = if (hasInternet) {
                    "Карта загружена · сеть доступна"
                } else {
                    "Офлайн-карта загружена · сеть недоступна"
                }

                redrawMarksOnMap()
                return
            }
        }

        subtitleText.text = if (hasInternet) {
            "Файл карты не найден · используется схема точек"
        } else {
            "Нет сети и файла карты · используется схема точек"
        }

        mapContainer.addView(createMapPlaceholder())
        addMapLegend()
        startDirectMapDownloadIfNeeded()
        updatePointsInfo()
    }

    private fun createMapsforgeMapView(mapFile: File): MapView {
        val view = MapView(this).apply {
            isClickable = true
            setBuiltInZoomControls(false)
        }

        val cache = AndroidUtil.createTileCache(
            this,
            "mapsforge-cache",
            view.model.displayModel.tileSize,
            1f,
            view.model.frameBufferModel.overdrawFactor
        )

        tileCache = cache

        val mapFileStore = MapFile(mapFile)
        mapDataStore = mapFileStore

        val boundingBox = mapFileStore.boundingBox()

        Log.d(TAG, "Map file: ${mapFile.absolutePath}")
        Log.d(TAG, "Map size: ${mapFile.length()}")
        Log.d(TAG, "Map bounding box: $boundingBox")

        val rendererLayer = TileRendererLayer(
            cache,
            mapFileStore,
            view.model.mapViewPosition,
            AndroidGraphicFactory.INSTANCE
        ).apply {
            setXmlRenderTheme(InternalRenderTheme.DEFAULT)
        }

        tileRendererLayer = rendererLayer
        view.layerManager.layers.add(rendererLayer)

        val centerLat = (boundingBox.minLatitude + boundingBox.maxLatitude) / 2.0
        val centerLon = (boundingBox.minLongitude + boundingBox.maxLongitude) / 2.0

        view.model.mapViewPosition.setMapLimit(boundingBox)
        view.model.mapViewPosition.setCenter(LatLong(centerLat, centerLon))
        view.model.mapViewPosition.setZoomLevel(OfflineMapConfig.DEFAULT_ZOOM)

        return view
    }

    private fun addAccuracyChip() {
        accuracyChip = TextView(this).apply {
            text = "GPS —"
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor(Colors.TEXT_PRIMARY))
            background = roundedDrawable(Colors.CARD, 20)
            setPadding(dp(12), 0, dp(12), 0)

            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                dp(32)
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                leftMargin = dp(14)
                topMargin = dp(14)
            }
        }

        mapContainer.addView(accuracyChip)
    }

    private fun addMapLegend() {
        val legend = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedDrawable(
                color = Colors.LEGEND_BACKGROUND,
                radiusDp = 10,
                strokeColor = Colors.BORDER,
                strokeWidthDp = 1
            )
            setPadding(dp(10), dp(8), dp(10), dp(8))
            elevation = dp(4).toFloat()

            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                topMargin = dp(14)
                rightMargin = dp(14)
            }

            addView(createLegendTitle("Интенсивность"))
            addView(createLegendRow(Colors.RED, "Высокая"))
            addView(createLegendRow(Colors.ORANGE, "Средняя"))
            addView(createLegendRow(Colors.YELLOW, "Слабая"))
            addView(createLegendTitle("Тип пожара"))
            addView(createLegendRow(Colors.BORDER_WHITE, "Низовой"))
            addView(createLegendRow(Colors.GRAY, "Верховой"))
            addView(createLegendRow(Colors.BROWN, "Торфяной"))
        }

        mapContainer.addView(legend)
    }

    private fun startDirectMapDownloadIfNeeded() {
        if (!directMapDownloadRunning) {
            startDirectMapDownload(force = false)
        }
    }

    private fun startDirectMapDownload(force: Boolean) {
        if (directMapDownloadRunning && !force) return

        directMapDownloadRunning = true

        Thread {
            runCatching {
                MapDownloadRunner.download(applicationContext)
            }

            runOnUiThread {
                directMapDownloadRunning = false
                refreshMapScreen()
            }
        }.start()
    }

    private fun createLegendTitle(textValue: String): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor(Colors.TEXT_PRIMARY))
            setPadding(0, dp(3), 0, dp(2))
        }
    }

    private fun createLegendRow(color: String, label: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(1), 0, dp(1))

            addView(TextView(this@MainActivity).apply {
                background = circleDrawable(color)
                layoutParams = LinearLayout.LayoutParams(dp(9), dp(9)).apply {
                    rightMargin = dp(6)
                }
            })

            addView(TextView(this@MainActivity).apply {
                text = label
                textSize = 10f
                setTextColor(Color.parseColor(Colors.TEXT_SECONDARY))
            })
        }
    }

    private fun createMapPlaceholder(): View {
        return PlaceholderMapView(this, database.getAllMarks()).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )

            setOnClickListener {
                Toast.makeText(
                    this@MainActivity,
                    "Файл карты должен лежать в: ${MapStorage.mapsDir(this@MainActivity).absolutePath}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showMarkInfoBubble(mark: Mark, anchorX: Float, anchorY: Float) {
        hideMarkInfoBubble()

        val bubble = TextView(this).apply {
            text = "ID: ${mark.id}\n${mark.pointType.label}\n${mark.getFormattedDate().substringAfter(" ")}"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor(Colors.TEXT_PRIMARY))
            background = roundedDrawable(
                color = Colors.CARD,
                radiusDp = 8,
                strokeColor = Colors.BORDER,
                strokeWidthDp = 1
            )
            setPadding(dp(10), dp(7), dp(10), dp(7))
            elevation = dp(6).toFloat()

            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = anchorX.toInt().coerceAtLeast(dp(8)) + dp(12)
                topMargin = (anchorY.toInt() - dp(46)).coerceAtLeast(dp(8))
            }
        }

        markInfoBubble = bubble
        mapContainer.addView(bubble)
    }

    private fun hideMarkInfoBubble() {
        markInfoBubble?.let { bubble ->
            mapContainer.removeView(bubble)
        }
        markInfoBubble = null
    }

    private fun setupCallbacks() {
        locationHelper.onLocationReceived = { lat, lon, _, accuracy ->
            currentLatitude = lat
            currentLongitude = lon
            currentHorizontalAccuracyMeters = accuracy

            updateAccuracyInfo()

            val map = mapView
            if (map != null) {
                val position = LatLong(lat, lon)
                addOrUpdateCurrentLocationMarker(position)
            }
        }

        locationHelper.onLocationError = { message ->
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            subtitleText.text = message
        }

        locationHelper.onStatusUpdate = { message ->
            subtitleText.text = message
        }
    }

    private fun requestCurrentLocationIfPossible() {
        if (!locationHelper.hasLocationPermission()) {
            return
        }

        if (!locationHelper.isGpsEnabled() && !locationHelper.isNetworkEnabled()) {
            return
        }

        locationHelper.getCurrentLocation(allowCached = true)
    }

    private fun addOrUpdateCurrentLocationMarker(position: LatLong) {
        val map = mapView ?: return

        currentLocationMarker?.let {
            map.layerManager.layers.remove(it)
        }

            val marker = Marker(
            position,
            createCircleBitmap(
                fillColor = Color.parseColor(Colors.BLUE),
                sizePx = dp(18)
            ),
            0,
            -dp(9)
        )

        currentLocationMarker = marker
        map.layerManager.layers.add(marker)
    }

    private fun redrawMarksOnMap() {
        val map = mapView ?: return

        markLayers.forEach { marker ->
            map.layerManager.layers.remove(marker)
        }
        markLayers.clear()

        val marks = database.getAllMarks()
        pointsInfoText.text = "Сохраненных точек: ${marks.size}"

        marks.forEach { mark ->
            val marker = object : Marker(
                LatLong(mark.latitude, mark.longitude),
                createCircleBitmap(
                    fillColor = colorForMark(mark),
                    sizePx = dp(22),
                    borderColor = borderColorForMark(mark)
                ),
                0,
                -dp(11)
            ) {
                override fun onTap(
                    tapLatLong: LatLong?,
                    layerXY: org.mapsforge.core.model.Point?,
                    tapXY: org.mapsforge.core.model.Point?
                ): Boolean {
                    val x = tapXY?.x?.toFloat() ?: 0f
                    val y = tapXY?.y?.toFloat() ?: 0f
                    showMarkInfoBubble(mark, x, y)
                    return true
                }
            }

            markLayers.add(marker)
            map.layerManager.layers.add(marker)
        }

        map.invalidate()
    }

    private fun updatePointsInfo() {
        if (this::pointsInfoText.isInitialized) {
            pointsInfoText.text = "Сохраненных точек: ${database.getAllMarks().size}"
        }
    }

    private fun colorForMark(mark: Mark): Int {
        return when (mark.intensity) {
            FireIntensity.HIGH -> Color.parseColor(Colors.RED)
            FireIntensity.MEDIUM -> Color.parseColor(Colors.ORANGE)
            FireIntensity.LOW -> Color.parseColor(Colors.YELLOW)
        }
    }

    private fun borderColorForMark(mark: Mark): Int {
        return when (mark.typeOfFire) {
            FireType.GROUND -> Color.parseColor(Colors.BORDER_WHITE)
            FireType.CROWN -> Color.parseColor(Colors.GRAY)
            FireType.PEAT -> Color.parseColor(Colors.BROWN)
        }
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

        Toast.makeText(this, "Определение координат…", Toast.LENGTH_SHORT).show()

        locationHelper.onLocationReceived = { lat, lon, _, accuracy ->
            currentLatitude = lat
            currentLongitude = lon
            currentHorizontalAccuracyMeters = accuracy

            updateAccuracyInfo()

            val map = mapView
            if (map != null) {
                addOrUpdateCurrentLocationMarker(LatLong(lat, lon))
            }

            showAddPointDialog()
        }

        locationHelper.getCurrentLocation(allowCached = false)
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

        if (this::accuracyChip.isInitialized) {
            accuracyChip.text = if (accuracy != null) {
                "GPS %.0f м".format(accuracy)
            } else {
                "GPS —"
            }
        }
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

            addView(
                createChipRow(
                    values = PointType.values().toList(),
                    selectedValue = selectedPointType,
                    chipMap = pointTypeChips
                ) { type ->
                    selectedPointType = type
                    updatePointTypeChips()
                }
            )

            addView(createDialogLabel("Интенсивность"))

            addView(
                createChipRow(
                    values = FireIntensity.values().toList(),
                    selectedValue = selectedIntensity,
                    chipMap = intensityChips
                ) { type ->
                    selectedIntensity = type
                    updateIntensityChips()
                }
            )

            addView(createDialogLabel("Тип пожара"))

            addView(
                createChipRow(
                    values = FireType.values().toList(),
                    selectedValue = selectedFireType,
                    chipMap = fireTypeChips
                ) { type ->
                    selectedFireType = type
                    updateFireTypeChips()
                }
            )

            addView(createDialogLabel("Комментарий"))
            addView(notesInput)
        }

        AlertDialog.Builder(this)
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
            .setNegativeButton("Отмена") { _, _ ->
                setupCallbacks()
            }
            .show()
    }

    private fun <T> createChipRow(
        values: List<T>,
        selectedValue: T,
        chipMap: MutableMap<T, TextView>,
        onSelect: (T) -> Unit
    ): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

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
            row.addView(chip)
        }

        return HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(row)
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
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(32)
            ).apply {
                rightMargin = dp(7)
            }

            setOnClickListener {
                onClick()
            }
        }
    }

    private fun updateChipStyle(chip: TextView, selected: Boolean) {
        chip.background = roundedDrawable(
            color = if (selected) Colors.GREEN else Colors.GREEN_LIGHT,
            radiusDp = 18
        )

        chip.setTextColor(
            Color.parseColor(if (selected) "#FFFFFF" else Colors.TEXT_PRIMARY)
        )

        chip.typeface = if (selected) {
            Typeface.DEFAULT_BOLD
        } else {
            Typeface.DEFAULT
        }
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
            setupCallbacks()
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

    private fun checkPermissions() {
        if (!locationHelper.hasLocationPermission()) {
            locationHelper.requestLocationPermission(this)
        }
    }

    private fun showGpsDialog() {
        AlertDialog.Builder(this)
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
        ScreenChrome.apply(this)
        refreshMapScreen()
        requestCurrentLocationIfPossible()
    }

    override fun onDestroy() {
        super.onDestroy()
        locationHelper.stopLocationUpdates()
        cleanupMapView()
        AndroidGraphicFactory.clearResourceMemoryCache()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (
            requestCode == LocationHelper.LOCATION_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Разрешение получено", Toast.LENGTH_SHORT).show()
            requestCurrentLocationIfPossible()
        }
    }

    private fun cleanupMapView() {
        val map = mapView

        markLayers.clear()
        currentLocationMarker = null
        tileRendererLayer = null

        mapDataStore?.close()
        mapDataStore = null

        tileCache?.destroy()
        tileCache = null

        if (map != null) {
            runCatching {
                map.destroyAll()
            }
        }

        mapView = null
    }

    private fun createCircleBitmap(
        fillColor: Int,
        sizePx: Int,
        borderColor: Int = Color.WHITE
    ): org.mapsforge.core.graphics.Bitmap {
        val androidBitmap = android.graphics.Bitmap.createBitmap(
            sizePx,
            sizePx,
            android.graphics.Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(androidBitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = borderColor
        }

        canvas.drawCircle(
            sizePx / 2f,
            sizePx / 2f,
            sizePx / 2f,
            paint
        )

        paint.color = fillColor

        canvas.drawCircle(
            sizePx / 2f,
            sizePx / 2f,
            sizePx * 0.35f,
            paint
        )

        return AndroidBitmap(androidBitmap)
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

    private inner class PlaceholderMapView(
        context: android.content.Context,
        private val marks: List<Mark>
    ) : View(context) {

        private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(Colors.MAP_PLACEHOLDER)
        }

        private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B8CEB4")
            strokeWidth = dp(2).toFloat()
        }

        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(Colors.TEXT_SECONDARY)
            textSize = dp(14).toFloat()
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }

        private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(Colors.TEXT_MUTED)
            textSize = dp(11).toFloat()
            textAlign = Paint.Align.CENTER
        }

        private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor(Colors.RED)
        }

        private val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (event.action != MotionEvent.ACTION_UP || marks.isEmpty()) {
                return true
            }

            val points = normalizeMarks(marks)
            val pairedMarks = marks.reversed()
            val hitRadius = dp(24)
            val hitRadiusSquared = hitRadius * hitRadius

            points.forEachIndexed { index, point ->
                val pointX = point.first * width
                val pointY = point.second * height
                val dx = event.x - pointX
                val dy = event.y - pointY

                if (dx * dx + dy * dy <= hitRadiusSquared) {
                    showMarkInfoBubble(pairedMarks[index], pointX, pointY)
                    return true
                }
            }

            hideMarkInfoBubble()
            return false
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

            drawGrid(canvas)
            drawScale(canvas)

            val points = if (marks.isNotEmpty()) {
                normalizeMarks(marks)
            } else {
                listOf(
                    0.28f to 0.58f,
                    0.42f to 0.62f,
                    0.57f to 0.55f,
                    0.70f to 0.43f
                )
            }

            for (index in 0 until points.size - 1) {
                val first = points[index]
                val second = points[index + 1]

                canvas.drawLine(
                    first.first * width,
                    first.second * height,
                    second.first * width,
                    second.second * height,
                    linePaint
                )
            }

            points.forEachIndexed { index, point ->
                val x = point.first * width
                val y = point.second * height
                val mark = marks.reversed().getOrNull(index)

                whitePaint.color = mark?.let { borderColorForMark(it) } ?: Color.WHITE
                canvas.drawCircle(x, y, dp(9).toFloat(), whitePaint)
                pointPaint.color = mark?.let { colorForMark(it) } ?: Color.parseColor(Colors.RED)
                canvas.drawCircle(x, y, dp(6).toFloat(), pointPaint)
            }

            if (marks.isEmpty()) {
                canvas.drawText(
                    "Файл карты не найден",
                    width / 2f,
                    height / 2f - dp(10),
                    textPaint
                )

                canvas.drawText(
                    "Показана схема расположения точек",
                    width / 2f,
                    height / 2f + dp(14),
                    subTextPaint
                )
            }
        }

        private fun drawScale(canvas: Canvas) {
            val left = dp(14).toFloat()
            val bottom = height - dp(16).toFloat()

            if (marks.size < 2) {
                canvas.drawText(
                    "Масштаб: схема",
                    left + dp(46),
                    bottom,
                    subTextPaint.apply { textAlign = Paint.Align.LEFT }
                )
                subTextPaint.textAlign = Paint.Align.CENTER
                return
            }

            val metersPerPixel = estimateMetersPerPixel() ?: return
            val targetWidthPx = dp(96).coerceAtMost((width * 0.34f).toInt()).coerceAtLeast(dp(56))
            val rawDistanceMeters = metersPerPixel * targetWidthPx
            val niceDistanceMeters = niceScaleDistance(rawDistanceMeters)
            val scaleWidthPx = (niceDistanceMeters / metersPerPixel).toFloat()

            val y = bottom - dp(10)
            val x2 = left + scaleWidthPx

            linePaint.strokeWidth = dp(3).toFloat()
            linePaint.color = Color.parseColor(Colors.TEXT_PRIMARY)
            canvas.drawLine(left, y, x2, y, linePaint)
            canvas.drawLine(left, y - dp(4), left, y + dp(4), linePaint)
            canvas.drawLine(x2, y - dp(4), x2, y + dp(4), linePaint)

            subTextPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(
                formatScaleDistance(niceDistanceMeters),
                left,
                y - dp(8),
                subTextPaint
            )
            subTextPaint.textAlign = Paint.Align.CENTER

            linePaint.color = Color.parseColor("#B8CEB4")
            linePaint.strokeWidth = dp(2).toFloat()
        }

        private fun estimateMetersPerPixel(): Double? {
            if (width <= 0 || marks.size < 2) return null

            val minLat = marks.minOf { it.latitude }
            val maxLat = marks.maxOf { it.latitude }
            val minLon = marks.minOf { it.longitude }
            val maxLon = marks.maxOf { it.longitude }
            val lonRange = maxLon - minLon
            if (lonRange <= 0.0) return null

            val centerLatRadians = Math.toRadians((minLat + maxLat) / 2.0)
            val metersPerDegreeLon = 111_320.0 * kotlin.math.cos(centerLatRadians)
            val visibleDataWidthPx = width * 0.76

            return lonRange * metersPerDegreeLon / visibleDataWidthPx
        }

        private fun niceScaleDistance(distanceMeters: Double): Double {
            val candidates = listOf(
                1.0, 2.0, 5.0,
                10.0, 20.0, 50.0,
                100.0, 200.0, 500.0,
                1_000.0, 2_000.0, 5_000.0,
                10_000.0, 20_000.0, 50_000.0
            )

            return candidates.lastOrNull { it <= distanceMeters } ?: candidates.first()
        }

        private fun formatScaleDistance(distanceMeters: Double): String {
            return if (distanceMeters >= 1_000.0) {
                "Масштаб: %.0f км".format(distanceMeters / 1_000.0)
            } else {
                "Масштаб: %.0f м".format(distanceMeters)
            }
        }

        private fun drawGrid(canvas: Canvas) {
            val step = dp(54)

            var x = 0
            while (x < width) {
                canvas.drawLine(
                    x.toFloat(),
                    0f,
                    x.toFloat(),
                    height.toFloat(),
                    linePaint
                )
                x += step
            }

            var y = 0
            while (y < height) {
                canvas.drawLine(
                    0f,
                    y.toFloat(),
                    width.toFloat(),
                    y.toFloat(),
                    linePaint
                )
                y += step
            }
        }

        private fun normalizeMarks(source: List<Mark>): List<Pair<Float, Float>> {
            if (source.size == 1) {
                return listOf(0.5f to 0.5f)
            }

            val minLat = source.minOf { it.latitude }
            val maxLat = source.maxOf { it.latitude }
            val minLon = source.minOf { it.longitude }
            val maxLon = source.maxOf { it.longitude }

            val latRange = (maxLat - minLat).takeIf { it > 0.0 } ?: 1.0
            val lonRange = (maxLon - minLon).takeIf { it > 0.0 } ?: 1.0

            return source.reversed().map { mark ->
                val x = ((mark.longitude - minLon) / lonRange).toFloat()
                val y = (1f - ((mark.latitude - minLat) / latRange).toFloat())

                val paddedX = 0.12f + x * 0.76f
                val paddedY = 0.12f + y * 0.76f

                paddedX to paddedY
            }
        }
    }
}

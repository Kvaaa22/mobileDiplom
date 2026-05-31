package com.example.geometka

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.geometka.data.Mark
import com.example.geometka.data.MarkDatabase
import com.example.geometka.data.OfflineMapConfig
import com.example.geometka.data.PointType
import com.example.geometka.helpers.LocationHelper
import com.example.geometka.helpers.OfflineMapFileManager
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.android.graphics.AndroidBitmap
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.datastore.MapDataStore
import org.mapsforge.map.layer.cache.TileCache
import org.mapsforge.map.layer.overlay.Marker
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.InternalRenderTheme

class OfflineMapActivity : Activity() {

    private lateinit var database: MarkDatabase
    private lateinit var locationHelper: LocationHelper
    private lateinit var mapView: MapView
    private lateinit var subtitleText: TextView
    private lateinit var accuracyChip: TextView
    private lateinit var pointsInfoText: TextView

    private var tileCache: TileCache? = null
    private var mapDataStore: MapDataStore? = null
    private var tileRendererLayer: TileRendererLayer? = null

    private val markLayers = mutableListOf<Marker>()
    private var currentLocationMarker: Marker? = null

    private object Colors {
        const val GREEN_DARK = "#0B2A18"
        const val GREEN = "#155E32"
        const val BACKGROUND = "#F8FBF7"
        const val CARD = "#FFFFFF"
        const val TEXT_PRIMARY = "#1F2A22"
        const val TEXT_SECONDARY = "#6F7D73"
        const val TEXT_MUTED = "#8FA098"
        const val RED = "#D7351F"
        const val ORANGE = "#F28C28"
        const val BLUE = "#2F80ED"
        const val BORDER = "#DCE8DE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AndroidGraphicFactory.createInstance(application)

        window.statusBarColor = Color.parseColor(Colors.GREEN_DARK)
        window.navigationBarColor = Color.WHITE

        database = MarkDatabase(this)
        locationHelper = LocationHelper(this)

        setContentView(createMainLayout())

        setupLocationCallbacks()
        loadOfflineMap()
        requestCurrentLocation()
    }

    private fun createMainLayout(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(Colors.BACKGROUND))
        }

        root.addView(createHeader())

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(0))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        content.addView(createMapCard())
        content.addView(createAddPointCard())

        root.addView(content)
        root.addView(createBottomMenu())

        return root
    }

    private fun createHeader(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(Colors.GREEN))
            setPadding(dp(16), dp(18), dp(16), dp(18))

            addView(TextView(this@OfflineMapActivity).apply {
                text = OfflineMapConfig.MAP_TITLE
                textSize = 21f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
            })

            subtitleText = TextView(this@OfflineMapActivity).apply {
                text = OfflineMapConfig.MAP_SUBTITLE
                textSize = 13f
                setTextColor(Color.parseColor("#C8DDCE"))
                setPadding(0, dp(4), 0, 0)
            }

            addView(subtitleText)
        }
    }

    private fun createMapCard(): FrameLayout {
        val container = FrameLayout(this).apply {
            background = roundedDrawable(
                color = Colors.CARD,
                radiusDp = 16,
                strokeColor = Colors.BORDER,
                strokeWidthDp = 1
            )

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                bottomMargin = dp(14)
            }
        }

        mapView = MapView(this).apply {
            isClickable = true
            setBuiltInZoomControls(false)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        container.addView(mapView)

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

        container.addView(accuracyChip)

        return container
    }

    private fun createAddPointCard(): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = roundedDrawable(Colors.CARD, 16)
            setPadding(dp(16), dp(12), dp(16), dp(12))

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(78)
            ).apply {
                bottomMargin = dp(8)
            }

            setOnClickListener {
                startActivity(Intent(this@OfflineMapActivity, MainActivity::class.java))
            }
        }

        val textBlock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        textBlock.addView(TextView(this).apply {
            text = "Добавить новую точку →"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor(Colors.TEXT_PRIMARY))
        })

        pointsInfoText = TextView(this).apply {
            text = "Сохраненных точек: 0"
            textSize = 11f
            setTextColor(Color.parseColor(Colors.TEXT_SECONDARY))
            setPadding(0, dp(5), 0, 0)
        }

        textBlock.addView(pointsInfoText)

        val plusButton = TextView(this).apply {
            text = "+"
            textSize = 34f
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = roundedDrawable(Colors.RED, 100)

            layoutParams = LinearLayout.LayoutParams(dp(56), dp(56))
        }

        card.addView(textBlock)
        card.addView(plusButton)

        return card
    }

    private fun createBottomMenu(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            setPadding(0, dp(7), 0, dp(8))

            addView(createMenuItem("⌖", "Карта", selected = true) {
                // Уже на экране карты.
            })

            addView(createMenuItem("●", "Точки", selected = false) {
                startActivity(Intent(this@OfflineMapActivity, MarkListActivity::class.java))
            })

            addView(createMenuItem("⇄", "Синхр.", selected = false) {
                startActivity(Intent(this@OfflineMapActivity, SyncActivity::class.java))
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
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener { onClick() }

            addView(TextView(this@OfflineMapActivity).apply {
                text = icon
                textSize = 18f
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor(if (selected) Colors.GREEN else Colors.TEXT_MUTED))
            })

            addView(TextView(this@OfflineMapActivity).apply {
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

    private fun loadOfflineMap() {
        try {
            val mapFile = OfflineMapFileManager.ensureMapFile(this)

            tileCache = AndroidUtil.createTileCache(
                this,
                "mapsforge-cache",
                mapView.model.displayModel.tileSize,
                1f,
                mapView.model.frameBufferModel.overdrawFactor
            )

            val mapFileStore = MapFile(mapFile)
            mapDataStore = mapFileStore

            val rendererLayer = TileRendererLayer(
                tileCache,
                mapFileStore,
                mapView.model.mapViewPosition,
                AndroidGraphicFactory.INSTANCE
            ).apply {
                setXmlRenderTheme(InternalRenderTheme.DEFAULT)
            }

            tileRendererLayer = rendererLayer
            mapView.layerManager.layers.add(rendererLayer)

            mapView.model.mapViewPosition.setCenter(
                LatLong(
                    OfflineMapConfig.DEFAULT_CENTER_LAT,
                    OfflineMapConfig.DEFAULT_CENTER_LON
                )
            )
            mapView.model.mapViewPosition.setZoomLevel(OfflineMapConfig.DEFAULT_ZOOM)

            redrawMarks()
        } catch (exception: Exception) {
            Toast.makeText(
                this,
                "Не удалось открыть карту: ${exception.message}",
                Toast.LENGTH_LONG
            ).show()

            subtitleText.text = "Проверь файл app/src/main/assets/maps/fire_area.map"
        }
    }

    private fun setupLocationCallbacks() {
        locationHelper.onLocationReceived = { lat, lon, _, accuracy ->
            val accuracyText = accuracy?.let { "%.0f м".format(it) } ?: "—"
            accuracyChip.text = "GPS $accuracyText"

            val position = LatLong(lat, lon)
            addOrUpdateCurrentLocationMarker(position)

            if (database.getAllMarks().isEmpty()) {
                mapView.model.mapViewPosition.setCenter(position)
            }
        }

        locationHelper.onLocationError = { message ->
            accuracyChip.text = "GPS —"
            subtitleText.text = message
        }

        locationHelper.onStatusUpdate = { message ->
            subtitleText.text = message
        }
    }

    private fun requestCurrentLocation() {
        if (!locationHelper.hasLocationPermission()) {
            locationHelper.requestLocationPermission(this)
            return
        }

        locationHelper.getCurrentLocation(allowCached = true)
    }

    private fun addOrUpdateCurrentLocationMarker(position: LatLong) {
        currentLocationMarker?.let {
            mapView.layerManager.layers.remove(it)
        }

        val marker = Marker(
            position,
            createCircleBitmap(Color.parseColor(Colors.BLUE), dp(18)),
            0,
            -dp(9)
        )

        currentLocationMarker = marker
        mapView.layerManager.layers.add(marker)
    }

    private fun redrawMarks() {
        if (!this::mapView.isInitialized) return

        markLayers.forEach { marker ->
            mapView.layerManager.layers.remove(marker)
        }
        markLayers.clear()

        val marks = database.getAllMarks()
        pointsInfoText.text = "Сохраненных точек: ${marks.size}"

        marks.forEach { mark ->
            val marker = Marker(
                LatLong(mark.latitude, mark.longitude),
                createCircleBitmap(colorForMark(mark), dp(22)),
                0,
                -dp(11)
            )

            markLayers.add(marker)
            mapView.layerManager.layers.add(marker)
        }

        if (marks.isNotEmpty()) {
            val newest = marks.first()
            mapView.model.mapViewPosition.setCenter(
                LatLong(newest.latitude, newest.longitude)
            )
        }
    }

    private fun colorForMark(mark: Mark): Int {
        return when (mark.pointType) {
            PointType.FRONT -> Color.parseColor(Colors.RED)
            PointType.FLANK -> Color.parseColor(Colors.ORANGE)
            PointType.REAR -> Color.parseColor(Colors.GREEN)
        }
    }

    private fun createCircleBitmap(
        color: Int,
        sizePx: Int
    ): org.mapsforge.core.graphics.Bitmap {
        val androidBitmap = android.graphics.Bitmap.createBitmap(
            sizePx,
            sizePx,
            android.graphics.Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(androidBitmap)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
        }

        canvas.drawCircle(
            sizePx / 2f,
            sizePx / 2f,
            sizePx / 2f,
            paint
        )

        paint.color = color

        canvas.drawCircle(
            sizePx / 2f,
            sizePx / 2f,
            sizePx * 0.35f,
            paint
        )

        return AndroidBitmap(androidBitmap)
    }

    override fun onResume() {
        super.onResume()
        redrawMarks()
        requestCurrentLocation()
    }

    override fun onDestroy() {
        super.onDestroy()

        locationHelper.stopLocationUpdates()

        markLayers.clear()

        currentLocationMarker = null
        tileRendererLayer = null

        mapDataStore?.close()
        mapDataStore = null

        tileCache?.destroy()
        tileCache = null

        if (this::mapView.isInitialized) {
            mapView.destroyAll()
        }

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
            requestCurrentLocation()
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

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
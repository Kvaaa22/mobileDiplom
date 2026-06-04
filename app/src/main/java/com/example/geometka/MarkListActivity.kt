package com.example.geometka

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import com.example.geometka.api.MarkSyncClient
import com.example.geometka.data.FireIntensity
import com.example.geometka.data.Mark
import com.example.geometka.data.MarkDatabase
import com.example.geometka.data.PointType
import com.example.geometka.data.SyncStatus
import com.example.geometka.data.VerificationStatus
import com.example.geometka.ui.ScreenChrome

class MarkListActivity : Activity() {

    private lateinit var database: MarkDatabase

    private lateinit var marksContainer: LinearLayout
    private lateinit var searchInput: EditText
    private lateinit var localCountText: TextView

    private lateinit var allFilterButton: TextView
    private lateinit var unsentFilterButton: TextView
    private lateinit var sentFilterButton: TextView
    private lateinit var frontFilterButton: TextView
    private lateinit var flankFilterButton: TextView
    private lateinit var rearFilterButton: TextView

    private var allMarks: List<Mark> = emptyList()
    private var currentFilter: MarkFilter = MarkFilter.ALL
    private var searchQuery: String = ""
    private var isSyncRunning = false

    private enum class MarkFilter {
        ALL,
        UNSENT,
        SENT,
        FRONT,
        FLANK,
        REAR
    }

    private object Colors {
        const val GREEN_DARK = "#0B2A18"
        const val GREEN = "#155E32"
        const val GREEN_LIGHT = "#EAF3EB"
        const val GREEN_TEXT = "#145A31"

        const val BACKGROUND = "#F7FAF7"
        const val CARD = "#FFFFFF"
        const val BORDER = "#C8DDC9"

        const val TEXT_PRIMARY = "#1F2A22"
        const val TEXT_SECONDARY = "#7D8A80"
        const val TEXT_MUTED = "#9AA69E"

        const val RED = "#D23A2E"
        const val ORANGE = "#E26A1B"
        const val YELLOW = "#F2C94C"
        const val CHIP_ORANGE = "#FCE8D7"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Убираем нижний ряд системных кнопок навигации
        ScreenChrome.apply(this)
        window.statusBarColor = Color.parseColor(Colors.GREEN_DARK)

        database = MarkDatabase(this)

        setContentView(createLayout())
        
        // Предотвращаем автоматическое открытие клавиатуры
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        
        loadMarks()
    }

    private fun createLayout(): View {
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(Colors.BACKGROUND))
        }

        rootLayout.addView(createHeader())
        rootLayout.addView(createContent(), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))
        rootLayout.addView(createBottomNavigation())

        return rootLayout
    }

    private fun createHeader(): View {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(Colors.GREEN))
            setPadding(dp(18), dp(18), dp(18), dp(16))
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleBlock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val title = TextView(this).apply {
            text = "Сохранённые точки"
            textSize = 20f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }

        localCountText = TextView(this).apply {
            text = "Локально: 0 • отправлено: 0"
            textSize = 12f
            setTextColor(Color.parseColor("#BFD7C5"))
            setPadding(0, dp(4), 0, 0)
        }

        titleBlock.addView(title)
        titleBlock.addView(localCountText)

        topRow.addView(titleBlock, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        header.addView(topRow)

        return header
    }

    private fun createContent(): View {
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), 0)
        }

        searchInput = createSearchInput()
        wrapper.addView(searchInput)

        wrapper.addView(createFiltersRow())

        val scrollView = ScrollView(this).apply {
            isFillViewport = false
        }

        marksContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(16))
        }

        scrollView.addView(marksContainer)

        wrapper.addView(scrollView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))

        wrapper.addView(createSendButton())

        return wrapper
    }

    private fun createSearchInput(): EditText {
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.WHITE)
            cornerRadius = dp(10).toFloat()
            setStroke(dp(1), Color.parseColor(Colors.BORDER))
        }

        return EditText(this).apply {
            hint = "Поиск по типу, комментарию, времени"
            textSize = 13f
            setTextColor(Color.parseColor(Colors.TEXT_PRIMARY))
            setHintTextColor(Color.parseColor(Colors.TEXT_MUTED))
            background = bg
            setSingleLine(true)
            setPadding(dp(16), 0, dp(16), 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(44)
            ).apply {
                bottomMargin = dp(12)
            }

            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) = Unit

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    searchQuery = s?.toString()?.trim().orEmpty()
                    renderMarks()
                }

                override fun afterTextChanged(s: Editable?) = Unit
            })
        }
    }

    private fun createFiltersRow(): View {
        val horizontalScroll = HorizontalScrollView(this).apply {
            isFillViewport = true
            isHorizontalScrollBarEnabled = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(8))

            allFilterButton = createFilterChip("Все", selected = true) {
                currentFilter = MarkFilter.ALL
                updateFilterButtons()
                renderMarks()
            }

            unsentFilterButton = createFilterChip("Не отправленные", selected = false) {
                currentFilter = MarkFilter.UNSENT
                updateFilterButtons()
                renderMarks()
            }

            sentFilterButton = createFilterChip("Отправленные", selected = false) {
                currentFilter = MarkFilter.SENT
                updateFilterButtons()
                renderMarks()
            }

            frontFilterButton = createFilterChip("Фронт", selected = false) {
                currentFilter = MarkFilter.FRONT
                updateFilterButtons()
                renderMarks()
            }

            flankFilterButton = createFilterChip("Фланг", selected = false) {
                currentFilter = MarkFilter.FLANK
                updateFilterButtons()
                renderMarks()
            }

            rearFilterButton = createFilterChip("Тыл", selected = false) {
                currentFilter = MarkFilter.REAR
                updateFilterButtons()
                renderMarks()
            }

            addView(allFilterButton)
            addView(unsentFilterButton)
            addView(sentFilterButton)
            addView(frontFilterButton)
            addView(flankFilterButton)
            addView(rearFilterButton)
        }

        horizontalScroll.addView(row)
        return horizontalScroll
    }

    private fun createFilterChip(
        textValue: String,
        selected: Boolean,
        onClick: () -> Unit
    ): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor(Colors.TEXT_PRIMARY))
            background = chipDrawable(selected, textValue == "Не отправленные")
            setPadding(dp(16), 0, dp(16), 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(30)
            ).apply {
                rightMargin = dp(8)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun createSendButton(): Button {
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor(Colors.GREEN))
            cornerRadius = dp(9).toFloat()
        }

        return Button(this).apply {
            text = "Передать неотправленные"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = bg
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
            ).apply {
                leftMargin = dp(3)
                rightMargin = dp(3)
                topMargin = dp(4)
                bottomMargin = dp(18)
            }
            setOnClickListener {
                syncMarksFromList()
            }
        }
    }

    private fun syncMarksFromList() {
        if (isSyncRunning) {
            Toast.makeText(this, "Синхронизация уже идет", Toast.LENGTH_SHORT).show()
            return
        }

        val marksToSend = database.getUnsyncedMarks()
        isSyncRunning = true

        Toast.makeText(
            this,
            if (marksToSend.isEmpty()) {
                "Проверяем статусы точек"
            } else {
                "Отправляем точек: ${marksToSend.size}"
            },
            Toast.LENGTH_SHORT
        ).show()

        Thread {
            val client = MarkSyncClient(applicationContext)

            try {
                marksToSend.forEach { mark ->
                    database.updateSyncStatus(mark.id, SyncStatus.PENDING)
                }

                val syncResults = client.sendMarks(marksToSend)

                marksToSend.forEach { mark ->
                    database.updateSyncStatus(mark.id, SyncStatus.SYNCED)
                }

                syncResults.forEach { result ->
                    result.verificationStatus?.let { status ->
                        database.updateVerificationStatus(result.localId, status)
                    }
                }

                runOnUiThread {
                    isSyncRunning = false
                    loadMarks()
                    Toast.makeText(
                        this,
                        "Отправлено: ${marksToSend.size}, статусов: ${syncResults.size}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (error: Exception) {
                marksToSend.forEach { mark ->
                    database.updateSyncStatus(mark.id, SyncStatus.LOCAL)
                }

                runOnUiThread {
                    isSyncRunning = false
                    loadMarks()
                    Toast.makeText(
                        this,
                        "Ошибка синхронизации: ${error.message ?: "неизвестная ошибка"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun createBottomNavigation(): LinearLayout {
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            setPadding(0, dp(10), 0, dp(22))
        }

        nav.addView(createNavItem("⌖", "Карта", selected = false) {
            ScreenChrome.navigateWithoutJump(
                this@MarkListActivity,
                Intent(this@MarkListActivity, MainActivity::class.java)
            )
        })

        nav.addView(createNavItem("●", "Точки", selected = true) {
            // Уже здесь
        })

        nav.addView(createNavItem("⇄", "Синхр.", selected = false) {
            ScreenChrome.navigateWithoutJump(
                this@MarkListActivity,
                Intent(this@MarkListActivity, SyncActivity::class.java)
            )
        })

        return nav
    }

    private fun createNavItem(
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

            addView(TextView(this@MarkListActivity).apply {
                text = icon
                textSize = 18f
                gravity = Gravity.CENTER
                setTextColor(
                    Color.parseColor(
                        if (selected) Colors.GREEN else Colors.TEXT_MUTED
                    )
                )
            })

            addView(TextView(this@MarkListActivity).apply {
                text = label
                textSize = 10f
                gravity = Gravity.CENTER
                setTextColor(
                    Color.parseColor(
                        if (selected) Colors.GREEN_TEXT else Colors.TEXT_MUTED
                    )
                )
                if (selected) {
                    typeface = Typeface.DEFAULT_BOLD
                }
            })
        }
    }

    private fun loadMarks() {
        allMarks = database.getAllMarks()
        updateCounters()
        renderMarks()
    }

    private fun updateCounters() {
        val localCount = allMarks.count {
            it.syncStatus == com.example.geometka.data.SyncStatus.LOCAL ||
                it.syncStatus == com.example.geometka.data.SyncStatus.PENDING
        }
        val sentCount = allMarks.count { it.syncStatus == com.example.geometka.data.SyncStatus.SYNCED }

        localCountText.text = "Локально: $localCount • отправлено: $sentCount"
    }

    private fun renderMarks() {
        marksContainer.removeAllViews()

        val filteredMarks = allMarks
            .filterByCurrentFilter()
            .filterBySearchQuery()

        if (filteredMarks.isEmpty()) {
            marksContainer.addView(createEmptyView())
        } else {
            filteredMarks.forEach { mark ->
                marksContainer.addView(createMarkCard(mark))
            }
        }
    }

    private fun List<Mark>.filterByCurrentFilter(): List<Mark> {
        return when (currentFilter) {
            MarkFilter.ALL -> this
            MarkFilter.UNSENT -> filter {
                it.syncStatus == com.example.geometka.data.SyncStatus.LOCAL ||
                    it.syncStatus == com.example.geometka.data.SyncStatus.PENDING
            }
            MarkFilter.SENT -> filter { it.syncStatus == com.example.geometka.data.SyncStatus.SYNCED }
            MarkFilter.FRONT -> filter { it.pointType == com.example.geometka.data.PointType.FRONT }
            MarkFilter.FLANK -> filter { it.pointType == com.example.geometka.data.PointType.FLANK }
            MarkFilter.REAR -> filter { it.pointType == com.example.geometka.data.PointType.REAR }
        }
    }

    private fun List<Mark>.filterBySearchQuery(): List<Mark> {
        if (searchQuery.isBlank()) return this

        val query = searchQuery.lowercase()

        return filter { mark ->
            mark.name.lowercase().contains(query) ||
                    mark.notes.orEmpty().lowercase().contains(query) ||
                    mark.pointType.label.lowercase().contains(query) ||
                    mark.intensity.label.lowercase().contains(query) ||
                    mark.typeOfFire.label.lowercase().contains(query) ||
                    mark.verificationStatus.label.lowercase().contains(query) ||
                    mark.getFormattedDate().lowercase().contains(query)
        }
    }

    private fun createEmptyView(): TextView {
        return TextView(this).apply {
            text = "Пока нет сохранённых точек\n\nПолучите координаты и создайте первую точку."
            textSize = 15f
            setTextColor(Color.parseColor(Colors.TEXT_SECONDARY))
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(70), dp(24), dp(24))
        }
    }

    private fun createMarkCard(mark: Mark): LinearLayout {
        val cardBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor(Colors.CARD))
            cornerRadius = dp(12).toFloat()
            setStroke(dp(1), Color.parseColor(Colors.BORDER))
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = cardBg
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(79)
            ).apply {
                bottomMargin = dp(10)
            }
        }

        val redDot = TextView(this).apply {
            background = circleDrawable(colorForMark(mark))
            layoutParams = LinearLayout.LayoutParams(dp(14), dp(14)).apply {
                rightMargin = dp(12)
            }
        }

        val textBlock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = buildCardTitle(mark)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor(Colors.TEXT_PRIMARY))
            maxLines = 1
        }

        val subtitle = TextView(this).apply {
            text = buildCardSubtitle(mark)
            textSize = 11f
            setTextColor(Color.parseColor(Colors.TEXT_SECONDARY))
            setPadding(0, dp(5), 0, 0)
            maxLines = 1
        }

        textBlock.addView(title)
        textBlock.addView(subtitle)

        val status = TextView(this).apply {
            val (statusText, statusColor) = when (mark.verificationStatus) {
                VerificationStatus.UNVERIFIED -> "Непроверено" to Colors.ORANGE
                VerificationStatus.CONFIRMED -> "Подтверждено" to Colors.GREEN
                VerificationStatus.DISPROVED -> "Отклонено" to Colors.RED
            }
            text = statusText
            textSize = 10f
            setTextColor(Color.parseColor(statusColor))
            gravity = Gravity.CENTER_VERTICAL
        }

        card.addView(redDot)
        card.addView(textBlock, LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        ))
        card.addView(status)

        card.setOnClickListener {
            val intent = Intent(this@MarkListActivity, EditMarkActivity::class.java)
            intent.putExtra("MARK_ID", mark.id)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        card.setOnLongClickListener {
            showDeleteDialog(mark)
            true
        }

        return card
    }

    private fun buildCardTitle(mark: Mark): String {
        return when (mark.pointType) {
            PointType.FRONT -> "Фронт пожара"
            PointType.FLANK -> {
                if (mark.name.contains("лев", ignoreCase = true)) {
                    "Левый фланг пожара"
                } else if (mark.name.contains("прав", ignoreCase = true)) {
                    "Правый фланг пожара"
                } else {
                    "Фланг пожара"
                }
            }
            PointType.REAR -> "Тыл пожара"
        }
    }

    private fun buildCardSubtitle(mark: Mark): String {
        val time = mark.getFormattedDate()
            .substringAfter(" ")
            .ifBlank { mark.getFormattedDate() }

        val accuracy = mark.horizontalAccuracyMeters?.let {
            "точность %.0f м".format(it)
        } ?: "точность не указана"

        return "$time · $accuracy"
    }

    private fun updateFilterButtons() {
        allFilterButton.background = chipDrawable(currentFilter == MarkFilter.ALL, false)
        unsentFilterButton.background = chipDrawable(currentFilter == MarkFilter.UNSENT, true)
        sentFilterButton.background = chipDrawable(currentFilter == MarkFilter.SENT, false)
        frontFilterButton.background = chipDrawable(currentFilter == MarkFilter.FRONT, false)
        flankFilterButton.background = chipDrawable(currentFilter == MarkFilter.FLANK, false)
        rearFilterButton.background = chipDrawable(currentFilter == MarkFilter.REAR, false)
    }

    private fun chipDrawable(selected: Boolean, orange: Boolean): GradientDrawable {
        val color = when {
            selected -> Colors.CHIP_ORANGE
            else -> "#EEF3EE"
        }

        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor(color))
            cornerRadius = dp(15).toFloat()
        }
    }

    private fun colorForMark(mark: Mark): String {
        return when (mark.intensity) {
            FireIntensity.HIGH -> Colors.RED
            FireIntensity.MEDIUM -> Colors.ORANGE
            FireIntensity.LOW -> Colors.YELLOW
        }
    }

    private fun circleDrawable(color: String): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(color))
        }
    }

    private fun showDeleteDialog(mark: Mark) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Удалить точку?")
            .setMessage("Удалить точку \"${mark.name}\"?")
            .setPositiveButton("Да") { _, _ ->
                database.deleteMark(mark.id)
                Toast.makeText(this, "Точка удалена", Toast.LENGTH_SHORT).show()
                loadMarks()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onResume() {
        super.onResume()
        ScreenChrome.apply(this)
        loadMarks()
    }
}

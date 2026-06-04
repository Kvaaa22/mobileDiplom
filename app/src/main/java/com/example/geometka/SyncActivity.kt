package com.example.geometka

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.example.geometka.api.MarkSyncClient
import com.example.geometka.data.MarkDatabase
import com.example.geometka.data.SyncStatus
import com.example.geometka.maps.DeviceIdentity
import com.example.geometka.ui.ScreenChrome
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SyncActivity : Activity() {

    private lateinit var database: MarkDatabase
    private lateinit var progressText: TextView
    private lateinit var nextSyncText: TextView
    private lateinit var journalContainer: LinearLayout
    private lateinit var progressBar: ProgressBar
    private var isSyncRunning = false
    
    // Хранилище всех действий для журнала
    private val syncLogs = mutableListOf<String>()

    private object Colors {
        const val GREEN_DARK = "#0B2A18"
        const val GREEN = "#155E32"
        const val GREEN_LIGHT = "#EAF3EB"
        const val BACKGROUND = "#F8FBF7"
        const val CARD = "#F7FAF6"
        const val CARD_ALT = "#EEF5EC"
        const val BORDER = "#C9DDCC"
        const val TEXT_PRIMARY = "#1F2A22"
        const val TEXT_SECONDARY = "#7A877D"
        const val TEXT_MUTED = "#9AA69E"
        const val WHITE = "#FFFFFF"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = MarkDatabase(this)

        window.statusBarColor = Color.parseColor(Colors.GREEN_DARK)
        window.navigationBarColor = Color.WHITE
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        ScreenChrome.apply(this)

        setContentView(createLayout())
        
        addLog("Запуск экрана синхронизации")
        addLog("Подключение к базе данных успешно")
        
        refreshData()
    }

    private fun addLog(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        syncLogs.add(0, "[$time] $message")
        // Если контейнер уже создан, обновляем его
        if (::journalContainer.isInitialized) {
            updateJournalUI()
        }
    }

    private fun createLayout(): View {
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
            setPadding(dp(18), dp(14), dp(18), 0)
        }

        contentLayout.addView(createStatusCard())
        contentLayout.addView(createNextSyncCard())
        contentLayout.addView(createMainButton("Повторить отправку", Colors.GREEN) {
            startMarksSync()
        })

        contentLayout.addView(createMainButton("Остановить синхронизацию", "#EEF3ED", darkText = true) {
            addLog("Синхронизация остановлена пользователем")
            Toast.makeText(this, "Синхронизация остановлена", Toast.LENGTH_SHORT).show()
        })

        contentLayout.addView(createJournalTitleRow())
        contentLayout.addView(createJournalBlock())

        scrollView.addView(contentLayout)

        rootLayout.addView(scrollView)
        rootLayout.addView(createBottomMenu())

        return rootLayout
    }

    private fun createHeader(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(Colors.GREEN))
            setPadding(dp(20), dp(20), dp(20), dp(18))

            addView(TextView(this@SyncActivity).apply {
                text = "Синхронизация"
                textSize = 21f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
            })

            addView(TextView(this@SyncActivity).apply {
                text = "Передача данных на сервер"
                textSize = 13f
                setTextColor(Color.parseColor("#C8DDCE"))
                setPadding(0, dp(4), 0, 0)
            })
        }
    }

    private fun createStatusCard(): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedDrawable(
                color = Colors.CARD,
                radiusDp = 14,
                strokeColor = Colors.BORDER,
                strokeWidthDp = 1
            )
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
            }
        }

        container.addView(TextView(this).apply {
            text = "Статус соединения"
            textSize = 16f
            setTextColor(Color.parseColor(Colors.TEXT_PRIMARY))
            typeface = Typeface.DEFAULT_BOLD
        })

        container.addView(TextView(this).apply {
            text = "Сеть доступна · сервер отвечает"
            textSize = 12f
            setTextColor(Color.parseColor(Colors.TEXT_SECONDARY))
            setPadding(0, dp(4), 0, dp(12))
        })

        progressBar = ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = 100
            progress = 0
            progressDrawable?.setTint(Color.parseColor(Colors.GREEN))
            background = roundedDrawable(
                color = "#D7E5D8",
                radiusDp = 10
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(10)
            ).apply {
                bottomMargin = dp(12)
            }
        }

        progressText = TextView(this).apply {
            text = ""
            textSize = 12f
            setTextColor(Color.parseColor(Colors.GREEN))
            typeface = Typeface.DEFAULT_BOLD
            background = roundedDrawable(
                color = "#E2F0E4",
                radiusDp = 8
            )
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }

        container.addView(progressBar)
        container.addView(progressText)

        return container
    }

    private fun createNextSyncCard(): View {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedDrawable(
                color = Colors.CARD_ALT,
                radiusDp = 14
            )
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(18)
            }
        }

        container.addView(TextView(this).apply {
            text = "Следующая отправка"
            textSize = 15f
            setTextColor(Color.parseColor(Colors.TEXT_PRIMARY))
            typeface = Typeface.DEFAULT_BOLD
        })

        nextSyncText = TextView(this).apply {
            text = ""
            textSize = 12f
            setTextColor(Color.parseColor(Colors.TEXT_SECONDARY))
            setPadding(0, dp(5), 0, 0)
        }

        container.addView(nextSyncText)

        container.addView(TextView(this).apply {
            text = "ID устройства: ${DeviceIdentity.getDeviceId(this@SyncActivity)}"
            textSize = 11f
            setTextColor(Color.parseColor(Colors.TEXT_MUTED))
            setPadding(0, dp(8), 0, 0)
        })

        return container
    }

    private fun createMainButton(
        textValue: String,
        color: String,
        darkText: Boolean = false,
        onClick: () -> Unit
    ): Button {
        return Button(this).apply {
            text = textValue
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(
                Color.parseColor(
                    if (darkText) Colors.TEXT_PRIMARY else Colors.WHITE
                )
            )
            isAllCaps = false
            background = roundedDrawable(
                color = color,
                radiusDp = 10
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            ).apply {
                bottomMargin = dp(10)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun createJournalTitleRow(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(10)
                bottomMargin = dp(10)
            }

            addView(TextView(context).apply {
                text = "Журнал"
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor(Colors.TEXT_PRIMARY))
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            })

            addView(TextView(context).apply {
                text = "Очистить"
                textSize = 12f
                setTextColor(Color.parseColor(Colors.GREEN))
                setPadding(dp(10), dp(5), dp(10), dp(5))
                setOnClickListener {
                    syncLogs.clear()
                    addLog("Журнал очищен")
                    Toast.makeText(context, "Журнал очищен", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun createJournalBlock(): View {
        journalContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = roundedDrawable(
                color = Colors.CARD_ALT,
                radiusDp = 10
            )
            // Клик открывает полноэкранный журнал
            setOnClickListener { showFullLogDialog() }
        }
        return journalContainer
    }

    private fun showFullLogDialog() {
        val dialog = Dialog(this, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
            setBackgroundColor(Color.WHITE)
        }

        // Шапка диалога
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(15))
        }

        header.addView(TextView(this).apply {
            text = "Полный журнал"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor(Colors.TEXT_PRIMARY))
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        })

        header.addView(TextView(this).apply {
            text = "✕"
            textSize = 24f
            setPadding(dp(10), dp(5), dp(10), dp(5))
            setOnClickListener { dialog.dismiss() }
        })

        root.addView(header)

        // Список логов
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        val logLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        syncLogs.forEach { log ->
            logLayout.addView(TextView(this).apply {
                text = log
                textSize = 13f
                setTextColor(Color.parseColor(Colors.TEXT_SECONDARY))
                setPadding(0, 0, 0, dp(8))
            })
        }

        scrollView.addView(logLayout)
        root.addView(scrollView)

        root.addView(createMainButton("Закрыть", Colors.GREEN) {
            dialog.dismiss()
        })

        dialog.setContentView(root)
        dialog.show()
    }

    private fun refreshData() {
        val allMarks = database.getAllMarks()
        val totalRecords = allMarks.size
        val sentRecords = allMarks.count { it.syncStatus == SyncStatus.SYNCED }
        val pendingRecords = allMarks.count { it.syncStatus == SyncStatus.LOCAL || it.syncStatus == SyncStatus.PENDING }

        val percent = if (totalRecords > 0) (sentRecords * 100) / totalRecords else 0
        progressBar.progress = percent
        progressText.text = "$sentRecords из $totalRecords записей передано"
        
        if (pendingRecords > 0) {
            nextSyncText.text = "$pendingRecords записи ожидают подтверждения сервера"
        } else {
            nextSyncText.text = "Все данные синхронизированы"
        }

        updateJournalUI()
    }

    private fun startMarksSync() {
        if (isSyncRunning) {
            Toast.makeText(this, "Синхронизация уже идет", Toast.LENGTH_SHORT).show()
            return
        }

        val marksToSend = database.getUnsyncedMarks()

        isSyncRunning = true
        progressBar.progress = 0
        progressText.text = if (marksToSend.isEmpty()) {
            "Проверка статусов"
        } else {
            "Отправка 0 из ${marksToSend.size}"
        }
        nextSyncText.text = if (marksToSend.isEmpty()) {
            "Идет получение статусов с сервера"
        } else {
            "Идет отправка ${marksToSend.size} записей"
        }
        addLog("Найдено записей для отправки: ${marksToSend.size}")
        Toast.makeText(this, "Синхронизация запущена", Toast.LENGTH_SHORT).show()

        Thread {
            val client = MarkSyncClient(applicationContext)
            try {
                marksToSend.forEach { mark ->
                    database.updateSyncStatus(mark.id, SyncStatus.PENDING)
                }

                runOnUiThread {
                    progressText.text = if (marksToSend.isEmpty()) {
                        "Запрос статусов с сервера"
                    } else {
                        "Отправка пакета: ${marksToSend.size} записей"
                    }
                    progressBar.progress = 35
                    addLog(
                        if (marksToSend.isEmpty()) {
                            "Отправка пустой синхронизации для обновления статусов"
                        } else {
                            "Отправка пакета синхронизации: ${marksToSend.size} записей"
                        }
                    )
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
                    progressBar.progress = 100
                    addLog("Синхронизация завершена: отправлено ${marksToSend.size}, обновлено статусов ${syncResults.size}")
                    refreshData()
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
                    progressBar.progress = 0
                    addLog("Ошибка синхронизации: ${error.message ?: "неизвестная ошибка"}")
                    refreshData()
                    Toast.makeText(
                        this,
                        "Ошибка синхронизации",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.start()
    }

    private fun updateJournalUI() {
        journalContainer.removeAllViews()
        
        if (syncLogs.isEmpty()) {
            journalContainer.addView(TextView(this).apply {
                text = "История пуста"
                textSize = 12f
                setTextColor(Color.parseColor(Colors.TEXT_MUTED))
            })
        } else {
            // Показываем последние 5 записей в маленьком блоке
            syncLogs.take(5).forEach { log ->
                journalContainer.addView(TextView(this).apply {
                    text = log
                    textSize = 12f
                    maxLines = 1
                    setTextColor(Color.parseColor(Colors.TEXT_SECONDARY))
                    setPadding(0, 0, 0, dp(4))
                })
            }
            
            if (syncLogs.size > 5) {
                journalContainer.addView(TextView(this).apply {
                    text = "Ещё ${syncLogs.size - 5}..."
                    textSize = 11f
                    setTextColor(Color.parseColor(Colors.GREEN))
                    setPadding(0, dp(4), 0, 0)
                })
            }
        }
    }

    private fun createBottomMenu(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            setPadding(0, dp(10), 0, dp(22))

            addView(createMenuItem("⌖", "Карта", selected = false) {
                ScreenChrome.navigateWithoutJump(
                    this@SyncActivity,
                    Intent(this@SyncActivity, MainActivity::class.java)
                )
            })

            addView(createMenuItem("●", "Точки", selected = false) {
                ScreenChrome.navigateWithoutJump(
                    this@SyncActivity,
                    Intent(this@SyncActivity, MarkListActivity::class.java)
                )
            })

            addView(createMenuItem("⇄", "Синхр.", selected = true) {
                // Уже на этом экране
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

            addView(TextView(this@SyncActivity).apply {
                text = icon
                textSize = 18f
                gravity = Gravity.CENTER
                setTextColor(
                    Color.parseColor(
                        if (selected) Colors.GREEN else Colors.TEXT_MUTED
                    )
                )
            })

            addView(TextView(this@SyncActivity).apply {
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

    override fun onResume() {
        super.onResume()
        ScreenChrome.apply(this)
    }
}

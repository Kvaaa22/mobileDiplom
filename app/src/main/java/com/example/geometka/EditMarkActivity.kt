package com.example.geometka

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import com.example.geometka.data.FireIntensity
import com.example.geometka.data.FireType
import com.example.geometka.data.Mark
import com.example.geometka.data.MarkDatabase
import com.example.geometka.data.PointType

class EditMarkActivity : Activity() {

    private lateinit var database: MarkDatabase
    private var mark: Mark? = null

    private lateinit var notesInput: EditText

    private val pointTypeChipViews = mutableMapOf<PointType, TextView>()
    private val intensityChipViews = mutableMapOf<FireIntensity, TextView>()
    private val fireTypeChipViews = mutableMapOf<FireType, TextView>()

    private var selectedPointType: PointType = PointType.FRONT
    private var selectedIntensity: FireIntensity = FireIntensity.MEDIUM
    private var selectedFireType: FireType = FireType.GROUND

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
        const val DANGER = "#D94324"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.parseColor(Colors.GREEN_DARK)
        window.navigationBarColor = Color.WHITE
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        database = MarkDatabase(this)
        val markId = intent.getLongExtra("MARK_ID", -1)

        if (markId == -1L) {
            Toast.makeText(this, "Ошибка загрузки метки", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        mark = database.getMarkById(markId)
        if (mark == null) {
            Toast.makeText(this, "Метка не найдена", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        selectedPointType = mark!!.pointType
        selectedIntensity = mark!!.intensity
        selectedFireType = mark!!.typeOfFire

        setContentView(createLayout())
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
            setPadding(dp(20), dp(16), dp(20), dp(20))
        }

        val m = mark!!

        contentLayout.addView(createCoordinatesCard(m))

        contentLayout.addView(createLabel("Тип точки"))
        contentLayout.addView(createPointTypeChips())

        contentLayout.addView(createLabel("Интенсивность горения"))
        contentLayout.addView(createIntensityChips())

        contentLayout.addView(createLabel("Тип пожара"))
        contentLayout.addView(createFireTypeChips())

        contentLayout.addView(createLabel("Комментарий"))
        notesInput = createStyledEditText(
            value = m.notes ?: "",
            hint = "Видимость снижена, сильный дым, рядом просека",
            multiline = true
        )
        contentLayout.addView(notesInput)

        contentLayout.addView(createPrimaryButton("Сохранить изменения") {
            saveChanges()
        })

        scrollView.addView(contentLayout)
        rootLayout.addView(scrollView)

        return rootLayout
    }

    private fun createHeader(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(Colors.GREEN))
            setPadding(dp(20), dp(20), dp(20), dp(18))

            addView(TextView(this@EditMarkActivity).apply {
                text = "Редактирование точки"
                textSize = 21f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
            })

            addView(TextView(this@EditMarkActivity).apply {
                text = "Изменение параметров кромки"
                textSize = 13f
                setTextColor(Color.parseColor("#C8DDCE"))
                setPadding(0, dp(4), 0, 0)
            })
        }
    }

    private fun createCoordinatesCard(mark: Mark): LinearLayout {
        val accuracyText = mark.horizontalAccuracyMeters?.let {
            " · точность %.0f м".format(it)
        } ?: ""

        return LinearLayout(this).apply {
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

            addView(TextView(this@EditMarkActivity).apply {
                text = "Координаты точки"
                textSize = 16f
                setTextColor(Color.parseColor(Colors.TEXT_PRIMARY))
                typeface = Typeface.DEFAULT_BOLD
            })

            addView(TextView(this@EditMarkActivity).apply {
                text = "${mark.getFormattedCoordinates()}$accuracyText"
                textSize = 12f
                setTextColor(Color.parseColor(Colors.TEXT_SECONDARY))
                setPadding(0, dp(7), 0, 0)
            })

            addView(TextView(this@EditMarkActivity).apply {
                text = "Координаты доступны только для просмотра"
                textSize = 11f
                setTextColor(Color.parseColor(Colors.TEXT_MUTED))
                setPadding(0, dp(6), 0, 0)
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
        value: String,
        hint: String,
        multiline: Boolean
    ): EditText {
        return EditText(this).apply {
            setText(value)
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
                setPadding(dp(16), 0, dp(16), 0)
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
                    if (selected) "#FFFFFF" else Colors.TEXT_PRIMARY
                )
            )
            background = choiceChipDrawable(selected)
            setPadding(dp(12), 0, dp(12), 0)
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
            chip.setTextColor(Color.parseColor(if (selected) "#FFFFFF" else Colors.TEXT_PRIMARY))
        }
    }

    private fun updateIntensityChips() {
        intensityChipViews.forEach { (intensity, chip) ->
            val selected = intensity == selectedIntensity
            chip.background = choiceChipDrawable(selected)
            chip.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            chip.setTextColor(Color.parseColor(if (selected) "#FFFFFF" else Colors.TEXT_PRIMARY))
        }
    }

    private fun updateFireTypeChips() {
        fireTypeChipViews.forEach { (fireType, chip) ->
            val selected = fireType == selectedFireType
            chip.background = choiceChipDrawable(selected)
            chip.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            chip.setTextColor(Color.parseColor(if (selected) "#FFFFFF" else Colors.TEXT_PRIMARY))
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
                topMargin = dp(22)
                bottomMargin = dp(8)
            }
            setOnClickListener {
                onClick()
            }
        }
    }

    private fun saveChanges() {
        val updatedMark = mark!!.copy(
            pointType = selectedPointType,
            intensity = selectedIntensity,
            typeOfFire = selectedFireType,
            notes = notesInput.text.toString().trim().ifEmpty { null },
            syncStatus = com.example.geometka.data.SyncStatus.LOCAL
        )

        if (database.updateMark(updatedMark) > 0) {
            Toast.makeText(this, "Изменения сохранены", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Ошибка сохранения", Toast.LENGTH_SHORT).show()
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

    private fun choiceChipDrawable(selected: Boolean): GradientDrawable {
        return roundedDrawable(
            color = if (selected) Colors.GREEN else Colors.GREEN_LIGHT,
            radiusDp = 18
        )
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
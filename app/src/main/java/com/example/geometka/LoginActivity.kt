package com.example.geometka

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Space
import android.widget.TextView
import android.widget.Toast
import com.example.geometka.api.ApiConfig
import com.example.geometka.api.AuthApiClient
import com.example.geometka.auth.AppSession
import com.example.geometka.maps.MapDownloadScheduler
import com.example.geometka.ui.ScreenChrome

class LoginActivity : Activity() {

    private lateinit var loginInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button

    private companion object {
        const val KEYBOARD_SCROLL_DELAY_MS = 260L
    }

    private object Colors {
        const val GREEN_DARK = "#0B2A18"
        const val GREEN = "#155E32"
        const val BACKGROUND = "#EEF6EC"
        const val CARD = "#FFFFFF"
        const val BORDER = "#C9DDCC"
        const val TEXT_PRIMARY = "#1F2A22"
        const val TEXT_SECONDARY = "#7A877D"
        const val TEXT_MUTED = "#8A968D"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.parseColor(Colors.GREEN_DARK)
        window.navigationBarColor = Color.parseColor(Colors.BACKGROUND)
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
        )
        ScreenChrome.apply(this)

        if (AppSession.isUnlocked(this)) {
            scheduleMapDownloads()
            openMainScreen()
            return
        }

        setContentView(createLayout())
    }

    private fun createLayout(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(Colors.BACKGROUND))
        }

        val topBar = View(this).apply {
            setBackgroundColor(Color.parseColor(Colors.GREEN_DARK))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(34)
            )
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(26), dp(20), dp(26), dp(20))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        val topSpace = Space(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1.1f
            )
        }

        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        form.addView(createTitle())
        form.addView(createSubtitle())

        loginInput = createStyledInput("Логин", false)
        form.addView(loginInput)

        passwordInput = createStyledInput("Пароль", true)
        form.addView(passwordInput)

        form.addView(createLoginButton())
        form.addView(createDemoText())

        val bottomSpace = Space(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                0.9f
            )
        }

        content.addView(topSpace)
        content.addView(form)
        content.addView(bottomSpace)
        content.addView(createFooterText())

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            addView(content)
        }

        keepInputAboveKeyboard(scrollView, loginInput)
        keepInputAboveKeyboard(scrollView, passwordInput)

        root.addView(topBar)
        root.addView(scrollView)

        return root
    }

    private fun keepInputAboveKeyboard(
        scrollView: ScrollView,
        input: EditText
    ) {
        input.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                scrollInputAboveKeyboard(scrollView, input)
            }
        }
    }

    private fun scrollInputAboveKeyboard(
        scrollView: ScrollView,
        input: View
    ) {
        scrollView.postDelayed({
            val targetBottom = input.bottom + dp(32)
            val scrollY = (targetBottom - scrollView.height).coerceAtLeast(0)
            scrollView.smoothScrollTo(0, scrollY)
        }, KEYBOARD_SCROLL_DELAY_MS)
    }

    private fun createTitle(): TextView {
        return TextView(this).apply {
            text = "АИС «Лесная разведка»"
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor(Colors.TEXT_PRIMARY))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
            }
        }
    }

    private fun createSubtitle(): TextView {
        return TextView(this).apply {
            text = "Сбор сведений о кромке пожара\nдля наземной разведки"
            textSize = 13f
            setTextColor(Color.parseColor(Colors.TEXT_SECONDARY))
            gravity = Gravity.CENTER
            setLineSpacing(dp(2).toFloat(), 1.0f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(30)
            }
        }
    }

    private fun createStyledInput(hintText: String, isPassword: Boolean): EditText {
        return EditText(this).apply {
            hint = hintText
            textSize = 14f
            setTextColor(Color.parseColor(Colors.TEXT_PRIMARY))
            setHintTextColor(Color.parseColor(Colors.TEXT_MUTED))
            background = roundedDrawable(
                color = Colors.CARD,
                radiusDp = 10,
                strokeColor = Colors.BORDER,
                strokeWidthDp = 1
            )
            setPadding(dp(14), 0, dp(14), 0)
            setSingleLine(true)

            inputType = if (isPassword) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            }

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(46)
            ).apply {
                bottomMargin = dp(14)
            }
        }
    }

    private fun createLoginButton(): Button {
        loginButton = Button(this).apply {
            text = "Войти"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            isAllCaps = false
            background = roundedDrawable(
                color = Colors.GREEN,
                radiusDp = 10
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            ).apply {
                topMargin = dp(10)
                bottomMargin = dp(18)
            }

            setOnClickListener {
                onLoginClick()
            }
        }

        return loginButton
    }

    private fun createDemoText(): TextView {
        return TextView(this).apply {
            text = "Демо-режим для ВКР"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor(Colors.GREEN))
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(4))

            setOnClickListener {
                AppSession.unlock(this@LoginActivity, "Демо-аккаунт")
                scheduleMapDownloads()
                openMainScreen()
            }
        }
    }

    private fun createFooterText(): TextView {
        return TextView(this).apply {
            text = "©2026 АИС «Лесная разведка»"
            textSize = 11f
            setTextColor(Color.parseColor(Colors.TEXT_SECONDARY))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(40)
            }
        }
    }

    private fun onLoginClick() {
        val login = loginInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (login.isEmpty()) {
            Toast.makeText(this, "Введите логин", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Введите пароль", Toast.LENGTH_SHORT).show()
            return
        }

        setLoginLoading(true)

        Thread {
            try {
                val result = AuthApiClient().login(
                    login = login,
                    password = password,
                    deviceId = ApiConfig.MOBILE_DEVICE_ID
                )
                val username = result.username ?: login

                runOnUiThread {
                    AppSession.unlock(
                        context = this,
                        username = username,
                        accessToken = result.accessToken,
                        refreshToken = result.refreshToken,
                        userId = result.userId
                    )
                    scheduleMapDownloads()
                    openMainScreen()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    setLoginLoading(false)
                    Toast.makeText(
                        this,
                        e.message ?: "Не удалось войти",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.start()
    }

    private fun setLoginLoading(isLoading: Boolean) {
        loginInput.isEnabled = !isLoading
        passwordInput.isEnabled = !isLoading
        loginButton.isEnabled = !isLoading
        loginButton.text = if (isLoading) "Вход..." else "Войти"
    }

    private fun openMainScreen() {
        ScreenChrome.navigateWithoutJump(this, Intent(this, MainActivity::class.java))
    }

    private fun scheduleMapDownloads() {
        MapDownloadScheduler.startAutomaticDownloads(this)
        MapDownloadScheduler.forceDownloadNow(this)
    }

    override fun onResume() {
        super.onResume()
        ScreenChrome.apply(this)
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

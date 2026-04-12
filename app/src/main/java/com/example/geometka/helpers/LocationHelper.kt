package com.example.geometka.helpers

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class LocationHelper(private val context: Context) {

    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val connectivityManager: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private var gpsListener: LocationListener? = null
    private var networkListener: LocationListener? = null
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null

    // Callbacks
    var onLocationReceived: ((latitude: Double, longitude: Double, provider: String, isCached: Boolean) -> Unit)? = null
    var onLocationError: ((message: String) -> Unit)? = null
    var onStatusUpdate: ((message: String) -> Unit)? = null

    // Проверка разрешений
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Запрос разрешений
    fun requestLocationPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    // Проверка наличия интернета
    fun hasInternetConnection(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    // Проверка включен ли GPS
    fun isGpsEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    // Проверка включен ли Network provider
    fun isNetworkEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    // Получить информацию о доступных методах
    fun getAvailableMethods(): String {
        val methods = mutableListOf<String>()

        if (isGpsEnabled()) methods.add("GPS")
        if (isNetworkEnabled()) methods.add("Network")
        if (hasInternetConnection()) methods.add("Internet")

        return if (methods.isEmpty()) "Нет доступных методов" else methods.joinToString(", ")
    }

    // Получение ТОЛЬКО новых координат (без кэша!)
    fun getCurrentLocation(allowCached: Boolean = false) {
        if (!hasLocationPermission()) {
            onLocationError?.invoke("Нет разрешения на геолокацию")
            return
        }

        val hasInternet = hasInternetConnection()
        val hasGps = isGpsEnabled()
        val hasNetwork = isNetworkEnabled()

        // Определяем стратегию
        val strategy = when {
            hasGps && hasInternet -> "GPS + A-GPS (быстро, точно)"
            hasGps && !hasInternet -> "GPS (медленно, но работает без связи)"
            !hasGps && hasNetwork -> "Network (требует интернет)"
            else -> null
        }

        if (strategy == null) {
            onLocationError?.invoke("GPS и Network отключены. Включите в настройках.")
            return
        }

        onStatusUpdate?.invoke("Метод: $strategy")

        try {
            // ТОЛЬКО если разрешен кэш
            if (allowCached) {
                val lastLocation = getBestLastKnownLocation()

                if (lastLocation != null && isLocationRecent(lastLocation)) {
                    onLocationReceived?.invoke(
                        lastLocation.latitude,
                        lastLocation.longitude,
                        lastLocation.provider ?: "unknown",
                        true  // это кэш!
                    )
                    return
                }
            }

            // Запускаем получение НОВОЙ локации
            onStatusUpdate?.invoke("Получение новых координат...")
            requestLocationUpdates(hasGps, hasNetwork, hasInternet)

        } catch (e: SecurityException) {
            onLocationError?.invoke("Ошибка безопасности: ${e.message}")
            e.printStackTrace()
        }
    }

    // Получить лучшую последнюю известную локацию (кэш)
    private fun getBestLastKnownLocation(): Location? {
        if (!hasLocationPermission()) return null

        try {
            val gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            return when {
                gpsLocation == null && networkLocation == null -> null
                gpsLocation == null -> networkLocation
                networkLocation == null -> gpsLocation
                gpsLocation.time > networkLocation.time -> gpsLocation
                else -> networkLocation
            }
        } catch (e: SecurityException) {
            return null
        }
    }

    // Проверка, свежая ли локация (не старше 30 секунд)
    private fun isLocationRecent(location: Location): Boolean {
        val thirtySecondsAgo = System.currentTimeMillis() - 30 * 1000
        return location.time > thirtySecondsAgo
    }

    // Запрос обновлений локации
    private fun requestLocationUpdates(hasGps: Boolean, hasNetwork: Boolean, hasInternet: Boolean) {
        if (!hasLocationPermission()) return

        var locationReceived = false

        // Callback для обработки локации
        val handleLocation: (Location) -> Unit = { location ->
            if (!locationReceived) {
                locationReceived = true
                cancelTimeout()
                onLocationReceived?.invoke(
                    location.latitude,
                    location.longitude,
                    location.provider ?: "unknown",
                    false  // это НЕ кэш, свежие данные!
                )
                stopLocationUpdates()
            }
        }

        try {
            // Запускаем GPS если доступен
            if (hasGps) {
                gpsListener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        handleLocation(location)
                    }
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {
                        onStatusUpdate?.invoke("GPS был отключен")
                    }
                }

                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0L,      // Минимальное время - сразу
                    0f,      // Минимальная дистанция - любая
                    gpsListener!!
                )

                onStatusUpdate?.invoke("🛰️ Поиск GPS спутников...")
            }

            // Запускаем Network если доступен
            if (hasNetwork) {
                networkListener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        handleLocation(location)
                    }
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {
                        onStatusUpdate?.invoke("Network был отключен")
                    }
                }

                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    0L,
                    0f,
                    networkListener!!
                )

                if (!hasGps) {
                    onStatusUpdate?.invoke("📡 Поиск через Network (Wi-Fi/вышки)...")
                }
            }

            // Устанавливаем таймаут
            val timeout = if (hasInternet && hasGps) {
                30000L  // 30 сек с интернетом
            } else if (hasGps) {
                180000L  // 180 сек GPS без интернета
            } else {
                20000L  // 20 сек только Network
            }

            startTimeout(timeout)

        } catch (e: SecurityException) {
            onLocationError?.invoke("Ошибка получения локации")
            e.printStackTrace()
        }
    }

    // Запуск таймаута
    private fun startTimeout(delayMillis: Long) {
        timeoutRunnable = Runnable {
            val message = when {
                !hasInternetConnection() && isGpsEnabled() ->
                    "Таймаут ${delayMillis/1000}с. GPS без интернета требует открытого неба и больше времени."
                isGpsEnabled() ->
                    "Таймаут ${delayMillis/1000}с. Попробуйте выйти на улицу или к окну."
                else ->
                    "Таймаут ${delayMillis/1000}с. Не удалось получить координаты."
            }
            onLocationError?.invoke(message)
            stopLocationUpdates()
        }
        handler.postDelayed(timeoutRunnable!!, delayMillis)
    }

    // Отмена таймаута
    private fun cancelTimeout() {
        timeoutRunnable?.let {
            handler.removeCallbacks(it)
            timeoutRunnable = null
        }
    }

    // Остановка обновлений
    fun stopLocationUpdates() {
        cancelTimeout()

        gpsListener?.let {
            try {
                locationManager.removeUpdates(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        networkListener?.let {
            try {
                locationManager.removeUpdates(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        gpsListener = null
        networkListener = null
    }

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}
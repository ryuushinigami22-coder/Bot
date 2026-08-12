// ==================== LOGIN_PAGE.DART (NAXRAT V3) ====================
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import 'package:url_launcher/url_launcher.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:device_info_plus/device_info_plus.dart';
import 'package:video_player/video_player.dart';
import 'dart:ui';

const String baseUrl = "http://gunturhosting.hoshino.my.id:3000";

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> with TickerProviderStateMixin {
  final userController = TextEditingController();
  final passController = TextEditingController();
  bool isLoading = false;
  String? androidId;
  bool _isObscure = true;
  bool _isMuted = false;

  late VideoPlayerController _videoController;
  late AnimationController _fadeController;
  late AnimationController _slideController;
  late AnimationController _pulseController;
  late Animation<double> _fadeAnimation;
  late Animation<Offset> _slideAnimation;
  late Animation<double> _pulseAnimation;

  // Warna NAXRAT RED THEME
  final Color primaryRed = const Color(0xFFE53935);
  final Color darkRed = const Color(0xFFB71C1C);
  final Color redGlow = const Color(0xFFE57373);
  final Color darkBg = const Color(0xFF0A0A0A);
  final Color textWhite = const Color(0xFFFFFFFF);
  final Color textGray = const Color(0xFF9E9E9E);

  @override
  void initState() {
    super.initState();
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.immersive);
    initLogin();

    // Animations
    _fadeController = AnimationController(
      duration: const Duration(milliseconds: 1200),
      vsync: this,
    );
    _slideController = AnimationController(
      duration: const Duration(milliseconds: 800),
      vsync: this,
    );
    _pulseController = AnimationController(
      duration: const Duration(milliseconds: 2000),
      vsync: this,
    )..repeat(reverse: true);

    _fadeAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(parent: _fadeController, curve: Curves.easeOut),
    );
    _slideAnimation = Tween<Offset>(
      begin: const Offset(0, 0.15),
      end: Offset.zero,
    ).animate(
      CurvedAnimation(parent: _slideController, curve: Curves.easeOutCubic),
    );
    _pulseAnimation = Tween<double>(begin: 1.0, end: 1.03).animate(
      CurvedAnimation(parent: _pulseController, curve: Curves.easeInOut),
    );

    _fadeController.forward();
    _slideController.forward();

    // Video Background
    _videoController = VideoPlayerController.asset('assets/videos/login.mp4')
      ..initialize().then((_) {
        setState(() {});
        _videoController.setLooping(true);
        _videoController.play();
        _videoController.setVolume(1.0); // SOUND ON
      });
  }

  @override
  void dispose() {
    userController.dispose();
    passController.dispose();
    _videoController.dispose();
    _fadeController.dispose();
    _slideController.dispose();
    _pulseController.dispose();
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
    super.dispose();
  }

  Future<void> initLogin() async {
    androidId = await getAndroidId();

    final prefs = await SharedPreferences.getInstance();
    final savedUser = prefs.getString("username");
    final savedPass = prefs.getString("password");
    final savedKey = prefs.getString("key");

    if (savedUser != null && savedPass != null && savedKey != null) {
      final uri = Uri.parse(
        "$baseUrl/api/auth/myInfo?username=$savedUser&password=$savedPass&androidId=$androidId&key=$savedKey",
      );
      try {
        final res = await http.get(uri);
        final data = jsonDecode(res.body);

        if (data['valid'] == true) {
          if (!mounted) return;
          Navigator.pushReplacementNamed(
            context,
            '/splash',
            arguments: {
              'username': savedUser,
              'password': savedPass,
              'role': data['role'],
              'key': data['key'],
              'expiredDate': data['expiredDate'],
              'listBug': data['listBug'] ?? [],
              'listPayload': data['listPayload'] ?? [],
              'listDDoS': data['listDDoS'] ?? [],
              'news': data['news'] ?? [],
            },
          );
        }
      } catch (_) {}
    }
  }

  Future<String> getAndroidId() async {
    final deviceInfo = DeviceInfoPlugin();
    final android = await deviceInfo.androidInfo;
    return android.id ?? "unknown_device";
  }

  Future<void> login() async {
    final username = userController.text.trim();
    final password = passController.text.trim();

    if (username.isEmpty || password.isEmpty) {
      _showAlert("Error", "Username and password are required.");
      return;
    }

    setState(() => isLoading = true);

    try {
      final validate = await http.post(
        Uri.parse("$baseUrl/api/auth/validate"),
        body: {
          "username": username,
          "password": password,
          "androidId": androidId ?? "unknown_device",
        },
      );

      final validData = jsonDecode(validate.body);

      if (validData['expired'] == true) {
        _showAlert("Access Expired", "Your access has expired.\nPlease renew it.", showContact: true);
      } else if (validData['valid'] != true) {
        _showAlert("Login Failed", "Invalid username or password.", showContact: true);
      } else {
        final prefs = await SharedPreferences.getInstance();
        await prefs.setString("username", username);
        await prefs.setString("password", password);
        await prefs.setString("key", validData['key']);

        if (!mounted) return;
        Navigator.pushReplacementNamed(
          context,
          '/splash',
          arguments: {
            'username': username,
            'password': password,
            'role': validData['role'],
            'key': validData['key'],
            'expiredDate': validData['expiredDate'],
            'listBug': validData['listBug'] ?? [],
            'listPayload': validData['listPayload'] ?? [],
            'listDDoS': validData['listDDoS'] ?? [],
            'news': validData['news'] ?? [],
          },
        );
      }
    } catch (_) {
      _showAlert("Connection Error", "Failed to connect to the server.");
    }

    setState(() => isLoading = false);
  }

  void _showAlert(String title, String msg, {bool showContact = false}) {
    showDialog(
      context: context,
      builder: (_) => BackdropFilter(
        filter: ImageFilter.blur(sigmaX: 5, sigmaY: 5),
        child: AlertDialog(
          backgroundColor: const Color(0xFF1A1A1A),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(20),
            side: BorderSide(color: primaryRed.withOpacity(0.5), width: 1),
          ),
          title: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  gradient: LinearGradient(colors: [darkRed, primaryRed]),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Icon(
                  title.contains("Error") || title.contains("Failed")
                      ? Icons.error_outline
                      : title.contains("Expired")
                          ? Icons.timer_off
                          : Icons.info_outline,
                  color: Colors.white,
                  size: 20,
                ),
              ),
              const SizedBox(width: 12),
              Text(
                title,
                style: const TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold),
              ),
            ],
          ),
          content: Text(msg, style: TextStyle(color: textGray, fontSize: 14)),
          actions: [
            if (showContact)
              TextButton.icon(
                onPressed: () async {
                  final uri = Uri.parse("tg://resolve?domain=chgunturx");
                  if (await canLaunchUrl(uri)) {
                    await launchUrl(uri, mode: LaunchMode.externalApplication);
                  } else {
                    await launchUrl(Uri.parse("https://t.me/chgunturx"), mode: LaunchMode.externalApplication);
                  }
                },
                icon: Icon(Icons.message, size: 18, color: primaryRed),
                label: Text("Contact Admin", style: TextStyle(color: textWhite)),
                style: TextButton.styleFrom(backgroundColor: primaryRed.withOpacity(0.2)),
              ),
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: Text("CLOSE", style: TextStyle(color: primaryRed, fontWeight: FontWeight.bold)),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _openTelegramBot() async {
    final uri = Uri.parse("tg://resolve?domain=gunturnaktido");
    if (await canLaunchUrl(uri)) {
      await launchUrl(uri, mode: LaunchMode.externalApplication);
    } else {
      await launchUrl(Uri.parse("https://t.me/gunturnaktido"), mode: LaunchMode.externalApplication);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: darkBg,
      body: Stack(
        children: [
          // Video Background Full Screen
          _videoController.value.isInitialized
              ? SizedBox.expand(
                  child: FittedBox(
                    fit: BoxFit.cover,
                    child: SizedBox(
                      width: _videoController.value.size.width,
                      height: _videoController.value.size.height,
                      child: VideoPlayer(_videoController),
                    ),
                  ),
                )
              : Container(color: darkBg),

          // Dark Overlay Gradient
          Container(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                colors: [
                  darkBg.withOpacity(0.7),
                  darkBg.withOpacity(0.4),
                  darkBg.withOpacity(0.8),
                  darkBg.withOpacity(0.95),
                ],
              ),
            ),
          ),

          // Red Glow Overlay
          Container(
            decoration: BoxDecoration(
              gradient: RadialGradient(
                center: const Alignment(0, -0.3),
                radius: 1.2,
                colors: [
                  primaryRed.withOpacity(0.2),
                  Colors.transparent,
                  darkBg.withOpacity(0.5),
                ],
              ),
            ),
          ),

          // Sound Control Button
          Positioned(
            top: 50,
            right: 20,
            child: Container(
              decoration: BoxDecoration(
                color: Colors.black.withOpacity(0.5),
                borderRadius: BorderRadius.circular(30),
                border: Border.all(color: primaryRed.withOpacity(0.5), width: 1),
              ),
              child: IconButton(
                onPressed: () {
                  setState(() {
                    if (_videoController.value.volume > 0) {
                      _videoController.setVolume(0);
                      _isMuted = true;
                    } else {
                      _videoController.setVolume(1.0);
                      _isMuted = false;
                    }
                  });
                },
                icon: Icon(
                  _isMuted ? Icons.volume_off_rounded : Icons.volume_up_rounded,
                  color: primaryRed,
                  size: 20,
                ),
              ),
            ),
          ),

          // Main Content
          SafeArea(
            child: Center(
              child: SingleChildScrollView(
                padding: const EdgeInsets.symmetric(horizontal: 28, vertical: 20),
                child: FadeTransition(
                  opacity: _fadeAnimation,
                  child: SlideTransition(
                    position: _slideAnimation,
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        // Logo dengan Pulse Animation
                        ScaleTransition(
                          scale: _pulseAnimation,
                          child: Container(
                            padding: const EdgeInsets.all(4),
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              border: Border.all(color: primaryRed.withOpacity(0.8), width: 3),
                              boxShadow: [
                                BoxShadow(
                                  color: primaryRed.withOpacity(0.5),
                                  blurRadius: 30,
                                  spreadRadius: 5,
                                ),
                              ],
                            ),
                            child: ClipOval(
                              child: Image.asset(
                                'assets/images/logo.png',
                                height: 100,
                                width: 100,
                                fit: BoxFit.cover,
                                errorBuilder: (_, __, ___) => Container(
                                  height: 100,
                                  width: 100,
                                  color: primaryRed,
                                  child: const Icon(Icons.android, color: Colors.white, size: 50),
                                ),
                              ),
                            ),
                          ),
                        ),
                        const SizedBox(height: 24),

                        // Title
                        ShaderMask(
                          shaderCallback: (bounds) => LinearGradient(
                            colors: [primaryRed, redGlow, primaryRed],
                          ).createShader(bounds),
                          child: const Text(
                            "NAXRAT V3",
                            style: TextStyle(
                              fontSize: 36,
                              fontWeight: FontWeight.w900,
                              letterSpacing: 5,
                              color: Colors.white,
                            ),
                          ),
                        ),
                        const SizedBox(height: 8),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
                          decoration: BoxDecoration(
                            color: primaryRed.withOpacity(0.15),
                            borderRadius: BorderRadius.circular(20),
                            border: Border.all(color: primaryRed.withOpacity(0.3)),
                          ),
                          child: Text(
                            "APK SADAP - GUNTUR DEV",
                            style: TextStyle(
                              color: textGray,
                              fontSize: 11,
                              letterSpacing: 2,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                        const SizedBox(height: 36),

                        // Login Form - Glassmorphism Card
                        ClipRRect(
                          borderRadius: BorderRadius.circular(24),
                          child: BackdropFilter(
                            filter: ImageFilter.blur(sigmaX: 20, sigmaY: 20),
                            child: Container(
                              padding: const EdgeInsets.all(24),
                              decoration: BoxDecoration(
                                gradient: LinearGradient(
                                  begin: Alignment.topLeft,
                                  end: Alignment.bottomRight,
                                  colors: [
                                    Colors.black.withOpacity(0.7),
                                    Colors.black.withOpacity(0.4),
                                  ],
                                ),
                                borderRadius: BorderRadius.circular(24),
                                border: Border.all(
                                  color: primaryRed.withOpacity(0.4),
                                  width: 1.5,
                                ),
                                boxShadow: [
                                  BoxShadow(
                                    color: primaryRed.withOpacity(0.2),
                                    blurRadius: 30,
                                    spreadRadius: 5,
                                  ),
                                ],
                              ),
                              child: Column(
                                children: [
                                  // Username Field
                                  _buildInput(
                                    controller: userController,
                                    hint: "Username",
                                    icon: Icons.person_outline,
                                  ),
                                  const SizedBox(height: 16),
                                  // Password Field
                                  _buildInput(
                                    controller: passController,
                                    hint: "Password",
                                    icon: Icons.lock_outline,
                                    isPassword: true,
                                  ),
                                  const SizedBox(height: 28),
                                  // Sign In Button
                                  Container(
                                    width: double.infinity,
                                    height: 55,
                                    decoration: BoxDecoration(
                                      borderRadius: BorderRadius.circular(16),
                                      gradient: LinearGradient(
                                        colors: [darkRed, primaryRed, redGlow],
                                      ),
                                      boxShadow: [
                                        BoxShadow(
                                          color: primaryRed.withOpacity(0.5),
                                          blurRadius: 20,
                                          spreadRadius: 2,
                                          offset: const Offset(0, 5),
                                        ),
                                      ],
                                    ),
                                    child: ElevatedButton(
                                      onPressed: isLoading ? null : login,
                                      style: ElevatedButton.styleFrom(
                                        backgroundColor: Colors.transparent,
                                        shadowColor: Colors.transparent,
                                        shape: RoundedRectangleBorder(
                                          borderRadius: BorderRadius.circular(16),
                                        ),
                                      ),
                                      child: isLoading
                                          ? const SizedBox(
                                              width: 24,
                                              height: 24,
                                              child: CircularProgressIndicator(
                                                color: Colors.white,
                                                strokeWidth: 2.5,
                                              ),
                                            )
                                          : const Row(
                                              mainAxisAlignment: MainAxisAlignment.center,
                                              children: [
                                                Icon(Icons.login_rounded, color: Colors.white, size: 20),
                                                SizedBox(width: 10),
                                                Text(
                                                  "SIGN IN",
                                                  style: TextStyle(
                                                    fontSize: 16,
                                                    color: Colors.white,
                                                    letterSpacing: 2,
                                                    fontWeight: FontWeight.bold,
                                                  ),
                                                ),
                                              ],
                                            ),
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ),
                        const SizedBox(height: 24),

                        // Buy Account Button
                        Container(
                          width: double.infinity,
                          height: 50,
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(25),
                            border: Border.all(color: primaryRed.withOpacity(0.5), width: 1.5),
                          ),
                          child: OutlinedButton.icon(
                            onPressed: _openTelegramBot,
                            icon: Container(
                              padding: const EdgeInsets.all(6),
                              decoration: BoxDecoration(
                                shape: BoxShape.circle,
                                gradient: LinearGradient(colors: [darkRed, primaryRed]),
                              ),
                              child: const Icon(Icons.shopping_cart_rounded, color: Colors.white, size: 14),
                            ),
                            label: Text(
                              "BUY PREMIUM ACCESS",
                              style: TextStyle(
                                fontSize: 13,
                                color: primaryRed,
                                letterSpacing: 1.5,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                            style: OutlinedButton.styleFrom(
                              backgroundColor: Colors.transparent,
                              side: BorderSide.none,
                              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(25)),
                            ),
                          ),
                        ),
                        const SizedBox(height: 20),

                        // Footer
                        Text(
                          "NAXRAT v3.0 | @chgunturx",
                          style: TextStyle(
                            color: textGray.withOpacity(0.4),
                            fontSize: 10,
                            letterSpacing: 1.5,
                          ),
                        ),
                      ],
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

  Widget _buildInput({
    required TextEditingController controller,
    required String hint,
    required IconData icon,
    bool isPassword = false,
  }) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.black.withOpacity(0.4),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: primaryRed.withOpacity(0.3), width: 1),
      ),
      child: TextField(
        controller: controller,
        obscureText: isPassword ? _isObscure : false,
        style: const TextStyle(color: Colors.white, fontSize: 15),
        cursorColor: primaryRed,
        decoration: InputDecoration(
          prefixIcon: Container(
            margin: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: primaryRed.withOpacity(0.15),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Icon(icon, color: primaryRed, size: 20),
          ),
          suffixIcon: isPassword
              ? IconButton(
                  icon: Icon(
                    _isObscure ? Icons.visibility_off_rounded : Icons.visibility_rounded,
                    color: primaryRed.withOpacity(0.6),
                    size: 20,
                  ),
                  onPressed: () => setState(() => _isObscure = !_isObscure),
                )
              : null,
          hintText: hint,
          hintStyle: TextStyle(color: textGray.withOpacity(0.5), fontSize: 14),
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(14),
            borderSide: BorderSide.none,
          ),
          focusedBorder: OutlineInputBorder(
            borderRadius: BorderRadius.circular(14),
            borderSide: BorderSide(color: primaryRed, width: 1.5),
          ),
          contentPadding: const EdgeInsets.symmetric(vertical: 16, horizontal: 16),
        ),
      ),
    );
  }
}
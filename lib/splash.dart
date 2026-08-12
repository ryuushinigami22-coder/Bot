// splash.dart
import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:video_player/video_player.dart';

import 'dashboard_page.dart';

class SplashPage extends StatefulWidget {
  final Map<String, dynamic> data;
  const SplashPage({super.key, required this.data});

  @override
  State<SplashPage> createState() => _SplashPageState();
}

class _SplashPageState extends State<SplashPage> {
  late VideoPlayerController _controller;
  bool _isInitialized = false;
  bool _isNavigated = false;

  final Color primaryRed = const Color(0xFFE53935);
  final Color darkRed = const Color(0xFFB71C1C);

  @override
  void initState() {
    super.initState();
    _initializeVideo();
  }

  void _initializeVideo() {
    _controller = VideoPlayerController.asset('assets/videos/load.mp4')
      ..initialize().then((_) {
        SystemChrome.setEnabledSystemUIMode(SystemUiMode.immersive);
        setState(() => _isInitialized = true);
        _controller.play();
        _controller.setVolume(1.0);
      }).catchError((e) {
        setState(() => _isInitialized = true);
      });

    _controller.addListener(() {
      if (_controller.value.isInitialized &&
          _controller.value.position >= _controller.value.duration &&
          !_isNavigated) {
        _navigateToDashboard();
      }
    });
  }

  void _navigateToDashboard() {
    if (_isNavigated) return;
    _isNavigated = true;

    _controller.pause();
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);

    if (mounted) {
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(
          builder: (context) => DashboardPage(
            username: widget.data['username'] ?? '',
            password: widget.data['password'] ?? '',
            role: widget.data['role'] ?? 'user',
            sessionKey: widget.data['key'] ?? '',
          ),
        ),
      );
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Widget _buildVideoBox() {
    if (!_isInitialized) {
      return const Center(
        child: CircularProgressIndicator(color: Color(0xFFE53935)),
      );
    }

    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 24),
        child: AspectRatio(
          aspectRatio: 16 / 9,
          child: Container(
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(24),
              border: Border.all(
                color: primaryRed.withOpacity(0.6),
                width: 2.5,
              ),
              boxShadow: [
                BoxShadow(
                  color: primaryRed.withOpacity(0.5),
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
              child: FittedBox(
                fit: BoxFit.cover,
                child: SizedBox(
                  width: _controller.value.size.width,
                  height: _controller.value.size.height,
                  child: VideoPlayer(_controller),
                ),
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
      backgroundColor: Colors.black,
      body: Stack(
        fit: StackFit.expand,
        children: [
          // Background hitam
          Container(color: Colors.black),

          // Video dalam kotak 16:9
          _buildVideoBox(),

          // Overlay gradient
          Container(
            decoration: BoxDecoration(
              gradient: RadialGradient(
                center: const Alignment(0, -0.3),
                radius: 1.2,
                colors: [
                  Colors.transparent,
                  Colors.black.withOpacity(0.7),
                  Colors.black.withOpacity(0.9),
                ],
              ),
            ),
          ),

          // Title
          Positioned(
            bottom: 100,
            left: 0,
            right: 0,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                ShaderMask(
                  shaderCallback: (bounds) => const LinearGradient(
                    colors: [Color(0xFFE53935), Color(0xFFB71C1C)],
                  ).createShader(bounds),
                  child: const Text(
                    "NAXRAT V3",
                    style: TextStyle(
                      fontSize: 48,
                      fontWeight: FontWeight.w900,
                      color: Colors.white,
                      letterSpacing: 4.0,
                    ),
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  "APK SADAP - GUNTUR DEV",
                  style: TextStyle(
                    fontSize: 12,
                    color: Colors.white.withOpacity(0.7),
                    letterSpacing: 2.0,
                  ),
                ),
                const SizedBox(height: 20),
                Container(
                  width: 50,
                  height: 2,
                  decoration: const BoxDecoration(
                    gradient: LinearGradient(
                      colors: [Color(0xFFE53935), Color(0xFFB71C1C)],
                    ),
                  ),
                ),
              ],
            ),
          ),

          // Skip button
          Positioned(
            top: 50,
            right: 25,
            child: GestureDetector(
              onTap: _navigateToDashboard,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 15, vertical: 8),
                decoration: BoxDecoration(
                  color: Colors.black.withOpacity(0.5),
                  borderRadius: BorderRadius.circular(30),
                  border: Border.all(
                    color: const Color(0xFFE53935).withOpacity(0.5),
                    width: 1,
                  ),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: const [
                    Text(
                      "SKIP",
                      style: TextStyle(
                        color: Color(0xFFE53935),
                        fontWeight: FontWeight.bold,
                        fontSize: 12,
                        letterSpacing: 1.5,
                      ),
                    ),
                    SizedBox(width: 5),
                    Icon(
                      Icons.arrow_forward_ios_rounded,
                      color: Color(0xFFE53935),
                      size: 12,
                    ),
                  ],
                ),
              ),
            ),
          ),

          // Footer
          Positioned(
            bottom: 20,
            left: 0,
            right: 0,
            child: Text(
              "© 2026 NaxRat Project | @chgunturx",
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 10,
                color: Colors.white.withOpacity(0.3),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
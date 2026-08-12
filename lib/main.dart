import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'landing_page.dart';
import 'login_page.dart';
import 'dashboard_page.dart';
import 'splash.dart';
import 'device_dashboard.dart';
import 'control_panel.dart';  // ← PASTIKAN NAMA FILE INI

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'NAXRAT V3',
      theme: ThemeData(
        brightness: Brightness.dark,
        scaffoldBackgroundColor: Colors.black,
        colorScheme: const ColorScheme.dark().copyWith(
          primary: Color(0xFFE53935),
          secondary: Color(0xFFB71C1C),
        ),
      ),
      initialRoute: '/',
      onGenerateRoute: (settings) {
        final args = settings.arguments as Map<String, dynamic>?;
        switch (settings.name) {
          case '/':
            return MaterialPageRoute(builder: (_) => const LandingPage());
          case '/login':
            return MaterialPageRoute(builder: (_) => const LoginPage());
          case '/splash':
            return MaterialPageRoute(builder: (_) => SplashPage(data: args ?? {}));
          case '/dashboard':
            return MaterialPageRoute(
              builder: (_) => DashboardPage(
                username: args?['username'] ?? '',
                password: args?['password'] ?? '',
                role: args?['role'] ?? 'user',
                sessionKey: args?['key'] ?? '',
              ),
            );
          case '/dashboard_rat':
            return MaterialPageRoute(builder: (_) => const DeviceDashboardPage());
          case '/control_panel':
            if (args == null || args['id'] == null) {
              return MaterialPageRoute(
                builder: (_) => const Scaffold(
                  body: Center(
                    child: Text("ERROR: Device data not found", style: TextStyle(color: Color(0xFFE53935))),
                  ),
                ),
              );
            }
            return MaterialPageRoute(
              builder: (_) => ControlPanelPage(device: args),
            );
          default:
            return MaterialPageRoute(
              builder: (_) => const Scaffold(
                body: Center(
                  child: Text("404 - PAGE NOT FOUND", style: TextStyle(color: Color(0xFFE53935))),
                ),
              ),
            );
        }
      },
    );
  }
}
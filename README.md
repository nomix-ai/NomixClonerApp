**NomixClonerApp** is used to verify the New Identity creation in [NomixCloner](https://nomixcloner.com). The service clones apps, giving them new package names and replacing device identifiers. You can verify how it works with the help of this app. The app basically obtains device identifiers and shows them on the screen. After cloning it will show fake data from the New Identity instead.

Steps to verify:
1. Open NomixCloner Telegram bot: [https://t.me/nomixcloner_bot](https://t.me/nomixcloner_bot)
2. Create a clone of **com.nomixcloner.app**
3. Install and compare with the original app (see the [releases](https://github.com/nomix-ai/NomixClonerApp/releases) section)

You will see that instead of real device identifiers the clone shows generated data. The same algorithm works for all apps in NomixCloner.

<img width="1024" src="https://github.com/nomix-ai/NomixClonerApp/assets/22825859/55dba75e-1f0d-4e97-ad2f-6c64e780610c" /><br /><br />

"New Identities" of clones include the following parameters:
```
- Package name
- Build props (board, bootloader, brand, brand_for_attestation, device, device_for_attestation, display, fingerprint, hardware, host, id, manufacturer, manufacturer_for_attestation, model, model_for_attestation, product, product_for_attestation, serial, user)
- CPU (cpu_abi, cpu_abi2, supported_32_bit_abis, supported_64_bit_abis, supported_abis)
- System (is_arc, is_debuggable, is_emulator, is_eng, is_treble_enabled, is_user, is_userdebug, permissions_review_required, hw_timeout_multiplier, tag, tags, time, type, odm_sku, sku, soc_manufacturer, soc_model, radio)
- Android version (version_active_codenames, version_all_codenames, version_base_os, version_codename, version_device_initial_sdk_int, version_incremental, version_known_codenames, version_media_performance_class, version_min_supported_target_sdk_int, version_preview_sdk_fingerprint, version_preview_sdk_int, version_release, version_release_or_codename, version_release_or_preview_display, version_resources_sdk_int, version_sdk, version_security_patch)
- Java machine (os_version, os_arch, os_name, java_vm_version, java_vm_name, java_vm_vendor, java_specification_version, java_specification_vendor, java_version, java_vendor, user_name, user_home, user_dir, file_separator, path_separator, line_separator, java_class_path, java_class_version, java_library_path, java_io_tmpdir, java_compiler, java_ext_dirs, sun_boot_library_path)
- Global settings (adb_enabled, always_finish_activities, development_settings_enabled, ro.secure, ro.adb.secure, ro.boot.flash.locked, ro.debuggable, ro.kernel.qemu, ro.kernel.android.qemud)
- System boot time
- DNS servers
- Google Advertising ID
- SIM operator code
- SIM operator name
- SIM country ISO
- Network operator code
- Network operator name
- Network country ISO code
- WiFi scan results
- WiFi Passpoint configurations
- WiFi network ID
- WiFi IP address
- WiFi SSID
- WiFi BSSID
- WiFi MAC address
- HTTPS traffic proxying via SOCKS5 (optionally)
- GPS location
- GMS location (Google Play Services)
- Native location (Android LocationManager)
- GSM Cell Tower Information
- CDMA Cell Tower Information
- Block permissions: READ_BASIC_PHONE_STATE, READ_PHONE_STATE, BLUETOOTH, BLUETOOTH_CONNECT, GET_ACCOUNTS, READ_PHONE_NUMBERS, QUERY_ALL_PACKAGES, POST_NOTIFICATIONS (optionally)
- WebView User Agent
- Fake Camera input (if Fake Camera feature enabled)
- Disable SSL pinning (optionally)
- Disable background jobs
- Disable wake-up events (Firebase messaging events, Boot completed, Time set/timezone changed, Package added/replaced/removed, User present, WorkManager diagnostics)
- Disable protection from recording/streaming
```

Read more in the [blog post](https://nomixcloner.com/tpost/tdx61p43p1-new-identity-explained).

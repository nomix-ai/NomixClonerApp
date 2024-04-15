**NomixClonerApp** is used to verify the New Identity creation in [NomixCloner](https://nomixcloner.com). In this repo you will find the sources and the Android apks of this app (see the [releases](https://github.com/nomix-ai/NomixClonerApp/releases) section).

<img src="https://github.com/nomix-ai/NomixClonerApp/assets/22825859/d71acc68-93f8-487e-9393-425d6c283384" width="344" height ="766" /><br /><br />

"Identity" of the device includes the following parameters for Android 10+:
```
- Android ID
- DNS servers
- Build props (model, manufacturer, brand, product, device, board, hardware, bootloader, fingerprint, display, id, security patch)
- OS version
- Google Advertising ID
- SIM info
- Location
```

The app originally shows the real identity of the device. After cloning it should show the new identity provided by the NomixCloner service. Same algorithm is applied for all apps NomixCloner works with.

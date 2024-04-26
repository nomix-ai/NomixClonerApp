**NomixClonerApp** is used to verify the New Identity creation in [NomixCloner](https://nomixcloner.com). The service clones apps, giving them new package names and replacing device identifiers. You can verify how it works with the help of this app. The app basically obtains device identifiers and shows them on the screen. After cloning it will show fake data from the New Identity instead.

Steps to verify:
1. Open NomixCloner Telegram bot: [https://t.me/nomixcloner_bot](https://t.me/nomixcloner_bot)
2. Create a clone of **com.nomixcloner.app**
3. Install and compare with the original app (see the [releases](https://github.com/nomix-ai/NomixClonerApp/releases) section)

You will see that instead of real device identifiers the clone shows generated data. The same algorithm works for all apps in NomixCloner.

<img width="1024" src="https://github.com/nomix-ai/NomixClonerApp/assets/22825859/55dba75e-1f0d-4e97-ad2f-6c64e780610c" /><br /><br />

"New Identities" of clones include the following parameters:
```
- Android ID
- DNS servers
- Build props (model, manufacturer, brand, product, device, board, hardware, bootloader, fingerprint, display, id, security patch)
- OS version
- Google Advertising ID
- SIM info
- Location
```

Read more in the [blog post](https://nomixcloner.com/tpost/tdx61p43p1-new-identity-explained).

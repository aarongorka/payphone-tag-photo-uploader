# payphone-tag-photo-uploader

Android app to quickly photograph a payphone and upload it to the [Payphone Tag Wiki](https://wiki.payphonetag.com/wiki/Main_Page) (MediaWiki).

## Use

1. Enter your wiki username and password and save (no 2FA or login CAPTCHA).
2. Open a wiki `Special:AddPayphonePhoto/<ID>` link (choose this app), or share the URL to it, to prefill the payphone ID.
3. **Take photo**, pick the type, **Upload**. New contributors' photos await moderation.

## Build

`gradle ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug`

## Release

Conventional commits on `main` drive release-please; merging its PR attaches a signed APK.
Secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

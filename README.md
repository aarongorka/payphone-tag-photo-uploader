# payphone-tag-photo-uploader

Android app to quickly photograph a payphone and upload it to the [Payphone Tag Wiki](https://wiki.payphonetag.com/wiki/Main_Page) (MediaWiki).

## Use

1. Create a [bot password](https://wiki.payphonetag.com/wiki/Special:BotPasswords) with upload rights.
2. Enter it in the app and save.
3. **Take photo**, name the payphone, **Upload**.

## Build

`gradle ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug`

## Release

Conventional commits on `main` drive release-please; merging its PR attaches a signed APK.
Secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

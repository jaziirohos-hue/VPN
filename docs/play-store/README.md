# Publishing Xalane VPN on Google Play

Everything needed for Play Console is in this folder, except the signed files and upload key, which are kept out of git.

## Files

| What | Where |
|---|---|
| App bundle to upload | `app-release.aab` (built with `./gradlew bundleRelease`, signed with the upload key) |
| Upload key | `keystore/xalane-upload.jks` + `keystore.properties` (not in git; keep them safe) |
| Store listing text | [store-listing.md](store-listing.md) |
| Policy and form answers | [app-content-answers.md](app-content-answers.md) |
| Privacy policy | [privacy-policy.html](privacy-policy.html) |
| Icon, feature graphic, screenshots | [graphics/](graphics/) |

## Steps

1. **Developer account.** Sign up at play.google.com/console (one-time $25 fee) and verify your identity.
2. **Create app.** Name `Xalane VPN`, default language English, type *App*, *Free*.
3. **Host the privacy policy.** Fill in the date and contact email in `privacy-policy.html`, then put it on any public web page (for example GitHub Pages or Google Sites) and copy the link.
4. **App content.** Complete every item under *Policy → App content* using [app-content-answers.md](app-content-answers.md), including the VPN service declaration and Data safety.
5. **Store listing.** Paste the text from [store-listing.md](store-listing.md) and upload the images from `graphics/`.
6. **Testing track first.** Go to *Test and release → Testing → Closed testing*, create a release and upload `app-release.aab`. Keep **Play App Signing** turned on (Google holds the final signing key; your file is only the upload key).
   - New personal developer accounts must run a closed test with at least **12 testers for 14 days** before Google allows production.
7. **Production.** After testing, promote the release to *Production* and send it for review.

## Release build vs test build

The release build hides the 13 Demo locations and the Premium screen, because Google rejects apps that show features that don't work yet. Users connect by importing WireGuard configs.

When Xalane servers and Google Play Billing exist, set `DEMO_CONTENT` to `true` for release (or replace the demo data with real servers) in `app/build.gradle.kts`, increase `versionCode`, and upload a new bundle.

## Keep the upload key safe

If you lose `xalane-upload.jks` or its password, you can ask Google to reset the upload key (Play App Signing allows it), but it takes time. Store a copy somewhere safe, such as a password manager or a private drive.

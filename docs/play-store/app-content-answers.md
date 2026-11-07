# App content answers (Play Console → Policy → App content)

These answers match what version 1.0.0 actually does. Check them before you submit.

## Privacy policy
Paste the public URL of `privacy-policy.html`. Fill in `[DATE]` and `[CONTACT EMAIL]` first.

## Ads
**Does your app contain ads?** No.

## App access
**All functionality is available without special access.** No login. (Reviewers can import any WireGuard config to test connecting; you can also give them one in the instructions field.)

## Content rating
Run the questionnaire. Category: **Utility, productivity, communication or other**. Answer **No** to violence, sexual content, profanity, drugs, gambling, user-generated content sharing and location sharing. Expected rating: Everyone / PEGI 3.

## Target audience
Choose **18 and over** (or 13+). Do not include children; this keeps the app out of the Families policy.

## News app
No.

## Data safety
- **Does your app collect or share any of the required user data types?** No.
  - No account, analytics, ads or crash reporting.
  - VPN traffic goes to the server the user configures; Xalane does not receive it.
  - Settings and configs stay on the device.
  - The IP check (`api.ipify.org`) and optional AdGuard DNS are requests the user's device makes to show its own IP and to filter DNS; the app does not keep or send that data anywhere else. If you want to be extra careful, you can declare "Approximate location (IP address)" as *shared, not collected, ephemeral, for app functionality*.
- **Is all user data encrypted in transit?** Yes (WireGuard and HTTPS).
- **Can users request data deletion?** Data lives only on the device; removing a server or uninstalling deletes it.

## VPN service declaration
Play asks apps that use `VpnService` to declare it.
- **Core functionality:** Device-level VPN. The VPN is the main purpose of the app.
- The app does not collect personal or sensitive data through the VPN, does not redirect or manipulate traffic for ads, and does not change ads.
- Encryption: WireGuard (ChaCha20-Poly1305).

## Government apps, Financial features, Health
Not applicable / No.

# Google Play Data Safety Responses

**App:** Speedometer (`letshangllc.speedometer`)  
**Version:** 3.0.0 (versionCode 100)  
**Last Updated:** November 2026  
**Target SDK:** 36

Use this when filling the Play Console Data safety form and permission declarations.

---

## Overview

Speedometer shows the device’s current speed using the GPS / network location providers while the app is in the foreground. It displays AdMob banners (with UMP consent) and does not require an account.

---

## Permissions (Play declarations)

| Permission | Why | Notes for Play Console |
|------------|-----|------------------------|
| `ACCESS_FINE_LOCATION` | Compute speed from GPS while the UI is open | **Approximate / precise location** — declare location data; **not** used in the background |
| `ACCESS_COARSE_LOCATION` | Fallback / approximate location for speed | Same as above; foreground only |
| `INTERNET` | Load ads / consent | Standard |
| `ACCESS_NETWORK_STATE` | Ads SDK connectivity | Standard |
| `ACCESS_WIFI_STATE` | Ads SDK connectivity | Standard |

**Not used:** `ACCESS_BACKGROUND_LOCATION`, notifications, camera, mic, contacts, storage.

Location updates are requested in `onResume` / after permission grant and **removed in `onPause`**, so location is foreground-only.

Play Console should answer:

- Location collected? **Yes** (precise and/or approximate, depending on what the user grants)
- Used for app functionality (speed display)? **Yes**
- Shared with third parties for ads? **No** — location is used only for on-device speed; AdMob may collect device/ad IDs separately (see below)
- Background location? **No**

---

## Data Collection Summary

| Data Type | Collected | Shared | Purpose |
|-----------|-----------|--------|---------|
| Approximate / precise location | Yes (on device, while app open) | No | App functionality (speed) |
| App interactions | Yes | Yes (AdMob) | Advertising |
| Advertising ID / Device or other IDs | Yes | Yes (AdMob) | Advertising |
| Diagnostics from ads SDKs | Possible | Yes (Google) | Advertising / fraud prevention |

No email, name, account, photos, or contacts are collected by the app itself.

---

## Third-Party SDKs

### Google AdMob (`play-services-ads`)
- **Data:** Advertising ID, app interactions, device info (per Google’s policies)
- **Purpose:** Banner ads
- **HTTPS:** Cleartext traffic disabled; network security config blocks non-HTTPS
- **Privacy:** https://policies.google.com/privacy

### Google User Messaging Platform (UMP)
- **Purpose:** GDPR / consent before personalized ads
- **Privacy:** https://policies.google.com/privacy

Amazon Mobile Ads and other legacy ad SDKs were **removed**.

---

## Data Security

- **In transit:** HTTPS / TLS for ad and consent traffic  
- **At rest:** No user account cloud storage in this app  
- **Deletion:** No persistent personal profile; users can revoke location and reset advertising ID in system settings

---

## Children's Privacy

Not directed at children under 13 (COPPA) / under 16 (GDPR).  
`setTagForUnderAgeOfConsent(false)` in UMP. Do not enroll in Designed for Families unless you switch to child-directed ad settings.

---

## Play Console Form — Suggested Answers

1. **Does your app collect or share required user data types?** Yes  
2. **Encrypted in transit?** Yes  
3. **Users can request deletion?** Limited (no account); revoke permissions / reset ad ID  

Declare at least:

1. **Location** — Approximate and Precise — Collected, not shared — Purpose: App functionality — Ephemeral / while in use  
2. **Device or other IDs** — Collected and shared (AdMob) — Purpose: Advertising  
3. **App interactions** (if listed) — Shared with AdMob — Advertising  

---

## Owner TODOs before publish

1. Replace `TODO_*` AdMob app / banner unit IDs in `app/src/main/res/values/admob_ids.xml`  
2. Complete Data safety + Location permission declarations as above  
3. Confirm ads policy / UMP messaging in AdMob for this app ID  

# Daily Reset 每日重置

Offline Android daily checklist. Fixed daily tasks + today-only tasks, resets at midnight. Data stays on the phone.

APK is built automatically by GitHub Actions on every push to `main` — download it from **Releases**.

Web version (GitHub Pages, works offline after first open): https://moonsiao.github.io/daily-checklist/

## Layout
- `web/index.html` — single source of the checklist page. After editing run `python3 tools/sync_web.py`.
- `app/` — Android WebView shell + home screen progress widget (`ProgressWidget.java`).
- `docs/` — GitHub Pages (PWA + privacy policy at `docs/privacy.html`).
- Release signing: GitHub secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

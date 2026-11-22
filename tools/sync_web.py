"""Build the app and GitHub Pages copies from web/index.html (the single source).

web/index.html            -> also published as the Claude artifact (has optional cloud backup)
app/src/main/assets/...   -> Android app (cloud backup removed, Android bridge)
docs/index.html           -> GitHub Pages PWA (cloud backup removed, service worker)
Run: python3 tools/sync_web.py
"""
import pathlib
root = pathlib.Path(__file__).resolve().parent.parent
src = (root / "web/index.html").read_text(encoding="utf-8")

start = src.index("  // ---------- optional private cloud backup")
end = src.index("})();\n</script>")
src = src[:start] + src[end:]
for a in ["    clearTimeout(saveTimer);\n    saveTimer = setTimeout(pushCloud, 800);\n",
          "  var cloud = null; // optional private backup in the viewer's own Claude account\n",
          "  var saveTimer = null;\n"]:
    assert a in src, a
    src = src.replace(a, "")
a = "    try {\n      navigator.clipboard.writeText(v)"
assert a in src
src = src.replace(a, '    if(window.Android && Android.copy){ Android.copy(v); toast(T("copied")); return; }\n' + a)
a = "if(navigator.vibrate){ try{navigator.vibrate(15);}catch(_){} }"
assert a in src
src = src.replace(a, "if(window.Android && Android.vibrate){ try{Android.vibrate();}catch(_){} } else " + a)
(root / "app/src/main/assets/index.html").write_text(src, encoding="utf-8")

title = src[src.index("<title>"):src.index("</title>") + 8]
web = src.replace(title, title + '\n<link rel="manifest" href="manifest.json">\n<link rel="icon" href="icon-192.png">\n<link rel="apple-touch-icon" href="icon-192.png">')
web = web.replace("</script>\n</body>", "</script>\n<script>\nif('serviceWorker' in navigator){ navigator.serviceWorker.register('sw.js').catch(function(){}); }\n</script>\n</body>")
(root / "docs/index.html").write_text(web, encoding="utf-8")
print("synced")

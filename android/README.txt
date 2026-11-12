SUSTAINA Android app (website in an app + 11 home-screen widgets in Small, Medium and Large).
Built by GitHub Actions. Widget taps are saved on the phone and applied to your records when SUSTAINA is opened.


=====================================================
WHAT'S NEW IN VERSION 1.1
=====================================================
1. Customise each widget before it is added
   When you add a widget (from the Android widget list, or with "Add to home screen" in My Widgets),
   a "Customize widget" screen opens. It shows a live preview and lets you choose, for that one widget:
   Match the app setting / Light / Dark. Tap "Add widget" to place it. Each widget keeps its own choice.
   On Android 12 and newer you can press and hold a placed widget and use Reconfigure to change it later.
   Size: pick Small, Medium or Large in the Android widget list (they appear as three entries per widget).
   The background style (Nature / Plain / Solid) and the habits shown are still set in the app's My Widgets page.

2. The phone's Back button now closes an open pop-up first (for example the full-screen chat),
   then goes back a page.

3. website/index.html is the matching website. Upload it to Vercel so the app shows the full-screen chat
   with a back arrow, photos and files in chats, the inbox layout fix and the home "My widgets" fix.

Limits that remain (Android rules, not bugs)
 - Widgets cannot hold a text box. "Add entry" and "Write reflection" open a small pop-up to type in.
 - Taps on widgets are saved on the phone and added to your SUSTAINA records the next time the app is opened.
 - This is a debug build. Releasing on Google Play needs a signing key.
 - It was not possible to compile or run this pack where it was written. The first GitHub build is the real test;
   send any error text back to fix it.

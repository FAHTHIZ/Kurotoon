# Troubleshooting

## A source shows an error

- Open the title or source in WebView (globe icon) to check whether the website works in a browser.
- Update the extension in **Browse → Extensions**.
- Websites change often; if the site itself is down there is nothing the app can do until it is back.

## A site is missing from Extensions

Search **Browse › Extensions** for the site's name or paste its address (for example `https://hitomi.la/`).

- If the site is 18+ and hidden by your content settings, a card names it and offers **Allow 18+**, behind your
  device lock.
- If the search says none of your stores lists it, the store has dropped that site. Stores remove sites from time
  to time; as of November 2026 the Keiyoushi store no longer carries Hitomi. Add another store that has it, or
  install the extension's APK from a file.

## Cloudflare

Some sites protect themselves with Cloudflare. If a source keeps failing with a Cloudflare error:

1. Open the source in WebView and complete the check.
2. Go back and try again.
3. If it keeps happening, clear cookies in **Settings → Advanced → Clear cookies** and update your
   WebView from the Play Store. Changing the **Default user agent string** in **Settings → Advanced** can also help.

## Library updates fail

Open the notification after the update to see a list of errors per title. Most errors come from the
source. A title that moved on the website can be fixed with **Migrate** (see
[source migration](source-migration.md)).

## Crashes

Go to **Settings → Advanced → Share crash logs** and attach the file to a
[new issue](https://github.com/FAHTHIZ/Kurotoon/issues/new/choose).

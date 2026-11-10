# Troubleshooting

## A source shows an error

- Open the title or source in WebView (globe icon) to check whether the website works in a browser.
- Update the extension in **Browse → Extensions**.
- Websites change often; if the site itself is down there is nothing the app can do until it is back.

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

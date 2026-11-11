# Collection packs

Everything in the Collection (Me → Collection) comes from **packs**. A pack is a folder with a
`pack.json` and, optionally, images. KuroToon ships a few packs built in, and you can import more
from a `.kpack` file with the upload button at the top of the Collection.

Every item is free to use. Older packs may still list a `price`; it's ignored. Nothing is sent
anywhere.

## Where packs live

| Kind | Location |
| --- | --- |
| Built in | `app/src/main/assets/kuro/packs/<folder>/pack.json` |
| Imported | `filesDir/kuro/packs/<pack id>/` on the device |

Built-in folders are shown in name order, which is why they start with a number
(`01-starter`, `02-avatars`, …). Add a new folder to add a new built-in pack; no code changes
are needed. `PackTest` checks every built-in pack on each build, including that every image it
names exists.

## pack.json

```json
{
  "id": "my.pack",
  "name": "My pack",
  "author": "You",
  "version": 1,
  "description": "Shown on the Packs page.",
  "cover": "cover.png",
  "items": [
    {
      "id": "fox",
      "kind": "avatar",
      "name": "Fox",
      "description": "A clever fox.",
      "price": 400,
      "rarity": "rare",
      "image": "avatars/fox.png"
    }
  ]
}
```

| Field | Notes |
| --- | --- |
| `id` | Letters, digits, `.`, `_` and `-`; up to 64 characters. Pack ids must be unique, and item ids must be unique within a pack. Purchases are stored as `<pack id>/<item id>`, so don't rename ids after release. |
| `kind` | `avatar`, `frame`, `theme`, `particles`, `sticker`, `badge` or `boost`. Items with an unknown kind are skipped. |
| `price` | Ignored; everything is free. Kept so older packs still load. |
| `rarity` | `common`, `rare`, `epic`, `legendary` or `mythic`. Sets the tile's glow colour. |
| `image` | Optional. A path inside the pack (PNG, WebP, JPEG or GIF). Paths that leave the pack are ignored. |
| `emoji` | Optional. Used when there is no image (stickers, badges, avatars). |
| `colors` | Optional hex colours (`#RGB`, `#RRGGBB` or `#AARRGGBB`). What they mean depends on the kind; see below. |
| `style` | Optional. Picks a built-in drawing; see below. |

Unknown fields are ignored, so newer packs still load on older versions of the app.

## Item kinds

| Kind | With an image | Without an image |
| --- | --- | --- |
| `avatar` | Shown in a circle, cropped to fill. Square images work best (512×512). | `emoji`, or Kuro drawn in `colors`: body light, body dark, eyes, aura. |
| `frame` | Drawn **over** the avatar, so use a transparent PNG with a clear middle (512×512, the avatar takes up the middle 80%). Add `"style": "spin"` to make it rotate. | `style` is `ring`, `starfield`, `nebula`, `coins`, `mythic` or `aurora`, coloured by `colors`. |
| `theme` | The image is only used as the Collection preview. | `colors[0]` is the accent and `colors[1]` its darker shade. Recolours KuroToon when the app theme is set to KuroToon. |
| `particles` | — | `colors` are used for the drifting embers and the tap sparks. |
| `sticker` | Shown as is, fitted to the tile. | `emoji`. |
| `badge` | Shown on a shield in `colors`. Up to three badges show on the profile. | `emoji` on the shield. |
| `boost` | Not shown. Boosts belonged to the old points shop. | — |

## Seasonal items

Seasonal items are free all year like everything else.

## Achievement medals

Each achievement shows `app/src/main/assets/kuro/achievements/<code>.webp` (256×256, transparent). Locked medals are drawn greyed out. The streak tracker uses `assets/kuro/ui/chest.webp` and `freeze.webp`.

## Making a .kpack

A `.kpack` is a zip of the pack folder. `pack.json` can be at the root of the zip or inside one
top-level folder.

```sh
cd my-pack
zip -r ../my-pack.kpack .
```

Limits: 512 files and 64 MB unpacked. Importing a pack with the same id as one you've already
imported replaces it, which is how you ship an update. Built-in pack ids can't be reused.

## Adding artwork to a built-in pack

1. Put the image next to the pack's `pack.json`, for example
   `app/src/main/assets/kuro/packs/02-avatars/fox.png`.
2. Add an item with `"image": "fox.png"`, or add `image` to an existing item to replace its drawn
   preview.
3. Run `./gradlew :app:testDebugUnitTest --tests 'kurotoon.shop.*'` to check the pack.

## Backups

What you're using is stored in app preferences, so they're included in
KuroToon backups. Imported pack files are not; keep your `.kpack` files and import them again on a
new device.

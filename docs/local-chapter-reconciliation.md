# Local chapter reconciliation

KuroToon's local chapter reconciler merges metadata snapshots that the user already has. It does not query extensions, call remote providers, fetch chapter pages, or resolve URLs.

## Where the data lives

Imported snapshots are stored in the app-private file:

~~~text
files/kurotoon/catalogue/chapter-snapshots.json
~~~

This file is independent of the inherited tachiyomi.db database. Removing a snapshot from the screen removes only that imported snapshot. This store does not upload or sync snapshot contents.

## Import format

Import one JSON object per catalogue/source snapshot:

~~~json
{
  "schemaVersion": 1,
  "seriesTitle": "Example Series",
  "sourceLabel": "Local catalogue A",
  "chapters": [
    { "recordId": "a-1", "title": "Chapter 1", "number": "1" },
    { "recordId": "a-5", "title": "Chapter 5", "number": "5", "releaseLabel": "Edition A" },
    { "recordId": "a-8", "title": "Chapter 8", "number": "8" }
  ]
}
~~~

Required fields are seriesTitle, sourceLabel, and a non-empty chapters array. Each chapter requires a title; number, volume, releaseLabel, recordId, and publishedAtEpochMillis are optional. A number can be a whole number, decimal, or lettered part, represented as a string.

## Merge behavior

- Series are grouped by a normalized series title.
- Chapter numbers are normalized so values such as 005, 5.0, and 5 reconcile to the same chapter.
- When a number is absent, only an exact normalized chapter-title match is merged.
- All original chapter titles, source labels, record IDs, volume labels, and release labels are retained as variants.
- Repeated numbered entries from the same source are marked for review instead of discarded.
- Numbering gaps are calculated from the union of whole-number chapters. The gap report is not inferred across decimal or lettered chapters.
- Conflicting titles remain visible. The app does not silently pick one source's title as the only truth.

## Current scope

This phase is an offline metadata reconciliation workspace. It does not yet replace the app's reader chapter list or automatically import from installed extensions. The next integration step is a deliberate mapping from reconciled local metadata into KuroToon's own library model, with user-controlled conflict resolution.

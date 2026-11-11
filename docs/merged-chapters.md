# Merged chapters across sources

Sites rarely carry the same chapters of a series. One might have chapters 1–4 and 9, another 5–7 and 10. KuroToon combines them for you.

## When it shows up

Open a series that's in your library from more than one source. If the other copies have chapters this one doesn't, an **All chapters, every source** card appears above the chapter list, for example "5 more chapters from your other copies · 2 sources".

Copies count as the same series when:

- their titles match once brackets, punctuation, accents and case are ignored ("Solo Leveling (Official)" matches "solo-leveling"), and
- their authors agree, or one of them lists no author. "ONE" matches "ONE, Murata", but two different named authors never merge.

## How the list is built

- Chapters are combined by chapter number. Each number appears once.
- A chapter opens from the series you are viewing when it has it. Otherwise it opens from the copy with the most chapters.
- A chapter read in any copy shows as read.
- Each row shows the source it opens from. Chapters from other copies are highlighted.

Nothing is moved or rewritten. Each chapter still belongs to its own library entry, so downloads, reading history and tracking work exactly as before.

## While reading

When you finish a chapter and the copy you're reading has nothing next, or jumps ahead (4 → 9 while another copy has 5), the reader shows **Next up, on <source> · Continue** for a few seconds. Tap Continue to open that chapter from the copy that has it.

## In the library

With **Library settings › Combine copies** on (the default), a series saved from several sources shows once, with a ×N badge. The copy you read most recently stands for the group.

## Related

- **Library > ⋮ > Duplicates** lists every series you saved more than once, so you can migrate or remove copies.
- The code is `kurotoon/manga/MergedChapters.kt` (matching and merging, with unit tests) and `kurotoon/ui/manga/MergedChaptersCard.kt` (the card and sheet), `kurotoon/reader/NextElsewhere.kt` (the reader pill) and `kurotoon/library/LibraryCombine.kt` (the combined library).

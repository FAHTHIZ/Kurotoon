# Repository instructions

Read [CONTRIBUTING.md](CONTRIBUTING.md) before changing the application.

On every fresh checkout, run `python scripts/install-git-hooks.py` before creating commits.
The local `commit-msg` hook removes recognized AI-tool and bot credit trailers while preserving human
co-authors, human sign-offs and the rest of the commit message. Run
`python scripts/test_commit_attribution.py` to test the filter.

The hook affects new local commits only. Do not rewrite existing Git history; commits created through
GitHub's web interface or external automation are not filtered by this local hook.

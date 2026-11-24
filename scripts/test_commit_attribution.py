#!/usr/bin/env python3
"""Regression tests for KuroToon's commit-attribution filter."""
import unittest

from commit_attribution import is_ai_credit, strip_ai_attribution


class CommitAttributionTests(unittest.TestCase):
    def test_removes_perplexity_coauthor(self):
        message = "Fix the reader state\n\nCo-authored-by: Perplexity <noreply@perplexity.ai>\n"
        self.assertEqual(strip_ai_attribution(message), "Fix the reader state\n")

    def test_removes_other_ai_tool_trailers(self):
        message = "Improve search\n\nCo-authored-by: GitHub Copilot <copilot@users.noreply.github.com>\n"
        self.assertEqual(strip_ai_attribution(message), "Improve search\n")

    def test_removes_generated_footer(self):
        message = "Update onboarding\n\nGenerated with Claude Code\n"
        self.assertEqual(strip_ai_attribution(message), "Update onboarding\n")

    def test_preserves_human_coauthor(self):
        line = "Co-authored-by: Ada Lovelace <ada@example.org>"
        self.assertFalse(is_ai_credit(line))
        self.assertEqual(strip_ai_attribution("Change parser\n\n" + line + "\n"),
                         "Change parser\n\n" + line + "\n")

    def test_preserves_human_signoff(self):
        line = "Signed-off-by: Dev Example <dev@example.org>"
        self.assertFalse(is_ai_credit(line))
        self.assertEqual(strip_ai_attribution("Fix typo\n\n" + line + "\n"),
                         "Fix typo\n\n" + line + "\n")

    def test_preserves_body_and_issue_references(self):
        message = (
            "Fix reader restoration\n\n"
            "Keep the scroll position after rotation.\n\n"
            "Fixes #123\n\n"
            "Co-authored-by: Perplexity <noreply@perplexity.ai>\n"
        )
        self.assertEqual(
            strip_ai_attribution(message),
            "Fix reader restoration\n\nKeep the scroll position after rotation.\n\nFixes #123\n",
        )


if __name__ == "__main__":
    unittest.main()

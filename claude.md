# CLAUDE.md

Repository-specific instructions for Claude Code. These apply to every
invocation (comment mentions, PR reviews, issue assignments) regardless of
what the trigger comment itself says.

## PR Review Context (always required)

Whenever you review a pull request, answer questions about one, or are asked
to check/verify implementation work on a PR, do the following **before**
responding:

1. Read `review.md` at the repository root. It contains code style
   guidelines and a review checklist — apply it throughout your review.

2. Check whether `.github/reviews/PR-<PR_NUMBER>/jira.md` exists, using the
   current PR's number.
   - If found: read it and verify the implementation satisfies its
     acceptance criteria.
   - If not found: state this explicitly, near the top of your comment.

3. Check whether `.github/reviews/PR-<PR_NUMBER>/testcases.md` exists.
   - If found: verify the PR's tests cover the scenarios it describes.
   - If not found: state this explicitly, near the top of your comment.

4. Regardless of outcome, state near the top of your comment whether
   `jira.md` and `testcases.md` were found and read. This makes it possible
   to audit, from the PR thread itself, whether ticket context was actually
   used — don't skip this even if both files are missing.

5. Use `origin/master-gke` as the base ref for diffs
   (`git diff origin/master-gke...HEAD`), not `main` or `master`.

This applies even when the trigger comment is a bare `@claude` with no
further instructions — the review checklist and Jira/test-case verification
are standing requirements for this repo, not optional add-ons.

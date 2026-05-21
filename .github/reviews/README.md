# PR Review Context

This folder holds PR-specific context files used by Claude Code Review.

## Structure

```
.github/reviews/
├── README.md              # This file
├── template/
│   ├── jira-template.md   # Template for Jira ticket context
│   └── testcases-template.md  # Template for test cases
└── PR-{NUMBER}/
    ├── jira.md            # Jira ticket details for this PR
    └── testcases.md       # Test cases for this PR
```

## How to Use

Before opening a PR (or shortly after), create a folder for your PR number and fill in the context files:

```bash
PR=123
mkdir -p .github/reviews/PR-$PR
cp .github/reviews/template/jira-template.md .github/reviews/PR-$PR/jira.md
cp .github/reviews/template/testcases-template.md .github/reviews/PR-$PR/testcases.md
```

Then fill in `jira.md` and `testcases.md` with the relevant details from your Jira ticket.

Claude Code Review will automatically pick these up when reviewing the PR.

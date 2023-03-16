# Welcome to the BOAT docs contributing guide <!-- omit in toc -->

Thank you for investing your time in contributing to our project!

To build locally without gpg use `-Dgpg.skip`

### Pull Request

When you're finished with the changes, create a pull request, also known as a PR.
- Don't forget to [link PR to issue](https://docs.github.com/en/issues/tracking-your-work-with-issues/linking-a-pull-request-to-an-issue) if you are solving one.
- Enable the checkbox to [allow maintainer edits](https://docs.github.com/en/github/collaborating-with-issues-and-pull-requests/allowing-changes-to-a-pull-request-branch-created-from-a-fork) so the branch can be updated for a merge.
  Once you submit your PR, a Docs team member will review your proposal. We may ask questions or request for additional information.
- We may ask for changes to be made before a PR can be merged, either using [suggested changes](https://docs.github.com/en/github/collaborating-with-issues-and-pull-requests/incorporating-feedback-in-your-pull-request) or pull request comments. You can apply suggested changes directly through the UI. You can make any other changes in your fork, then commit them to your branch.
- As you update your PR and apply changes, mark each conversation as [resolved](https://docs.github.com/en/github/collaborating-with-issues-and-pull-requests/commenting-on-a-pull-request#resolving-conversations).
- If you run into any merge issues, checkout this [git tutorial](https://github.com/skills/resolve-merge-conflicts) to help you resolve merge conflicts and other issues.
- Make sure you update the release notes in the README.md file.

### Your PR is merged!

Congratulations :tada::tada: The Backbase team thanks you :sparkles:.

### Publish a new release
- This repository uses the GitHub releases mechanism to publish a new release. Release notes for those are generated automatically from the merged PRs descriptions, and a new release draft is created at every release event.
- Versioning of new releases happens automatically. The released version number the same present at the HEAD of the main branch, minus the `SNAPSHOT` suffix.
- The main branch will always contain a `SNAPSHOT` development version. In case you want to bump the next release version to a major or a minor version, you can do it manually by editing the POMs or using the `mvn versions:set` command. For Example:
```bash
    mvn versions:set -DnewVersion=1.0.0-SNAPSHOT
```
- In order to trigger the release process, a maintaner just has to hit the "Publish" button in the GitHub release page.
- Make sure the README.md contains the latest release notes.

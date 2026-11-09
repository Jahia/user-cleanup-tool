# Changelog

All notable changes to the User Cleanup Tool module are documented in this file.

## [Unreleased]

### Fixed
- Pinned `jahia-maven-plugin` to 6.12 so the module builds on a current JDK 17 toolchain (the parent 8.1.6.1 pulled 6.7, whose `prepare-package-dependencies` goal failed with `ClassNotFoundException: ParsingContext`).
- `RemovalUtility.removeNode` now verifies each node is a `jnt:ace` or `jnt:member` before deleting it (defense-in-depth; previously it would remove any path passed in).
- Admin tool page hardened against a non-numeric `nextAce`/`nextMember` query parameter (was a 500 `NumberFormatException`); provider key/mount-point output is now escaped.

### Changed
- Resolved 7 SonarQube issues in `RemovalUtility`: private constructor, extracted duplicated string literals (`j:principal`, `default`), de-duplicated the external user/group provider lookups into a shared helper (cognitive complexity), diamond operators, and a `final` logger.

### Accessibility
- Reworked the `cleanup-users` admin page toward WCAG 2.2 AAA: page `<title>` and `lang`, `<label>` association for every checkbox, a single `<main>` landmark and `<h1>`, visible `:focus-visible` indicators, responsive widths, and contrast raised to ≥7:1 (the previous red-on-yellow warning was ~2.5:1). Fixed duplicate element ids.

### Added
- First unit-test suite (JUnit 4 + Mockito, 10 tests) covering the `Scroller` pagination/offset algorithm, the `User` DTO, and the `jnt:ace`/`jnt:member` removal type-guard. JaCoCo coverage wiring for SonarQube.
- Expanded README describing the tool, the provider-inactivity caution, and the safety guard.

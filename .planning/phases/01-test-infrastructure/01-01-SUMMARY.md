---
phase: 01-test-infrastructure
plan: 01
subsystem: test-infrastructure
status: complete
tags: [testing, mockk, kotlin, jvm-unit-test, package-declaration]
completed: 2026-08-18T03:29:36Z

dependency_graph:
  requires: []
  provides:
    - io.github.oin.titanpocketkeyboard.VietnameseTextInput (packaged, importable from test code)
    - io.mockk:mockk:1.13.5 on test classpath
    - app/src/test/java/io/github/oin/titanpocketkeyboard/ (test source root)
  affects:
    - Phase 2 test suite (depends on this test infrastructure)
    - Phase 3 bug fixes (depends on working test runner)

tech_stack:
  added:
    - io.mockk:mockk:1.13.5 (testImplementation — Kotlin 1.9.x compatible MockK)
  patterns:
    - JUnit 4 @Test / @After conventions for Android JVM unit tests
    - mockkStatic(Log::class) for stubbing android.util.Log in JVM tests
    - Same-package test placement (no import needed for VietnameseTextInput)

key_files:
  created:
    - app/src/test/java/io/github/oin/titanpocketkeyboard/SmokeTest.kt
  modified:
    - app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt (package declaration added)
    - app/build.gradle.kts (testImplementation mockk added)
    - app/src/main/java/io/github/oin/titanpocketkeyboard/InputMethodService.kt (stale import removed)

decisions:
  - key: mockk-version-downgrade
    summary: "Used io.mockk:mockk:1.13.5 instead of plan-specified 1.14.9 — 1.14.9 requires Kotlin 2.x stdlib (metadata 2.2.0) which is incompatible with the project's Kotlin 1.9.0 compiler (max readable 2.0.0); 1.13.5 is the last MockK release targeting Kotlin 1.x"

metrics:
  duration_minutes: 12
  tasks_completed: 3
  commits: 3
  files_changed: 4

estimate:
  tokens: 45000

actuals:
  tokens: 8000
  tasks: 3
  commits: 3
---

# Phase 01 Plan 01: Test Infrastructure Summary

JVM unit test infrastructure established — package declaration on VietnameseTextInput.kt plus MockK 1.13.5 on test classpath, proven by a passing 3-test smoke suite via `./gradlew test`.

## What Was Built

The phase removed two blockers preventing JVM unit testing of core logic classes:

1. **INFRA-01 (Package declaration):** Added `package io.github.oin.titanpocketkeyboard` as the first line of `VietnameseTextInput.kt`, matching the four sibling files. The class was previously in the default package, making it unimportable by any packaged test class.

2. **INFRA-02 (MockK dependency):** Added `testImplementation("io.mockk:mockk:1.13.5")` to `app/build.gradle.kts`. MockK enables stubbing of `android.util.Log` and other Android framework classes in JVM tests without triggering `RuntimeException: Stub!`.

3. **INFRA-03 (Smoke test):** Created `SmokeTest.kt` with three `@Test` methods proving both fixes work end-to-end, and `./gradlew test` exits 0.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 (tracer) | Wire end-to-end test path | 553c022 | VietnameseTextInput.kt, app/build.gradle.kts |
| 2 (auto/tdd) | Create smoke test | f6ea843 | SmokeTest.kt (created) |
| 3 (auto) | Run ./gradlew test green | 26be0cc | app/build.gradle.kts, InputMethodService.kt |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Stale `import VietnameseTextInput` in InputMethodService.kt**
- **Found during:** Task 3 (first `./gradlew test` run)
- **Issue:** `InputMethodService.kt` line 3 had `import VietnameseTextInput` — a default-package import that worked before Task 1's package declaration. After adding the package, Kotlin cannot import a class from the default package when the importing file is itself in a package; the class is now same-package so the import is both invalid and redundant.
- **Fix:** Removed `import VietnameseTextInput` from InputMethodService.kt. Same-package membership makes the import unnecessary.
- **Files modified:** `app/src/main/java/io/github/oin/titanpocketkeyboard/InputMethodService.kt`
- **Commit:** 26be0cc

**2. [Rule 1 - Bug] MockK version 1.14.9 incompatible with Kotlin 1.9.0**
- **Found during:** Task 3 (first `./gradlew test` run after fixing deviation #1)
- **Issue:** `io.mockk:mockk:1.14.9` (plan-specified) ships Kotlin 2.2.x stdlib (metadata version 2.2.0). The project's Kotlin compiler is 1.9.0, which can only read metadata up to 2.0.0. This produced `Class 'kotlin.Unit' was compiled with an incompatible version of Kotlin` errors on every MockK API call in SmokeTest.kt.
- **Fix:** Downgraded to `io.mockk:mockk:1.13.5` — the last MockK series targeting Kotlin 1.x. All three INFRA requirements are still satisfied: mockkStatic compiles, Log.d stubbing works, and no `Stub!` exception is thrown.
- **Files modified:** `app/build.gradle.kts`
- **Commit:** 26be0cc

**3. [Rule 3 - Blocking] Missing local.properties in worktree**
- **Found during:** Task 3 (initial `./gradlew test` attempt)
- **Issue:** The worktree did not inherit `local.properties` from the main repo, causing `SDK location not found` before any compilation started.
- **Fix:** Created `local.properties` in the worktree root with `sdk.dir=/home/baongoc/Android/Sdk` (same SDK path as main repo). File is gitignored and not committed.
- **Files modified:** `local.properties` (not committed — gitignored)

## Verification Results

All phase-level checks passed:

1. `grep -c '^package io\.github\.oin\.titanpocketkeyboard' VietnameseTextInput.kt` → `1` (INFRA-01)
2. `grep -Fc 'io.mockk:mockk:1.13.5' app/build.gradle.kts` → `1` (INFRA-02)
3. `./gradlew test --console=plain` → `BUILD SUCCESSFUL` with 3/3 SmokeTest methods passing (INFRA-03)
4. `grep -c 'returnDefaultValues' app/build.gradle.kts` → `0` (design constraint upheld)
5. `import android.util.Log` still present and unmodified in VietnameseTextInput.kt

Test results from `TEST-io.github.oin.titanpocketkeyboard.SmokeTest.xml`:
- `tests="3" skipped="0" failures="0" errors="0"`
- `mockkStaticLogWorks` — 0.779s PASSED
- `packagedClassIsImportable` — 0.001s PASSED
- `runnerExecutesCoreLogic` — 0.000s PASSED

## Known Stubs

None. All three smoke tests exercise real behavior.

## Threat Flags

None. No new network endpoints, auth paths, or schema changes introduced. The MockK dependency is `testImplementation` only — it does not enter the release APK.

## Self-Check: PASSED

- `app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt` — FOUND, package declared on line 1
- `app/build.gradle.kts` — FOUND, contains `io.mockk:mockk:1.13.5`
- `app/src/test/java/io/github/oin/titanpocketkeyboard/SmokeTest.kt` — FOUND, 3 @Test + 1 @After
- Commits 553c022, f6ea843, 26be0cc — all present in git log

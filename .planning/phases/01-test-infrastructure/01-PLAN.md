---
phase: 01-test-infrastructure
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt
  - app/build.gradle.kts
  - app/src/test/java/io/github/oin/titanpocketkeyboard/SmokeTest.kt
autonomous: true
requirements: [INFRA-01, INFRA-02, INFRA-03]

estimate:
  tokens: 45000
  raw_tokens: 30000
  tasks: 3
  confidence: low

must_haves:
  truths:
    - "`./gradlew test` completes with exit code 0 — no compilation errors and the smoke test passes (INFRA-03)"
    - "A test class declared in package io.github.oin.titanpocketkeyboard can import and instantiate VietnameseTextInput without a 'cannot access default-package class' error (INFRA-01)"
    - "MockK is resolvable on the test classpath so `mockkStatic(Log::class)` compiles and runs without NoClassDefFoundError (INFRA-02)"
  artifacts:
    - "app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt with a `package io.github.oin.titanpocketkeyboard` first line"
    - "app/build.gradle.kts containing testImplementation(\"io.mockk:mockk:1.14.9\")"
    - "app/src/test/java/io/github/oin/titanpocketkeyboard/SmokeTest.kt — a JUnit4 smoke test that imports VietnameseTextInput and exercises MockK"
  key_links:
    - "VietnameseTextInput package declaration ↔ SmokeTest import statement — the smoke test import only resolves once the package is declared"
    - "MockK testImplementation entry ↔ SmokeTest mockkStatic(Log::class) call — the mock call only compiles once the dependency is on the test classpath"
---

<objective>
Enable JVM unit testing of the core logic classes by declaring the missing package on `VietnameseTextInput.kt`, adding the MockK test dependency, and proving both work end-to-end with a compiling, passing smoke test run via `./gradlew test`.

Purpose: Every downstream phase (the Phase 2 test suite, the Phase 3 fixes) depends on a working test runner. Today `VietnameseTextInput.kt` sits in the default package, so no packaged test can import it, and there is no mock framework to stub `android.util.Log` in JVM tests. This phase removes those two blockers and verifies them.
Output: Package declaration on `VietnameseTextInput.kt`, MockK on the test classpath, and `app/src/test/java/io/github/oin/titanpocketkeyboard/SmokeTest.kt` that compiles and passes.
</objective>

<execution_context>
@/home/baongoc/workspaces/titanpocketkeyboard-vi/.claude/gsd-core/workflows/execute-plan.md
@/home/baongoc/workspaces/titanpocketkeyboard-vi/.claude/gsd-core/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@.planning/STATE.md
@.planning/REQUIREMENTS.md

# Source of truth for the two edits and the class API the smoke test uses
@app/build.gradle.kts
@app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt
@app/src/main/java/io/github/oin/titanpocketkeyboard/Modifier.kt
</context>

<tasks>

<task type="tracer">
  <name>Task 1: Wire the end-to-end test path — declare package on VietnameseTextInput and add MockK dependency</name>
  <files>app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt, app/build.gradle.kts</files>
  <read_first>
    - app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt (currently first line is `import android.util.Log` — NO package declaration)
    - app/src/main/java/io/github/oin/titanpocketkeyboard/Modifier.kt (reference: its first line is the correct `package io.github.oin.titanpocketkeyboard` declaration to mirror)
    - app/build.gradle.kts (the `dependencies { }` block already has `testImplementation("junit:junit:4.13.2")` on the line to insert after)
  </read_first>
  <action>
    Two edits that together open the end-to-end test path — this is the thin slice every other phase builds on.

    Edit 1 (INFRA-01): In app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt, add `package io.github.oin.titanpocketkeyboard` as the new first line of the file, above the existing `import android.util.Log` line. Match the exact package string used by the four sibling files (InputMethodService.kt, Modifier.kt, MultipressController.kt, SettingsActivity.kt all declare `package io.github.oin.titanpocketkeyboard`). Do not change, reorder, or remove the existing `import android.util.Log` line or any other content — this is a pure prepend of one line plus a blank line separating the package from the import, following Kotlin official style.

    Edit 2 (INFRA-02): In app/build.gradle.kts, inside the `dependencies { }` block, add `testImplementation("io.mockk:mockk:1.14.9")` on the line immediately after the existing `testImplementation("junit:junit:4.13.2")` entry. Preserve the tab indentation used by the surrounding dependency lines. Do not modify, upgrade, or reorder any other dependency, and do not add `unitTests.returnDefaultValues` anywhere — MockK is the chosen mechanism for stubbing android framework classes, not default return values.

    These two edits are the only production changes in the phase; the smoke test in Task 2 proves both fired correctly by compiling against the newly-packaged class and against MockK on the classpath.
  </action>
  <verify>
    <automated>grep -q '^package io\.github\.oin\.titanpocketkeyboard' app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt && grep -Fq 'io.mockk:mockk:1.14.9' app/build.gradle.kts && echo TRACER_WIRED</automated>
  </verify>
  <done>VietnameseTextInput.kt begins with the `package io.github.oin.titanpocketkeyboard` declaration, and app/build.gradle.kts contains the `testImplementation("io.mockk:mockk:1.14.9")` entry. The grep verify prints TRACER_WIRED.</done>
  <acceptance_criteria>
    - Source assertion: line 1 of app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt matches `^package io\.github\.oin\.titanpocketkeyboard\s*$`
    - Source assertion: `import android.util.Log` still present in VietnameseTextInput.kt and unmodified
    - Source assertion: app/build.gradle.kts contains the literal `io.mockk:mockk:1.14.9` as a `testImplementation` dependency
    - Source assertion: app/build.gradle.kts still contains `testImplementation("junit:junit:4.13.2")` (existing dependency preserved)
    - CLI output: the `<automated>` verify command prints `TRACER_WIRED`
  </acceptance_criteria>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Create the smoke test proving the packaged import and MockK both work</name>
  <files>app/src/test/java/io/github/oin/titanpocketkeyboard/SmokeTest.kt</files>
  <read_first>
    - app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt (public API the smoke test calls: constructor `VietnameseTextInput()`, `fun processKey(char: Char): String?`, `fun reset()`, and public property `modifiableChars: Set<Char>`; note the class body does NOT itself call `android.util.Log`, so instantiation alone will not throw the Log stub)
    - app/build.gradle.kts (confirms `io.mockk:mockk:1.14.9` and `junit:junit:4.13.2` are on the test classpath after Task 1)
    - .planning/codebase/TESTING.md (JUnit 4 `@Test` / `@Before` conventions and the `src/test/java/io/github/oin/titanpocketkeyboard/` location convention)
  </read_first>
  <behavior>
    - Test A (proves INFRA-01, packaged import): construct a `VietnameseTextInput` from within package io.github.oin.titanpocketkeyboard and assert the instance is non-null; assert `modifiableChars` contains `'a'`. This only compiles if the default-package blocker is gone.
    - Test B (proves INFRA-02, MockK on classpath and functional): call `mockkStatic(Log::class)`, stub `Log.d(any(), any())` to return 0, invoke `android.util.Log.d("SmokeTest", "hello")`, assert it returns 0 without throwing `RuntimeException: Stub!`, then `unmockkStatic(Log::class)` in teardown. This only compiles and runs if MockK resolved on the test classpath.
    - Test C (proves the runner executes core logic end-to-end): call `processKey('a')` on a fresh instance and assert the returned `String?` is non-null (a bare vowel commits itself), demonstrating the core class runs under the JVM test runner.
  </behavior>
  <action>
    Create app/src/test/java/io/github/oin/titanpocketkeyboard/SmokeTest.kt as a JUnit4 test class named `SmokeTest` declared in `package io.github.oin.titanpocketkeyboard`. Import `org.junit.Test`, `org.junit.After`, `org.junit.Assert.assertNotNull`, `org.junit.Assert.assertTrue`, `org.junit.Assert.assertEquals`, `android.util.Log`, `io.mockk.every`, `io.mockk.mockkStatic`, `io.mockk.unmockkStatic`. Because the test lives in the same package as `VietnameseTextInput`, reference it directly with no import needed.

    Write three `@Test` methods matching the three behaviors above: `packagedClassIsImportable()` (construct VietnameseTextInput, assertNotNull the instance, assertTrue that `modifiableChars.contains('a')`), `mockkStaticLogWorks()` (mockkStatic(Log::class); every { Log.d(any(), any()) } returns 0; assertEquals(0, Log.d("SmokeTest", "hello"))), and `runnerExecutesCoreLogic()` (assertNotNull(VietnameseTextInput().processKey('a'))). Add an `@After` method `tearDown()` that calls `unmockkStatic(Log::class)` so the static mock does not leak between tests. Do not use `testOptions { unitTests.returnDefaultValues = true }` as an alternative — MockK is the required mechanism per INFRA-02.
  </action>
  <verify>
    <automated>test -f app/src/test/java/io/github/oin/titanpocketkeyboard/SmokeTest.kt && grep -q '^package io\.github\.oin\.titanpocketkeyboard' app/src/test/java/io/github/oin/titanpocketkeyboard/SmokeTest.kt && grep -q 'mockkStatic(Log::class)' app/src/test/java/io/github/oin/titanpocketkeyboard/SmokeTest.kt && echo SMOKE_TEST_PRESENT</automated>
  </verify>
  <done>SmokeTest.kt exists in the declared package with three @Test methods and an @After teardown; it imports VietnameseTextInput by package membership and calls mockkStatic(Log::class). The verify command prints SMOKE_TEST_PRESENT.</done>
  <acceptance_criteria>
    - Source assertion: app/src/test/java/io/github/oin/titanpocketkeyboard/SmokeTest.kt exists and line 1 matches `^package io\.github\.oin\.titanpocketkeyboard`
    - Source assertion: the file contains three methods annotated `@Test` and one annotated `@After`
    - Source assertion: the file contains `mockkStatic(Log::class)` and `unmockkStatic(Log::class)`
    - Behavior assertion: `packagedClassIsImportable()` constructs `VietnameseTextInput()` and asserts non-null (compiles only if INFRA-01 landed)
    - Behavior assertion: `mockkStaticLogWorks()` stubs and invokes `Log.d` without a `Stub!` RuntimeException (runs only if INFRA-02 landed)
    - CLI output: the `<automated>` verify command prints `SMOKE_TEST_PRESENT`
  </acceptance_criteria>
</task>

<task type="auto">
  <name>Task 3: Prove the whole slice — run ./gradlew test green (INFRA-03)</name>
  <files>app/src/test/java/io/github/oin/titanpocketkeyboard/SmokeTest.kt</files>
  <read_first>
    - app/build.gradle.kts (test task is the standard Android unit-test task; `./gradlew test` runs the debug + release unit test variants)
    - app/src/test/java/io/github/oin/titanpocketkeyboard/SmokeTest.kt (the test under execution)
  </read_first>
  <action>
    Run `./gradlew test` from the repository root and confirm it completes with exit code 0. This is the single end-to-end proof of the phase goal: it exercises the package declaration (compilation of the packaged import), the MockK dependency resolution (the mockkStatic call), and the JUnit4 runner (the three smoke tests execute and pass).

    If the run fails, diagnose from the gradle output and fix within scope: (a) a `cannot access class 'VietnameseTextInput'` or unresolved-reference error means the package declaration in Task 1 is missing or misspelled — re-verify line 1 of VietnameseTextInput.kt; (b) an `Unresolved reference: mockk` or `NoClassDefFoundError: io/mockk/...` means the MockK dependency did not resolve — re-verify the `testImplementation("io.mockk:mockk:1.14.9")` line in app/build.gradle.kts and that gradle can reach the repository; (c) a `RuntimeException: Stub!` from `Log` means the mockkStatic stub in SmokeTest.kt is not covering the invoked overload — align the `every { Log.d(...) }` stub with the exact `Log.d` overload the test calls. Do not resolve failures by adding `unitTests.returnDefaultValues = true` — that silently masks unmocked framework calls and is explicitly rejected for this project.
  </action>
  <verify>
    <automated>./gradlew test --console=plain</automated>
  </verify>
  <done>`./gradlew test` exits 0 with the three SmokeTest methods reported as passing and no compilation errors.</done>
  <acceptance_criteria>
    - CLI output: `./gradlew test --console=plain` exits with status 0 and prints `BUILD SUCCESSFUL`
    - Behavior assertion: the three `SmokeTest` methods (`packagedClassIsImportable`, `mockkStaticLogWorks`, `runnerExecutesCoreLogic`) all pass — no failures or errors in the test report
    - Behavior assertion: no `cannot access default-package class`, `Unresolved reference: mockk`, `NoClassDefFoundError`, or `RuntimeException: Stub!` appears in the gradle output
  </acceptance_criteria>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| dependency resolver → build | Gradle downloads `io.mockk:mockk:1.14.9` from a remote package repository; an untrusted or typo-squatted coordinate would execute in the build/test JVM |

## STRIDE Threat Register

| Threat ID | Category | Component | Severity | Disposition | Mitigation Plan |
|-----------|----------|-----------|----------|-------------|-----------------|
| T-01-SC | Tampering | Gradle testImplementation install of `io.mockk:mockk:1.14.9` | high | mitigate | Package legitimacy verified against research audit below; exact pinned version `1.14.9` (no dynamic `+` range); coordinate `io.mockk:mockk` is the canonical MockK artifact published on Maven Central |
| T-01-01 | Tampering | Editing VietnameseTextInput.kt (production source) | low | accept | Change is a single additive `package` line mirroring four sibling files; no behavioral code path altered; `./gradlew test` confirms compilation integrity |

## Package Legitimacy Audit

| Package | Verdict | Evidence |
|---------|---------|----------|
| io.mockk:mockk:1.14.9 | [VERIFIED] | MockK is the standard Kotlin mocking library; canonical Maven Central coordinate `io.mockk:mockk`; version pinned exactly per research finding (the ONLY new dependency this phase adds). Consumed only as `testImplementation` — never shipped in the release APK. |
</threat_model>

<verification>
Phase-level checks (all must pass):
1. `grep -c '^package io\.github\.oin\.titanpocketkeyboard' app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt` returns 1 (INFRA-01)
2. `grep -Fc 'io.mockk:mockk:1.14.9' app/build.gradle.kts` returns 1 (INFRA-02)
3. `./gradlew test --console=plain` exits 0 with `BUILD SUCCESSFUL` and all three SmokeTest methods passing (INFRA-03)
4. No `returnDefaultValues` appears in app/build.gradle.kts (design constraint: MockK is the mocking mechanism)
</verification>

<success_criteria>
1. `./gradlew test` completes without compilation errors — SmokeTest compiles and passes (matches phase success criterion 1)
2. A test class in package io.github.oin.titanpocketkeyboard imports/instantiates VietnameseTextInput with no "cannot access default-package class" error (matches phase success criterion 2)
3. MockK is on the test classpath so `mockkStatic(Log::class)` compiles and runs without NoClassDefFoundError (matches phase success criterion 3)
</success_criteria>

<artifacts_produced>
New symbols/files created or modified this phase:
- `package io.github.oin.titanpocketkeyboard` declaration added to `VietnameseTextInput.kt` (INFRA-01) — makes the class importable from packaged test code across Phases 2 and 3
- `testImplementation("io.mockk:mockk:1.14.9")` in `app/build.gradle.kts` (INFRA-02) — MockK available to all future JVM unit tests for stubbing `android.util.Log` and other framework classes
- `app/src/test/java/io/github/oin/titanpocketkeyboard/SmokeTest.kt` (INFRA-03) — JUnit4 class with `packagedClassIsImportable()`, `mockkStaticLogWorks()`, `runnerExecutesCoreLogic()`, and `tearDown()`; establishes the `src/test/java/io/github/oin/titanpocketkeyboard/` test source root that Phase 2's suite populates
</artifacts_produced>

<output>
Create `.planning/phases/01-test-infrastructure/01-01-SUMMARY.md` when done
</output>

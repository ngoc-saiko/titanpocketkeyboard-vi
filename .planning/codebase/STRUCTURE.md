# Codebase Structure

**Analysis Date:** 2026-08-18

## Directory Layout

```
titanpocketkeyboard-vi/
├── app/                                    # Android application module
│   ├── src/
│   │   └── main/
│   │       ├── java/io/github/oin/
│   │       │   └── titanpocketkeyboard/   # All Kotlin source files
│   │       │       ├── InputMethodService.kt          # Main service (800+ lines)
│   │       │       ├── VietnameseTextInput.kt         # Text composition (450+ lines)
│   │       │       ├── MultipressController.kt        # Key multipress logic (175 lines)
│   │       │       ├── Modifier.kt                    # Modifier state classes (116 lines)
│   │       │       └── SettingsActivity.kt            # Settings UI (75 lines)
│   │       ├── res/
│   │       │   ├── xml/
│   │       │   │   ├── preferences.xml                # Settings screen definition
│   │       │   │   ├── method.xml                     # InputMethod configuration
│   │       │   │   ├── backup_rules.xml               # Android backup config
│   │       │   │   └── data_extraction_rules.xml      # Android data rules
│   │       │   ├── layout/
│   │       │   │   └── settings_activity.xml          # Settings activity layout
│   │       │   ├── values/
│   │       │   │   ├── strings.xml                    # String resources
│   │       │   │   ├── arrays.xml                     # Template lists and arrays
│   │       │   │   ├── colors.xml                     # Color definitions
│   │       │   │   └── themes.xml                     # Light theme
│   │       │   ├── values-night/
│   │       │   │   └── themes.xml                     # Dark theme
│   │       │   ├── drawable/
│   │       │   │   ├── shift.xml                      # Shift key icon
│   │       │   │   ├── alt.xml                        # Alt key icon
│   │       │   │   ├── sym.xml                        # Sym key icon
│   │       │   │   ├── shiftlock.xml                  # Shift locked icon
│   │       │   │   ├── symshift.xml                   # Sym+shift icon
│   │       │   │   ├── ic_launcher_*.xml              # Launcher icons
│   │       │   │   └── ic_launcher_*.svg              # Icon sources
│   │       │   └── mipmap-*/
│   │       │       └── ic_launcher.xml                # Density-specific launchers
│   │       └── AndroidManifest.xml                    # App manifest
│   └── build.gradle.kts                               # App build config
├── gradle/wrapper/                        # Gradle wrapper version files
├── build.gradle.kts                       # Project-level Gradle config
├── settings.gradle.kts                    # Gradle settings (module definition)
├── gradlew, gradlew.bat                   # Gradle executables
├── gradle.properties                      # Gradle properties
├── local.properties                       # Local SDK/NDK paths (gitignored)
├── key.jks, key2.jks                      # Release signing keystores (gitignored)
├── README.md                              # Project documentation
├── .gitignore                             # Git ignore rules
└── .idea/                                 # IntelliJ IDEA config (generated)
```

## Directory Purposes

**`app/src/main/java/io/github/oin/titanpocketkeyboard/`:**
- Purpose: Core application logic in Kotlin
- Contains: Input method service, text composition, key processing, settings
- Key files: `InputMethodService.kt` (entry point), `VietnameseTextInput.kt` (Vietnamese composition)

**`app/src/main/res/xml/`:**
- Purpose: Android XML configuration files
- Contains: Preference definitions, input method config, backup and data extraction rules
- Key files: `preferences.xml` (user-facing settings), `method.xml` (IME configuration)

**`app/src/main/res/layout/`:**
- Purpose: Activity layouts
- Contains: Single file `settings_activity.xml` for the preferences UI container

**`app/src/main/res/values/`:**
- Purpose: String, array, color, and theme resources
- Contains: Localization strings, template lists, color definitions, light theme
- Key files: `strings.xml` (app name, labels), `arrays.xml` (language templates)

**`app/src/main/res/values-night/`:**
- Purpose: Dark theme resources
- Contains: Theme override for night mode

**`app/src/main/res/drawable/`:**
- Purpose: Vector drawable icons
- Contains: Modifier key icons (shift, alt, sym) and launcher icons
- Key files: Shift, Alt, Sym icon vectors; launcher foreground/background

**`app/src/main/res/mipmap-*/`:**
- Purpose: Density-specific launcher icons
- Contains: App icon in various resolutions (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi, anydpi)

## Key File Locations

**Entry Points:**
- `app/src/main/java/io/github/oin/titanpocketkeyboard/InputMethodService.kt`: Main service entry point; handles all key events
- `app/src/main/java/io/github/oin/titanpocketkeyboard/SettingsActivity.kt`: Settings UI entry point; user configuration
- `app/src/main/AndroidManifest.xml`: App manifest defining service and activity exports

**Configuration:**
- `app/src/main/res/xml/preferences.xml`: User-facing settings screen (toggles, sliders, templates)
- `app/src/main/res/xml/method.xml`: Android InputMethod metadata
- `app/src/main/res/values/arrays.xml`: Language templates and arrays (Vietnamese, English, French, etc.)
- `build.gradle.kts`: App build configuration; dependencies, SDK levels, compilation settings

**Core Logic:**
- `app/src/main/java/io/github/oin/titanpocketkeyboard/InputMethodService.kt`: 800+ lines; service lifecycle, key routing, modifier tracking
- `app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt`: 450+ lines; Vietnamese tone marks and character composition
- `app/src/main/java/io/github/oin/titanpocketkeyboard/MultipressController.kt`: 175 lines; multi-level key press handling
- `app/src/main/java/io/github/oin/titanpocketkeyboard/Modifier.kt`: 116 lines; modifier state machine (Shift, Alt, Sym)

**Testing:**
- Not detected; no test directory found

## Naming Conventions

**Files:**
- Kotlin source: PascalCase (InputMethodService.kt, VietnameseTextInput.kt)
- Resource files: snake_case (settings_activity.xml, preferences.xml, ic_launcher_background.xml)
- Java package: reverse domain + project name (io.github.oin.titanpocketkeyboard)

**Directories:**
- Standard Android structure: `src/main/java`, `src/main/res`, `src/main/res/{values,layout,drawable,xml,mipmap-*}`
- Resource types grouped by function: `values` (strings/arrays/colors/themes), `xml` (config), `drawable` (vectors), `layout` (activities)

**Classes:**
- Activity: Suffix `-Activity` (SettingsActivity)
- Service: Suffix `-Service` (InputMethodService)
- Helpers/Controllers: Functional names (Modifier, MultipressController, VietnameseTextInput)

**Constants:**
- Module-level constants use SCREAMING_SNAKE_CASE (e.g., MPSUBST_BYPASS, MPSUBST_NOTHING, KEYCODE_FUNCTION)
- Preference keys are CamelCase matching Android preference naming (AutoCapitalize, DotSpace, FirstLevelTemplate)

## Where to Add New Code

**New Input Processing Feature (e.g., custom key handling):**
- Primary code: `app/src/main/java/io/github/oin/titanpocketkeyboard/InputMethodService.kt` - add new case in `onKeyDown()` or `onKeyUp()` around line 500
- State tracking: Add field to InputMethodService class if state needs to persist across key presses
- Configuration: If user-configurable, add preference to `app/src/main/res/xml/preferences.xml`

**New Language Template:**
- Template definition: Add entry to `templates` HashMap in `InputMethodService.kt` around line 63
- UI option: Add item to `first_level_template` and `first_level_template_values` arrays in `app/src/main/res/values/arrays.xml`
- Selection logic: Ensure `InputMethodService.updateFromPreferences()` loads the new template by key

**New Settings Option:**
- UI: Add SwitchPreference, ListPreference, or SeekBarPreference to `app/src/main/res/xml/preferences.xml`
- Preference key: Use CamelCase (e.g., NewOption)
- Kotlin logic: Add field to InputMethodService; read value in `updateFromPreferences()` around line 700
- Default value: Set default in both preferences.xml and Kotlin code

**Text Composition Enhancement (Vietnamese-specific):**
- Core logic: `app/src/main/java/io/github/oin/titanpocketkeyboard/VietnameseTextInput.kt`
- Character maps: Update `vowelMap`, `tonedVowelSet`, `removeToneMap`, `charModifiers`, `wCharModifiers`, or `toneMarks` as needed (lines 10-98)
- Composition flow: Modify `addChar()`, `applyToneMark()`, or add new methods for new behavior
- Integration: Ensure InputMethodService calls updated methods in `onKeyDown()`/`onKeyUp()`

**Modifier Key Behavior Change:**
- Logic: `app/src/main/java/io/github/oin/titanpocketkeyboard/Modifier.kt` for Shift/Alt or SimpleModifier for Sym
- State transitions: Modify `onKeyDown()`, `onKeyUp()`, or threshold checks in `lockThreshold`/`nextThreshold`
- Integration: InputMethodService manages modifier instances; ensure key codes trigger modifier updates

**Utilities / Shared Helpers:**
- Location: Add to `app/src/main/java/io/github/oin/titanpocketkeyboard/` as new Kotlin file (e.g., TextUtils.kt, KeyboardUtils.kt)
- Import: InputMethodService and other components import as needed
- No separate utils directory; keep all logic in main package for simplicity

**Feedback/Haptics Enhancement:**
- Vibration/tone logic: Currently in InputMethodService `provideFeedback()` method
- Extract to: New class (e.g., KeyboardFeedback.kt) if expanding beyond vibration/tone
- Configuration: Add preference to `preferences.xml` if user should control feedback

## Special Directories

**`build/`:**
- Purpose: Build artifacts
- Generated: Yes
- Committed: No (in .gitignore)

**`.gradle/`:**
- Purpose: Gradle cache and build metadata
- Generated: Yes
- Committed: No (in .gitignore)

**`.idea/`:**
- Purpose: IntelliJ IDEA project configuration
- Generated: Yes
- Committed: Partially (some files tracked for consistency)

**`gradle/wrapper/`:**
- Purpose: Gradle wrapper distribution
- Generated: No (checked in)
- Committed: Yes (needed for builds without Gradle pre-installed)

---

*Structure analysis: 2026-08-18*

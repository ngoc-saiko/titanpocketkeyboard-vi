**TitanPocketKeyboard** is an Android input method that entirely relies on the embedded keyboard of the Unihertz Titan Pocket and provides most features expected on a full-sized keyboard.

This is a fork from https://github.com/oin/titanpocketkeyboard and is heavily customized to work with Vietnamese.

Demo video from the main repo: https://github.com/oin/titanpocketkeyboard/assets/433830/59be4e0c-d9e0-4350-939b-1fdef5572e33

# Installation

1. Download the latest APK from the [releases](https://github.com/oin/TitanPocketKeyboard/releases) page.
2. Install the APK on your Unihertz Titan Pocket.
3. Go to `Settings` > `System` > `Languages & input` > `Virtual keyboard` > `Manage keyboards` and enable `TitanPocketKeyboard`.

# Features

- Full keyboard layout with all keys and symbols.
- No space taken on the screen.
- Long press for alternate characters (see list below).
- Multiple key presses (in quick succession) for accented characters, and other alternate symbols (see list below).
- Keyboard navigation, simulating arrow keys and home/end/page up/page down keys, using the `sym` modifier.
- Lock modifier keys by double-tapping them, while a single tap will only have effect for the next key press.
- See modifier key state in the status bar.
- Auto-capitalization of the first letter of a sentence.
- Two spaces after a period automatically replaced by a period and a space.
- **Vietnamese Telex input** with correct tone mark placement.
- **Shortcuts** — expand abbreviations to full text on Enter.
- **Speech to text** — dictate into any text field.

# Vietnamese Input (Telex)

Select the **Vietnamese** template in Settings → Accented characters → Template to enable Telex mode.

## Tone marks

Type the tone key **after** the vowels of the syllable:

| Key | Tone | Example |
| --- | --- | --- |
| `s` | sắc (´) | `as` → `á` |
| `f` | huyền (`) | `af` → `à` |
| `r` | hỏi (ˀ) | `ar` → `ả` |
| `x` | ngã (~) | `ax` → `ã` |
| `j` | nặng (.) | `aj` → `ạ` |

The tone is automatically placed on the correct vowel nucleus, even in complex syllables (e.g. `yeus` → `yếu`, `thuongs` → `thướng`).

## Diacritics

| Keys | Result | Keys | Result |
| --- | --- | --- | --- |
| `aa` | `â` | `oo` | `ô` |
| `aw` | `ă` | `ow` | `ơ` |
| `ee` | `ê` | `uw` | `ư` |
| `dd` | `đ` | | |

Pressing `w` after `a`, `o`, or `u` also works as a shorthand: `aw` → `ă`, `ow` → `ơ`, `uw` → `ư`.

Type the same modifier twice to undo it (e.g. `aaw` → `aw`).

## Examples

| Type | Result |
| --- | --- |
| `viet` | `việt` |
| `namf` | `nàm` |
| `tieets` | `tiết` |
| `yeus` | `yếu` |
| `thuongs` | `thướng` |
| `dduwowngf` | `đường` |

# Shortcuts

Shortcuts let you expand abbreviations into full phrases automatically when you press Enter.

## Setup

1. Go to Settings → Accented characters → **Shortcuts**.
2. Add one shortcut per line in the format:

```
shortcut ## Full text
```

**Example:**

```
btw ## by the way
addr ## 123 Main Street, Hanoi
tks ## Cảm ơn bạn rất nhiều!
```

Matching is case-insensitive. The shortcut key can be lowercase; it will match regardless of how you typed it.

## Usage

1. Type the shortcut in any text field.
2. Press **Enter** — the shortcut is instantly replaced with the full text.

# Speech to Text

Dictate text into any text field using the device microphone.

## Setup

Grant the microphone permission the first time you use the feature (a system dialog will appear).

## Usage

1. **Long-press the `Fn` key** to start listening. The IME begins capturing your speech.
2. **Speak** — recognized text is inserted into the text field in real time.
3. **Press `Space`** to stop listening.

# Keyboard layout

The keyboard layout is based on the QWERTY layout of the Unihertz Titan Pocket, extended to allow access to most characters and symbols expected on a full-sized keyboard.

## Accented characters

You can select a template that specifies the characters that can be accessed by multiple key presses in quick succession (_multipress_).
There is an option to disable the multipress feature for consonants.

### French

| Key | Multipress |
| --- | --- |
| `a` | `à`, `â`, `æ` |
| `e` | `é`, `è`, `ê`, `ë` |
| `i` | `î`, `ï` |
| `o` | `ô`, `œ` |
| `u` | `ù`, `û`, `ü` |
| `y` | `ÿ` |
| `c` | `ç` |

### Spanish

| Key | Multipress |
| --- | --- |
| `a` | `á` |
| `e` | `é` |
| `i` | `í` |
| `o` | `ó` |
| `u` | `ú` |

### German

| Key | Multipress |
| --- | --- |
| `a` | `ä` |
| `o` | `ö` |
| `u` | `ü` |
| `s` | `ß` |

### Portuguese

| Key | Multipress |
| --- | --- |
| `a` | `á`, `â`, `à`, `ã` |
| `e` | `é`, `ê` |
| `i` | `í` |
| `o` | `ó`, `ô`, `õ` |
| `u` | `ú` |
| `c` | `ç` |

### French + ES/DE/PT (default)

| Key | Multipress |
| --- | --- |
| `a` | `à`, `â`, `á`, `ä`, `ã` |
| `e` | `é`, `è`, `ê`, `ë` |
| `i` | `î`, `í`, `ï`, `ì` |
| `o` | `ô`, `ó`, `ò`, `ö`, `õ` |
| `u` | `ù`, `û`, `ú`, `ü` |
| `c` | `ç` |

### áàâäã

| Key | Multipress |
| --- | --- |
| `a` | `á`, `à`, `â`, `ä`, `ã` |
| `e` | `é`, `è`, `ê`, `ë`, `ẽ` |
| `i` | `í`, `ì`, `î`, `ï`, `ĩ` |
| `o` | `ó`, `ò`, `ô`, `ö`, `õ` |
| `u` | `ú`, `ù`, `û`, `ü`, `ũ` |

### àáâäã

| Key | Multipress |
| --- | --- |
| `a` | `à`, `á`, `â`, `ä`, `ã` |
| `e` | `è`, `é`, `ê`, `ë`, `ẽ` |
| `i` | `ì`, `í`, `î`, `ï`, `ĩ` |
| `o` | `ò`, `ó`, `ô`, `ö`, `õ` |
| `u` | `ù`, `ú`, `û`, `ü`, `ũ` |

## Additional characters

The following table shows the characters that can be accessed with a long press and subsequent multipresses.
The characters in bold are the ones that are marked on the labels of the keys.

| Key | Long press |
| --- | --- |
| **`q`** | **`0`**, `°` (degree) |
| **`w`** | **`1`**, `&`, `↑` |
| **`e`** | **`2`**, `€`, `∃` |
| **`r`** | **`3`**, `®` |
| **`t`** | **`(`**, `[`, `{`, `<`, `≤`, `†`, `™` |
| **`y`** | **`)`**, `]`, `}`, `>`, `≥` |
| **`u`** | **`-`**, `–` (em dash), `–` (en dash), `∪` |
| **`i`** | **`_`**, `\|` |
| **`o`** | `ô`, `ó`, `ò`, `ö`, `õ` | **`/`**, `\`, `œ`, `º` (masc. ordinal), `÷` |
| **`p`** | **`:`**, `;`, `¶` |
| | |
| **`a`** | **`@`**, `æ`, `ª`, `←` |
| **`s`** | **`4`**, `ß`, `§`, `↓` |
| **`d`** | **`5`**, `∂`, `→`, `⇒` |
| **`f`** | **`6`**, `^` |
| **`g`** | **`*`**, `•`, `·` |
| **`h`** | **`#`**, `²`, `♯` |
| **`j`** | **`+`**, `=`, `≠`, `≈`, `±` |
| **`k`** | **`"`**, `%`, `‰`, `‱` |
| **`l`** | **`'`**, `` ` `` |
| | |
| **`z`** | **`!`**, `¡`, `‽` |
| **`x`** | **`7`**, `×`, `χ` |
| **`c`** | **`8`**, `ç` `©`, `¢`, `⊂`, `⊄`, `⊃`, `⊅` |
| **`v`** | **`9`**, `∀`, `√` |
| **` ` (space bar)** | `	` (tab), `⇥` |
| **`b`** | **`.`**, `…`, `ß`, `∫`, `♭` |
| **`n`** | **`,`**, `ñ`, `¬`, `∩` |
| **`m`** | **`?`**, `$`, `€`, `£`, `¿` |

## `sym` modifier map

Using the `sym` modifier, you can access more keys and symbols.
For instance, you can use `WASD` or `HJKL` to navigate in text.

![`sym` modifier map](readme-symbehavior.png)

The Cut/Copy/Paste actions are only available when no modifier is pressed.

# Customizing and contributing

Feel free to adjust the layout to your needs by modifying the code and building your own version. If you think your changes could be useful to others, please consider contributing them back to this project or making a public fork.

---
name: Spend Tracker
description: The month's spending set as a supermarket shelf price, readable from arm's length.
colors:
  tag-yellow: "#FFD400"
  tag-ink: "#151412"
  needs-answer-red: "#D90D25"
  paper: "#F4F1E8"
  white: "#FFFFFF"
  ground-light: "#FAF8F1"
  raised-light: "#F1EEE3"
  raised-dark: "#26241F"
  stone-light: "#E9E5D9"
  stone-dark: "#302D28"
  muted-on-light: "#5C584E"
  muted-on-dark: "#B8B4A8"
  outline: "#8D897D"
  rule-light: "#DAD6C9"
  rule-dark: "#34322D"
  category-pale: "#D3CFC2"
  category-dim: "#66625A"
typography:
  price-home:
    fontFamily: "Golos Text"
    fontSize: "92sp"
    fontWeight: 900
    letterSpacing: "-0.04em"
  price-prompt:
    fontFamily: "Golos Text"
    fontSize: "72sp"
    fontWeight: 900
    letterSpacing: "-0.04em"
  price-row:
    fontFamily: "Golos Text"
    fontSize: "22sp"
    fontWeight: 800
    lineHeight: "28sp"
    letterSpacing: "0em"
    fontFeature: "tnum"
  price-row-small:
    fontFamily: "Golos Text"
    fontSize: "14sp"
    fontWeight: 700
    lineHeight: "20sp"
    letterSpacing: "0em"
    fontFeature: "tnum"
  headline-small:
    fontFamily: "Golos Text"
    fontSize: "24sp"
    fontWeight: 800
    lineHeight: "32sp"
    letterSpacing: "0em"
  title-large:
    fontFamily: "Golos Text"
    fontSize: "22sp"
    fontWeight: 800
    lineHeight: "28sp"
    letterSpacing: "0em"
  title-medium:
    fontFamily: "Golos Text"
    fontSize: "18sp"
    fontWeight: 700
    lineHeight: "24sp"
    letterSpacing: "0em"
  title-small:
    fontFamily: "Golos Text"
    fontSize: "16sp"
    fontWeight: 700
    lineHeight: "24sp"
    letterSpacing: "0em"
  row-title:
    fontFamily: "Golos Text"
    fontSize: "16sp"
    fontWeight: 500
    lineHeight: "24sp"
    letterSpacing: "0em"
  body-large:
    fontFamily: "Golos Text"
    fontSize: "16sp"
    fontWeight: 400
    lineHeight: "24sp"
    letterSpacing: "0em"
  body-small:
    fontFamily: "Golos Text"
    fontSize: "14sp"
    fontWeight: 400
    lineHeight: "20sp"
    letterSpacing: "0em"
  label-large:
    fontFamily: "Golos Text"
    fontSize: "16sp"
    fontWeight: 700
    lineHeight: "24sp"
    letterSpacing: "0em"
  label-small:
    fontFamily: "Golos Text"
    fontSize: "14sp"
    fontWeight: 700
    lineHeight: "20sp"
    letterSpacing: "0em"
rounded:
  swatch: "2dp"
  tag: "12dp"
  add: "14dp"
  sheet: "20dp"
spacing:
  hair: "2dp"
  xs: "4dp"
  sm: "8dp"
  md: "12dp"
  lg: "16dp"
  edge: "24dp"
  section: "32dp"
  add-clearance: "96dp"
components:
  top-bar:
    backgroundColor: "{colors.tag-yellow}"
    textColor: "{colors.tag-ink}"
    typography: "{typography.title-large}"
  shelf-field:
    backgroundColor: "{colors.tag-yellow}"
    textColor: "{colors.tag-ink}"
    padding: "0 24dp 16dp 24dp"
  button-add:
    backgroundColor: "{colors.tag-yellow}"
    textColor: "{colors.tag-ink}"
    typography: "{typography.label-large}"
    rounded: "{rounded.add}"
    height: "56dp"
  button-filled-light:
    backgroundColor: "{colors.tag-ink}"
    textColor: "{colors.white}"
    typography: "{typography.label-large}"
    rounded: "{rounded.tag}"
    height: "48dp"
  button-filled-dark:
    backgroundColor: "{colors.tag-yellow}"
    textColor: "{colors.tag-ink}"
    typography: "{typography.label-large}"
    rounded: "{rounded.tag}"
    height: "48dp"
  button-answer-light:
    backgroundColor: "{colors.stone-light}"
    textColor: "{colors.tag-ink}"
    typography: "{typography.title-medium}"
    rounded: "{rounded.tag}"
    padding: "0 24dp"
    height: "64dp"
  button-answer-dark:
    backgroundColor: "{colors.stone-dark}"
    textColor: "{colors.paper}"
    typography: "{typography.title-medium}"
    rounded: "{rounded.tag}"
    padding: "0 24dp"
    height: "64dp"
  button-save-typed:
    backgroundColor: "{colors.tag-yellow}"
    textColor: "{colors.tag-ink}"
    typography: "{typography.label-large}"
    rounded: "{rounded.tag}"
    height: "56dp"
  text-field:
    rounded: "{rounded.tag}"
    typography: "{typography.body-large}"
  row-purchase-light:
    backgroundColor: "{colors.ground-light}"
    textColor: "{colors.tag-ink}"
    typography: "{typography.row-title}"
    padding: "12dp 24dp"
    height: "72dp"
  row-purchase-dark:
    backgroundColor: "{colors.tag-ink}"
    textColor: "{colors.paper}"
    typography: "{typography.row-title}"
    padding: "12dp 24dp"
    height: "72dp"
  row-purchase-unanswered:
    backgroundColor: "{colors.needs-answer-red}"
    textColor: "{colors.white}"
    typography: "{typography.row-title}"
    padding: "12dp 24dp"
    height: "72dp"
  month-bar:
    backgroundColor: "{colors.tag-ink}"
    rounded: "{rounded.swatch}"
    height: "56dp"
  category-swatch:
    rounded: "{rounded.swatch}"
    size: "12dp"
  category-split-bar:
    rounded: "{rounded.swatch}"
    height: "16dp"
  dialog-light:
    backgroundColor: "{colors.raised-light}"
    textColor: "{colors.tag-ink}"
    typography: "{typography.headline-small}"
    rounded: "{rounded.sheet}"
  dialog-dark:
    backgroundColor: "{colors.raised-dark}"
    textColor: "{colors.paper}"
    typography: "{typography.headline-small}"
    rounded: "{rounded.sheet}"
---

# Design System: Spend Tracker

## Overview

**Creative North Star: "The Shelf Tag"**

The app is a supermarket price label that happens to know your month. Price-label yellow runs edge to edge across the top of every screen with ink on it, and the month's total sits there as a shelf price: a huge number with a small raised currency sign and raised, underlined cents. The same tag appears on Home, in the purchase pop-up and, small, on every purchase row. It is read from arm's length, one-handed, standing up.

Below the yellow the app goes quiet: an ink ground in dark theme, a warm off-white in light, thin rules between rows. One heavy grotesque carries everything. Colour is rationed so it can mean something, and the one red on the shelf means a purchase is still waiting for an answer. Loud where the number is and plain where the list is, it has to clear the owner's bar of neither plain nor childish.

The world was chosen against the category default of a dark screen with a rounded summary card and a blue accent. It is still built on Material 3 navigation, controls and system Back, re-coloured and re-shaped, not replaced.

**Key Characteristics:**
- Yellow field and top bars, edge to edge, in both themes
- The shelf price as the one signature, at three sizes
- One family, Golos Text, with nothing under 14sp
- Red for one meaning only
- Flat, divided by thin rules and not by cards
- Motion only to show where things went: sliding pages, gliding rows, the month change

## Colors

A loud label yellow and ink, one rationed red, and warm greys that stay out of the way.

### Primary
- **Price-Label Yellow** (#FFD400): the top bar on every screen, the Home field, the band at the top of the pop-up, the Add button and the pop-up's Save, in both themes. Also the launcher background and splash. In dark theme it is the interactive colour too: filled buttons, dialog actions, focused fields, checkboxes, radios and the credit link.
- **Tag Ink** (#151412): everything set on yellow (12.9:1), the dark theme ground, and the text and interactive colour in light theme.

### Secondary
- **Needs-Answer Red** (#D90D25): the full-width fill of an unanswered purchase row, with white text on it (5.2:1), the "Not answered yet" segment and swatch in the category breakdown, and the Delete action on a purchase, in its swipe panel and its confirmation button. It uses the Material errorContainer slot, but it does not mean error.
- **Change Green** (#1B7F45): only the Change panel behind a swiped purchase, with white text on it (5.0:1). The owner asked for a green pen here.

### Neutral
- **Paper** (#F4F1E8): text on the dark ground (16.3:1), and the second category colour in dark theme.
- **White** (#FFFFFF): text on red, and on ink filled buttons in light theme.
- **Warm Off-White** (#FAF8F1): background and surface in light theme.
- **Raised Light** (#F1EEE3) and **Raised Dark** (#26241F): the Add dialog's container, the surfaceContainerHigh role.
- **Stone Light** (#E9E5D9) and **Stone Dark** (#302D28): the pop-up's answer buttons, the secondaryContainer role.
- **Muted on Light** (#5C584E, 6.7:1) and **Muted on Dark** (#B8B4A8, 8.9:1): secondary text, trailing chevrons and "Not a purchase".
- **Outline** (#8D897D): text field outlines in both themes, and the 1dp edge on pale category fills on the light ground.
- **Rule Light** (#DAD6C9) and **Rule Dark** (#34322D): the thin rules between rows.
- **Chart colours** are solid and come only from the palette above, spread far apart in lightness. In dark theme the first, second and third purchase options are Price-Label Yellow, Paper and **Deep Yellow** (#A88B00, the yellow darkened toward ink). In light theme they are Tag Ink, Yellow and Deep Yellow. Every pair that can touch, including where the ring and column wrap round, passes the dataviz validator for colour-blind (worst 9.9 dark, 20.6 light) and normal vision (worst 16.7 dark, 21.3 light).
- **Others Grey** (#6E6A60 dark, #C9C5B9 light): the "Others" group in the spending chart. It always sits between Deep Yellow and the red, which are too alike for some colour-blind readers to sit side by side.

A colour belongs to an option's place in the Purchase options list, not to how much was spent, so each option keeps its colour every month. The groups always run in that order: the three options, Others, then "Not answered yet" in red. On the light ground, pale colours (yellow, Others grey) get a 1dp outline.

### Named Rules
**The Two Reds Rule.** Red means only "a purchase still waiting for an answer" or "delete this purchase". It never marks errors, overspending or emphasis, and it never joins the category sequence.

**The Yellow Lid Rule.** Every screen's top bar is yellow with ink on it in both themes, whatever the ground below, so the status bar icons stay dark in both themes.

**The Fixed Colour Rule.** The spending chart uses exactly five solid colours, all from the app's palette: three for the options, grey for Others and red for unanswered, always in that order. Never add a new hue, a pattern or a sixth colour, and never give a colour to a group by rank. Change Green appears only while a row is swiped.

## Typography

**Display Font:** Golos Text, bundled as a variable font and licensed under the OFL (licence at `app/src/main/assets/licenses/OFL-GolosText.txt`)
**Body Font:** Golos Text
**Label/Mono Font:** Golos Text, with tabular figures for amounts in lists

**Character:** One grotesque doing every job, from a Black-weight shelf price down to plain 14sp notes. Hierarchy comes from weight jumps, not from a second family. The app is English only, and the face also carries the ₸ sign.

### Hierarchy
- **Shelf Price** (900, 92sp on Home and 72sp in the pop-up, -0.04em tracking on the whole number): the amount, and nothing else. The currency sign is 0.28 of the whole size and the cents 0.36, both top-aligned to the capitals.
- **Row Price** (800, 22/28sp whole; 700, 14/20sp sign and cents; tabular): the same tag on a purchase row.
- **Headline** (800, 24/32sp): the Add dialog's title.
- **Title Large** (800, 22/28sp): top bar titles, section headings ("Purchases", "Finish setting up"), "What was this for?", the empty state.
- **Title Medium** (700, 18/24sp): the month name and the pop-up's answer labels.
- **Title Small** (700, 16/24sp): the purchase count, "Charged" and small headings.
- **Row Title** (500, 16/24sp): the first line of purchase, setup and settings rows.
- **Body** (400, 16/24sp): the default text; empty state copy and category names.
- **Body Small** (400, 14/20sp): second lines of rows, explanatory notes, the rate note, unselected month labels. Material's bodyMedium has the same values.
- **Label** (700, 16/24sp on buttons; 700, 14/20sp on the selected month label). Material's labelMedium has the 14/20sp values.

Letter spacing is 0 everywhere except the shelf price's whole number. Nothing is set in capitals by style.

### Named Rules
**The One Family Rule.** Golos Text is the only typeface. New hierarchy comes from weight and size, never from a second face.

**The 14sp Floor Rule.** Nothing is smaller than 14sp, so the app can be read at a glance. All type is in sp and follows the system font size.

**The Tabular List Rule.** Amounts in lists use tabular figures so they line up down the column. The shelf price does not need them.

## Layout

A single scrolling column on a phone. No breakpoint logic ships. Everything hangs off one 24dp edge: the title, the month chevrons, the price, the bars, row text and rules all land on it. Small offsets exist to make that true, including the month arrows (20dp), the Home title (8dp) and the bar row's 4dp overhang.

Home runs in a fixed order: the yellow field, setup steps while any remain, the category breakdown, then Purchases or an empty state. The top bar stays pinned and content scrolls under it. Section tops are 24dp, and the Purchases heading has 32dp above and 12dp below. Rows are at least 72dp, with 12dp vertical padding and a 16dp gap between text and amount.

The Add button is centred at the bottom because amounts sit on the right of each row, and it floats over the list while scrolling. The list carries 96dp of bottom padding so the last row can clear it.

The app draws edge to edge, and the navigation bar area is painted in the ground colour so scrolling content does not show through behind the system buttons.

The pop-up is anchored to the bottom for the thumb. Tapping outside it means Later. While the keyboard is open the answer buttons hide, so the field and Save stay above it.

### Named Rules
**The 48dp Thumb Rule.** Every target is at least 48dp. Answers are 64dp, top bar icons and pop-up actions 56dp, rows 72dp. Month bars count their gaps as part of the target.

**The 24dp Edge Rule.** New content sits on the 24dp edge. Full-width fills, such as the yellow field and the red row, bleed to the screen edge while their content stays on it.

## Elevation & Depth

The system is flat. Top bars, the yellow field, rows and sheets have no authored shadows, and the top bar does not change colour when content scrolls under it. Depth comes from colour blocks and from the platform's dim behind the pop-up and dialog.

### Shadow Vocabulary
- **Add button** (Material 3's default extended FAB elevation, 6dp): the only resting shadow in the app, because the button floats over the scrolling list.

### Named Rules
**The Flat Shelf Rule.** Nothing casts a shadow except the one button that floats over content. A new surface separates by colour.

## Shapes

Corners are small and consistent, a cut label and not a soft pill. Buttons and fields share a 12dp corner (`TagCorners`), the Add button 14dp, and the pop-up's top corners and the dialog 20dp. Month bars have 2dp top corners, and category segments and swatches have 2dp all round, sharp enough to read as data and not as controls. The yellow field and every row are square and full width.

The launcher icon is the tag itself, ink on yellow: a large 9 with a raised, underlined 99, and no currency sign. The same foreground is the themed monochrome icon and the Android 12 splash icon on yellow. The notification icon is a white tag silhouette with a punched hole.

### Named Rules
**The Tag Corner Rule.** New buttons and fields use the shared 12dp corner. Material's default pill must be overridden on filled buttons.

## Components

### Price Tag
The signature. A shelf price that scales its sign, whole number and cents together, so a long amount still fits on one line.
- **Parts:** small raised currency sign, Black whole number, raised cents with a solid rule beneath them. The rule is 0.045 of the whole size and takes the content colour.
- **Spacing:** 4dp between the parts.
- **Fit:** measured and scaled down until it fits the width, at 92sp on Home and 72sp in the pop-up.
- **Tenge:** the sign moves after the amount and sits under the ruled cents.
- **Accessibility:** read as one description, not as three pieces.
- **Fallback:** an amount not converted yet is plain text in its own currency, Title Large in the pop-up and Title Medium on a row.
- **Known gap:** the fit has no floor. The sign drops below the 14sp floor once the whole number is under 50sp, which takes an amount of about nine digits on Home. Treat this as a defect to fix, not as permission to go smaller.

### Row Price
The same tag at list size: sign and cents at 14sp, whole number at 22sp, a fixed 2dp rule, 2dp gaps, tabular figures. It does not scale. Category rows do not use it, and show a plain formatted amount in Medium weight with tabular figures.

### Top Bar
Material's small top app bar in yellow with ink. Home has the title "Spending" and a Settings icon on the right; every sub-screen has a Back arrow and a title. Icon buttons are 56dp.

### Shelf Field
The yellow block at the top of Home: month arrows and name, the shelf price, purchase count, an optional rate note, then the month bars. The arrows stay put whatever the month's name, so they can be tapped repeatedly. The next arrow is disabled on the current month, at Material's disabled tone.

### Month Bars
Six recent months at one shared scale, so they can be compared at a glance.
- **Style:** ink bars up to 56dp tall, never under 4dp, with month labels below at 14sp.
- **State:** the selected month is solid with a bold label. The others are at 50% ink with a regular label.
- **Interaction:** tapping a bar opens that month.

**The Where-It-Went Motion Rule.** Motion only shows where things went. Pages move like Android's own: a deeper page slides in from the right over 380ms on Material's emphasised deceleration while the page under it shifts a quarter left and dims, Back reverses this over 300ms, and the calendar picker rises from below like a sheet. The back swipe moves the page with the finger and lets go early without leaving. Purchase rows glide into place when one is added or deleted. The month change tweens bar heights and tone over 220ms and crossfades the total over 180ms. A swiped row and the spending chart follow the finger: the row springs back on release, and the chart glides to its nearest resting place on a non-bouncing spring, with a small tick when release would land on a new shape. Everything else that moves is platform behaviour. Add no decorative animation.

The crossfade is keyed to the total and the bars to their values, so they also run if the numbers change while Home is open.

### Buttons
- **Filled:** ink with white in light theme, yellow with ink in dark, 12dp corners. Used for setup actions (at least 48dp) and the Purchase options Save (56dp, full width).
- **Add:** Material's extended FAB in yellow with ink in both themes, with a plus icon, 56dp tall, 14dp corners.
- **Answer:** the pop-up's three options, full width and 64dp tall, in stone with Title Medium text aligned left. Long labels end in an ellipsis.
- **Save typed answer:** yellow with ink, at least 56dp, beside the field. Disabled until there is text, at Material's disabled tones.
- **Text:** "Later" in the main text colour and "Not a purchase" in muted, both at least 56dp. Dialog actions use the primary colour.
- **Pressed and focus:** Material's default state layers. Nothing authored on top.

### Inputs / Fields
Material's outlined text field with 12dp corners and a 1dp outline. Focus is a thicker outline and label in the primary colour: ink in light, yellow in dark. There is no error style. Invalid input disables the action instead (Next in the dialog, Save in Purchase options).

### Purchase Row
- **Layout:** title and date on the left, Row Price on the right, at least 72dp, with a thin rule above inset to the 24dp edge. A converted purchase shows its original amount next to the date.
- **Answered:** ground colour, Medium title, muted second line.
- **Unanswered:** full-width red with "Choose a category", white text and a 40% white rule. Tapping opens the pop-up.
- **Swipe:** the row follows the finger at 80% and stops dead at 30% of its width. Reaching the stop gives a haptic tick and enlarges the label by 25%. Letting go there acts: right opens Change, left asks to delete. Letting go earlier does nothing. Either way the row springs back to rest. The panels grow with the pull: green with a pen and "Change" on the left, red with a trash icon and "Delete" on the right, with a 3dp gap in the ground colour to keep them distinct from a red row. Screen readers get both from the row's actions menu.
- **Hint:** "Swipe left to delete, right to change." sits under the Purchases heading in muted Body Small.

**The Rules-Not-Cards Rule.** Thin rules divide rows; content rows are never wrapped in cards. Purchases, setup steps and settings all use a rule on the 24dp edge. Category rows need none because the swatch leads each one, and pick lists need none because a checkbox or radio ends each one.

**Known gap:** the Bank alerts screen still wraps each alert in a 16dp rounded surface with 14dp padding, in the ground colour so it does not show, with no rules between entries. That screen and the Bank apps search field also sit on a 16dp gutter instead of the 24dp edge. Neither is part of the system.

### Category Breakdown
A train on a rail. As a line it is 16dp thick with 3dp gaps: today's three purchase options in list order, then "Others" for any other answer, including options since renamed, then "Not answered yet" in red, so the shares add up to the total above. Any share gets at least 2% of its length, so nothing listed goes undrawn. Below it are rows with a 12dp swatch, name, chevron and amount, each a 48dp target. Tapping an option or "Not answered yet" opens a page of its purchases, and tapping "Others" opens its own page under a yellow top bar: the month or dates, the Others total as a 56sp Price Tag, then one 72dp row per answer, largest first, with its purchase count and former options labelled "Old option", and a chevron. Tapping one opens a page of its purchases.

A group's page has the same yellow top bar titled with the group, the dates and its total as a 56sp Price Tag with its purchase count, then the same purchase rows and swipe actions as Home. Back returns to the page it came from. On the light ground, fills with luminance above 0.5 get a 1dp outline so they don't disappear.

Swiping right runs the line nose first onto a circular rail until it closes into a 20dp ring on the right, while the rows narrow and slide up beside it, with each amount dropping under its name. The ring's circumference is 90%, 110% or 140% of the line, from the Ring size setting. Names are limited to 16 characters. From the line, swiping left instead runs it, left end first, round a 24dp bend and down the left side into a 20dp column 75% as tall as the line is long, largest share at the bottom, while the rows move 36dp right and up beside it. The chart rests as a column, line or ring, swiping one step at a time. It follows the finger, with each change taking 40% of the width, and one swipe moves at most one step. On release it glides to the nearest resting place on a non-bouncing spring that carries the finger's speed, and a quick flick moves one step even if it was short.

**Known gap:** Muted on Dark, the fourth category in light theme, falls just under that threshold, so it gets no outline at 1.95:1 on the ground. Its name and amount sit beside it.

### Purchase Pop-up
A sheet from the bottom with 20dp top corners, over a platform dim. A yellow band on top holds the 72sp price and "Charged" with the original amount, then comes "What was this for?", three answers, a typed answer with Save, and "Not a purchase" and "Later". The notification asking the same question shows the amount as its title.

### Date Range
Tapping the month name opens a full-screen Material range calendar under a yellow top bar, with an X at the left and Show at the right. Today and earlier can be chosen; a single tapped day counts as a range of one. On Home, a chosen range replaces the month name and hides the arrows and month bars, with an X at the right of the field to return to the month.

### Change Purchase
A sub-screen with the amount in the purchase's own currency, a line showing the converted result at the same purchase day's rate, "What was this for?", the three options at 56dp (the chosen one in yellow), a typed answer, and a full-width Save. An empty answer leaves the purchase unanswered. The date, currency and saved rate never change.

### Settings Rows
Title, muted detail and a trailing chevron, at least 72dp, with rules between them. Pick lists use full-width rows ending in Material's checkbox or radio, in the primary colour. The rate credit is a Body Small underlined link in the primary colour.

### Dialog
Material's alert dialog with 20dp corners on the raised colour. It is used for Add a purchase and to confirm a delete. The delete confirmation reads "Do you really want to delete them?", offers Cancel, and sets Delete as a red filled button.

## Do's and Don'ts

### Do:
- **Do** keep the month's total the first thing on Home, in the yellow field, set with the Price Tag.
- **Do** set a converted purchase's amount with the Price Tag or Row Price, with the sign, raised cents and rule intact.
- **Do** use Material colour roles from the theme, so both themes keep working.
- **Do** check every new screen in both themes: yellow top bar in each, ink ground in dark and warm off-white in light.
- **Do** keep every target at 48dp or more and every label at 14sp or more, in sp.
- **Do** disable an action when input is invalid, as the dialog and Purchase options do.

### Don't:
- **Don't** use red for anything except a purchase still waiting for an answer or deleting a purchase. That rules out errors and Material's `isError` state.
- **Don't** delete a purchase from a swipe without the confirmation dialog.
- **Don't** use Change Green anywhere but the Change panel.
- **Don't** put the total in a rounded summary card, or add a blue accent. This is the category default the world was chosen against.
- **Don't** wrap content rows in cards; divide them with thin rules.
- **Don't** add a second typeface or a second accent hue.
- **Don't** set anything smaller than 14sp or make any target smaller than 48dp.
- **Don't** add motion that doesn't show where something went, or any bounce.
- **Don't** add shadows beyond the Add button's.

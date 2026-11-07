---
version: 1
slug: "app-src-main-java-app-spendtracker-mainactivity-kt"
primary_target: "app/src/main/java/app/spendtracker/MainActivity.kt"
related_targets: ["app/src/main/java/app/spendtracker/CategoryPrompt.kt"]
---

# Home, purchase pop-up and settings

Mode: Operate. Scope: every screen of the Android app, plus the launcher, notification and splash icons.

Audience and job: people checking their month, or answering one question with one thumb right after paying.
Constraints: Material 3 navigation and controls, system Back, 48dp targets, sp type, both themes.
Memorable moment: the month's total read from arm's length as a shelf price.
Unresolved: none.

## Direction contract

THESIS: The monthly total is a supermarket shelf price: a huge number you can read from arm's length. It refuses the category default of a dark screen with a rounded summary card and a blue accent.

OWN-WORLD: Price-label yellow #FFD400 runs edge to edge as the top field and every top bar, with ink #151412 on it. Below it, an ink ground in dark theme and warm off-white in light. Red #D90D25 means one thing only: a purchase still waiting for an answer. One heavy Russian grotesque, Golos Text, carries everything. Prices have a small raised currency sign and raised underlined cents. Thin rules divide rows, not cards.

STORY: You see what you spent this month before anything else, see how it compares with recent months and where it went, spot what still needs an answer, and fix it in one tap. After paying, the pop-up shows the same price tag and three large answers.

FIRST VIEWPORT: Pinned yellow top bar with "Spending" and Settings. Below it the yellow field holds month arrows and the month name, the huge price, the purchase count, and six month bars at one fixed scale, with the selected month solid. Below the yellow are the setup steps when any remain, then a split bar with category rows, then Purchases. A centred yellow Add button sits at the bottom in the thumb zone.

Signature move: the price tag itself, with raised cents, used on Home, the pop-up and each purchase row. Tapping a month bar animates the bars and crossfades the price. It is the one authored motion.

FORM: Shelf Tag, supermarket price labels, position 4 on the ordered list. seed:bfbf8ba0ef8b8334

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance

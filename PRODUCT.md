# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

People who pay by card or phone and want to know where their money goes, without typing in every purchase. The first user is the app's owner, on a Samsung S24, whose bank card charges in USD and who views spending in HKD. It is also meant for other people, so a stranger must understand it without help, including first-time setup.

Typical moment: standing at a till or walking out of a shop, phone in one hand, a few seconds of attention.

## Product Purpose

Spend Tracker logs purchases on its own by reading bank notifications, asks one quick question about what each was for, and shows the month's spending. Success is knowing your monthly total at a glance, with almost no effort per purchase.

## Positioning

A bank notification becomes a one-tap question right after you pay. There is no bank login and no manual entry, and spending data stays on the phone.

## Operating Context

- The pop-up can open on top of any other app the moment a purchase happens. It may only get a few seconds of attention, so it must work with one thumb.
- If the phone is locked, the user gets a notification instead and answers from there.
- The home screen is opened occasionally to check the month.
- Alerts come in different languages and number formats, and a purchase can involve more than one currency.

## Capabilities and Constraints

- Display and bank currencies: KZT, USD, HKD, SGD. Amounts in other currencies are read and converted from alerts.
- Each purchase saves its original amount and currency, plus the converted amount at that day's rate, which never changes afterwards.
- Three editable purchase options plus a free-text answer. "Later" and "Not a purchase" must always be available.
- Home shows a month, or any date range chosen from a calendar.
- Spending is split into the three options, "Others" for any other typed answer, and "Not answered yet". Purchases under a renamed option move to "Others". Tapping "Others" opens its own page listing what's inside it by name, with former options kept as their own groups, labelled "Old option", for good. Swiping the chart right turns the line into a ring, in a size chosen in Settings. Swiping it left turns the line into a column down the left side. The chart rests as a column, line or ring.
- Tapping any group under the chart, or any answer on the Others page, opens a page of its purchases for the same dates, with the same tap and swipe actions as Home.
- Option names and typed answers are limited to 16 characters.
- A full swipe left on a purchase asks whether to delete it. A full swipe right opens Change, for its amount and answer. A partial swipe springs back with nothing done. A changed amount keeps its purchase day's rate.
- Native Android, Jetpack Compose, Material 3. No account and no backend. The internet is used only to download exchange rates.
- The rate source requires the credit line "Rates By Exchange Rate API", linked to its site.
- The whole app is in English, including dates and months, whatever language the phone uses. Bank alerts can still be read in other languages.

## Brand Commitments

- Name: Spend Tracker.
- The owner's bar: it must not feel plain, and it must not feel childish.

## Evidence on Hand

Real alert format from Freedom Bank (purchase and cashback). No users, reviews or usage numbers exist, and none may be invented.

## Product Principles

- The monthly total is the first thing anyone sees.
- A purchase is answered in one tap, with one thumb, without reading anything twice.
- Say what happened to the money honestly: show the original amount next to any converted one.
- Never block the user: every question can be put off.
- Spending data stays on the phone.

## Accessibility & Inclusion

Large touch targets (48dp minimum), text that follows the system font size, and readable contrast in both light and dark themes. Standing use with one hand is the design case.

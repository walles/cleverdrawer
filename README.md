# CleverDrawer

App drawer that reads your mind.

It then uses that information to sort everything launchable with
the things you launch most often first.

# TODO Before Installing on Johan's Phone
* List installed apps
* Present installed apps with icon and name in a grid
* Launch app when clicked
* Make the grid scrollable
* Sort app list alphabetically by title

# TODO Before Getting Beta Testers
* Add Crashlytics crash reporting
* Add Timber logging
* Add a search button that filters the app list as you type
* Keep per-app stats on:
  * Number of launches
  * Time of first launch
  * Time of last launch
* Sort list by `(number of launches) * (age of first launch) / (age of last launch)`

# TODO Before First Public Release

# TODO Misc
* Add contacts to the list. Multiply contact scores by 0.5.
* Keep per-app stats on launches per weekday vs launches per sat / sun.
Incorporate these stats in the scoring algorithm.
* Add system settings to the list.
* Long clicking app should bring up a menu:
  * Show on Google Play
  * Uninstall
* Clicking a contact should bring up a menu:
  * Dial
  * SMS
  * E-mail
  * Open Contact

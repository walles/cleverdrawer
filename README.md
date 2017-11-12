# CleverDrawer

App drawer that reads your mind.

It then uses that information to sort everything launchable with
the things you launch most often first.

# TODO Before Getting Beta Testers
* Add [Crashlytics crash reporting](https://fabric.io/kits/android/crashlytics/install)
* Point Timber logs to Crashlytics
* Run unit tests in [Travis](https://travis-ci.org/)
* Run [Android Lint](http://tools.android.com/tips/lint-checks) in Travis
* Run [Findbugs](https://docs.gradle.org/current/userguide/findbugs_plugin.html) in Travis
* Run [ErrorProne](https://github.com/google/error-prone/blob/master/examples/gradle/build.gradle) in Travis
* Add a search button that filters the app list as you type
* Test grid with different font sizes and app name lengths
* Keep per-app stats on:
  * Number of launches
  * Time of first launch
  * Time of last launch
* Sort list by `(number of launches) * (age of first launch) / (age of last launch)`

# TODO Before First Public Release

# TODO Misc
* Add system settings to the list.
* Add contacts to the list. Multiply contact scores by 0.5.
* Keep per-app stats on launches per weekday vs launches per sat / sun.
Incorporate these stats in the scoring algorithm.
* Keep per-app stats on launches vs most recently used other app. Incorporate
these stats in the scoring algorithm.
* Long clicking app should bring up a menu:
  * Show on Google Play
  * Uninstall
* Clicking a contact should bring up a menu:
  * Dial
  * SMS
  * E-mail
  * Open Contact

# DONE
* Add Timber logging
* List installed apps
* List launchable activities in `MainActivityFragment.java`
* Show a grid. It can be empty or have some contents.
* Put some contents in the grid. Anything goes.
* Sort app list alphabetically by title
* List installed app names in the grid
* Add an icon above each app name
* Make the grid scrollable
* Launch app when clicked
* Remove app from running apps after launching
* Adjust number of columns

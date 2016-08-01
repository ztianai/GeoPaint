# Geo-Paint

### Objectives
By completing this assignment you will practice and master the following skills:

* Accessing location sensors (e.g., GPS) found on mobile devices
* Integrating Google Play Services and displaying interactive maps
* Handling run-time permission requests
* Loading and using packages and third-party libraries in Android
* Saving data to the file system and sharing it via menus
* Cryptographically signing applications


## User Stories
The user stories for the Geo-Paint app are:

* As a user, I want to be able to draw a picture mirroring my movements
* As a user, I want to see my drawing displayed on a map
* As a user, I want to draw in different colors
* As a user, I want to save my drawing and share it with others
* Extra Credit: As a user, I want to view drawings shared with me


#### Play Services and Keys
You will need to add the **Google Play Services** package to your application as well, so that you can use the included Location services. In Android Studio, go to `Tools > Android > SDK Manager`, and under the `SDK Tools` tab select `Google Play services` and `Google Repository` (see [here](http://developer.android.com/sdk/installing/adding-packages.html) for details).

You'll also need to modify your `build.gradle` file so that you can get access to the Location classes. In the ___module-level___ `build.gradle` file, under `dependencies` add
  ```
  compile 'com.google.android.gms:play-services-location:8.4.0'
  ```
  This will load in the location services (but not the other services, which require keys)
  
  - If you've built your app with with the Maps Activity template, you should already have all the services loaded in.

  - In past testing I've hit a couple of issues getting the [gradle plugin](https://developers.google.com/android/guides/google-services-plugin?hl=en) setup; basically making sure that you provide the correct keys and files to access play services. The above dependency _should_ be sufficient. If there are issues, you _might_ need to register your app for [cloud messaging access](https://developers.google.com/mobile/add) in order to get required `.json` configuration file (though it shouldn't be required; it may just be a bug in Android). If you have any troubles, please check in with us!

Lastly, you will **also** need to sign up for a [Google Maps API Key](https://developers.google.com/maps/documentation/android-api/start#step_4_get_a_google_maps_api_key), and include it in your application. Again, starting with a Maps Activity template will give you instructions for this as well.

- **DO NOT** hard-code this key directly into your `Manifest`! Instead it should be stored in a `values` resource.

- You'll also eventually need to add a second SHA-1 certificate fingerprint for the "release" version of your app. See _Generating a Release APK_ below for details.


## Overall User Interface
Your application's interface should center on a displayed map. This map will display the current drawing the user is creating.

This map should be _dynamic_, in that the user can use the built-in interface to move the camera around, etc. It should at a _minimum_ include [zoom controls](https://developers.google.com/maps/documentation/android-api/controls#zoom_controls) (because pinching doesn't work on the emulator), but can also include any other [UI settings](https://developers.google.com/maps/documentation/android-api/map#using_xml_attributes) you wish. 

While the user will draw on the map by walking around (and having their location automatically create drawings on the map), your app will also need to support a few other interactions, including raising and lowering the "pen" and selecting the color (see _Changing the Pen Color_ below). You should provide this functionality via an [options menu](http://developer.android.com/guide/topics/ui/menus.html#options-menu) item. Additionally, your menu will need to support a the ability to share the drawing; a clean interface for this is to use a [`SharedActionProvider`](http://developer.android.com/training/sharing/shareaction.html) (see _Saving and Sharing_ below for details).


### Getting the Location
Although Android includes a built-in framework for working with location, the framework provided by the Google Play services is much more robust and is the preferred system (though note that this will not be available on devices that don't support Google Play services!). See [Making Your App Location-Aware](http://developer.android.com/training/location/index.html) for details about fetching location ([this](http://blog.teamtreehouse.com/beginners-guide-location-android) is another decent tutorial).

Per the documentation, you will need to [build](http://developer.android.com/reference/com/google/android/gms/common/api/GoogleApiClient.Builder.html) a [`GoogleApiClient`](http://developer.android.com/reference/com/google/android/gms/common/api/GoogleApiClient.html) object and `connect()` it (handled via the appropriate callbacks). You can then use the [LocationServices.FusedLocationApi](https://developers.google.com/android/reference/com/google/android/gms/location/FusedLocationProviderApi) to [request location updates](http://developer.android.com/training/location/receive-location-updates.html).

- A `LocationRequest` at a 10 second interval (5 second fastest) with a "high accuracy" priority is considered appropriate for getting the real-time location we're interested. You can also slow down the interval if needed.

Note that you will need permission to access a user's location (specifically, `ACCESS_FINE_LOCATION` for GPS-precision location). This is considered a [**dangerous**](http://developer.android.com/guide/topics/security/permissions.html#normal-dangerous) permission, and thus requires special handling in Marshmallow (API 23) and higher. _In addition_ to including the permission request in the `Manifest`, you will also need to [request permission at run-time](http://developer.android.com/training/permissions/requesting.html) (that's a guide link you should definitely look at).

- The basic idea is that whenever you're going to get the location (e.g, with the `FusedLocationApi`), you will need to use `ContextCompat.checkSelfPermission()` to check if you have permission. If you do, then you can go about your business. But if not, you'll need to request permissions via `ActivityCompat.requestPermissions()`. This request will eventually issue a callback (`onRequestPermissionsResult()`) where you can "try again" to make your location request. If the user won't give you permission, then the user won't be drawing right now---so you can let the user know that the app won't function and `finish()` it, for example.

  - Refactoring your location requests into a separate method is great for avoid code repetition!

  - You are not required to show a "rationale" for why you need the permission for this assignment, though this would be a good place for a simple `AlertDialog` (in an appropriate `DialogFragment` of course)!

Testing location updates using your phone's built-in GPS can be a bit of a hassle (since you need to move around). However, by using the "extra options" in the toolbar of the latest version of the emulator, you can "send" the emulator a location as if it had picked that up from the GPS. Basically you can can act as a manual version of the GPS antenna to report location!

- There are also third-party apps that can be useful to testing location systems on a physical device. Previous students found [Mock Locations](https://play.google.com/store/apps/details?id=ru.gavrikov.mocklocations&hl=en) to be particularly useful.


### Drawing on a Map
To show a drawing on the map, you'll need to draw a [Shape](https://developers.google.com/maps/documentation/android-api/shapes) on it. Shapes are determined by a series of `LatLng` coordinates, which is exactly what you'll be getting from location updates!

In particular, you'll be interested in drawing [Polylines](https://developers.google.com/maps/documentation/android-api/shapes#polylines), which are multi-segment lines. The basic algorithm to use is that every time the user moves (so you get a new location update), add that spot to the multi-line, effectively creating a new short line to the current location from the previous one, thus tracing movements!

  - Because `Polylines` are drawn once defined, in order to "add" a point you'll need to <a href="https://developers.google.com/android/reference/com/google/android/gms/maps/model/Polyline.html#getPoints()">get</a> a list of all the points in the line, add your new point(s) to that, and then `set` the updated list as the Polyline's points.

The drawing doesn't need to be a single, continuous line: the user is able to either "raise" or "lower" the virtual pen on the map. If the pen is _up_, then no drawing should occur. If the pen is _down_, then you add more points to the `Polyline`.

- When the app starts, the pen should begin in the "up" position, so that the user needs to opt-in to drawing.
- When the user selects to put the pen down, your app can start drawing a _new_ `Polyline`, and will add new points as the user moves.
- If the user selects to raise the pen again, you should "end" the current `Polyline`. This isn't an explicit step, but it means that when the the user puts the pen down again, they'll be drawing a new line!
- If the user changes the color of the line (see below), you should end the current Polyline and immediately create another starting at the current location (but with the new color). That is, the pen should stay either "up" or "down" on a color change.


**Important:** Normally, map drawings and state (e.g., the location currently shown) will **reset** every time the Activity is re-created... and Activities get re-created a lot (like from rotating the phone). The _easiest_ workaround for this is to specify that the MapFragment should be <a href="http://developer.android.com/reference/android/app/Fragment.html#setRetainInstance(boolean)">retained</a> across activity re-creation. This will let you be able to save the current fragment's appearance, including the drawing, across configuration changes or some app switches.

- Note that this is only a work-around; if the Activity gets destroyed (either at user request or because of low memory), the drawing will still be lost if it hasn't been explicitly persisted. The _ideal_ solution would be to save each Polyline and its points in a structured database of some kind, but that's beyond the scope ofthis assignment. Alternatively, you can periodically save the current drawing to a file, which is functionality you'll need to include anyway (you'd of course want to do this in a _background thread_, such as with an `ASyncTask`). This can help if you find yourself losing your drawings a lot.


### Changing the Pen Color
In order to make the artwork more visually appealing, you should allow the user to specify the color of the pen. Android provides a [`Color`](http://developer.android.com/reference/android/graphics/Color.html) class similar to Java's that allows color specification in terms of RGB, HSV, hex codes, etc.

It would be nice for the user to be able to select a color from a complex interface, such as a color wheel or color picker. Setting up such a picker sounds like a lot of work... and is a common enough component that you'd expect someone else to have implemented it. Thus you should find a **third party library** that provides a color picker, and use that to let the user choose a color!

A good place to look for third-party Android libraries is [jCenter](https://bintray.com/bintray/jcenter). This is a repository (similar to `npm` in Node) that lists Java packages that can be easily loaded in through Gradle; _jCenter_ is the default repository for Android. Search this site (using the search bar at the top!) for an appropriate library to include (`android` and `color` are good keywords).

- Once you've found a library, you'll probably want to visit it's GitHub page for more details; _jCenter_ doesn't usually show module documentation. Generally you'll be able to install and use a library by including it as another `dependency` in Gradle, and then simply importing and using the classes like you would any other Android component.

- A Google search also found [Android Arsenal](https://android-arsenal.com/) as a potential source for libraries, if you can't find anything you like).

- Be sure and include a note about what library you used in your `SUBMISSION.md` file.

- This step will involve practicing learning to use new components on your own!

You might consider using a [SharedPreferences](http://developer.android.com/training/basics/data-storage/shared-preferences.html) file (with or without a Settings fragment) to save the chosen color(s) across application executions, so if I always want to draw my picture in Purple I can.

- You could allow the user to specify and save other drawing properties, like the pen thickness, as well if you wish.

### Go out and Art!
You should actually use your app to produce some art! This can be anything you want; I'm hoping for something on the complexity of a short word, a happy-face, or a [butterfly](http://www.gpsdrawing.com/gallery/land/nbutterlfy.htm). Scale or distance traveled doesn't matter. You should upload a screen-capture of your app displaying the drawing (in the `screenshots/` folder of the repo).

It's spring-time and we actually have some sunny days (you can use your Sun Spotter to confirm this!) I highly encourage you to actually go outside and draw something by running around (rather than just faking it with the emulator). It's fun, and the experience could be a nice inspiration for your final project as you thinking about how people use mobile devices while _mobile_.


## Saving & Sharing Drawings
Art is most meaningful when it can be shared with others, so you should include a way for the user to _save_ and _share_ the drawings they've created (so they don't lose all their hard creative work!). While a database is a good way to _save_ data, it makes it hard to share the picture. The `GoogleMap` class does include an <a href="https://developers.google.com/android/reference/com/google/android/gms/maps/GoogleMap.html#snapshot(com.google.android.gms.maps.GoogleMap.SnapshotReadyCallback)">`.snapshot()`</a> method for taking a screen capture, but for copyright reasons these images are not supposed to be used outside of the app (e.g., they can't be shared). It's also hard to add more lines to a screenshot later...

To get around this, your app should allow the user to save their drawing to a **file**. In order to make drawings as shareable as possible, you should save drawings using the [GeoJSON](http://geojson.org/) format, which is an extension of JSON data that represents geographic information (a basic example is [here](http://geojson.org/geojson-spec.html#examples)). The GeoJSON format can be read by Google Maps (the online version), as well as other mapping systems like [Mapbox](https://www.mapbox.com/) or [Leaflet](http://leafletjs.com/).

### Saving The Drawing
The first step is being able to save the current drawing. Google does provide utility methods for working with GeoJSON data in the [Maps Utility Library](https://developers.google.com/maps/documentation/android-api/utility/) ([docs](http://googlemaps.github.io/android-maps-utils/)). However, these utilities are for **reading** existing GeoJSON data; so in order to save and **write** data in that format, we've included a `GeoJsonConverter` class you can use to convert a `List` of `Polylines` into a GeoJSON String.

- This class's single method isn't complex at all---it's a basic String-building method like you probably wrote in CSE 142. But we're deep enough into the framework that you don't need to spend the time practicing with and debugging this Java code (unless you want to make your own version of this method for fun).

When the user chooses to **save** the drawing (such as by selecting an option from the menu), you will need to fetch all of the current `Polylines` on the map, and then convert them into a String representing a GeoJSON object. 

You will then need to write this String to a file saved [_privately_](http://developer.android.com/guide/topics/data/data-storage.html#AccessingExtFiles) on [External Storage](http://developer.android.com/guide/topics/data/data-storage.html#filesExternal). This will make the files world-readable so they can be shared later, but will also keep them "hidden" from the user as part of the app. (The data is specialized enough that we're not going to put it in the [public directories](http://developer.android.com/guide/topics/data/data-storage.html#SavingSharedFiles); this will also help keep junk files from a homework assignment off your device).

  - You will need to request permission to write to external storage on Android versions lower than 18 (since we support those, even if we don't target them).

  - Remember to check for media availability!

  - You can get access to a "private" external folder with the <a href="http://developer.android.com/reference/android/content/Context.html#getExternalFilesDir(java.lang.String)">`getExternalFilesDir()`</a> method. You can then create a new `FileOutputStream` object to write to:
  ```java
  File file = new File(this.getExternalFilesDir(null), "drawing.geojson");
  FileOutputStream outputStream = new FileOutputStream(file);
  outputStream.write(string.getBytes()); //write the string to the file
  outputStream.close(); //close the stream
  ```

**Important:** You should save your file with the `.geojson` extension. Technical it should be a `.json` file, but by being more specific you'll be prepared for your application to be able to _open_ these files later if you wanted.

  - You can test your saved content by pasting it into [this linter](http://geojsonlint.com/) or [this interactive drawer](http://geojson.io/#map=2/20.0/0.0). Note that styling information (color, etc) won't be shown, as they are not part of the core GeoJSON spec.


### Sharing the Drawing
You should enable the user to share your drawing (the file!) through an [Action Provider](http://developer.android.com/training/appbar/action-views.html#action-provider). This is a handy widget that produces a quick "Share" menu button, that will allow some data to be shared with any app that supports it. See the [reference](http://developer.android.com/reference/android/support/v7/widget/ShareActionProvider.html) for more details about setting this up (it's much clearer than the training doc). Be careful not to mix up the support and non-support versions!

- You will need to craft an `Intent` to share the drawing through. This should use `ACTION_SEND`, a type of `text/plain`, and should include the file `Uri` as an `EXTRA` (specifically, an `EXTRA_STREAM`).


### Loading a Drawing (extra credit!)
As extra credit, add the ability for your application to "open" `.geojson` files and show them in the drawing. There are a few parts to this, all of them complex:

- You'll need to add an `<intent-filter>` so that your application can open downloaded `.geojson` files (see [here](http://richardleggett.co.uk/blog/2013/01/26/registering_for_file_types_in_android/) for one unverified example).

- You'll also need to load that GeoJSON String into a [`GeoJSONLayout`](http://googlemaps.github.io/android-maps-utils/javadoc/) using the [Map Utility Library](http://googlemaps.github.io/android-maps-utils/).

- You may also want to add interfaces that allow the user to select _which_ `.geojson` file to open. You may be able to find a File Picker library to use (similar to the Color Picker).


## Generating a Release APK
When you signed up for a Google Maps API key, that key was tied to the _computer you are developing on_. While this is fine for building and testing your app through Android Studio, the API key won't be valid if your app is built and installed on another computer (e.g., your instructors' when they try to grade your work). So in order to make sure we can actually run your program, you'll need to tun in both the source code AND a already-built **`.apk`** file that we can install and run.

By default, Android Studio builds and [cryptographically signs](http://developer.android.com/tools/publishing/app-signing.html) apps in ___debug mode___. This allows you to build and sign your app without having to enter a password every time. But if you want to distribute an app to other people, you need to build and sign it in ___release mode___. This involves generating a digital [certificate](https://en.wikipedia.org/wiki/Digital_signature) (think: generating a password) and using it to sign your built app.

Luckily, Android Studio makes generating and using your own certificate [super easy](http://developer.android.com/tools/publishing/app-signing.html#studio) (follow that link!) You'll need to:

1. Select `Build > Generate Signed APK...` from the menu bar
2. Fill in the forms
3. Click `Finish` on the last one to generate your release APK.

There is one minor complication: you'll need to make sure that your Android Maps API key is available in release mode. To do this, you'll need to fill in the API key in the `google_maps_api.xml` resource inside the `release/res/values/` folder (in addition to the default `debug/res/values/` folder). You can find this folder with the file browser or viewing your project in `Project Files` view (at the top of the left-hand file-navigation pane).

- You will need to add your app's **certificate fingerprint** (signature) to your Maps API key (found in the [Google Developer Console](https://console.developers.google.com/apis/credentials) where you originally registered). You can view your release certificate fingerprint once you've generated a keystore by following [these instructions](https://developers.google.com/maps/documentation/android-api/signup#release-cert).

Once you've generated your signed APK, you should be able to find it in the `app` folder of your project (it's called `app-release.apk` by default). _Make sure that you push this file to your github repo!_

- If you want to test things, you can copy this file to your phone (e.g., email it to yourself) and open it to install your program. You app should work fine (with the Google Maps loading!)


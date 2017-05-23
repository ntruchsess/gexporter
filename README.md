# gexporter
Android App to export GPX and FIT to garmin devices

## HOWTO
* Android Studio -> Settings -> System Settings -> Android SDK -> "SDK Tools" Tab -> Check "Support Repository/Constraint Layout"
* compile and install the app on your device where the Garmin Connect app is also running
* go to settings of the app and give sdcard permission for file reading
* start the app
* point your browser on your mobile device to http://127.0.0.1:22222/dir.json to check what is exported
* start the connect IQ app on your garmin device https://github.com/gimportexportdevs/gimporter

## TODO
* rename to org.gexporter
* request sdcard android permission
* create proper GUI
* display what is exported
* make the server a background service
* configure the list of directories to scan
* configure port
* convert GPX to FIT (somehow GPX import does not work for most devices (yet?))
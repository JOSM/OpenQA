README
======
The author of OpenQA is Taylor Smock <taylor.smock@kaartgroup.com>.

OpenQA is a plugin designed to take various QA systems for OpenStreetMap and integrate them with JOSM.
OpenQA currently only supports KeepRight and Osmose.

OpenQA is licensed under the GPL v2 or later, due to copying CachedFile from JOSM.


# Building
## Gradle
`./gradlew clean build`
## Ant
For `ant`, you *must* have cloned this repository into the JOSM plugin directory.

See [https://josm.openstreetmap.de/wiki/Source%20code#Getthesource](https://josm.openstreetmap.de/wiki/Source%20code#Getthesource).
Specifically, you want the url that indicates "you're also interested in plugins".

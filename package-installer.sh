#!/usr/bin/env sh
# Builds a native installer for the OS this script runs on (deb/rpm on Linux, dmg on macOS).
# Needs JDK 26 with jpackage and Maven. For Windows use package-installer.bat.
set -e
cd "$(dirname "$0")"
mvn -B clean package
jpackage --name CleanCheck --app-version 1.0 \
  --input target --main-jar cleancheck.jar \
  --main-class com.mycompany.cleancheck.Launcher \
  --dest target/installer
echo "Installer written to target/installer"

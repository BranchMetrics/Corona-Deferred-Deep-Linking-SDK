#!/bin/sh
cp ios/Build/Products/Debug-iphoneos/libplugin_branch.a plugin/plugins/2015.2544/iphone
cp ios/Build/Products/Debug-iphonesimulator/libplugin_branch.a plugin/plugins/2015.2544/iphone-sim
cp android/bin/plugin.branch.jar plugin/plugins/2015.2544/android
cp android/libs/branch*.jar plugin/plugins/2015.2544/android
cp Corona/main.lua plugin/samples

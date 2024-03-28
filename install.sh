#!/bin/bash

# Download the latest release
wget https://download2.gluonhq.com/openjfx/17.0.10/openjfx-17.0.10_linux-x64_bin-sdk.zip

# Unzip the downloaded file
unzip openjfx-17.0.10_linux-x64_bin-sdk.zip

# Set up the ANDROID_HOME environment variable
echo 'export ANDROID_HOME=~/Android/Sdk' >> ~/.bashrc

# Set up the ANDROID_EMULATOR_HOME environment variable
echo 'export ANDROID_EMULATOR_HOME=$ANDROID_HOME/emulator' >> ~/.bashrc

# Set up the ANDROID_AVD_HOME environment variable
echo 'export ANDROID_AVD_HOME=$ANDROID_HOME/.android/avd' >> ~/.bashrc

# Add the Android SDK tools to the PATH
echo 'export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin:$ANDROID_HOME/platform-tools' >> ~/.bashrc

# Inform the user to source the ~/.bashrc file manually
source ~/.bashrc

echo "You can close this terminal and open a new one to start using the Android SDK tools."
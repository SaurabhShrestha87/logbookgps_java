#!/bin/bash

# Step 1: Download the latest release
wget https://download2.gluonhq.com/openjfx/17.0.10/openjfx-17.0.10_linux-x64_bin-sdk.zip

# Step 2: Unzip the downloaded file
unzip openjfx-17.0.10_linux-x64_bin-sdk.zip

# Step 3: Add the following lines to the end of your ~/.bashrc file
echo 'export ANDROID_HOME=~/Android/sdk' >> ~/.bashrc
echo 'export PATH=$PATH:/home/hp-pc/Android/Sdk/emulator' >> ~/.bashrc
echo 'export PATH=$ANDROID_HOME/platform-tools/:$PATH' >> ~/.bashrc
echo 'export PATH=$ANDROID_HOME/cmdline-tools/latest/bin/:$PATH' >> ~/.bashrc

# Step 4: Run the following command to apply the changes
source ~/.bashrc
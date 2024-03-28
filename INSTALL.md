1. download the latest release from the [javafx 17.0.10](https://download2.gluonhq.com/openjfx/17.0.10/openjfx-17.0.10_linux-x64_bin-sdk.zip)
2. unzip the downloaded file at download location
3. add following lines to end of your ~/.bashrc file
```
export ANDROID_HOME=~/Android/sdk
export PATH=$PATH:/home/hp-pc/Android/Sdk/emulator
export PATH=$ANDROID_HOME/platform-tools/:$PATH
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin/:$PATH
```
4. run the following command to apply the changes
```source ~/.bashrc```


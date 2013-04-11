RoboBoy - a personal wiki for Android with Git synchronization
==============================================================

Always take your brain extension with you!

RoboBoy is a sister project of [Vimboy](https://github.com/blinry/vimboy),
and serves exactly the same purpose: Managing a personal wiki of interlinked
text files. It can synchronize with any Git server via Public Key Authentication.

Installation
------------

Have a recent Android SDK, and `ant`. Clone this repo, connect your device, issue

    ant debug install

SSH key Setup
-------------

This is still very hacky and probably won't work for everyone.

On your computer, generate a new SSH key pair with

    ssh-keygen -f phone -N ""

Passphrases are not supported yet. Now, copy the key pair to your android device with

    adb push phone /sdcard/.ssh/phone
    adb push phone.pub /sdcard/.ssh/phone.pub

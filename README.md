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

License
-------

RoboBoy is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

RoboBoy is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

See LICENSE for a copy of the GNU General Public License.

    Copyright (C) 2013  Sebastian Morr <sebastian@morr.cc>

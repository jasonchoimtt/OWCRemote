Android Bluetooth remote for ENGG1100B
======================================

This is an Android remote implementation for the omni-wheel car project of
ENGG1100B Introduction to Engineering Design, CUHK (Spring Semester 2015).

This app is tested only on Lollipop. It may not work for KitKat and will likely
not work for pre-KitKat devices due to the Bluetooth API changes in KitKat.
Blame El Goog or patch it yourself.

Protocol
--------

A superset of the standard protocol is used. This remote should be compatible
with the remote board. I give no guarantee though.

```python

##############################
### Remote-to-car messages ###
##############################
x:128;y:128; # Reports the position of the joystick

io7:1;       # Reports the state of button 7 (1 is up, 0 is donw)

##############################
### Car-to-remote messages ###
##############################
foo:bar;    # Reports the value of the variable foo as bar
            # The variables will be shown on the right-pane of the remote log
lorem ipsum # Other arbitrary messages will be shown on the left-pane
```

Architecture
------------

```
hk.multitude.owcremote
      User interface:
  -   ConnectActivity: activity for choosing a Bluetooth device
  -   ControlActivity: activity housing the control interface and log displays

      Connection:
  -   BTConnectionThread: thread managing the Bluetooth connection
  -   ControlState: data structure for storing the control state (for threading
      reasons)
  -   DeviceConnection: interface for abstracting the Bluetooth connection for
      the contoller thread

      Remote logic:
  -   ControllerThread: thread responsible for actually crafting the messages

hk.multitude.owcremote.widgets
  -   ButtonPad: a 3x3 button pad with slide-in/out and multitouch support
  -   JoystickPad: a nice-looking joystick widget
```

License
-------

The MIT License (MIT)

Copyright (c) 2015 Jason Choi

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

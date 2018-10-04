#!/bin/bash

python botinfoupdate.py 1 192.168.1.19
python testmainupdate.py 1 192.168.1.19
cp TestMain.java ../src/main/java/testmain/
cp BotInfoSelector.java ../src/main/java/testmain/
echo "192.168.1.19"
./simpleclient 192.168.1.19echo "done"
#add lines for functions for and waiting for messagespython botinfoupdate.py 2 192.168.1.18
python testmainupdate.py 2 192.168.1.18
cp TestMain.java ../src/main/java/testmain/
cp BotInfoSelector.java ../src/main/java/testmain/
echo "192.168.1.18"
./simpleclient 192.168.1.18echo "done"
#add lines for functions for and waiting for messages
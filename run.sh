#!/bin/sh
HOME_DIR=`echo "$HOME"`
java -cp target/newLib-0.1-BasicFunctionality.jar:"$HOME_DIR"/.m2/repository/log4j/log4j/1.2.17/log4j-1.2.17.jar:lib/* testmain.TestMain	//testmain.follow.main

#roslauch rosbridge_server rosbridge_websocket.launch
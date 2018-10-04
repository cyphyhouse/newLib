#!/bin/bash
import sys

def updatetestmain(testmainfile,botnum,indents):
    lineno = 0
    f = open(testmainfile,"r")
    s = ""
    first = True
    for line in f.readlines():
        if "selectedRobot" in line and first:
            s += indents*" "+"private static int selectedRobot = "+ str(botnum)+";\n"
            first = False 
        else:
            s+= line 
    f.close() 
    f = open(testmainfile,"w")
    f.write(s)
    f.close() 

updatetestmain("TestMain.java",int(sys.argv[1]),8)

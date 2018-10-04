#!/bin/bash
import sys

def updatebotinfo(botinfofile,botnum,botip,indents):
    lineno = 0
    f = open(botinfofile,"r")
    s = ""
    first = True
    second = False
    third = False
    skip = False
    ipline = indents*" "+ 'ip = "'+botip + '";\n';
    for line in f.readlines():
        if skip:
           s+= ipline
           skip = False 
           continue
        if "NEXUS7" in line and first:
           s += line
           first = False
           second = True
           skip = False

        elif "NEXUS7" in line and second and botnum == 1:
           s += line
           second = False
           third = True
           skip = True
        elif "NEXUS7" in line and third and botnum == 2:
           s += line
           third = False
           skip = True

        else:
            s+= line 
    f.close() 
    f = open(botinfofile,"w")
    f.write(s)
    f.close() 

updatebotinfo("BotInfoSelector.java",int(sys.argv[1]),sys.argv[2],16)

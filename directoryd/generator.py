#!/bin/bash
import sys 
MAIN = 0
QUADCOPTER = 1 
def getips(ipfile):
    f = open(ipfile,"r")
    l = f.readlines()
    botdict = {}
    for line in l:
        p = line.strip().split(" ")
        botdict[p[1]] = p[2]
    return botdict

#print botdict

def generatescript(key,ip):
    s = ""
    s+= "python botinfoupdate.py " + str(key) + " " + str(ip) +"\n"
    s+= "python testmainupdate.py " + str(key) + " " + str(ip) +"\n"
    s+= "cp TestMain.java ../src/main/java/testmain/\n"
    s+= "cp BotInfoSelector.java ../src/main/java/testmain/\n"
    #s+= "mvn compile\n"
    #s+= "mvn clean install\n"
    #s+= "git checkout quadcopter\n"
    #s+= "git add -A\n"
    #s+= 'git commit -m "update"\n'
    #s+= "git push\n"
    s+= "echo "+'"'+str(ip)+'"\n'
    s+= "./bin/simple_client "+str(ip)+"\n"
    s+= "echo "+'"done"\n'
    s+= "#add lines for functions for and waiting for messages" 
    return s 
    
    
launchscript = "launch.sh"
s = "#!/bin/bash\n\n"
ipdict = getips("addresses.txt") 
for key in ipdict.keys():
    s+= generatescript(key,ipdict[key]) 
f = open(launchscript,"w") 
f.write(s)
f.close()
#print botdict.keys()
#updatebotinfo("botinfofile",1,botdict['1'],4)

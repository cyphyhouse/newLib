import socket

UDP_IP = "127.0.0.1"
#UDP_IP = "192.168.1.103"
UDP_PORT = 4000
MESSAGE = "%|bot0|-1679|-513|0|0|0|0|192.168.1.10|&\n"
#MESSAGE = "%|bot0|0|-513|0|0|0|0|192.168.1.10|&\n"

print "UDP target IP:", UDP_IP
print "UDP target port:", UDP_PORT
print "message:", MESSAGE

sock = socket.socket(socket.AF_INET, # Internet
                     socket.SOCK_DGRAM) # UDP
while(True):
    sock.sendto(MESSAGE, (UDP_IP, UDP_PORT))

import socket
import sys
from time import sleep

x = 4
y = 4
while True:
	# Create a TCP/IP socket
	sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
	# Connect the socket to the port where the server is listening
	server_address = ('localhost', 8888)

	try:
		sock.connect(server_address)
		message = str(x) + " " + str(y)
		sock.sendall(message)
		x -= 0.1
		y -= 0.1
		if x < 0:
			x = 4
			y = 4
	except:
		pass

	 
	

	sleep(0.1)
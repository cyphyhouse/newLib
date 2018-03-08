import socket

s = socket.socket()
s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)


port = 12345
s.bind(('',port))
print("socket binded to port",port)
s.listen(5)
print("socket is listening")

x = 3.1
y = 3.1

while True:
    c, addr = s.accept()
    print("Got connection from",addr)


    message = (str(x) + " " + str(y))
    c.send(message.encode())
    x -= 0.1
    y -= 0.1
    if x < -3:
        x = 3.1
        y = 3.1
    c.close()

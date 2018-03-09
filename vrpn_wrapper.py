#!/usr/bin/env python
# license removed for brevity
import rospy
import socket
from geometry_msgs.msg import Point, PoseStamped
from multiprocessing import Value, Array, Process, Lock



def callback(data, args):

    pos_array = args
    pos_array[0] = data.pose.position.x
    pos_array[1] = data.pose.position.y
     
    
    
def listener(pos_array):
    rospy.init_node('listener', anonymous=True)
    rospy.Subscriber("/vrpn_client_node/cyphyhousecopter/pose", PoseStamped, callback, (pos_array))

    rospy.spin()
    
    
def send_to_UI(pos_array):
    
    
    s = socket.socket()
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    port = 12345
    s.bind(('',port))
    print("socket binded to port",port)
    s.listen(5)
    print("socket is listening")
    
    x = pos_array[0]
    y = pos_array[1]
    
    while True:
        c, addr = s.accept()
        print("Got connection from",addr)
        message = (str(x) + " " + str(y))
        print(message)
        c.send(message.encode())
        x = pos_array[0]
        y = pos_array[1]
        c.close()
    
    


def main():
    
    pos_array = Array('d',[0.0,0.0])
    #lock = Lock()
    listener_process = Process(target=listener, args=(pos_array,))
    send_to_UI_process = Process(target=send_to_UI, args=(pos_array,))
    
    listener_process.start()
    send_to_UI_process.start()
    
    listener_process.join()
    send_to_UI_process.join()
    
    
main()




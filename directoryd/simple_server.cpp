#include <iostream>
#include <string>
#include <fstream>
#include <zmq.hpp>
#include <unistd.h>
#include <stdio.h>

int main()
{
    zmq::context_t context (1);
    zmq::socket_t socket (context, ZMQ_REP);
    socket.bind ("tcp://*:5555");

    zmq::message_t request;

    socket.recv (&request);
    std::cout << "Received stop message" << std::endl;

    sleep(1);
    FILE * output = popen("cd .. && git pull", "w");
    pclose(output);

    zmq::message_t reply (3);
    memcpy (reply.data (), "ACK", 3);
    socket.send (reply);
    return 0;
}

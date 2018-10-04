#include <iostream>
#include <string>
#include <fstream>
#include <zmq.hpp>

int main(int argc, char * argv[])
{
    std::string address = argv[1];

    zmq::context_t context (1);
    zmq::socket_t socket (context, ZMQ_REQ);
    socket.connect("tcp://" + address + ":5555");

    zmq::message_t message(4);
    memcpy(message.data(), "stop", 4);
    std::cout << "Sending stop message" << std::endl;
    socket.send(message);
    zmq::message_t reply;
    socket.recv(&reply);
    std::cout << "Received acknowledgement" << std::endl;
    return 0;
}

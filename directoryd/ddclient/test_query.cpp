#include <iostream>
#include <string>
#include <fstream>
#include <zmq.hpp>
#include "query.hpp"

int main()
{
    zmq::context_t context (1);
    zmq::socket_t publisher (context, ZMQ_PUB);
    publisher.bind("epgm://wlp3s0;239.192.1.1:5555");

    std::ifstream vehicles("vehicles.txt");
    std::ofstream addresses("addresses.txt");

    if (vehicles.fail())
    {
        std::cout << "vehicles.txt not found\n";
        return -1;
    }

    while (!vehicles.eof())
    {
        std::string vehicle_type;
        int vehicle_number;

        vehicles >> vehicle_type >> vehicle_number;

        while (vehicle_number)
        {
            auto result = DDClient::find({}, vehicle_type + std::to_string(vehicle_number));
            if (!result.empty())
            {
                zmq::message_t message (8);
                addresses << vehicle_type << " " << vehicle_number << " " << result.back().address << std::endl;
                snprintf ((char *) message.data(), 8, "stop %d", vehicle_number);
                publisher.send(message);
                --vehicle_number;
            }
            sleep(1);
        }
    }

    vehicles.close();
    addresses.close();

    return 0;
}

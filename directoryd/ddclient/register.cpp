#include <vector>
#include <iostream>
#include <czmq.h>
#include <string>
#include <algorithm>
#include "../protobuf/Services.pb.h"
#include <google/protobuf/text_format.h>
using namespace google::protobuf;
bool debug = true;

#include "register.hpp"

namespace DDClient {

    void unregister(std::string const &name);

    class RegistrationManager {
	std::vector<std::string> registered_names;
	int registered_count;

	RegistrationManager() {}

    public:

	void unregister_all() {
	    for (auto &name : registered_names) {
		unregister(name);
	    }
	    registered_names.clear();
	}

	void add(std::string const &name) {
	    registered_names.push_back(name);
	    registered_count++;
	}

	void remove(std::string const &name) {
	    auto result = find(registered_names.begin(),
			       registered_names.end(),
			       name);
	    if (result != registered_names.end()) {
		registered_names.erase(result);
		registered_count--;
	    }
	}

	~RegistrationManager() {
	    try {
		unregister_all();
	    }
	    catch (std::string &error) {
		;
	    }
	}

	static RegistrationManager &instance() {
	    static RegistrationManager instance;
	    return instance;
	}
    };

    void register_service(std::string const &name,
			  int port,
			  std::map<std::string, std::string> const &txt)
    {
	directoryd::ServiceRequest request;
	request.set_type(directoryd::REGISTER);
	auto *r = request.mutable_register_();
	auto l = r->add_location();
	l->set_port(port);
	l->set_type("starl");
	r->set_name(name);
	for (auto &t : txt) {
	    auto txtfield = r->add_txt();
	    txtfield->set_key(t.first);
	    txtfield->set_value(t.second);
	}
	zframe_t *sf = zframe_new(NULL, request.ByteSize());
	assert (sf != NULL);
	request.SerializeToArray(zframe_data(sf),zframe_size(sf));

	string buffer;
	if (debug && TextFormat::PrintToString(request, &buffer)) {
	    fprintf(stderr, "request: %s\n", buffer.c_str());
	}

	int retval = zframe_send(&sf, DDClient::instance().register_socket(), 0);
	assert(retval == 0);

	zframe_t *rf = zframe_recv (DDClient::instance().register_socket());
	directoryd::ServiceReply reply;
	reply.ParseFromArray(zframe_data(rf),zframe_size(rf));

	if (debug && TextFormat::PrintToString(reply, &buffer)) {
	    fprintf(stderr, "reply: %s\n", buffer.c_str());
	}

	zframe_destroy(&rf);

	if (reply.type() != directoryd::REGISTER) {
	    throw RegistrationError("Got back incorrect message type when trying to register.");
	}
	if (reply.success() != true) {
	    throw RegistrationError(reply.result());
	}

	RegistrationManager::instance().add(name);
    }

    void unregister(std::string const &name) {
	directoryd::ServiceRequest request;
	request.set_type(directoryd::UNREGISTER);
	auto *r = request.mutable_unregister();
	r->set_name(name);

	zframe_t *sf = zframe_new(NULL, request.ByteSize());
	assert (sf != NULL);
	request.SerializeToArray(zframe_data(sf),zframe_size(sf));
	int retval = zframe_send(&sf,
				 DDClient::instance().register_socket(), 0);
	assert(retval == 0);

	zframe_t *rf = zframe_recv (DDClient::instance().register_socket());
	directoryd::ServiceReply reply;
	reply.ParseFromArray(zframe_data(rf),zframe_size(rf));
	zframe_destroy(&rf);

	if (reply.type() != directoryd::UNREGISTER) {
	    throw RegistrationError("Got back incorrect message type when trying to unregister.");
	}
	if (reply.success() != true) {
	    throw RegistrationError(reply.result());
	}
    }

    void unregister_service(std::string const &name) {
	unregister(name);
	RegistrationManager::instance().remove(name);
    }

    void heartbeat() {
	directoryd::ServiceRequest request;
	request.set_type(directoryd::HEARTBEAT);
	zframe_t *sf = zframe_new(NULL, request.ByteSize());
	assert (sf != NULL);
	request.SerializeToArray(zframe_data(sf),zframe_size(sf));
	int retval = zframe_send(&sf,
				 DDClient::instance().register_socket(), 0);
	assert(retval == 0);

	zframe_t *rf = zframe_recv (DDClient::instance().register_socket());
	directoryd::ServiceReply reply;
	reply.ParseFromArray(zframe_data(rf),zframe_size(rf));
	zframe_destroy(&rf);

	if (reply.type() != directoryd::HEARTBEAT) {
	    throw RegistrationError("Got back incorrect message type when trying to send heartbeat.");
	}
	if (reply.success() != true) {
	    throw RegistrationError(reply.result());
	}
    }
}


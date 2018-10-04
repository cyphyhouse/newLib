#include <unistd.h>
#include <zmq.h>

#include "../protobuf/Services.pb.h"
#include "ddclient.hpp"

namespace DDClient {
    DDClient::DDClient () {

	GOOGLE_PROTOBUF_VERIFY_VERSION;

	context_ = zmq_ctx_new();
	query_socket_ = zmq_socket(context_, ZMQ_DEALER);
	register_socket_  = zmq_socket(context_, ZMQ_DEALER);

	zsock_set_identity (query_socket_, "DDclient::query");
	zsock_set_identity (register_socket_, "DDclient::register");

	assert(zmq_connect (query_socket_,"ipc:///tmp/directoryd") == 0);
	assert(zmq_connect (register_socket_,"ipc:///tmp/directoryd") == 0);
    }

    DDClient::~DDClient () {
	google::protobuf::ShutdownProtobufLibrary();
    }

    DDClient &DDClient::instance() {
	static DDClient instance;
	return instance;
    }
};

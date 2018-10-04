#include <iostream>
#include <vector>
#include <string>
#include <unordered_map>
#include <utility>

#include <zmq.h>

#include <google/protobuf/text_format.h>
using namespace google::protobuf;

#include "../protobuf/Services.pb.h"
#include "ddclient.hpp"
#include "query.hpp"

static bool debug = true;

namespace DDClient {
    std::vector<Service> find(std::map<std::string, std::string> const &txt,
			      std::string const &name)
    {
	directoryd::ServiceRequest request;
	request.set_type(directoryd::FIND);
	directoryd::ServiceRequest::Find *f = request.mutable_find();
	f->set_type("starl");
	if (name.empty() == false) {
	    f->set_name(name);
	}
	for (auto &t : txt) {
	    directoryd::TxtField *txtfield = f->add_txt();
	    txtfield->set_key(t.first);
	    txtfield->set_value(t.second);
	}

	string buffer;
	if (debug && TextFormat::PrintToString(request, &buffer)) {
	    fprintf(stderr, "request: %s\n", buffer.c_str());
	}

	zframe_t *sf = zframe_new(NULL, request.ByteSize());
	assert (sf != NULL);
	request.SerializeToArray(zframe_data(sf),zframe_size(sf));

	int retval = zframe_send(&sf, DDClient::instance().query_socket(), 0);
	assert(retval == 0);

	zmsg_t *repmsg = zmsg_recv(DDClient::instance().query_socket());
	if (debug)
	    zmsg_fprint(repmsg, stderr);

	zframe_t *rf = zmsg_pop(repmsg);
	directoryd::ServiceReply reply;
	reply.ParseFromArray(zframe_data(rf),zframe_size(rf));

	if (debug && TextFormat::PrintToString(reply, &buffer)) {
	    fprintf(stderr, "reply: %s\n", buffer.c_str());
	}

	zframe_destroy(&rf);
	zmsg_destroy(&repmsg);

	std::vector<Service> services;
	if (reply.type() != directoryd::FIND) {
	    throw QueryError("Got back incorrect message type when trying to query.");
	}
	if (reply.success() != true) {
	    throw QueryError(reply.result());
	}
	for (int i = 0; i < reply.findresult_size(); ++i) {
	    Service s;
	    auto location = reply.findresult(i).location();
	    s.address = location.address();
	    s.port = location.port();
	    for (int j = 0; j < reply.findresult(i).txt_size(); ++j) {
		auto t = reply.findresult(i).txt(j);
		s.txt[t.key()] = t.value();
	    }
	    services.push_back(s);
	}

	return services;
    }
}

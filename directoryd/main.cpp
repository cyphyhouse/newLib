#include <iostream>
#include <vector>
#include <string>
#include <unordered_map>
#include <utility>

#include <unistd.h>
#include <sys/stat.h>

#include <czmq.h>

#include "Avahi.hpp"
#include "Browser.hpp"
#include "Registrar.hpp"

#include "protobuf/Services.pb.h"

#include <google/protobuf/text_format.h>
using namespace google::protobuf;

using namespace std;

bool debug = false;


struct ActiveServices {

    unique_ptr<AvahiTimeout, Avahi::timeoutdeleter> timeout;

    vector<Avahi::ServiceGroup *> service_groups;

    pair<string, unordered_map<string, ActiveServices*>*> *service_pair;

    ActiveServices(int time,
		   pair<string, unordered_map<string, ActiveServices*>*> *pp,
		   Avahi::ServiceGroup *s)
    {
	/* Add the service to the service group */
	service_groups.push_back(s);

	/* Store the service (location, services) pair,
	   for use by the timeout deleter
	*/
	service_pair = pp;

	/* Set the timeout.
	 *  If we don't get data from the service by the time it expires,
	 * remove it
	 */
	auto t = Avahi::set_timeout(time, [](AvahiTimeout *timeout, void *data) {
		(void)timeout;
		auto p = static_cast<pair<string, unordered_map<string, ActiveServices*>*>*>(data);
		auto as = (*p->second)[p->first];
		p->second->erase(p->first);
		delete as;
		return;
	    }, pp);

	/* Set the timeout pointer */
	swap(timeout, t);
    }

    /* Whether or not the service expired,
       we need to delete the saved service pair */
    ~ActiveServices() {
	delete service_pair;
	for (auto &sg : service_groups) {
	    delete sg;
	}
    }
};

/* Update timeouts for a particular service. This means we got data from the service,
 * either a heartbeat or another request, so we can assume it is still alive.
 */
void update_timeouts(vector<string> const &header,
		     unordered_map<string, ActiveServices*> &active_services)
{
    /* We need to lock to avoid conflicts with the asynchronous timer in ActiveServices */
    Avahi::Avahi::instance().lock();
    auto service = active_services.find(header.back());
    if (service != active_services.end()) {
	Avahi::update_timeout(service->second->timeout, 2);
    }
    Avahi::Avahi::instance().unlock();
}

/* Register a new service, or add new services to a service group */
bool register_services(Avahi::Registrar &registrar,
		       unordered_map<string, ActiveServices*> &active_services,
		       vector<string> const &header,
		       directoryd::ServiceRequest const &service)
{
    if (service.has_register_() == false) {
	return false;
    }
    /* First, create a new instance of the service object. */

    /* Take the requested txt and reformat it into a map */
    map<string, string> txt;
    for (auto &kv : service.register_().txt()) {
	txt.insert({kv.key(), kv.value()});
    }

    /*
     * Lock in order to avoid conflicts with the asynchronous timer. We're looking for other
     * services at the same location, and updating a shared datastructure with the new data
     */

    Avahi::Avahi::instance().lock();
    /* Create the servicegroup with the parameters that were passed in. */
    Avahi::ServiceGroup *sg = new Avahi::ServiceGroup(service.register_().name());

    /* Add all of the services provided with the request to the service group we just created */
    for (auto &loc : service.register_().location()) {
	sg->add_service("_hotdec._tcp", loc.port(), txt);
    }
    registrar.add_services(*sg);

    if (active_services.find(header.back()) == active_services.end()) { // new service location
	auto pp = new pair<string, unordered_map<string, ActiveServices*>*>(header.back(), &active_services);
	active_services.insert({header.back(), new ActiveServices(2, pp, sg)});
    }
    else { // service location exists
	auto service = active_services.find(header.back());
	service->second->service_groups.push_back(sg);
	Avahi::update_timeout(service->second->timeout, 2);
    }
    Avahi::Avahi::instance().unlock();
    return true;
}

/* Unregister a service */
bool unregister_services(unordered_map<string,
			 ActiveServices *> &active_services,
			 vector<string> const &header,
			 directoryd::ServiceRequest const &remove_service,
			 std::string &fail_reason)
{
    if (remove_service.has_unregister() == false) {
	fail_reason = "Invalid request: missing UNREGISTER field";
	return false;
    }
    /* Take the datastructure that we built above, and just pop the matching entries out of it. It will get
     * destructed, and in the process, the service will get removed.
     */
    Avahi::Avahi::instance().lock();
    auto service = active_services.find(header.back());
    /* Check if the service we're trying to unregister has been registered.
     * If it doesn't exist, it either expired or was already removed, or never registered.
     * Notify the client of failure.
     */
    if (service == active_services.end()) {
	fail_reason = "Service doesn't exist";
	Avahi::Avahi::instance().unlock();
	return false;
    }

    /* Check if the name we want to remove exists. If it does not, notify of failure as above.
     * Otherwise, remove the service, and notify the client of success */
    auto name = remove_service.unregister().name();
    auto result = find_if(service->second->service_groups.begin(),
			  service->second->service_groups.end(),
			  [&name](Avahi::ServiceGroup *sg) {
			      return sg->name() == name;
			  });

    if (result == service->second->service_groups.end()) {
	fail_reason = "Name doesn't exist";
	Avahi::Avahi::instance().unlock();
	return false;
    }
    else {
	delete *result;
	service->second->service_groups.erase(result);
    }

    // remove group if no remaining services
    if (service->second->service_groups.empty()) {
	auto as = active_services[header.back()];
	active_services.erase(header.back());
	delete as;
    }
    else { //update the timeouts for the remaining services in the group
	Avahi::update_timeout(service->second->timeout, 2);
    }

    Avahi::Avahi::instance().unlock();
    return true;
}

/* Find matching services by txt value and optionally the name */
bool find_services(Avahi::Browser const &browser,
		   directoryd::ServiceRequest const &request,
		   std::vector<Avahi::Service> &result)
{
    if (request.has_find() == false) {
	return false;
    }
    auto txt = request.find().txt();

    /* A function passed to find, used to filter by given txt values */
    auto pred = [&txt](Avahi::Service const &x) {
	for(auto &entry : txt) {
	    auto kv = x.txt().find(entry.key());
	    if (kv == x.txt().end()) {
		return false;
	    }
	    if (kv->second != entry.value()) {
		return false;
	    }
	}
	return true;
    };

    // If we have a name, perform a combination search on name and txt
    if (request.find().has_name()) {
	result = browser.lookup_by_name(request.find().name(), pred);
    }
    else { // Otherwise, just look for matching txt values in all services
	result = browser.lookup(pred);
    }
    return true;
}

/* Send a reply to the client */
void reply(void *socket,
	   vector<string> const &header,
	   directoryd::RequestType type,
	   bool success,
	   std::string const &result = std::string(),
	   std::vector<Avahi::Service> const &findresult = std::vector<Avahi::Service>())
{
    directoryd::ServiceReply reply;
    reply.set_type(type);
    reply.set_success(success);

    /* If we are given a result message, set it */
    if (result.empty() == false) {
	reply.set_result(result);
    }

    /* If this is a reply to a find, add the data we found */
    if (findresult.empty() == false) {
	for (auto &r : findresult) {
	    directoryd::ServiceReply::FindResult *findres = reply.add_findresult();
	    findres->mutable_location()->set_address(r.address());
	    findres->mutable_location()->set_port(r.port());
	    for (auto &txt : r.txt()) {
		directoryd::TxtField *txtfield = findres->add_txt();
		txtfield->set_key(txt.first);
		txtfield->set_value(txt.second);
	    }
	}
    }

    string buffer;
    if (debug && TextFormat::PrintToString(reply, &buffer)) {
	fprintf(stderr, "reply: %s\n", buffer.c_str());
    }

    zframe_t *sf = zframe_new(NULL, reply.ByteSize());
    assert (sf != NULL);
    reply.SerializeToArray(zframe_data(sf),zframe_size(sf));

    for (auto &h : header) {
	int retval = zstr_sendm (socket,h.c_str());
	assert(retval == 0);
    }

    int retval = zframe_send(&sf, socket, 0);
    assert(retval == 0);
}

void reply_result(void *socket,
		  vector<string> const &header,
		  directoryd::RequestType type,
		  std::vector<Avahi::Service> const &findresult)
{
    reply(socket, header, type, true, std::string(), findresult);
}

void reply_success(void *socket,
		   vector<string> const &header,
		   directoryd::RequestType type,
		   std::string const &result = std::string())
{
    reply(socket, header, type, true, result);
}

void reply_failure(void *socket,
		   vector<string> const &header,
		   directoryd::RequestType type,
		   std::string const &result)
{
    reply(socket, header, type, false, result);
}

void usage(std::string const &name) {
    std::cout << name << " [-dh]\n\n"
	      << "\t-d\tlaunch as daemon\n"
	      << "\t-h\thelp\n" << std::endl;
}

int main(int argc, char *const argv[]) {

    int opt;
    bool daemonize = false;
    opterr = 0;
    while ((opt = getopt(argc, argv, "hdD")) != -1) {
	switch(opt) {
	case 'd':
	    daemonize = true;
	    break;
	case 'D':
	    debug = true;
	    break;
	case 'h':
	default:
	    usage(argv[0]);
	    return 0;
	}
    }

    if (daemonize) daemon(0, 0);

    GOOGLE_PROTOBUF_VERIFY_VERSION;

    while (true) {
	Avahi::Browser browser("_hotdec._tcp");
	Avahi::Registrar registrar;

	/* List of active services, key is the zeromq id assigned to the client */
	unordered_map<string, ActiveServices*> active_services;

	zsock_t *socket = zsock_new (ZMQ_ROUTER);
	zsock_bind (socket,"ipc:///tmp/directoryd");
	chmod("/tmp/directoryd", S_IRWXO|S_IRWXU|S_IRWXG);

	directoryd::ServiceRequest request;
	bool success;
	std::string fail_reason;
	std::vector<Avahi::Service> result;

	/* Work loop; receive and process requests */
	while (true) {
	    zmsg_t *reqmsg = zmsg_recv(socket);
	    if (reqmsg == NULL)
		continue;
	    if (debug)
		zmsg_fprint (reqmsg, stderr);
	    char *client = zmsg_popstr(reqmsg);

	    zframe_t *rxf = zmsg_pop(reqmsg);
	    if (rxf == NULL)
		continue;

	    request.ParseFromArray(zframe_data(rxf),zframe_size(rxf));
	    zframe_destroy(&rxf);

	    vector<string> header;
	    header.push_back(string(client));
	    free(client);
	    zmsg_destroy(&reqmsg);

	    string buffer;
	    if (debug && TextFormat::PrintToString(request, &buffer)) {
		fprintf(stderr, "request: %s\n", buffer.c_str());
	    }

	    /* Process the request based on type */
	    switch (request.type()) {

	    case directoryd::REGISTER:
		success = register_services(registrar, active_services, header, request);
		if (success == true) {
		    reply_success(socket, header, directoryd::REGISTER);
		}
		else {
		    reply_failure(socket, header, directoryd::REGISTER, "Invalid request: missing REGISTER field");
		}
		break;

	    case directoryd::UNREGISTER:
		success = unregister_services(active_services, header, request, fail_reason);
		if (success == true) {
		    reply_success(socket, header, directoryd::UNREGISTER);
		}
		else {
		    reply_failure(socket, header, directoryd::UNREGISTER, fail_reason);
		}
		break;

	    case directoryd::FIND:
		result.clear();
		success = find_services(browser, request, result);
		if (success == true) {
		    reply_result(socket, header, directoryd::FIND, result);
		}
		else {
		    reply_failure(socket, header, directoryd::FIND, "Invalid request: missing FIND field");
		}
		break;

	    case directoryd::HEARTBEAT:
		/* Update disconnect timeouts */
		update_timeouts(header, active_services);
		reply_success(socket, header, directoryd::HEARTBEAT);
		break;
	    }
	}
    }

    google::protobuf::ShutdownProtobufLibrary();

    return 0;
}


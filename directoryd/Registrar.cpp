#include <iostream>
#include <memory>
#include <list>

#include <assert.h>

#include <avahi-client/client.h>
#include <avahi-client/lookup.h>
#include <avahi-client/publish.h>
#include <avahi-common/simple-watch.h>
#include <avahi-common/error.h>
#include <avahi-common/alternative.h>
#include <avahi-common/malloc.h>

#include "Avahi.hpp"
#include "Registrar.hpp"
#include "Service.hpp"
#include "ServiceGroup.hpp"

using std::cout;
using std::cerr;
using std::endl;

namespace Avahi {

    void Registrar::add_service(ServiceGroup &g, Service &s)
    {
	AvahiStringList *strlst = NULL;
	for(auto &kv : s.txt()) {
	    strlst = avahi_string_list_add_pair(strlst, kv.first.c_str(), kv.second.c_str());
	}

	int ret = avahi_entry_group_add_service_strlst(g.entry_group(), AVAHI_IF_UNSPEC, AVAHI_PROTO_UNSPEC, (AvahiPublishFlags)0,
						       g.name().c_str(), "_hotdec._tcp", NULL, NULL, s.port(), strlst);
	avahi_string_list_free(strlst);

	if (ret < 0) {
	    if (ret == AVAHI_ERR_COLLISION)
		throw CollisionError(ret);

	    throw RegistrarError(ret);
	}
    }

    void Registrar::add_services(ServiceGroup &g)
    {
	AvahiEntryGroup *group;
	int ret;
	bool success = false;

	group = avahi_entry_group_new(Avahi::instance().client(), entry_group_callback, &g);
	if (!group) {
	    throw EntryGroupError();
	}
	g.entry_group(group);

	while (success == false) {
	    success = true;
	    for (auto &s : g.services()) {
		try {
		    add_service(g, s);
		} catch (CollisionError &e) {
		    char *n = avahi_alternative_service_name(g.name().c_str());
		    g.name(n);
		    avahi_free(n);
		    avahi_entry_group_reset(group);
		    success = false;
		    break;
		}
	    }
	}

	if ((ret = avahi_entry_group_commit(group)) < 0) {
	    cerr << "Failed to commit entry group: " << avahi_strerror(ret) << endl;
	    throw EntryGroupError(ret);
	}
	g.registrar(this);
    }

#if 0
    void Registrar::reregister_services() {
	for (auto &sg : unregistered_services_) {
	    for (auto &s : sg.services_) {
		add_service(sg, s);
	    }
	    /* Tell the server to register the service */
	    if ((ret = avahi_entry_group_commit(group)) < 0) {
		cerr << "Failed to commit entry group: " << avahi_strerror(ret) << endl;
		goto fail;
	    }
	    registered_services_.push_back(sg);
	    unregistered_services_.pop_front();
	}
    }
#endif

#if 0
    void Registrar::reset_groups()
    {
	for (auto &sg : registered_services_) {
	    avahi_entry_group_reset(sg.entry_group());
	}
	unregistered_services_.splice(unregistered_services_.begin(), registered_services_);
    }
#endif

    void Registrar::entry_group_callback(AvahiEntryGroup *g,
					 AvahiEntryGroupState state,
					 void *data)
    {
	assert(data);
	ServiceGroup *group = static_cast<ServiceGroup *>(data);

	/* Called whenever the entry group state changes */

	switch (state) {
	case AVAHI_ENTRY_GROUP_ESTABLISHED:
	    /* The entry group has been established successfully */
	    //group->callback(0);
	    //cerr << "Service successfully established." << endl;
	    break;

	case AVAHI_ENTRY_GROUP_COLLISION:
	    //group->callback(-1);
	    avahi_entry_group_reset(g);
	    cerr << "Service successfully established." << endl;
	    group->registrar()->add_services(*group);
	    break;

	case AVAHI_ENTRY_GROUP_FAILURE :
	    /* Some kind of failure happened while we were registering our services */
	    cerr << "Entry group failure: " << avahi_strerror(avahi_client_errno(avahi_entry_group_get_client(g))) << endl;
	    //group->callback(-1);
	    throw EntryGroupError();
	    break;

	case AVAHI_ENTRY_GROUP_UNCOMMITED:
	case AVAHI_ENTRY_GROUP_REGISTERING:
	    ;
	}
    }

    ServiceGroup *Registrar::add(std::string type,
				 std::string name,
				 int port,
				 std::map<std::string, std::string> txt)
    {
	if (ready() == false) throw RegistrarError("Not ready.");
	ServiceGroup *sg = new ServiceGroup(name);
	if (port == 0) throw RegistrarError("Added empty service");
	sg->add_service(type, port, txt);
	//Avahi::instance().lock();
	add_services(*sg);
	//Avahi::instance().unlock();
	return sg;
    }

};

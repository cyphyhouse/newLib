#if !defined(_AVAHI_REGISTRAR_HPP)
#define _AVAHI_REGISTRAR_HPP
#include <memory>
#include <list>
#include <map>
#include <iostream>
#include <string>

#include <avahi-client/lookup.h>
#include <avahi-common/error.h>
#include <avahi-common/malloc.h>

#include "Service.hpp"
#include "ServiceGroup.hpp"

namespace Avahi {

    class Registrar {
	class RegistrarError : public AvahiError {
	public:
	    explicit RegistrarError (const std::string & what) : AvahiError(what) {};
	    explicit RegistrarError (int error_code) : AvahiError(error_code) {};
	    explicit RegistrarError () : AvahiError() {};
	};
	class EntryGroupError : public AvahiError {
	public:
	    explicit EntryGroupError (const std::string & what) : AvahiError(what) {};
	    explicit EntryGroupError (int error_code) : AvahiError(error_code) {};
	    explicit EntryGroupError () : AvahiError() {};
	};
	class CollisionError : public AvahiError {
	public:
	    explicit CollisionError (const std::string & what) : AvahiError(what) {};
	    explicit CollisionError (int error_code) : AvahiError(error_code) {};
	    explicit CollisionError () : AvahiError() {};
	};

	void add_service(ServiceGroup &g, Service &s);
	static void entry_group_callback(AvahiEntryGroup *g, AvahiEntryGroupState state, void *data);

    public:
	void add_services(ServiceGroup &g);
	ServiceGroup *add(std::string type,
			  std::string name,
			  int port,
			  std::map<std::string, std::string> txt);

	bool ready() const {
	    return Avahi::instance().ready();
	}

    };

};

#endif

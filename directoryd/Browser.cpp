#include <iostream>
#include <memory>
#include <list>

#include <avahi-client/client.h>
#include <avahi-client/lookup.h>
#include <avahi-common/simple-watch.h>
#include <avahi-common/error.h>
#include <avahi-common/alternative.h>
#include <avahi-common/malloc.h>

#include "Avahi.hpp"
#include "Browser.hpp"

using std::cout;
using std::cerr;
using std::endl;

namespace Avahi {

    void Browser::resolve_callback(AvahiServiceResolver *r,
				   AvahiIfIndex,
				   AvahiProtocol,
				   AvahiResolverEvent event,
				   const char *name,
				   const char *type,
				   const char *,
				   const char *host_name,
				   const AvahiAddress *address,
				   uint16_t port,
				   AvahiStringList *txt,
				   AvahiLookupResultFlags,
				   void *data)
    {
	assert(data);
	Browser *browser = static_cast<Browser *>(data);
	assert(r);

	switch (event) {
	case AVAHI_RESOLVER_FAILURE:
	    //cerr << "[Resolver] Failed to resolve service '" << name << "' of type '" << type << "' in domain '" << domain
	    //	 << "': " << avahi_strerror(avahi_client_errno(avahi_service_resolver_get_client(r))) << endl;
	    break;

	case AVAHI_RESOLVER_FOUND:
	    //cerr << "[Resolver] Service '" << name << " (" << host_name << ":" << port << ")"
	    //     << "' of type '" << type <<"' in domain '" << domain << "'" << endl;

	    //cout << a->browse_services_.size() << endl;
	    browser->add_service(name, Service(type, host_name, address, port, txt));
	    // XXX cache service, broadcast

#if 0
            avahi_address_snprint(a, sizeof(a), address);
            t = avahi_string_list_to_string(txt);
            fprintf(stderr,
                    "\t%s:%u (%s)\n"
                    "\tTXT=%s\n"
                    "\tcookie is %u\n"
                    "\tis_local: %i\n"
                    "\tour_own: %i\n"
                    "\twide_area: %i\n"
                    "\tmulticast: %i\n"
                    "\tcached: %i\n",
                    host_name, port, a,
                    t,
                    avahi_string_list_get_service_cookie(txt),
                    !!(flags & AVAHI_LOOKUP_RESULT_LOCAL),
                    !!(flags & AVAHI_LOOKUP_RESULT_OUR_OWN),
                    !!(flags & AVAHI_LOOKUP_RESULT_WIDE_AREA),
                    !!(flags & AVAHI_LOOKUP_RESULT_MULTICAST),
                    !!(flags & AVAHI_LOOKUP_RESULT_CACHED));

            avahi_free(t);
#endif
	}

	avahi_service_resolver_free(r);
    }

    void Browser::browse_callback(AvahiServiceBrowser *b,
				  AvahiIfIndex interface,
				  AvahiProtocol protocol,
				  AvahiBrowserEvent event,
				  const char *name,
				  const char *type,
				  const char *domain,
				  AvahiLookupResultFlags,
				  void* data)
    {
	assert(b);
	AvahiClient *client = avahi_service_browser_get_client(b);
	assert(data);
	Browser *browser = static_cast<Browser *>(data);

	switch (event) {
	case AVAHI_BROWSER_FAILURE:
	    cerr << "[Browser] " << avahi_strerror(avahi_client_errno(avahi_service_browser_get_client(b))) << endl;
	    throw BrowserError(avahi_client_errno(client));
	    //a->Stop();
	    return;

	case AVAHI_BROWSER_NEW:
	    //cerr << "[Browser] New service '" << name << "' of type '" << type << "' in domain '" << domain << "'" << endl;

	    if (!(avahi_service_resolver_new(client, interface, protocol, name, type, domain, AVAHI_PROTO_UNSPEC,
					     (AvahiLookupFlags)0, resolve_callback, browser)))
		cerr << "Failed to resolve service '" << name << "': " << avahi_strerror(avahi_client_errno(client)) << endl;
	    break;

	case AVAHI_BROWSER_REMOVE:
	    //cerr << "[Browser] Removed service " << name << " of type " << type << " in domain " << domain << endl;
	    // XXX Remove cached service, broadcast
	    browser->remove_service(name);
	    break;

	case AVAHI_BROWSER_ALL_FOR_NOW:
	    //cerr << "[Browser] All for now." << endl;
	    break;

	case AVAHI_BROWSER_CACHE_EXHAUSTED:
	    //cerr << "[Browser] Cache exhausted." << endl;
	    break;
	}
    }

}

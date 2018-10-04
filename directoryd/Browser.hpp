#if !defined(_AVAHI_BROWSER_HPP)
#define _AVAHI_BROWSER_HPP
#include <memory>
#include <iostream>
#include <string>
#include <unordered_map>
#include <algorithm>

#include <avahi-client/client.h>
#include <avahi-client/lookup.h>
#include <avahi-common/error.h>
#include <avahi-common/malloc.h>

#include "Service.hpp"

namespace Avahi {

    class Browser {
	AvahiServiceBrowser *sb_;
	std::unordered_multimap<string, Service> services;
	std::string type_;

	class BrowserError : public AvahiError {
	public:
	    explicit BrowserError (const std::string & what) : AvahiError(what) {};
	    explicit BrowserError (int error_code) : AvahiError(error_code) {};
	    explicit BrowserError () : AvahiError() {};
	};
	class ResolverError : public AvahiError {
	public:
	    explicit ResolverError (const std::string & what) : AvahiError(what) {};
	    explicit ResolverError (int error_code) : AvahiError(error_code) {};
	    explicit ResolverError () : AvahiError() {};
	};

	static void browse_callback(AvahiServiceBrowser *b,
				    AvahiIfIndex interface,
				    AvahiProtocol protocol,
				    AvahiBrowserEvent event,
				    const char *name,
				    const char *type,
				    const char *domain,
				    AvahiLookupResultFlags flags,
				    void* data);

	static void resolve_callback(AvahiServiceResolver *r,
				     AvahiIfIndex interface,
				     AvahiProtocol protocol,
				     AvahiResolverEvent event,
				     const char *name,
				     const char *type,
				     const char *domain,
				     const char *host_name,
				     const AvahiAddress *address,
				     uint16_t port,
				     AvahiStringList *txt,
				     AvahiLookupResultFlags flags,
				     void *data);

	void add_service(std::string const &name, Service const &s)
	{
	    services.insert(std::pair<std::string, Service>(name, s));
	}

	void remove_service(std::string const &name)
	{
	    services.erase(name);
	}

    public:
	explicit Browser (string const &type) : type_(type) {
	    Avahi::instance().lock();
	    sb_ = avahi_service_browser_new(Avahi::instance().client(),
					    AVAHI_IF_UNSPEC,
					    AVAHI_PROTO_UNSPEC,
					    type_.c_str(),
					    NULL,
					    (AvahiLookupFlags)0,
					    browse_callback,
					    this);
	    Avahi::instance().unlock();
	    if (!sb_) {
		throw BrowserError();
	    }
	}

	~Browser () {
	    Avahi::instance().lock();
	    avahi_service_browser_free(sb_);
	    Avahi::instance().unlock();
	}

	bool ready() const {
	    return Avahi::instance().ready();
	}

	const std::vector<Service> lookup_by_name (std::string const &name) const
	{
	    Avahi::instance().lock();
	    auto result = services.equal_range(name);
	    std::vector<Service> found;
	    std::for_each(result.first,
			  result.second,
			  [&found](decltype(*result.first) &elt) {
			      found.push_back(elt.second);
			  });
	    Avahi::instance().unlock();
	    return found;
	}

	template <class Predicate>
	const std::vector<Service> lookup_by_name (std::string const &name,
						   Predicate const &pred) const
	{
	    std::vector<Service> found;
	    Avahi::instance().lock();
	    auto result = services.equal_range(name);
	    auto s = std::find_if(result.first,
				  result.second,
				  [&pred](std::pair<std::string, Service> const &x)
				  {
				      return pred(x.second);
				  });
	    while (s != result.second) {
		found.push_back(s->second);
		s++;
		s = std::find_if(s,
				 result.second,
				 [&pred](std::pair<std::string, Service> const &x) {
				     return pred(x.second);
				 });
	    }
	    Avahi::instance().unlock();
	    return found;
	}

	template <class Predicate>
	const std::vector<Service> lookup (Predicate const &pred) const
	{
	    std::vector<Service> found;
	    Avahi::instance().lock();
	    auto s = std::find_if(services.begin(),
				  services.end(),
				  [&pred](std::pair<std::string, Service> const &x) {
				      return pred(x.second);
				  });
	    while (s != services.end()) {
		found.push_back(s->second);
		s++;
		s = std::find_if(s,
				 services.end(),
				 [&pred](std::pair<std::string, Service> const &x) {
				     return pred(x.second);
				 });
	    }
	    Avahi::instance().unlock();
	    return found;
	}
    };

};

#endif

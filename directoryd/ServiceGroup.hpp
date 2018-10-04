#if !defined(_SERVICEGROUP_HPP)
#define _SERVICEGROUP_HPP
#include <avahi-client/publish.h>
#include "Service.hpp"
#include <string>
#include <vector>
#include <list>
#include <memory>

using std::string;
using std::list;
using std::shared_ptr;
using std::unique_ptr;
using std::vector;

namespace Avahi {

    class Registrar;

    class ServiceGroup {
	string name_;
	vector<Service> services_;
	AvahiEntryGroup *entry_group_;
	Registrar *registrar_;

    public:

	ServiceGroup() : entry_group_(nullptr) {}

	explicit ServiceGroup(string name) : name_(name), entry_group_(nullptr) {}

	~ServiceGroup()
	{
	    if (entry_group_) {
		avahi_entry_group_reset(entry_group_);
		avahi_entry_group_free(entry_group_);
	    }
	};

	//ServiceGroup(ServiceGroup const &s) : name_(s.name_), services_(s.services_),
	//    entry_group_(s.entry_group_), registrar_(s.registrar_) {
	//}
	ServiceGroup(ServiceGroup const &s) = delete;

	friend void swap(ServiceGroup &first, ServiceGroup &second)
	{
	    using std::swap;
	    swap(first.name_, second.name_);
	    swap(first.services_, second.services_);
	    swap(first.entry_group_, second.entry_group_);
	    swap(first.registrar_, second.registrar_);
	}

	ServiceGroup(ServiceGroup &&other) : entry_group_(nullptr)
	{
	    swap(*this, other);
	}

	ServiceGroup &operator=(ServiceGroup other)
	{
	    swap(*this, other);
	    return *this;
	}

	void entry_group(AvahiEntryGroup *entry_group)
	{
	    entry_group_ = entry_group;
	}

	AvahiEntryGroup *entry_group() { return entry_group_; };

	void add_service(string type,
			 unsigned short port, map<string, string> txt)
	{
	    services_.push_back(Service(type, port, txt));
	};

	string &name() { return name_; }
	string &name(string n) { name_ = n; return name_; }
	vector<Service> &services() { return services_; }
	Registrar *registrar() { return registrar_; }
	void registrar(Registrar *r) { registrar_ = r; }
    };

};
#endif

#if !defined(_SERVICE_HPP)
#define _SERVICE_HPP
#include <string>
#include <vector>
#include <map>
#include <avahi-common/strlst.h>

using std::string;
using std::vector;
using std::map;

namespace Avahi {

    class Service {
	unsigned short port_;
	map<string, string> txt_;
	string hostname_;
	string address_;
	string type_;

	void parse_txt(AvahiStringList *txt)
	{
	    char *key, *value;
	    while (txt != NULL) {
		avahi_string_list_get_pair(txt, &key, &value, NULL);
		txt_[string(key)] = string(value);
		txt = avahi_string_list_get_next(txt);
		avahi_free(key);
		avahi_free(value);
	    }
	}

    public:
	Service(string const &type,
		unsigned short port,
		map<string, string> const &txt) : port_(port), txt_(txt), type_(type) {}

	Service(string const &type,
		string const &hostname,
		unsigned short port,
		AvahiStringList *txt) : port_(port), hostname_(hostname), type_(type)
	{
	    parse_txt(txt);
	}

	Service(string const &type,
		string const &hostname,
		const AvahiAddress *address,
		unsigned short port,
		AvahiStringList *txt)

	    : port_(port), hostname_(hostname), type_(type)
	{
	    char buf[AVAHI_ADDRESS_STR_MAX];
	    avahi_address_snprint(buf, AVAHI_ADDRESS_STR_MAX, address);
	    address_ = string(buf);
	    parse_txt(txt);
	}

	friend class ServiceGroup;

	const map<string, string> &txt() const { return txt_; }
	unsigned short port() const { return port_; }
	const string &hostname() const { return hostname_; }
	const string &address() const { return address_; }
	const string &type() const { return type_; }

	Service() {};
	~Service() {};
    };

}

#endif

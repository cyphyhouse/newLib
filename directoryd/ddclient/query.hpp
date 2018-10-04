#if !defined(_QUERY_HPP)
#define _QUERY_HPP
#include <vector>
#include <string>
#include <map>
#include <unordered_map>
#include <stdexcept>

#include "ddclient.hpp"

namespace DDClient {
    struct Service {
	std::string address;
	int port;
	std::map<std::string, std::string> txt;
    };

    std::vector<Service> find(std::map<std::string, std::string> const &txt,
			      std::string const &name = std::string());

    class QueryError : public std::runtime_error {
    public:
	explicit QueryError (const std::string & what) : std::runtime_error(what) {};
	explicit inline QueryError ();
    };
}

#endif

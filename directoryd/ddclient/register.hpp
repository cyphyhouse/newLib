#if !defined(_REGISTER_HPP)
#define _REGISTER_HPP
#include "ddclient.hpp"
#include <string>
#include <map>
#include <stdexcept>

namespace DDClient {

    void register_service(std::string const &name,
			  int port,
			  std::map<std::string, std::string> const &txt);

    void unregister_service(std::string const &name);

    void heartbeat();

    class RegistrationError : public std::runtime_error {

    public:

	explicit RegistrationError (const std::string & what)
	    : std::runtime_error(what) {};

	explicit inline RegistrationError ();
    };
}

#endif

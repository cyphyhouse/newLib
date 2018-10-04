#if !defined(_DDCLIENT_HPP)
#define _DDCLIENT_HPP
#include <czmq.h>

namespace DDClient {
    class DDClient {
	void * context_;
	void * query_socket_;
	void * register_socket_;
	DDClient ();
	~DDClient ();

    public:

	void * &query_socket() {
	    return query_socket_;
	}

	void * &register_socket() {
	    return register_socket_;
	}

	static DDClient &instance();
    };

}
#endif

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
#include <avahi-common/timeval.h>

#include "Avahi.hpp"

using std::cout;
using std::cerr;
using std::endl;
using std::list;

namespace Avahi {

    std::unique_ptr<AvahiTimeout, timeoutdeleter> set_timeout(int seconds, void (*callback)(AvahiTimeout *, void *), void *data)
    {
	timeval tv;
	//Avahi::instance().lock();
	auto poll = avahi_threaded_poll_get(Avahi::instance().poll());
	unique_ptr<AvahiTimeout, timeoutdeleter>
	    p(poll->timeout_new(poll, avahi_elapse_time(&tv, 1000*seconds, 0), callback, data));
	//Avahi::instance().unlock();
	return p;
    }

    void update_timeout(std::unique_ptr<AvahiTimeout, timeoutdeleter> const &t, int seconds)
    {
	timeval tv;
	//Avahi::instance().lock();
	auto poll = avahi_threaded_poll_get(Avahi::instance().poll());
	poll->timeout_update(t.get(), avahi_elapse_time(&tv, 1000*seconds, 0));
	//Avahi::instance().unlock();
    }

    void cancel_timeout(std::unique_ptr<AvahiTimeout, timeoutdeleter> const &t)
    {
	//Avahi::instance().lock();
	auto poll = avahi_threaded_poll_get(Avahi::instance().poll());
	poll->timeout_update(t.get(), NULL);
	//Avahi::instance().unlock();
    }

    void Avahi::client_callback(AvahiClient *c, AvahiClientState state, void * data)
    {
	assert(c);
	assert(data);
	Avahi *a = static_cast<Avahi *>(data);

	/* Called whenever the client or server state changes */

	switch (state) {
        case AVAHI_CLIENT_S_RUNNING:

            /* The server has startup successfully and registered its host
             * name on the network, so it's time to create our services */
            //a->create_services();
            a->state_ = State::Running;
            break;

        case AVAHI_CLIENT_FAILURE:
            cerr << "Server connection failure: " << avahi_strerror(avahi_client_errno(c)) << endl;
            throw ClientError();
            break;

        case AVAHI_CLIENT_S_COLLISION:

            /* Let's drop our registered services. When the server is back
             * in AVAHI_SERVER_RUNNING state we will register them
             * again with the new host name. */

        case AVAHI_CLIENT_S_REGISTERING:

            /* The server records are now being established. This
             * might be caused by a host name change. We need to wait
             * for our own records to register until the host name is
             * properly esatblished. */

            //if (a->state_ == State::Running)
            //    a->reset_groups();

            a->state_ = State::NotRunning;

            break;

        case AVAHI_CLIENT_CONNECTING:
            ;
	}
    }

};

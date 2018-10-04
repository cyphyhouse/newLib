#if !defined(_AVAHI_HPP)
#define _AVAHI_HPP
#include <memory>
#include <list>
#include <map>
#include <iostream>
#include <string>
#include <stdexcept>

#include <avahi-client/client.h>
#include <avahi-common/thread-watch.h>
#include <avahi-common/error.h>
#include <avahi-common/alternative.h>
#include <avahi-common/malloc.h>

using std::unique_ptr;

namespace Avahi {

    struct timeoutdeleter;

    std::unique_ptr<AvahiTimeout, timeoutdeleter> set_timeout(int seconds, void (*callback)(AvahiTimeout *, void *), void *data);
    void update_timeout(std::unique_ptr<AvahiTimeout, timeoutdeleter> const &t, int seconds);
    void cancel_timeout(std::unique_ptr<AvahiTimeout, timeoutdeleter> const &t);

    class AvahiError : public std::runtime_error {
    public:
	explicit AvahiError (const std::string & what) : std::runtime_error(what) {};
	explicit AvahiError (int error_code) : runtime_error(avahi_strerror(error_code)) {};
	explicit inline AvahiError ();
    };

    class Avahi {
	enum class State { Running, NotRunning } state_;
	unique_ptr<AvahiThreadedPoll, void (*)(AvahiThreadedPoll *)> poll_;
	unique_ptr<AvahiClient, void (*)(AvahiClient *)> client_;

	class PollError : public AvahiError {
	public:
	    explicit PollError (const std::string & what) : AvahiError(what) {};
	    explicit PollError (int error_code) : AvahiError(error_code) {};
	    explicit PollError () : AvahiError() {};
	};
	class ClientError : public AvahiError {
	public:
	    explicit ClientError (const std::string & what) : AvahiError(what) {};
	    explicit ClientError (int error_code) : AvahiError(error_code) {};
	    explicit ClientError () : AvahiError() {};
	};

	static void client_callback(AvahiClient *c, AvahiClientState state, void * data);

    public:
	Avahi() : poll_(nullptr, avahi_threaded_poll_free), client_(nullptr, avahi_client_free)
	{
	    int error;

	    poll_.reset(avahi_threaded_poll_new());
	    if (!poll_.get()) {
		throw PollError("Failed to create poll object.");
	    }

	    client_.reset(avahi_client_new(avahi_threaded_poll_get(poll_.get()), (AvahiClientFlags)0, client_callback, this, &error));
	    if (!client_.get()) {
		throw ClientError(error);
	    }

	    if ((error = avahi_threaded_poll_start(poll_.get())) < 0) {
		throw PollError(error);
	    }
	}

	~Avahi() {
	    avahi_threaded_poll_stop(poll_.get());
	}

	AvahiClient * client(void) {
	    return client_.get();
	}

	AvahiThreadedPoll *poll(void) {
	    return poll_.get();
	}

	bool ready(void) {
	    //lock();
	    bool ready = (state_ == State::Running);
	    //unlock();
	    return ready;
	}

	static Avahi &instance(void) {
	    static Avahi instance;
	    return instance;
	}

	void lock(void) {
	    avahi_threaded_poll_lock(poll_.get());
	}

	void unlock(void) {
	    avahi_threaded_poll_unlock(poll_.get());
	}
    };

    struct timeoutdeleter {
	void operator() (AvahiTimeout *timeout) {
	    //Avahi::instance().lock();
	    avahi_threaded_poll_get(Avahi::instance().poll())->timeout_free(timeout);
	    //Avahi::instance().unlock();
	}
    };

    AvahiError::AvahiError() : runtime_error(avahi_strerror(avahi_client_errno(Avahi::instance().client()))) {};

};

#endif

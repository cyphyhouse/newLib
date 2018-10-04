#include <iostream>
#include <unistd.h>
#include <stdio.h>
#include <map>

#include "Avahi.hpp"
#include "Browser.hpp"
#include "Service.hpp"

using namespace std;

void printservice (const Avahi::Service &s)
{
    fprintf(stderr, "\nhostname '%s'  '%s' %u '%s'\n",
	    s.hostname().c_str(),
	    s.address().c_str(),
	    s.port(),
	    s.type().c_str());

    for (auto iter = s.txt().begin();
    	 iter != s.txt().end();
    	 ++iter) {
    	fprintf(stderr, "\t%s = %s\n",
    		iter->first.c_str(),
    		iter->second.c_str());
    }
}

int main()
{
    Avahi::Browser browser("_hotdec._tcp");

    sleep(2);

    while (!browser.ready()) sleep(1);

    auto result = browser.lookup_by_name("fooservice");
    for_each (result.begin(), result.end(), printservice);


    result = browser.lookup_by_name("vision");
    for_each (result.begin(), result.end(), printservice);


    result = browser.lookup_by_name("ruby-vision",
				    [](Avahi::Service const &x){
					(void)x;
					return true;
				    });
    for_each (result.begin(), result.end(), printservice);

    return 0;
}

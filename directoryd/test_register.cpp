#include <iostream>
#include <unistd.h>

#include "Avahi.hpp"
#include "Registrar.hpp"

using namespace std;

int main()
{

    Avahi::Registrar registrar;
    while (registrar.ready() == false);

    registrar.add("_hotdec._tcp",
		  "fooservice",
		  1234,
		  {{"foo", "bar"}, {"bar", "baz"}});
    sleep(10);
    return 0;
}

# newLib
Migrating from old StarLib to a library with only Java dependencies. In order
to support new CyphyHouse project on various of platforms.

Original repo can be found here: [`StarLib`](https://github.com/detree/StarL1.5/tree/master/trunk/android/StarLib)
in [StarL1.5 - yixiao.](https://github.com/lin187/StarL1.5)

## to compile and run:
  * Please be sure to install `maven` and `java`. Currently building under `java1.8`
  * In the folder after clone:
    * Type in terminal: `mvn clean install` then `./run.sh`
  * You may want to toggle the libraries used when running the compiled target, especially for the `log4j` package.
   __To do so please edit the `run.sh`.__ Default is using the `maven` download libraries, which is under
   `~/.m2/repository/log4j/log4j/1.2.17/log4j-1.2.17.jar`(note that `java` may not support `~` as
   `$HOME_DIR`).

## programs under `tools` folder
  * `send.py` sends UDP packages to a designated address. Currently used for fake position info.
  * `simul_recv.c` receives UDP packages under port 5556. Currently used for fake ARDrone instance.
  * trying to use more `python` for simplicity
# newLib
Migrating from old StarLib to a library with only Java dependencies

## to compile and run:
  * Please be sure to install `maven` and `java`. Currently building under `java1.8`
  * In the folder after clone:
    * Type in terminal: `mvn clean install` then `./run.sh`
  * You may want to toggle the libraries used when running the compiled target, especially for the `log4j` package. __To do so please edit the `run.sh`.__ Default is using the `maven` download libraries, which is under `~/.m2/repository/log4j/log4j/1.2.17/log4j-1.2.17.jar`.

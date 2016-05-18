Readme for the environment manager tools project
================================================

This project contains a client for the Environment Manager REST API via Jenkins.

Prerequisites for building:
--------------------------
 - Java 6 JDK
 - Maven 3

To build:
---------

mvn clean install

To run:
-------

mvn hpi:run


To perform a release:
--------------------

mvn release:prepare release:perform


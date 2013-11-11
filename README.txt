Readme for the environment manager tools projects.
=================================================

This project contains a client for the Environment Manager REST API.  As well
as integration components for Jenkins, Ant, and Maven.

Prerequisites for building:
--------------------------
 - Java 6 JDK
 - Maven 3

To build:
---------

mvn clean install


To perform a release:
--------------------

mvn release:prepare
mvn release:perform


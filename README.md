Readme for the environment manager tools project
================================================

This project contains a client for the Environment Manager REST API via Jenkins.

For documentation please visit the link [Environment Manager Plugin for Jenkins](https://docs.parasoft.com/display/SOAVIRT9103CTP310/Environment+Manager+Plugin+for+Jenkins+2.5)

Prerequisites for building:
--------------------------
 - Java 6 JDK
 - Maven 3

To build:
---------

mvn clean install

To run:
-------

mvn hpi:run -Djetty.port=8082


To perform a release:
--------------------

mvn release:prepare release:perform


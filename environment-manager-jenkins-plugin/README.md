Environment Manager Jenkins plugin

To Build:

mvn clean install


To test, you can either take the .hpi file created by the build process, or you
can run an embeded jetty server which loads the plugin.

To run the embedded server:

mvn clean hpi:run

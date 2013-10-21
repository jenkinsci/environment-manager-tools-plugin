How to use the environment manager ant plugin

1. Determin the environmentId for the instance you would like to provision
   using ant. This can be found using the environments api method in
   Environment Manager.
2. Using the environmentId from step 1, find the instanceId for the instance
   that you would like to provision.  This can be found using the environment
   instances api method in Environment Manager.
3. Add the environment manager ant jar file in your ant lib directory. The
   current version of the jar can be found at:
   http://build.parasoft.com/maven/com/parasoft/environment-manager-ant-plugin/1.3/environment-manager-ant-plugin-1.3-antjar.jar
   (Note: the version number appears two times in that URl, be sure to adjust
   the URL to indicate the most recent version number.
4. Example ant script

<?xml version="1.0"?>
<project name="provisioning" default="main" 
  xmlns:em="antlib:com.parasoft.environmentmanager.antlib">
    <import file="build.xml"/>

    <target name="main">
        <echo>Provisioning an environment manager instance</echo>

        <!-- perform provisioning -->
        <em:provision url="http://{host}:{port}/em" username="admin" password="admin"
            environmentId="129" instanceId="45"/>
    </target>
</project>


The key parts here are:
  - the xmlns:em attribute declaration on the project element.
  - the <em:provision .../> ant task using the environmentId and instanceId
    found earlier. Also, fill in the correct host, port, username, and
    password.


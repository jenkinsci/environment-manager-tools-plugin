How to use the environment manager ant plugin

1. Determin the environmentId for the instance you would like to provision
   using ant. This can be found using the environments api method in
   Environment Manager.
2. Using the environmentId from step 1, find the instanceId for the instance
   that you would like to provision.  This can be found using the environment
   instances api method in Environment Manager.
3. Example maven pom

<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  ...
  <build>
    <plugins>
        <plugin>
            <groupId>com.parasoft</groupId>
            <artifactId>environment-manager-maven-plugin</artifactId>
            <version>1.3</version>
            <configuration>
                <url>http://host:port/em</url>
                <username>admin</username>
                <password>admin</password>
                <environmentId>129</environmentId>
                <instanceId>46</instanceId>
                <abortOnFailure>true</abortOnFailure>
            </configuration>
            <executions>
                <execution>
                    <id>provision-environment-manager-instance</id>
                    <phase>initialize</phase>
                    <goals>
                        <goal>provision</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
  </build>
  <pluginRepositories>
    <pluginRepository>
      <id>Parasoft</id>
      <url>http://build.parasoft.com/maven</url>
    </pluginRepository>
  </pluginRepositories>
</project>

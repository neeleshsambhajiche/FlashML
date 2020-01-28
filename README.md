# FlashML

This is a project from Data Science Engineering team at [[24]7.ai](https://www.247.ai/) for creating an internal framework for training all machine learning models deployed by the Data Science Group. This is built on top of Apache Spark.

## Dependencies
0. JDK 8. OpenJDK 8 can be used without any issues.
1. Hadoop (>2.7.x) and Hive (1.2.2, download from: https://archive.apache.org/dist/hive/hive-1.2.2/)
   - The default installation of hive is with derby, a single-user database, as the backend. Follow the instructions in this page for using mysql as the backend: https://saurzcode.in/2015/01/configure-mysql-metastore-hive/
2. Scala: Version 2.11.12
    - The binaries should be in the path
3. Apache Spark: Version 2.4.x
    - The binaries should be in the path
4. Maven: Version 3.3.9 or above
    - The binaries should be in the path
5. [Optional, for development] IntelliJ IDEA Community Edition
6. [Optional, for log search] ELK Stack (see section [Logging](#Logging) below)

### Important !
* If you are starting off in a new VM, you can use `install-scripts/installer-debian.sh` to install the following components: OpenJDK 8, mysql, Scala, Hadoop, Spark, Hive, IntelliJ IDEA, Maven.
* The code HAS to be complied against scala version 2.11.x with Java 8 for now.

## Running with docker

### Installing docker
Following are the links for docker installation.
1. [Ubuntu/Debian](https://docs.docker.com/engine/installation/linux/docker-ce/ubuntu/)
2. [CentOS](https://docs.docker.com/engine/installation/linux/docker-ce/centos/)
3. [Windows](https://docs.docker.com/toolbox/toolbox_install_windows/)

### Building and packaging
1. `cd flashml-core`
2. `mvn clean scala:compile package -DskipTests`

### Running FlashML tests
1. `cd ../docker`
2. To run the complete test suite:
 
     `./run-docker.sh "test"`
        
   To run a single test:
   
     `./run-docker.sh "test" "systemTests.MultiIntentSVMTest"`
     
For a complete list of test classes, navigate to [`flashml-core/src/test/scala/com/tfs/flashml`](flashml-core/src/test/scala/com/tfs/flashml) 

### Using docker shell with FlashML
The following commands would drop you into a docker bash shell with all FlashML dependencies ready. There you can run spark-submit and other usual commands. We also load the FlashML jar in docker HDFS, please follow the notes on screen.

1. `cd ../docker`
2. `./run-docker.sh`

It is also possible to mount a host folder with your data into the docker container, and use those files and folders to run a Spark job. For example, to load `/home/samik/project/yelp` from host to docker, use the following command:

	 ./run-docker.sh -m /home/samik/project/yelp
	 
Once you are inside the container, this folder will be available at `/FlashML/project` inside the container. Again,  please follow the notes on screen to lauch a Spark job in the container.

#### Running Spark history server on docker
If you want to run spark history server, run the following command while your are inside docker:
    
   `$SPARK_HOME/sbin/start-history-server.sh`
   
The UI would be available at port 18080, and can be accessed at http://localhost:18080, or at the corresponding URL for a specific machine.
   
To stop the history server:

   `$SPARK_HOME/sbin/stop-history-server.sh`

## Running without docker

### Setting up environment
1. Start hadoop and yarn processes (as root):
  * `start-dfs.sh`
  * `start-yarn.sh`
2. Load data into hive: From the project root folder, using user login (root login not required) run the following:
     `hive -f scripts/<scriptname.sql>`
3. Test if the table shows up properly:
  * Run hive: `hive`
  * Check tables: `show tables;`
  * Count rows in the table just loaded
4. Start hive metastore to be used by the FlashML process, using user login: 

     `hive --service metastore &`
     
5. Build latest FlashML at the `flashml-core` folder:

   `mvn clean scala:compile package -DskipTests`  

### Running a test from command line

1. Make sure that the relevant data is loaded using `hive -f`, and the relevant/required support files are available at the correct location as per config file. For example, in our case, change the working directory to `docker/Flashml`

	`cd docker/FlashML`

2. Run a particular test using the command as below.

	`scala -J-Xmx2g -cp "../scalatest_2.11-3.0.5.jar:../scalactic_2.11-3.0.5.jar:../flashml-<version>.jar" org.scalatest.tools.Runner -o -R ../flashml-<version>-tests.jar -s com.tfs.flashml.systemTests.MultiIntentSVMTest`
	

### Running FlashML code from comamand line

1. Set up a folder with the data, support files (if required), a SQL script to load the data to hive, and the config file.

2. To run on your local dev box (e.g., your VM):

    `spark-submit --driver-memory 2g --executor-memory 2g --class com.tfs.flashml.FlashML /path/to/flashml-<version>.jar <config file>` or simply `spark-submit --class com.tfs.flashml.FlashML /path/to/flashml-<version>.jar <config file>`
    
3. To run on a cluster:

	* Copy the compiled `flashml-<version>.jar` to a location in HDFS, using `hadoop fs -put`. This is not necessary but makes things easy. 
    * `spark-submit --num-executors 2 --driver-memory 2g --executor-memory 2g --executor-cores 2 --master yarn --deploy-mode cluster --files <comma separated list of file paths> --class com.tfs.flashml.FlashML <hdfs:///path/to/flashml-<version>.jar> <config file>`

<b>Notes:</b> 
* The `<config file>` at the end of the command can be ommitted if the config file with name "config.json" is available at the folder from where the spark job is launched.
* We can attach the IntelliJ debugger even if we are using the `spark-submit` command from shell, by using the technique of remote debugging in JVM.
 
	* Use the following command for `spark-submit`:

	`spark-submit --conf spark.driver.extraJavaOptions=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005 --class com.tfs.flashml.FlashML /path/to/flashml-<version>.jar <config file>`
	
	* Now launch the debug process from IntelliJ, e.g., as described in [this link](https://stackoverflow.com/questions/30403685/how-can-i-debug-spark-application-locally). 	


### Running FlashML from IDE

<b>Running FlashML:</b> Follow the steps below to run FlashML using your favorite IDE.
- Modify `flashml-core/config.json` as required
- Right click on `com.tfs.flashml.FlashML.scala` and select `Run FlashML`

	<b>Developer Notes:</b> The file `flashml-core/config.json` is included in the `.gitignore` file at the root level, and is expected to be checked in only when there is an update to the configurations (e.g., adding/removal/update of keys).

<b>Running FlashML Tests:</b> We recommend running tests from IDE. Right-click on the test class name in the IDE Project Explorer and choose `Run <XXXTest>`

<b>Running FlashML with a specific config parameter:</b> In this case, the `<FlashML Root>/config.json` should contain all the correct configurations. Right-click on `com.tfs.flashml.FlashML.scala` and select `Run FlashML`

## Packaging
1. The compiled classes gets packaged to a jar using the same maven command mentioned above (it also does packaging and shading for the main jar - flashml-<version>.jar). The artifact version will be changed for every release of FlashML.

 - *NOTE*: Change VersionNumber in both root `pom.xml` and `docker/run-flashml-container.sh` parameter
 
2. To build a thin jar, add `<scope>provided</scope>` in the `pom.xml` to all the Spark dependencies. 

3. The artifact will be generated in the docker folder inside the project with flashml-<version>.jar name.

## Upgrading Dependencies
Any dependency change will require change in two places:

 - Root `pom.xml` : for changes to application dependencies
 - `docker test infra` : Change in docker images for Test Infra
    
NOTE: The instructions for changing DockerHub Infra are in [docker/dockerhub/README.md](docker/dockerhub/README.md)
    
For example, to Upgrade Spark/Hadoop/Hive version, change version in `pom.xml` and then change dockerhub image to use new version.

    
## Logging with ELK Stack

1. Create a copy of the FlashML jar. Lets call it flashmlLog.jar. For more details why we need to do this: https://stackoverflow.com/questions/32843186/custom-log4j-appender-in-spark-executor

   Else we get the error: "java.lang.ClassNotFoundException: org.apache.log4j.net.FlashMLSocketAppender"

2. To forward the FlashML logs to ELK stack, add: 

    `--files log4j.properties,flashmlLog.jar --conf spark.driver.extraJavaOptions='-Dlog4j.configuration=file:log4j.properties' --conf spark.driver.extraClassPath='file:flashmlLog.jar' --conf spark.executor.extraJavaOptions='-Dlog4j.configuration=file:log4j.properties' --conf spark.executor.extraClassPath='file:flashmlLog.jar'`

    to the spark submit command. 

3. Remaining details related to the socket server receiving the logs are available in the [README.md in FlashML Log Server repo](https://github.com/247-ai/FlashML-Log-Server/blob/master/README.md). 

## Release Notes
Release notes for all versions of FlashML are at [Release Notes](Release%20Notes.md).

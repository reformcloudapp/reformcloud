# ReformCloud [![CodeFactor](https://www.codefactor.io/repository/github/reformcloudapp/reformcloud/badge)](https://www.codefactor.io/repository/github/reformcloudapp/reformcloud)

ReformCloud is a cloud system programmed and optimized for all sizes of networks. The cloud system
provides a huge api to access all internal functions, group, processes etc. It's made for **synchronized as well as asynchronous programming** and is integrated into the main executor **Node**
but as well into the apis for **Waterdog / Nukkit etc**. So the
development work to integrate something like a private server system into the cloud is not that much
work and get be made easily by every developer who spends 5 minutes to read **the documentation of
the api**. ReformCloud is basically just an application to start and manage the minecraft servers
and proxies. If you need more features like ingame commands or a permission system reformcloud also
provides these things as an external module. ReformCloud is build to flexible manage your **minecraft servers and proxies which are internally called "processes"**. The processes can get 
started bases on a **group** which are
**not sorted by the version**. A group can have **multiple** templates and **all of them can have
other versions**. If you need **more than one template** for a group to start or need a **path
inclusion** you can easily create them in the group file and the cloud will copy the paths or
templates at the next startup of a process.

### Support
If you need help, join our [Discord Server](https://discord.gg/MWesg3y3MU).

### Currently supported minecraft bedrock edition softwares:

| Version Name | Version Type |                
|--------------|--------------|
| NukkitX      | Server       |
| WaterdogPE   | Proxy        |

### Currently planned minecraft bedrock edition softwares:

| Version Name | Version Type |                
|--------------|--------------|
| Allay        | Server       |
| Cloudburst   | Server       |

# Run ReformCloud the first time

## System requirements

- 8 GB Memory
- 4 CPU Core
- A little bit of space on the hard disk

## Supported Java Versions
- Java 23

## Startup

You can download the latest release version from
the [GitHub Releases](https://github.com/reformcloudapp/reformcloud/releases)

Just save the file named as `runner.jar` in the folder you want to run the cloud in and start the
runner using:

```
java -XX:+UseG1GC -XX:MaxGCPauseMillis=50 -XX:CompileThreshold=100 -Xmx512m -Xms256m -jar runner.jar
```

The runner is now going to download the needed libraries for the runtime, so **make sure you have an
internet connection**
during the first startup!

## Node

The node will ask six questions:

1) Firstly you have to provide the name of the new node which ***is not used already in the
   cluster***
2) After this the node needs the host on which the servers and proxies should get bound to
3) Then the node asks for the ***internal*** network port (default `1809`)
4) And then the node asks for the ***internal*** web port (default `2008`)
5) Then you have to provide the connection key for other nodes. If you want to setup a cluster and
   already have a node in this cluster copy the key from the other node located
   in `NODE_DIR/reformcloud/files/.connection/connection.json`. If you want to generate a random
   connection key type `gen`

# Found a bug or have a proposal?

Please
[**open an issue**](https://github.com/reformcloudapp/reformcloud/issues/new)
and ***describe the bug/proposal as detailed as possible*** and **look into your email if we have
replied to your issue and answer upcoming questions**.

# Support our work

If you like reformcloud and want to support our work you can **star** :star2: the project, leave a (
positive)

But the best support for our work is very simple: ***use the cloud system!***

# Developer Information

## Want to contribute?

You can simply
[**fork the project**](https://github.com/reformcloudapp/reformcloud/fork)
make the changes you want to add and create a **pull request**. If your pull request got approved
and merged you will get added to the list of contributors.

## Build this project
```
git clone https://github.com/reformcloudapp/reformcloud.git
cd reformcloud/
mvn clean package
```

## Maven

**Repository:**
```xml
    <repository>
        <id>astralbe-repository-releases</id>
        <name>AstralBE Repository</name>
        <url>http://repo.astralbe.net/releases</url>
    </repository>
    <repository>
        <id>astralbe-repository-snapshots</id>
        <name>AstralBE Repository</name>
        <url>http://repo.astralbe.net/snapshots</url>
    </repository>
```

**Dependency:**

```xml
    <dependency>
        <groupId>app.reformcloud</groupId>
        <!-- replace with needed artifact for example 'embedded' -->
        <artifactId>api</artifactId>
        <version>1.0.0-pre.1-dev</version>
        <scope>provided</scope>
    </dependency>
```

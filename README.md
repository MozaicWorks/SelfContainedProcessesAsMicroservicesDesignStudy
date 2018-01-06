# Self-contained processes instead of microservices - a design study

Note: this is still a work in progress

## Rationale

### Microservices are another iteraction of the UNIX processes. So why don't we use them instead?

Microservices are the newest way of creating modular architecture. Yet, microservices are in principle another iteration of the UNIX processes. 

>> Write programs that do one thing and do it well.
   Write programs to work together.
   Write programs to handle text streams, because that is a universal interface.

I was therefore curious to try using simple UNIX processes instead of long-running microservices. Here are a few reasons:

* **A service is just an optimization of an executable**. Services cache certain things (e.g. database connections) and therefore work faster from a certain load up. But before we reach that load, why do we need to keep them running?
* The way we write microservices today is pretty bulky. The typical use is a REST interface over HTTP, a protocol known for its latency. It makes me wonder: is the start-up time of a process really that slow compared to the HTTP latency? How about RAM usage? Remember, if it's a service it needs an http server.
* I understand that the likes of Netflix or Amazon would need continuously running services for their loads. But I don't think that's true of a startup, for example. Once the processes are written, it should be fairly easy to turn them into services. 
* Executables scale up just as well. I can run as many processes as the OS and the database allows me to run, no problems. 

I tried therefore to strip down everything that's not needed for such a process. I ended up with a groovy script, since it's very easy to write one and it's difficult to manage a long one - which is an advantage in this case.

I was also pleasantly surprised to find out that groovy has a built-in option for running a script as a socket server.

### Self-contained executables

Traditionally, we have separated test code from production code, and the program from its setup, backup, restore and other operational needs. But do we have a good reason to do this? Or is it just due to historical reasons? 

Therefore, I decided to write the executable with built-in options for all these things. Indeed, if you run it without parameters you will get:

~~~~~~~~~~~~~~~

usage: createUser -[create|selfSetup|selfTest|selfCleanup]
-create,--create              creates a user
-help,--help                  show usage information
-selfBackup,--self-backup     backs up the database
-selfCleanup,--self-cleanup   drops database and user
-selfRestore,--self-restore   restores the last backup
-selfSetup,--self-setup       creates database and user
-selfTest,--self-test         runs self test

~~~~~~~~~~~~~~~

### What this doesn't solve

This is still a distributed system with eventual consistency. Therefore, it will have the same problems with distributed transactions, distributed logging, debugging etc.

## About design studies

Design studies are a specific type of deliberate practice. You can find more information here: [](https://github.com/MozaicWorks/SoftwareDesignStudies).

## Description

There are two parts to the study:

* a library, CommonDbLib, that offers certain services to the script
* one executable, CreateUser.groovy
* one test program, runMany.groovy, used for running as many scripts in parallel as you want

Everything is implemented in groovy. The library uses gradle for build. Minimal dependencies are used for everything.


Setup instructions:

* install mysql server
* install groovy-server using [sdkman](http://sdkman.io/install.html). It's not mandatory, but it speeds up the script considerably
* go to CommonDbLib. Run gradle build and copy the resulting library to the ~/.groovy/lib folder (create it if it's not there)
* create two configuration files: `adminSecrets.groovy` and `user.groovy`, with the mysql credentials for a user who can create databases and for the db user, in the following format:

~~~~
username='user'
password='password'
~~~~

* run groovy CreateUser.groovy or ./CreateUser.groovy (it requires groovy-server). On first run, it should install everything it needs, and it will take a bit of time. Once setup is done, everything should work faster.
* play with it and let me know what you think.

## What's next

* Add a runtime option for instrumentation (e.g. run-time performance reporting). I'm still thinking of solutions, but I think a simple one would be to implement a special program that can receive logs of different types. When running the process with a special option (e.g. --performance-report), it should start the program and report to it. This can be expanded for other reasons: security, debugging etc.
* Add more scripts and integrate them into a web application. I'm considering using a messaging system for separation (e.g. rabbitmq)
* Improve the database operations code. For example, use a special backup user, and ensure the most tight permissions are used for mysql
* Improve the management of secrets. This is a typical problem on Linux, with no clear answer. Most people just use files with restricted access; maybe there are better ways.
* Could we make the script install all dependencies, including the OS packages? Kind of like an embedded puppet script? It would be interesting to try


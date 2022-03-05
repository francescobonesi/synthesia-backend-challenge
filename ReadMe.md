# Synthesia Backend Tech Challenge

Solution for Synthesia Backend Tech Challenge: [link](https://www.notion.so/Synthesia-Backend-Tech-Challenge-52a82f750aed436fbefcf4d8263a97be)

## Overview

The solution is composed by a rest api service (Api-service), exposing `/crypto/sign` endpoint, 
and an asynchronous worker (Job-service) that 
integrates with Synthesia `/crypto/sign` and `crypto/verify` apis.

The two components communicate via RabbitMQ message broker 
using two queues in order to be decoupled, independent and asynchronous.

For each http request, the Api-service will publish a message in a queue that will be consumed by Job-service.
When the elaboration of signature is done by Job-service, it will publish the signature in a separate queue and
the Api-service will consume it and store it in a database.

The Job-service will also verify, with `crypto/verify` api, the correctness of signature before sending it.

For each http request, after sending the message, the Api-service will wait 2 seconds for the signature to arrive.
If signature arrives within 2 seconds, it will be provided in response.
Otherwise, if 2 seconds pass, then it will be returned to caller a courtesy message and a link to a single page application
that will display the signature as soon as the job finishes processing it.

The services are meant to scale independently between each other, due to decoupled architecture.

The single page application polls a second api from Api-service, `/signature/<identifier>`, in order to get 
stored signatures when available. This webapp will entertain the requester while they wait for signature.

## Prerequisites

* java jdk 17
* docker
* docker-compose
* node 13.12.0
* npm 6.14.4

## Components

* RabbitMQ [link](https://www.rabbitmq.com/): message broker used in this project for queues and asynchronous patterns
* MariaDB [link](https://mariadb.org/): Relational DB used for storing in progress jobs and signed messages
* React Single Page Application, called Waiting website: SPA with signature when available
* Spring Boot api service: rest api service exposing reliable endpoint for signature
* Spring Boot job service: asynchronous worker for obtaining signature as soon as available

## Structure

Here follows the tree of solution.

```
.
├── artillery
├── build.gradle
├── docker-compose.yml
├── gradle
├── gradlew
├── gradlew.bat
├── Makefile
├── ReadMe.md
├── settings.gradle
├── synthesia-api  ## Spring boot api service
│   ├── build
│   ├── build.gradle
│   ├── Dockerfile
│   └── src
├── synthesia-job  ## Spring boot job service
│   ├── build
│   ├── build.gradle
│   ├── Dockerfile
│   └── src
└── waiting-page  ## React single page app
    ├── Dockerfile
    ├── package.json
    ├── public
    └── src
```

## Setup

### Overview

For all setup, commands have been collected in a Makefile.

In order to make the solution work, the following steps must be followed:
* compile
* build
* initialization
* configuration
* run

In following sections each step is described.

### Compile

Use java 17 jdk to compile both applications. 

The command
```
make java_home=<path_to_jdk> compile
```
will compile and build jar file.

It uses gradle wrapper, configured inside project. 
This also executes application unit tests.

### Build docker images

The command
```
make build
```
will build 3 docker images:

* `francesco/api`  with spring boot api service
* `francesco/job`  with spring boot job service
* `francesco/waiting`  with waiting react spa

### Initialization

The command

```
make init
```

will run `docker-compose up` command in order to start containers from images:

* `rabbitmq`, with forwarded ports 5672 and 15672 
* `mariadb`, with forwarded port 3306 and database name `messagedb`
* `francesco/waiting`, with forwarded port 3000

### Configuration

The command

```
make configure
```

is mandatory in order to create the two queues used by services. 

Allow some time (few seconds) after `make init` before running this, because
it needs RabbitMQ to be up and running.

### Run services

The services can be run in two ways:
* launching them from terminal with `java`
* launching them as docker images

For the `java` option, you can use:
```
java -jar ./synthesia-api/build/libs/synthesia-api-0.0.1-SNAPSHOT.jar # api service
SYNTHESIA_KEY=<synthesia_api_key> java -jar ./synthesia-job/build/libs/synthesia-job-0.0.1-SNAPSHOT.jar # job-service
```

For the `docker` option, you can use:
```
make synthesia_key=<synthesia_api_key> run
```
or to run them in background:
```
make synthesia_key=<synthesia_api_key> run-background
```

This will start both services and docker will forward port 8080, 
where Api-service will be exposed.

The Synthesia api key refers to one given for accessing `hiring.synthesia.io` apis. 

## For cleaning at the end

When needed to clean all, stop docker containers of two services and also issue the command

```
make clean
```

to clean and remove containers and network created by docker-compose.

## Usage

### Invoke sign api

Api-service is exposed on port 8080 in http. It is possible to call it using curl or with swagger UI.

The curl is:
```
curl http://localhost:8080/crypto/sign?message=<messageText>
```

Otherwise, browsing `http://localhost:8080/swagger-ui.html`, it is possible
to use swagger ui to call the `crypto/sign` api.

### What to expect

When calling `crpyto/sign` api the results could be two:

* response within 2 seconds with signature filled:

```
{
    "signature": "<complete filled signature>",
    "info": "Here is your signature!",
    "waitingWebsite": null,
    "pollingPath": null
}
```

in this case the response contains signature, so everything is ok.


* response of ~2 seconds with signature absent

```
{
    "signature": null,
    "info": "We are working for you, don't worry!",
    "waitingWebsite": "http://localhost:3000/?identifier=<identifier>",
    "pollingPath": "/signature/<identifier>"
}
```

since signature process is ongoing, we provide two methods for caller to 
be aware of readiness of signature:

* [programmatic way] `pollingPath` field contains a path that can be used for polling the signature.
In this case, calling `GET http://localhost:8080/signature/<identifier>` you will get the signature, 
as soon as synthesia api will reply
* [waiting way] `waitingWebsite` field contains the path to react waiting single page application.
As soon as synthesia api will reply, you will see signature in the page.

## Note on storage

In mariadb database, requested messages and obtained signatures will be stored.

This storage has been used for two reasons:
* avoid to request same message many times, when still waiting for signature
* get signatures for already "signed" messages 


## Performance tests

For performance tests, `artillery` ([link](https://www.artillery.io/)) could be used.

If not installed:
```
npm -g i artillery
```

Then, it is possible to run performance test in this way:

```
cd artillery
artillery run config.yml -o output.json
artillery report output.json  ## that produces output.json.html readable report
```
or simply
```
make performance-test
```

This test will execute 3 phases:
* Warm up (60 req per min) for 60 seconds
* Ramp up load (from 60 to 300 req per min) in 60 seconds
* Sustained load (300 req per min) for 180 seconds

As for messages, lines of file `keyword.csv` have been used.
This file contains randomly generated messages.

## "Same message many times" test

For simulating the sending of the same message many times, always using `artillery`:

```
cd artillery
artillery run single-message-many-times.yml -o smmt-output.json
artillery report smmt-output.json  ## that produces smmt-output.json.html readable report
```
or simply
```
make same-message-many-times
```

This test will execute 1 phase:
* High load (600 req per min) for 180 seconds

This test will issue always the same message but the job will only be triggered the first time. If the signature 
is given immediately, then all following requests will find the saved value in db. Otherwise, if signature 
is not given and needs time, all requests will exceed 2 seconds but never issue the message, already queued.


## What if we were in AWS world

The developed solution is an on-premise one, but adaptable to AWS (or generally cloud) scenario.

* AWS SQS instead of RabbitMQ for queue management
* AWS RDS instead of MariaDB 
* Spring Boot api service: deployed and exposed as AWS Lambda through AWS API Gateway, 
with adaptation and java native compilation (for optimizing start up times)
* Spring Boot job service: deployed as ECS service, because takes too much time to be deployed as Lambda.
* React single page app: deployed as hosted static website on AWS S3, with AWS Cloudfront on top if needed


## Author

Francesco Bonesi

<francesco.bonesi90@gmail.com>

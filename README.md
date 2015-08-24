# HTWplus

HTWplus is a straightforward and lightweight social network. It offers essential social networking functionalities: Friendships, groups, a personal newsfeed and file sharing. It is built with the Play Framework 2, PostgreSQL and Elasticsearch.

## Installation

### Requirements

* JDK 8
* Activator (https://www.playframework.com/download)
* Git
* Node.js
* PostgreSQL
* Elasticsearch

### Setup

* Clone this repository
* Copy conf/application.sample.conf to application.conf
* Edit the application.conf
 * Provide database settings
 * Provide media.path and media.tempPath
* Copy conf/META-INF/persistence.sample.xml to persistence.xml
* Open the repository directory in shell
* Install Node.js packages with `npm install`
* Install frontend dependencies with Bower
 * `cd public`
 * `..\node_modules\.bin\bower install`

### Run

* Open the repository directory in shell
* Execute `activator run` (`activator -jvm-debug 9999` for debug mode)
* for HTTPS support during development also add the flag `-Dhttps.port=9443`, note that generating the certificate might
 take some time at first. Also, you might have to import this self-signed certificate within your tools, such as Postman.
* Browse to http://localhost:9000/
* Login as admin (user: admin@htwplus.de, password: 123456)

### Initialize Elasticsearch

* Browse to http://localhost:9000/admin/indexing
* Execute all options, but DELETE Index

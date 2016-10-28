# HTWplus

HTWplus is a straightforward and lightweight social network. It offers essential social networking functionalities: Friendships, groups, a personal newsfeed and file sharing. It is built with the Play Framework 2, PostgreSQL and Elasticsearch.

## Installation

### Requirements

* JDK 8
* Play 2.4.6 (activator 1.3.7) (https://downloads.typesafe.com/typesafe-activator/1.3.7/typesafe-activator-1.3.7-minimal.zip)
* Git
* Node.js
* PostgreSQL 9.3 or higher
* Elasticsearch 2.1 - 2.4.1

### Setup

* Clone this repository
* Copy conf/application.sample.conf to conf/application.conf
* Edit the application.conf
 * Provide database settings
 * Provide media.path, media.tempPath and media.fileStore
* Open the repository directory in shell
* Install Node.js packages with `npm install`
* Install frontend dependencies with Bower
 * `cd public`
 * `..\node_modules\.bin\bower install`

### Run

* Open the repository directory in shell
* Execute `activator run` (`activator -jvm-debug 9999` for debug mode)
* Browse to http://localhost:9000/
* Login as admin (user: admin@htwplus.de, password: 123456)

### Initialize Elasticsearch

* Browse to http://localhost:9000/admin/indexing
* Execute all options, but DELETE Index

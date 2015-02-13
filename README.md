# HTWplus

HTWplus is a straightforward and lightweight social network. It offers essential social networking functionalities: Friendships, groups, a personal newsfeed and file sharing. It is build with the Play Framework 2, PostgreSQL and Elasticsearch. 

## Installation

### Requirements

* JDK 7 or higher
* Activator (https://www.playframework.com/download)
* Git
* PostgreSQL
* Elasticsearch
* 

### Setup

* Clone this repository
* Copy conf/application.sample.conf to application.conf
* Edit the application.conf
 * Provide database settings
 * Provide media.path and media.tempPath
* Copy conf/META-INF/persistence.sample.xml to persistence.xml

### Run

* Open the repository directory in shell
* Execute `activator run` (`activator -jvm-debug 9999` for debug mode)
* Browse to http://localhost:9000/
* Login as admin (user: admin@htwplus.de, password: 123456)

### Initialize Elasticsearch

* Browse to http://localhost:9000/admin/indexing
* Excecute all Options, but DELETE Index

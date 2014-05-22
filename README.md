HTWplus
=====================================

How to start a local instance

preparations
- install git
- install play 2.2.3
- install postgresql oder mysql database

setup
- clone htwplus repository
- rename conf/application.sample.conf to application.conf
- rename conf/META-INF/persistence.sample.xml to persistence.xml
- edit application.conf 
  - change database settings
- edit persistence.xml
  - if you are running a unix based OS, change 'hibernate.search.default.indexBase' value to an absolute path

start htwplus
- open a shell
- navigate to repository
- type 'play run'
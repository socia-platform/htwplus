HTWplus
=====================================

**How to start a local instance**

**preparations**
- install git (http://git-scm.com/downloads)
- install activator (https://www.playframework.com/download) 
- install postgresql (http://www.postgresql.org/download/) or mysql (http://dev.mysql.com/downloads/) database

**setup**
- clone htwplus repository
- rename conf/application.sample.conf to application.conf
- rename conf/META-INF/persistence.sample.xml to persistence.xml
- edit application.conf 
  - change database settings
  - change media.path and media.tempPath
  - edit 'General Settings' if you want

**start htwplus**
- open a shell
- navigate to your htwplus repository
- type 'activator' or 'activator -jvm-debug 9999' for debug mode

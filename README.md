ProxyChain
==========

A Java project aimed at injecting behavior into black-box object systems (JDBC drivers, e.g.)


This README and the whole project is TBD, however I'm hoping to use a Builder pattern to do stuff like this:

x.whenReturns(Connection.class).whenMethodCalled(Connection.getMethod("getMetaData")).delegateTo(myObject)

x.whenReturns(Connection.class).whenMethodThrows(SQLException.class)

x.whenReturns(Connection.class).whenReturns(DatabaseMetaData.class)

x.use(new Driver())


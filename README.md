## Overview

Home for a set of utilities useful when working with JMS.

There are now two utilities 

##### JmsBridgePerformance & JmsBridgeReliability

Given two queues, put messages on the one queue and then try to read messages of the output queue. Correlate using the specified header, and measure throughput and latency (performance) or compare each message sent or received to check for duplications and lost messages (JmsBridgeReliability) .

Using the same queue as input and output measures the performance/reliability of the ActiveMQ server itself in a very simple scenario.
The reliability also includes a timer since, by definition, not all messages may be received.

Number of consumers can be tuned (see config.properties). Don't expect this to matter much in persistent messaging scenarios, as disk limits faster than number of consumers.

See the properties file to select the message to put on the queue, and how the header is set for correlation.

## Todo
The thread handling in the case of termination after a time is heavy handed in that it exits.  This could be more elegant.
The reliability code has been separated into a file for each class to allow for re-use.  The performance code should be rewritten to use the common libraries. 

## Usage

Download:

	git clone http://github.com/sixtree/jms-utils

Package up with Maven:

	mvn clean package

Check out config.properties and modify as required (only one kind of test supported right now).

Run the test:

	java -cp target/jms-utils-1.0-SNAPSHOT-jar-with-dependencies.jar au.com.sixtree.util.jms.JmsBridgePerformance
	java -cp target/jms-utils-1.0-SNAPSHOT-jar-with-dependencies.jar au.com.sixtree.util.jms.JmsBridgeReliability

Alternatively, provide a different properties file on the command line:

	java -cp target/jms-utils-1.0-SNAPSHOT-jar-with-dependencies.jar au.com.sixtree.util.jms.JmsBridgePerformance customPropertiesFile.properties
	java -cp target/jms-utils-1.0-SNAPSHOT-jar-with-dependencies.jar au.com.sixtree.util.jms.JmsBridgeReliability customPropertiesFile.properties
	
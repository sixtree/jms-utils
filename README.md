## Overview

Home for a set of utilities useful when working with JMS.

As of now, working on a single one only.

##### JmsBridgePerformance

Given two queues, put messages on the one queue and then try to read messages of the output queue. Correlate using the specified header, and measure throughput and latency.

Using the same queue as input and output measures the performance of the ActiveMQ server itself in a very simple scenario.

Number of consumers can be tuned (see config.properties). Don't expect this to matter much in persistent messaging scenarios, as disk limits faster than number of consumers.

Currently needs work in defining the actual message to put on the queue, and how the header is set for correlation.

## Usage

Download:

	git clone http://github.com/sixtree/jms-utils

Package up with Maven:

	mvn clean package

Check out config.properties and modify as required (only one kind of test supported right now).

Run the test:

	java -cp target/jms-utils-1.0-SNAPSHOT-jar-with-dependencies.jar au.com.sixtree.util.jms.JmsBridgePerformance

Alternatively, provide a different properties file on the command line:

	java -cp target/jms-utils-1.0-SNAPSHOT-jar-with-dependencies.jar au.com.sixtree.util.jms.JmsBridgePerformance customPropertiesFile.properties

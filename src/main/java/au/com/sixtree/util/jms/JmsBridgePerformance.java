package au.com.sixtree.util.jms;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.commons.io.FileUtils;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;

public class JmsBridgePerformance {
    public static void main(String[] args) throws Exception {
		Properties prop = new Properties();
		prop.load(new FileInputStream(args.length > 0 ? args[0] : "config.properties"));
		String server = prop.getProperty("server", "tcp://localhost:61616");
		int numMessages = Integer.valueOf(prop.getProperty("messages", "1000"));
		int numConsumers = Integer.valueOf(prop.getProperty("consumers", "1"));
		int sleepTime = Integer.valueOf(prop.getProperty("sleepTime", "0"));
		String header = prop.getProperty("header");
		Boolean persistent = Boolean.valueOf(prop.getProperty("persistent", "true"));
		String queueIn = prop.getProperty("queue.in");
		String queueOut = prop.getProperty("queue.out");
		String messageFilename = prop.getProperty("messageFile");
		File file = new File(messageFilename);
		String messageContents = FileUtils.readFileToString(file);
		String resultsFilename = prop.getProperty("detailedResultsFile");
		

		System.out.println(messageContents);
		Monitor monitor = new Monitor(numMessages);

    	for (int i = 1; i <= numConsumers; i++){
    		thread(new Consumer(monitor, server, queueOut, header));
    	}

        thread(new Producer(monitor, server, queueIn, header, numMessages, persistent, sleepTime, messageContents));

        monitor.waitUntilAllReceived();
        monitor.printResults(resultsFilename);
    }

    public static void thread(Runnable runnable) {
        Thread brokerThread = new Thread(runnable);
        brokerThread.start();
    }

    public static Connection getConnection(String server) throws Exception{
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(server);
        Connection connection = connectionFactory.createConnection();
        return connection;
    }

    public static class Monitor {
    	int numMessages;
    	AtomicInteger messagesReceived = new AtomicInteger(0);
    	Long startTime;
    	Long endTime;
    	ConcurrentHashMap<String,Long> results = new ConcurrentHashMap<>();
    	CountDownLatch latch = new CountDownLatch(1);

    	public Monitor(int numMessages) {
    		this.numMessages = numMessages;
    	}

    	public void start() {
    		startTime = System.nanoTime();
    	}

    	public void reportMessageSent(String header) {
    		results.put(header, System.nanoTime());
    	}

    	public void reportMessageReceived(String header) {
			results.put(header, System.nanoTime() - results.get(header));
			int messages = messagesReceived.incrementAndGet();

    		if (messages == numMessages) {
    			endTime = System.nanoTime();
    			latch.countDown();
    		}			
    	}

    	public void waitUntilAllReceived() throws Exception {
    		latch.await();
    	}

    	public void printResults(String resultsFilename) throws IOException {
	        double processedTimeMs = (endTime - startTime)/1000000;
	        double messagesPerSecond = Math.round((1000/processedTimeMs) * numMessages);
	        double averageLatencyMs = calculateAverage(results.values())/1000000;

	        System.out.println(numMessages + " messages were processed in " + processedTimeMs + "ms (" + messagesPerSecond + "/sec)");
			System.out.println("Average latency per message was " + averageLatencyMs + "ms");
			
			if(resultsFilename != null) {
				PrintStream ResultsStream = new PrintStream(new File(resultsFilename));
				ResultsStream.println("ESB tracking ID, Time (nanoseconds)");
				
				for(Map.Entry<String,Long> entry: results.entrySet()){
					ResultsStream.println(entry.getKey()+ ", "+ entry.getValue());
				}
				ResultsStream.flush();
				ResultsStream.close();	
				System.out.println("Results written to " + resultsFilename);
			}
    	}

	    private double calculateAverage(Collection<Long> values) {
		    Long sum = new Long(0);
		    if(!values.isEmpty()) {
		    	for (Long value : values) {
		        	sum += value;
		    	}
		    	return sum.doubleValue() / values.size();
		  	}
		  	return sum;
		}
    }

    public static class Producer implements Runnable {
    	Monitor monitor;
    	String server;
    	String queue;
    	String jmsHeader;
    	int numMessages;
    	int sleepTime;
    	boolean persistent;
    	String tradeMessage;

    	public Producer(Monitor monitor,
    			String server,
    			String queue,
    			String jmsHeader,
    			int numMessages,
    			boolean persistent,
    			int sleepTime,
    			String tradeMessage) {
    		this.monitor = monitor;
    		this.server = server;
    		this.queue = queue;
    		this.jmsHeader = jmsHeader;
    		this.numMessages = numMessages;
    		this.persistent = persistent;
    		this.sleepTime = sleepTime;
    		this.tradeMessage = tradeMessage;
    	}

        public void run() {
            try {
                Connection connection = JmsBridgePerformance.getConnection(server);
                connection.start();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Destination destination = session.createQueue(queue);
                MessageProducer producer = session.createProducer(destination);
                producer.setDeliveryMode(persistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT);

                monitor.start();
                

                for (int i = 1; i <= numMessages; i++) {
                	String header = String.valueOf(i);
                	Message message = session.createTextMessage(tradeMessage);
                	message.setStringProperty(jmsHeader, header);

                	monitor.reportMessageSent(header);                	

                	producer.send(message);
                	Thread.sleep(sleepTime);
                }
 
                session.close();
                connection.close();
            }
            catch (Exception e) {
                System.out.println("Producer error: " + e);
                e.printStackTrace();
            }
        }
    }

    public static class Consumer implements Runnable, ExceptionListener {
    	Monitor monitor;
    	String server;
    	String queue;
    	String jmsHeader;

    	public Consumer(Monitor monitor, String server, String queue, String jmsHeader) {
    		this.monitor = monitor;
    		this.server = server;
    		this.queue = queue;
    		this.jmsHeader = jmsHeader;
    	}

    	private class ConsumerListener implements MessageListener {
    		public void onMessage(Message message) {
    			try {
                    String header = message.getStringProperty(jmsHeader);
                    monitor.reportMessageReceived(header);
               	} catch (Exception e) {
	                System.out.println("Listener error: " + e);
	                e.printStackTrace();
            	}
    		}
    	}

    	public void run() {
            try {
                Connection connection = JmsBridgePerformance.getConnection(server);
                connection.start();
                connection.setExceptionListener(this);
 
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                Destination destination = session.createQueue(queue);
 
                MessageConsumer consumer = session.createConsumer(destination);
                consumer.setMessageListener(new ConsumerListener());
 
                monitor.waitUntilAllReceived();
 
                consumer.close();
                session.close();
                connection.close();
            } catch (Exception e) {
                System.out.println("Consumer error: " + e);
                e.printStackTrace();
            }
        }
 
        public synchronized void onException(JMSException e) {
            System.out.println("Consumer JMS error: " + e);
            e.printStackTrace();
        }
    }
}

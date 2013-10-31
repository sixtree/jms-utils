package au.com.sixtree.util.jms;

import org.apache.commons.io.FileUtils;

import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;

public class JmsBridgeReliability {
    public static void main(String[] args) throws Exception {
		Properties prop = new Properties();
		prop.load(new FileInputStream(args.length > 0 ? args[0] : "config.properties"));
		String server = prop.getProperty("server", "tcp://localhost:61616");
		int numMessages = Integer.valueOf(prop.getProperty("messages", "1000"));
		int numConsumers = Integer.valueOf(prop.getProperty("consumers", "1"));
		int sleepTime = Integer.valueOf(prop.getProperty("sleepTime", "0"));
		final int finalWaitTime = Integer.valueOf(prop.getProperty("finalWaitTime", "5000"));
		String header = prop.getProperty("header");
		Boolean persistent = Boolean.valueOf(prop.getProperty("persistent", "true"));
		String queueIn = prop.getProperty("queue.in");
		String queueOut = prop.getProperty("queue.out");
		String messageFilename = prop.getProperty("messageFile");
		String messageContents = "test";
		if(messageFilename != null)
		{
			File file = new File(messageFilename);
			messageContents = FileUtils.readFileToString(file);
		}
		String resultsFilename = prop.getProperty("detailedResultsFile");
		
        final Thread mainThread = Thread.currentThread();

        Thread interruptingThread = new Thread(new Runnable() {
            @Override public void run() {
                // Let the main thread start to sleep
                try {
                    Thread.sleep(finalWaitTime);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                mainThread.interrupt();
            }
        });

        interruptingThread.start();

		System.out.println(messageContents);
		JmsBridgeMonitor monitor = new JmsBridgeMonitor(numMessages);

    	for (int i = 1; i <= numConsumers; i++){
    		thread(new JmsBridgeConsumer(monitor, server, queueOut, header ));
    	}

        thread(new JmsBridgeProducer(monitor, server, queueIn, header, numMessages, persistent, sleepTime, messageContents));

        try {	

        	monitor.waitUntilAllReceived();
        }
        catch (InterruptedException e)
        {
        	System.out.println("Call Timed out - " + numMessages + " expected but received " + monitor.messagesReceived);
        }

        monitor.printResults(resultsFilename);
        
        System.exit(0);
    }

    public static void thread(Runnable runnable) {
        Thread brokerThread = new Thread(runnable);
        brokerThread.start();
    }


 
}

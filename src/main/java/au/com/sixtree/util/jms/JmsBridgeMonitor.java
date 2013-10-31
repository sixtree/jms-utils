package au.com.sixtree.util.jms;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class JmsBridgeMonitor {
	int numMessages;
	AtomicInteger messagesReceived = new AtomicInteger(0);
	Long startTime;
	Long endTime;
	ConcurrentHashMap<String,Long> results = new ConcurrentHashMap<>();
	CountDownLatch latch = new CountDownLatch(1);

	public JmsBridgeMonitor(int numMessages) {
		this.numMessages = numMessages;
	}

	public void start() {
		startTime = System.nanoTime();
	}

	public void reportMessageSent(String header) {
		results.put(header, 0L);
	}

	public void reportMessageReceived(String header) {
		long currentCount = results.get(header);
		
		results.put(header, currentCount+1);

		// ignore repeated messages
		if(currentCount==0)
		{
			int messages = messagesReceived.incrementAndGet();

    		if (messages == numMessages) {
    			endTime = System.nanoTime();
    			latch.countDown();
    		}			
		}
	}

	public void waitUntilAllReceived() throws Exception {
		latch.await();
	}
	

	public void printResults(String resultsFilename) throws IOException {
		if( endTime == null )
		{
			endTime  = System.nanoTime();
		}
        double processedTimeMs = (endTime - startTime)/1000000;
        long mismatches = checkForExceptions(results.values());

        System.out.println(messagesReceived + " messages were processed in " + processedTimeMs + "ms");
		System.out.println("There were " + mismatches + " mismatches");
		if(mismatches > 0 && resultsFilename != null)
		{
			System.out.println("See the results file for details");
		}
		
		if(resultsFilename != null) {
			PrintStream ResultsStream = new PrintStream(new File(resultsFilename));
			ResultsStream.println("ESB tracking ID, messages received");
			
			for(Map.Entry<String,Long> entry: results.entrySet()){
				ResultsStream.println(entry.getKey()+ ", "+ entry.getValue());
			}
			ResultsStream.flush();
			ResultsStream.close();	
			System.out.println("Results written to " + resultsFilename);
		}
	}

    private Long checkForExceptions(Collection<Long> values) {
	    Long count = new Long(0);
	    if(!values.isEmpty()) {
	    	for (Long value : values) {
	        	if(value != 1)
	        		count++;
	    	}
	    	return count;
	  	}
	  	return count;
	}
}


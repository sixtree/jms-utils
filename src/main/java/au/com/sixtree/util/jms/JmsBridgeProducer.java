package au.com.sixtree.util.jms;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;

import au.com.sixtree.util.jms.JmsBridgeMonitor;

public class JmsBridgeProducer implements Runnable {
	JmsBridgeMonitor monitor;
	String server;
	String queue;
	String jmsHeader;
	int numMessages;
	int sleepTime;
	boolean persistent;
	String tradeMessage;

	public JmsBridgeProducer(JmsBridgeMonitor monitor, String server,
			String queue, String jmsHeader, int numMessages,
			boolean persistent, int sleepTime, String tradeMessage) {
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
			Connection connection = JmsBridgeConnection.getConnection(server);
			connection.start();
			Session session = connection.createSession(false,
					Session.AUTO_ACKNOWLEDGE);
			Destination destination = session.createQueue(queue);
			MessageProducer producer = session.createProducer(destination);
			producer.setDeliveryMode(persistent ? DeliveryMode.PERSISTENT
					: DeliveryMode.NON_PERSISTENT);

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
		} catch (Exception e) {
			System.out.println("Producer error: " + e);
			e.printStackTrace();
		}
	}
}

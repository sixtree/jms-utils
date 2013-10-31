package au.com.sixtree.util.jms;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;

import au.com.sixtree.util.jms.JmsBridgeMonitor;

public class JmsBridgeConsumer implements Runnable, ExceptionListener {
	JmsBridgeMonitor monitor;
	String server;
	String queue;
	String jmsHeader;

	public JmsBridgeConsumer(JmsBridgeMonitor monitor, String server,
			String queue, String jmsHeader) {
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
			Connection connection = JmsBridgeConnection.getConnection(server);
			connection.start();
			connection.setExceptionListener(this);

			Session session = connection.createSession(false,
					Session.AUTO_ACKNOWLEDGE);
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

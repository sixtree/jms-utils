package au.com.sixtree.util.jms;

import javax.jms.Connection;

import org.apache.activemq.ActiveMQConnectionFactory;

public class JmsBridgeConnection {

    public static Connection getConnection(String server) throws Exception{
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(server);
        Connection connection = connectionFactory.createConnection();
        return connection;
    }

}

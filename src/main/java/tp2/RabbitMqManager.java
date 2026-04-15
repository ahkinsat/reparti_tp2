package tp2;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RabbitMqManager {
    private static Connection connection;

    private static synchronized Connection getConnection() throws IOException, TimeoutException {
        if (connection == null || !connection.isOpen()) {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(Config.rmqHost());
            factory.setPort(Config.rmqPort());
            factory.setUsername(Config.rmqUser());
            factory.setPassword(Config.rmqPassword());
            connection = factory.newConnection();
        }
        return connection;
    }

    public static Channel createChannel() throws IOException, TimeoutException {
        return getConnection().createChannel();
    }

    public static void declareQueue(Channel channel) throws IOException {
        channel.queueDeclare(Config.rmqQueue(), true, false, false, null);
    }
}

import com.rabbitmq.client.*;

import javax.print.Doc;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Doctor {

    private static final String EXCHANGE_NAME = "Hospital";
    private static final String ADMIN_INFO_ROUTINGKEY = "INFO";
    private String QUEUE_NAME = "Default";

    private BufferedReader userInput;

    private String name = "DefaultDoc";

    private Connection connection;
    private Channel channel;
    private Consumer consumer;


    public Doctor() {
        userInput = new BufferedReader(new InputStreamReader(System.in));

        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");

        try {
            System.out.print("Type doc name:");
            name = userInput.readLine();

            connection = connectionFactory.newConnection();
            channel = connection.createChannel();

            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);
            channel.queueDeclare(name, false, false, false, null);
            channel.queueBind(name, EXCHANGE_NAME, "*.result."+name);
            initConsumer(name, true);

            //Bindowanie do admina
            String admInfoQueue = channel.queueDeclare().getQueue();
            channel.queueBind(admInfoQueue, EXCHANGE_NAME, ADMIN_INFO_ROUTINGKEY);
            initConsumer(admInfoQueue, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initConsumer(String queueName, Boolean autoAck) throws IOException{
        consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println("Received: " + message + "\t|RoutingKey: " + envelope.getRoutingKey());
            }
        };
        channel.basicConsume(queueName, autoAck, consumer);
        System.out.println("initialized.");
    }

    public static void main(String[] args) {

        Doctor d = new Doctor();
        try {
            System.out.println("To order examination type: [elbow|knee|hip]-<clientName>");
            while(true) {
                System.out.print("> ");
                String examinationToDo = d.userInput.readLine();
                String[] tokens = examinationToDo.split("-");
                System.out.println("examination."+tokens[0].toUpperCase());
                d.channel.basicPublish(EXCHANGE_NAME, tokens[0].toUpperCase()+".examination."+d.name, null, tokens[1].getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

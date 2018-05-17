import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.*;

public class Technican {
    private static final String EXCHANGE_NAME = "Hospital";
    private static final String ADMIN_INFO_ROUTINGKEY = "INFO";
    private String QUEUE_NAME = "Kappa";

    private Connection connection;
    private Channel channel;
    private Consumer consumer;
    private final List<Specialization> specializations;

    public Technican() {
        specializations = generateSpecs();

        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");

        try {
            connection = connectionFactory.newConnection();
            channel = connection.createChannel();
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);
            channel.basicQos(1);

//            Bind for specs
            for(Specialization spec : specializations){
                String routingKey = spec.toString()+".examination.*";
                String specQueue = channel.queueDeclare(spec.toString(), false, false, false, null).getQueue();
                System.out.print("Bind with key: " + routingKey + '\n');
                channel.queueBind(specQueue, EXCHANGE_NAME, routingKey);
                initConsumer(specQueue);
            }

            initAdmConsumer();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.out.println("Specs: ");
        Technican t = new Technican();
        System.out.println("Wait for msg");
    }

    public void initAdmConsumer() throws Exception {
        //Bindowanie do admina
        String admInfoQueue = channel.queueDeclare().getQueue();
        channel.queueBind(admInfoQueue, EXCHANGE_NAME, ADMIN_INFO_ROUTINGKEY);
        Consumer admInfoConsumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println("Admin: " + message);
            }
        };
        channel.basicConsume(admInfoQueue, true, admInfoConsumer);
    }

    public void initConsumer(String queueName){
        initConsumer(queueName, false);
    }

    public void initConsumer(String queueName, Boolean autoAck) {
        consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                try {
                    String message = new String(body, "UTF-8");
                    System.out.println("Received: " + message +
                                    "\t|RoutingKey: " + envelope.getRoutingKey() +
                                    "\t|Reply RoutingKey: "+envelope.getRoutingKey().replace("examination", "result"));

                    channel.basicPublish(EXCHANGE_NAME,
                                        envelope.getRoutingKey().replace("examination", "result"),
                                        null,
                                        (message+" DONE").getBytes()
                    );

                    channel.basicAck(envelope.getDeliveryTag(),false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        try {
            channel.basicConsume(queueName, autoAck, consumer);
        }catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("initialized.");
    }

    private List<Specialization> generateSpecs() {
        List<Specialization> specs = new LinkedList();
        Specialization s = Specialization.getRandomSpecialization();
        specs.add(s);
        do {
            s = Specialization.getRandomSpecialization();
        } while (specs.contains(s));
        specs.add(s);
        return specs;
    }

    public void closeConnection() {
        if( connection.isOpen() ) {
            try {
                channel.close();
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

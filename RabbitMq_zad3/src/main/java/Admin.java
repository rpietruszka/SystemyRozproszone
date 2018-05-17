import com.rabbitmq.client.*;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Admin {

    private Channel channel;
    private static final String EXCHENGE_NAME = "Hospital";
    private static final String ADMIN_INFO_ROUTINGKEY = "INFO";
    private String queue;
    private BufferedReader userInput;

    public static void main(String[] args) {
        try {
            Admin a = new Admin();
            a.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Admin() throws Exception{
        System.out.println("Creating admin");

        userInput = new BufferedReader(new InputStreamReader(System.in));

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();

        channel = connection.createChannel();
        channel.exchangeDeclare(EXCHENGE_NAME, BuiltinExchangeType.TOPIC);

        queue = channel.queueDeclare().getQueue();
        channel.queueBind(queue, EXCHENGE_NAME, "#");
        initConsumer(queue, true);
    }

    public void initConsumer(String queueName, Boolean autoAck) throws IOException {
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println("Received: " + message + "\t|RoutingKey: " + envelope.getRoutingKey());
            }
        };
        channel.basicConsume(queueName, autoAck, consumer);
        System.out.println("consumer initialized.");
    }


    public void run() {
        try {
            while(true) {
                System.out.print("adm_prompt> ");
                String prompt = userInput.readLine();

                if(prompt.equals("exit"))
                    break;

                System.out.println("Sending info to ALL\n" + prompt + "\n\n");
                channel.basicPublish(EXCHENGE_NAME, ADMIN_INFO_ROUTINGKEY, null, prompt.getBytes());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package iot;
import java.net.URI;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.qpid.jms.JmsConnection;
import org.apache.qpid.jms.JmsConnectionListener;
import org.apache.qpid.jms.message.JmsBytesMessage;
import org.apache.qpid.jms.message.JmsInboundMessageDispatch;
import org.apache.xml.security.utils.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmqpJavaClientDemo {

    private final static Logger logger = LoggerFactory.getLogger(AmqpJavaClientDemo.class);

    //ҵ�����첽�̳߳أ��̳߳ز������Ը�������ҵ���ص������������Ҳ�����������첽��ʽ������յ�����Ϣ��
    private final static ExecutorService executorService = new ThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors(),
        Runtime.getRuntime().availableProcessors() * 2, 60, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(50000));

    public static void main(String[] args) throws Exception {
        //����˵������μ��ĵ���AMQP�ͻ��˽���˵����
        String accessKey = "LTAI4GE1dZD5nCVrofFD7wZG";
        String accessSecret = "oyEGMLE82T6KPIlVUNCXNNkbvhAQfF";
        String consumerGroupId = "DEFAULT_GROUP";
        //iotInstanceId�������ʵ������дʵ��ID������ʵ��������ַ���""��
        String iotInstanceId = ""; 
        long timeStamp = System.currentTimeMillis();
        //ǩ��������֧��hmacmd5��hmacsha1��hmacsha256��
        String signMethod = "hmacsha1";
        //����̨����˶�����������״̬ҳ�ͻ���IDһ������ʾclientId������
        //����ʹ�û���UUID��MAC��ַ��IP��Ψһ��ʶ����ΪclientId������������ʶ��ͬ�Ŀͻ��ˡ�
        String clientId = "192.168.135.149-AC-ED-5C-7D-95-42";

        //UserName��װ��������μ��ĵ���AMQP�ͻ��˽���˵����
        String userName = clientId + "|authMode=aksign"
            + ",signMethod=" + signMethod
            + ",timestamp=" + timeStamp
            + ",authId=" + accessKey
            + ",iotInstanceId=" + iotInstanceId
            + ",consumerGroupId=" + consumerGroupId
            + "|";
        //password��װ��������μ��ĵ���AMQP�ͻ��˽���˵����
        String signContent = "authId=" + accessKey + "&timestamp=" + timeStamp;
        String password = doSign(signContent,accessSecret, signMethod);
        //����qpid-jms�Ĺ淶����װ����URL��
        //String connectionUrl = "failover:(amqps://${uid}.iot-amqp.${regionId}.aliyuncs.com:5671?amqp.idleTimeout=80000)"
        String connectionUrl = "failover:(amqps://1383336053315599.iot-amqp.cn-shanghai.aliyuncs.com:5671?amqp.idleTimeout=80000)"
            + "?failover.reconnectDelay=30";

        Hashtable<String, String> hashtable = new Hashtable<>();
        hashtable.put("connectionfactory.SBCF",connectionUrl);
        hashtable.put("queue.QUEUE", "default");
        hashtable.put(Context.INITIAL_CONTEXT_FACTORY, "org.apache.qpid.jms.jndi.JmsInitialContextFactory");
        Context context = new InitialContext(hashtable);
        ConnectionFactory cf = (ConnectionFactory)context.lookup("SBCF");
        Destination queue = (Destination)context.lookup("QUEUE");
        // Create Connection
        Connection connection = cf.createConnection(userName, password);
        ((JmsConnection) connection).addConnectionListener(myJmsConnectionListener);
        // Create Session
        // Session.CLIENT_ACKNOWLEDGE: �յ���Ϣ����Ҫ�ֶ�����message.acknowledge()��
        // Session.AUTO_ACKNOWLEDGE: SDK�Զ�ACK���Ƽ�����
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        connection.start();
        // Create Receiver Link
        MessageConsumer consumer = session.createConsumer(queue);
        consumer.setMessageListener(messageListener);
    }

    private static MessageListener messageListener = new MessageListener() {
        @Override
        public void onMessage(Message message) {
            try {
                //1.�յ���Ϣ֮��һ��ҪACK��
                // �Ƽ�����������Sessionѡ��Session.AUTO_ACKNOWLEDGE��������Զ�ACK��
                // ��������������Sessionѡ��Session.CLIENT_ACKNOWLEDGE������һ��Ҫ��message.acknowledge()��ACK��
                // message.acknowledge();
                //2.�����첽�����յ�����Ϣ��ȷ��onMessage������û�к�ʱ�߼���
                // ���ҵ�����ʱ���̹�������ס�̣߳����ܻ�Ӱ��SDK�յ���Ϣ��������ص���
                executorService.submit(() -> processMessage(message));
            } catch (Exception e) {
                logger.error("submit task occurs exception ", e);
            }
        }
    };

    /**
     * �����ﴦ�����յ���Ϣ��ľ���ҵ���߼���
     */
    private static void processMessage(Message message) {
        try {
            JmsBytesMessage bytesMessage = (JmsBytesMessage) message;
            byte[] body = bytesMessage.getBody(byte[].class);
            String content = new String(body);
            String topic = message.getStringProperty("topic");
            String messageId = message.getStringProperty("messageId");
            System.out.println("receive message"
                + ", topic = " + topic
                + ", messageId = " + messageId
                + ", content = " + content);
        } catch (Exception e) {
            logger.error("processMessage occurs error ", e);
        }
    }

    private static JmsConnectionListener myJmsConnectionListener = new JmsConnectionListener() {
        /**
         * ���ӳɹ�������
         */
        @Override
        public void onConnectionEstablished(URI remoteURI) {
            logger.info("onConnectionEstablished, remoteUri:{}", remoteURI);
        }

        /**
         * ���Թ�������Դ���֮����������ʧ�ܡ�
         */
        @Override
        public void onConnectionFailure(Throwable error) {
            logger.error("onConnectionFailure, {}", error.getMessage());
        }

        /**
         * �����жϡ�
         */
        @Override
        public void onConnectionInterrupted(URI remoteURI) {
            logger.info("onConnectionInterrupted, remoteUri:{}", remoteURI);
        }

        /**
         * �����жϺ����Զ������ϡ�
         */
        @Override
        public void onConnectionRestored(URI remoteURI) {
            logger.info("onConnectionRestored, remoteUri:{}", remoteURI);
        }

        @Override
        public void onInboundMessage(JmsInboundMessageDispatch envelope) {}

        @Override
        public void onSessionClosed(Session session, Throwable cause) {}

        @Override
        public void onConsumerClosed(MessageConsumer consumer, Throwable cause) {}

        @Override
        public void onProducerClosed(MessageProducer producer, Throwable cause) {}
    };

    /**
     * passwordǩ�����㷽������μ��ĵ���AMQP�ͻ��˽���˵����
     */
    private static String doSign(String toSignString, String secret, String signMethod) throws Exception {
        SecretKeySpec signingKey = new SecretKeySpec(secret.getBytes(), signMethod);
        Mac mac = Mac.getInstance(signMethod);
        mac.init(signingKey);
        byte[] rawHmac = mac.doFinal(toSignString.getBytes());
        return Base64.encode(rawHmac);
    }
}
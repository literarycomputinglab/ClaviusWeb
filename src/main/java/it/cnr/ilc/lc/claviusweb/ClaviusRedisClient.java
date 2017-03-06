/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.ilc.lc.claviusweb;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.pubsub.RedisPubSubAdapter;
import com.lambdaworks.redis.pubsub.StatefulRedisPubSubConnection;
import com.lambdaworks.redis.pubsub.api.sync.RedisPubSubCommands;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author simone
 */
public class ClaviusRedisClient {

    private static Logger log = LogManager.getLogger(ClaviusRedisClient.class);

    private static final String CHANNEL = "kgraph";

    RedisClient redisClient = RedisClient.create("redis://localhost:6379/");

    Set<StatefulRedisPubSubConnection<String, String>> s = Collections.synchronizedSet(new HashSet<>());

    public ClaviusRedisClient() {
        init();
    }

    private void init() {

        log.info("Initialize ClaviusRedisClient");

        StatefulRedisPubSubConnection<String, String> connection = null;

        s.add(redisClient.connectPubSub());

        synchronized (s) {
            
            Iterator<StatefulRedisPubSubConnection<String, String>> i = s.iterator(); // Must be in the synchronized block
            connection = i.next();
        }

        RedisPubSubAdapter rpsa = new RedisPubSubAdapter<String, String>() {

            ExecutorService executor = Executors.newFixedThreadPool(10);

            @Override
            public void message(String channel, String message) {
                log.debug("channel=(" + channel + "), message=(" + message + ")");
                executor.submit(() -> {
                    System.err.println(String.format(Thread.currentThread().getName() + " Channel: %s, Message: %s", channel, message));
                });
            }

            @Override
            public void unsubscribed(String channel, long count) {
                System.err.println(String.format(Thread.currentThread().getName() + " UNSUBSCRIBE Channel: %s, Message: %d", channel, count));
                try {
                    log.info("attempt to shutdown executor");
                    executor.shutdown();
                    executor.awaitTermination(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    log.warn("tasks interrupted");
                } finally {
                    if (!executor.isTerminated()) {
                        log.warn("cancel non-finished tasks");
                    }
                    executor.shutdownNow();
                    log.info("shutdown finished");
                }
            }
        };

        connection.addListener(rpsa);

        RedisPubSubCommands<String, String> sync = connection.sync();

        sync.subscribe(CHANNEL);
        log.info("Channel " + CHANNEL + " Subscribed");

    }

    public void close() {

        synchronized (s) {
            Iterator<StatefulRedisPubSubConnection<String, String>> i = s.iterator(); // Must be in the synchronized block
            while (i.hasNext()) {
                StatefulRedisPubSubConnection<String, String> c = i.next();
                c.sync().unsubscribe("kgraph");
                c.close();
            }
        }

        redisClient.shutdown();
    }

    public static void main(String[] args) throws InterruptedException {

        ClaviusRedisClient crc = new ClaviusRedisClient();

        RedisClient client = RedisClient.create("redis://localhost:6379/");
        StatefulRedisConnection<String, String> sender = client.connect();

        for (int i = 0;
                i < 100; i++) {
            sender.sync().publish("kgraph", "Message " + i);
            log.info(Thread.currentThread().getName() + " il sender ha mandato il " + i + " messaggio");

            Thread.sleep((long) Math.random() * 1000);

        }

        Thread.sleep(2200);
        sender.close();
        crc.close();
        client.shutdown();
        log.info(String.format(Thread.currentThread().getName() + " client.shutdown()"));

    }

}

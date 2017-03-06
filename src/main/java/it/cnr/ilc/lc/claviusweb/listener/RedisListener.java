/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.ilc.lc.claviusweb.listener;

import it.cnr.ilc.lc.claviusweb.ClaviusRedisClient;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author simone
 */
@WebListener
public class RedisListener implements ServletContextListener {

    private static Logger log = LogManager.getLogger(RedisListener.class);
    ClaviusRedisClient crc;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("RedisListener initialized");
        crc = new ClaviusRedisClient();

    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {

        try {
            if (null != crc) {
                crc.close();
                log.info("RedisListener is now closed");
            } else {
                log.warn("RedisListener is null!");
            }
        } catch (IllegalStateException e) {
            log.error("On close RedisListener: " + e.getMessage());
        }

        log.info("RedisListener Listener destroyed");
    }
}

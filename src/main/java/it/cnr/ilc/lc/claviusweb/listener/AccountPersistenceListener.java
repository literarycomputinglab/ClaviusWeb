/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.ilc.lc.claviusweb.listener;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author angelo
 */
@WebListener
public class AccountPersistenceListener implements ServletContextListener {

    private static EntityManagerFactory entityManagerFactory = null;
    private static Logger log = LogManager.getLogger(AccountPersistenceListener.class);

    public static synchronized EntityManager getEntityManager() throws Exception {

        log.info("entityManagerFactory is null? " + (null == entityManagerFactory));

        if (null == entityManagerFactory) {
            entityManagerFactory = Persistence.createEntityManagerFactory("clavius-account");
            log.info("entityManagerFactory is now open? " + entityManagerFactory.isOpen());
        } else if (!entityManagerFactory.isOpen()) {
            entityManagerFactory = Persistence.createEntityManagerFactory("clavius-account");
        }
        return entityManagerFactory.createEntityManager();

    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("Account Listener initialized");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
//        try {
//            if (null != entityManager) {
//                entityManager.close();
//                log.info("entityManager.close()");
//
//            } else {
//                log.warn("entityManager is null!");
//            }
//        } catch (IllegalStateException e) {
//            log.error("On close entityManager: " + e.getMessage());
//        }

        try {
            if (null != entityManagerFactory) {
                entityManagerFactory.close();
                log.info("entityManagerFactory.close()");
            } else {
                log.warn("entityManagerFactory is null!");
            }
        } catch (IllegalStateException e) {
            log.error("On close entityManagerFactory: " + e.getMessage());
        }

        log.info("Account Listener destroyed");
    }

}

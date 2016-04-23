/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.ilc.lc.claviusweb.listener;

import it.cnr.ilc.lc.claviusweb.ClaviusSearch;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Web application lifecycle listener.
 *
 * @author simone
 */
@WebListener
public class PersistenceListener implements ServletContextListener {

    private static EntityManagerFactory entityManagerFactory = null;
    private static Logger log = LogManager.getLogger(PersistenceListener.class);

    public static synchronized EntityManager getEntityManager() throws Exception {

        log.info("entityManagerFactory is null? " + (null == entityManagerFactory));
        
        if (null == entityManagerFactory) {
            entityManagerFactory = Persistence.createEntityManagerFactory("clavius");
            log.info("entityManagerFactory is now open? " + entityManagerFactory.isOpen());
        } else if (!entityManagerFactory.isOpen()) {
            entityManagerFactory = Persistence.createEntityManagerFactory("clavius");
        }
        return entityManagerFactory.createEntityManager();

    }

//    public static synchronized EntityManager getEntityManager2() throws Exception {
//
//        try {
//
//            log.info("entityManagerFactory is null? " + (null == entityManagerFactory));
//            if (null == entityManagerFactory) {
//                entityManagerFactory = Persistence.createEntityManagerFactory("clavius");
//                log.info("entityManagerFactory is now open? " + entityManagerFactory.isOpen());
//
//                if (null == entityManager) {
//                    entityManager = entityManagerFactory.createEntityManager();
//                    log.info("entityManager was null, is now open? " + entityManager.isOpen());
//
//                } else if (!entityManager.isOpen()) {
//                    entityManager = entityManagerFactory.createEntityManager();
//                    log.info("entityManager was close, is now open? " + entityManager.isOpen());
//                }
//            } else if (!entityManagerFactory.isOpen()) {
//                entityManagerFactory = Persistence.createEntityManagerFactory("clavius");
//                log.info("entityManagerFactory was close, is now open? " + entityManagerFactory.isOpen());
//
//                entityManager = entityManagerFactory.createEntityManager();
//                log.info("entityManager is now open? " + entityManager.isOpen());
//            } else if (null == entityManager) {
//                entityManager = entityManagerFactory.createEntityManager();
//                log.info("entityManager was null, is now open? " + entityManager.isOpen());
//
//            } else if (!entityManager.isOpen()) {
//                entityManager = entityManagerFactory.createEntityManager();
//                log.info("entityManager was close, is now open? " + entityManager.isOpen());
//            } else {
//                entityManager = entityManagerFactory.createEntityManager();
//                log.info("entityManagerFactory and entityManager are already open");
//            }
//            log.info("entityManager is create successfully");
//        } catch (Exception e) {
//            log.error(e.getMessage());
//            throw e;
//        }
//        return entityManager;
//    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("Listener initialized");
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

        log.info("Listener destroyed");
    }
}

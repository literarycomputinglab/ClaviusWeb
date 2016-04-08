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

/**
 * Web application lifecycle listener.
 *
 * @author simone
 */
public class PersistenceListener implements ServletContextListener {

    private static EntityManagerFactory entityManagerFactory;
    private static EntityManager entityManager;

    public static synchronized EntityManager getEntityManager() {
        if (null == entityManagerFactory) {
            entityManagerFactory = Persistence.createEntityManagerFactory("clavius");
            if (null == entityManager) {
                entityManager = entityManagerFactory.createEntityManager();
            }
        }
        return entityManager;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {

    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}

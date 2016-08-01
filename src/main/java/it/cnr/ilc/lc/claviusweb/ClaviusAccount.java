/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.ilc.lc.claviusweb;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import it.cnr.ilc.lc.claviusweb.entity.User;
import it.cnr.ilc.lc.claviusweb.listener.AccountPersistenceListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;

/**
 *
 * @author simone
 * @author angelo
 * @author davide
 */
@WebServlet(name = "ClaviusAccount", urlPatterns = {"/ClaviusAccount/*"})
public class ClaviusAccount extends HttpServlet {

    private static Logger log = LogManager.getLogger(ClaviusAccount.class);
    private static final Long INVALID_ACCOUNTID_VALUES = -1l;
    private static final String INVALID_USER_INFO = "FALSE";

    @Override
    public void init(ServletConfig config) throws ServletException {
        log.info("Clavius Account init");
    }

    @Override
    public void destroy() {
        log.info("Clavius Account destroy");
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

//        String ip = getClientIpAddr(request);
//        Gson gson = new Gson();
        String command = request.getPathInfo().substring(1);
        String json = readPost(request);

        /**
         * Comando di login: il client invoca la procedura con il path
         * http://url-di-claviusweb/ClaviusAccount/login
         */
        if ("login".equals(command)) {
            User u = null;
            // there is no control on a multiple login action

            try {
                u = new Gson().fromJson(json, User.class);
            } catch (JsonSyntaxException e) {
                log.info("login: malformed json", e);

            }

            if (null != u && null != u.getUsername() && null != u.getPassword()) {

                String username = u.getUsername();
                String password = u.getPassword();
                Long accountId = retrieve(username, password); // mettere una try catch??
                log.info(accountId);
                if (accountId != null && !accountId.equals(INVALID_ACCOUNTID_VALUES)) {
                    HttpSession session = request.getSession(true);
                    session.setAttribute("accountId", accountId);
                    u.setAccountID(accountId);
                } else {
                    if (null != request.getSession(false)) {
                        request.getSession().invalidate();
                    }
                    u.setAccountID(INVALID_ACCOUNTID_VALUES);
                }

            } else {
                log.warn("the user object has problem in its own instance: " + u.toString());
                nobodyloggedin(u);
            }

            response.getWriter().append(new Gson().toJson(u));
        }/**
         * Comando di logout: il client invoca la procedura con il path
         * http://url-di-claviusweb/ClaviusAccount/logout
         */
        else if ("logout".equals(command)) {
            User u = new User();
            HttpSession session = request.getSession(false);
            if (session == null) {
                // errore ci chiedono logout ma la sessione non esiste
                log.info("session already closed where loguout is requested");
            } else {
                session.invalidate();
                log.info("session invalidated");
            }

            nobodyloggedin(u);

            response.getWriter().append(new Gson().toJson(u));

        } /**
         * Comando di verifica login: il client invoca la procedura con il path
         * http://url-di-claviusweb/ClaviusAccount/getAccountId
         */
        else if ("getAccountId".equals(command)) {
            User u = new User();
            HttpSession session = request.getSession(false);
            if (session == null) {
                log.warn("Session doedn't exist!!!");

                nobodyloggedin(u);

            } else {
                Long accountId = (Long) session.getAttribute("accountId");
                if (accountId == null || accountId.equals(INVALID_ACCOUNTID_VALUES)) {
                    log.warn("Session is valid but nobody is logged in!!!");
                    nobodyloggedin(u);
                } else {
                    u = retrieveUserByAccountID(accountId);
                }
            }
            log.info(u);
            response.getWriter().append(new Gson().toJson(u));

        } /**
         * Comando di creazione utenti: il client invoca la procedura con il
         * path http://url-di-claviusweb/ClaviusAccount/createuser
         */
        else if ("createUser".equals(command)) {
            User u = null;
            try {
                u = new Gson().fromJson(json, User.class);

            } catch (JsonSyntaxException je) {
                log.info("malformed json in create user account", je);
            }

            Long uid = createUserAccount(u);
            response.getWriter().append(new Gson().toJson(uid));
        } else {
            User u = null;
            log.error("Command (" + command + ") is not valid");
            nobodyloggedin(u);
            response.getWriter().append(new Gson().toJson(u));
        }
    }

    private String readPost(HttpServletRequest request) throws IOException {
        try (BufferedReader reader = new BufferedReader(request.getReader())) {
            String line;
            StringBuilder builder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        }
    }

    /**
     *
     * @param username The name of the user
     * @param password The password of the user
     * @return the AccountID of the specified user or -1 if no user has found
     */
    private Long retrieve(String username, String password) {
        Long ret = INVALID_ACCOUNTID_VALUES;
        EntityManager entityManager = null;
        try {
            entityManager = AccountPersistenceListener.getEntityManager();
        } catch (Exception e) {
            log.warn("unable to create an Account Entity Manager", e);

        }

        if (null != entityManager) {
            log.info("created an Account Entity Manager");

            try {

                entityManager.getTransaction().begin();
                CriteriaBuilder cb = entityManager.getCriteriaBuilder();
                CriteriaQuery<User> cq = cb.createQuery(User.class);
                Root<User> user = cq.from(User.class);

                Predicate p1 = cb.equal(user.get("username"), username);
                Predicate p2 = cb.equal(user.get("password"), password);

                cq.where(p1, p2);

                TypedQuery<User> q = entityManager.createQuery(cq);
                List<User> results = q.getResultList();

                entityManager.getTransaction().commit();

                log.info(results);

                if (results.size() > 0) {
                    ret = results.get(0).getAccountID();
                }
            } catch (Exception e) {
                log.error("error in retrieving user account", e);
                ret = INVALID_ACCOUNTID_VALUES;
            }
        }

        try {
            if (null != entityManager) {
                entityManager.close();
            }
        } catch (Exception e) {
            log.error("error while cosing entitymanager", e);
        }
        return ret;
    }

    private Long createUserAccount(User u) {

        Long ret = INVALID_ACCOUNTID_VALUES;

        if (null != u) {

            u.setAccountID(
                    (fingerPrint(u)));

            try {
                save(u);
            } catch (Exception e) {
                log.error("error in creating user account", e);
            }

            log.info(u.toString());

            ret = u.getAccountID();
        }
        return ret;
    }

    private void save(User u) throws Exception {

        EntityManager entityManager = null;
        try {
            entityManager = AccountPersistenceListener.getEntityManager();
        } catch (Exception e) {
            log.info("unable to create Account Entity Manager", e);
            throw new Exception("unable to create Account Entity Manager");
        }

        if (null == entityManager) {
            log.info("unable to create Account Entity Manager");
            throw new Exception("unable to create Account Entity Manager");
        }

        try {
            EntityTransaction transaction = entityManager.getTransaction();

            transaction.begin();
            entityManager.persist(u);
            transaction.commit();

            entityManager.close();
        } catch (Exception e) {
            log.error("error in persisting the user account", e);
            throw e;
        } finally {
            try {
                if (null != entityManager && entityManager.isOpen()) {
                    entityManager.close();
                }
            } catch (Exception e) {
                log.error("error while closing entity manager");
            }
        }
    }

    private Long fingerPrint(User u) {
        return (Long.valueOf((((u.getUsername().length() + u.getPassword().length()) % 50) + 255) % 64) + u.hashCode());
    }

    private void nobodyloggedin(User u) {
        // nessuno è loggato

        if (null == u) {
            u = new User();
        }

        u.setAccountID(INVALID_ACCOUNTID_VALUES);
        u.setEmail(INVALID_USER_INFO);
        u.setPassword(INVALID_USER_INFO);
        u.setResourses(null);
        u.setUsername(INVALID_USER_INFO);
    }

    private User retrieveUserByAccountID(Long accountId) {
        User u = new User();
        EntityManager entityManager = null;

        try {

            entityManager = AccountPersistenceListener.getEntityManager();

        } catch (Exception e) {

            log.warn("unable to create account entity manager", e);
            nobodyloggedin(u);
        }

        if (null != entityManager) {
            log.warn("created account entity manager");

            try {
                entityManager.getTransaction().begin();
                log.info(entityManager.toString());
                CriteriaBuilder cb = entityManager.getCriteriaBuilder();
                CriteriaQuery<User> cq = cb.createQuery(User.class
                );
                Root<User> user = cq.from(User.class);

                Predicate p1 = cb.equal(user.get("accountID"), accountId);

                cq.where(p1);

                TypedQuery<User> q = entityManager.createQuery(cq);
                List<User> results = q.getResultList();
//        log.info(Thread.currentThread());
//        try {
//            Thread.sleep(30000);
//        } catch (InterruptedException ie) {
//            log.info("", ie);
//        }

                entityManager.getTransaction()
                        .commit();

                log.info(results);

                if (results.size()
                        > 0) {
                    u = results.get(0);
                } else {
                    nobodyloggedin(u);

                }
            } catch (Exception e) {

                log.error("errori while retrieving user from DB", e);
                nobodyloggedin(u);

            }
        }

        try {
            if (null != entityManager) {
                entityManager.close();
            }
        } catch (Exception e) {
            log.error("error while closing the entity manager");

        }
        return u;
    }
}

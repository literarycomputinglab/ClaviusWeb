/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.ilc.lc.claviusweb;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import it.cnr.ilc.lc.claviusweb.entity.Annotation;
import it.cnr.ilc.lc.claviusweb.entity.Concept;
import it.cnr.ilc.lc.claviusweb.entity.User;
import it.cnr.ilc.lc.claviusweb.fulltextsearch.ClaviusHighlighter;
import it.cnr.ilc.lc.claviusweb.listener.PersistenceListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.RollbackException;
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
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TokenSources;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;

/**
 *
 * @author simone
 * @author angelo
 * @author davide
 */
@WebServlet(name = "ClaviusAccount", urlPatterns = {"/ClaviusAccount/*"})
public class ClaviusAccount extends HttpServlet {

    private static Logger log = LogManager.getLogger(ClaviusAccount.class);

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
        Gson gson = new Gson();
        String command = request.getPathInfo().substring(1);
        String json = readPost(request);

        /**
         * Comando di login: il client invoca la procedura con il path
         * http://url-di-claviusweb/ClaviusAccount/login
         */
        if ("login".equals(command)) {
            User u = new Gson().fromJson(json, User.class);
            String username = u.getUsername();
            String password = u.getPassword();
            Long accountId = retrieve(username, password);
            log.info(accountId);
            if (accountId != null && accountId != -1) {
                HttpSession session = request.getSession(true);
                session.setAttribute("accountId", accountId);
                u.setAccountID(accountId);
            } else {
                u.setAccountID(Long.valueOf(-1));
            }

            response.getWriter().append(new Gson().toJson(u));

        } /**
         * Comando di loout: il client invoca la procedura con il path
         * http://url-di-claviusweb/ClaviusAccount/logout
         */
        else if ("logout".equals(command)) {
            HttpSession session = request.getSession(false);
            if (session == null) {
                // errore ci chiedono logout ma la sessione non esiste
            } else {
                session.invalidate();
                log.info("session invalidated");
            }

        } /**
         * Comando di verifica login: il client invoca la procedura con il path
         * http://url-di-claviusweb/ClaviusAccount/getAccountId
         */
        else if ("getAccountId".equals(command)) {
            User u = new User();
            HttpSession session = request.getSession(false);
            if (session == null) {
                nobodyloggedin(u);

            } else {
                Long accountId = (Long) session.getAttribute("accountId");
                if (accountId == null || accountId == -1) {
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
        else if ("createuser".equals(command)) {
            Long uid = createUserAccount(json);
            response.getWriter().append(new Gson().toJson(uid));
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
        //  return Long.MIN_VALUE;
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("clavius-account");
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        // Interroga il DB chiedendo l'accountID dell'utente con la password indicata
        // se non esiste nessun utente con tali credenziali ritorna -1

        entityManager.getTransaction().begin();
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> cq = cb.createQuery(User.class);
        Root<User> pet = cq.from(User.class);

        Predicate p1 = cb.equal(pet.get("username"), username);
        Predicate p2 = cb.equal(pet.get("password"), password);
        cq.where(p1, p2);

        TypedQuery<User> q = entityManager.createQuery(cq);
        List<User> results = q.getResultList();
        entityManager.getTransaction().commit();

        entityManager.close();
        entityManagerFactory.close();

        log.info(results);
        if (results.size() > 0) {
            return results.get(0).getAccountID();
        } else {
            return Long.valueOf(-1);
        }

    }

    private Long createUserAccount(String json) {
        Long ret = Long.valueOf(-1);

        Gson gson = new Gson();
        User u = gson.fromJson(json, User.class);
        u.setAccountID((fingerPrint(u)));

        save(u);
        log.info(u.toString());

        ret = u.getAccountID();
        return ret;
    }

    private void save(User u) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("clavius-account");
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();

        transaction.begin();
        entityManager.persist(u);
        transaction.commit();

        entityManager.close();
        entityManagerFactory.close();

    }

    private Long fingerPrint(User u) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.

        return (Long.valueOf((((u.getUsername().length() + u.getPassword().length()) % 50) + 255) % 64) + u.hashCode());
    }

    private void nobodyloggedin(User u) {
        String invalid = "-1";
        // nessuno p loggato
        u.setAccountID(Long.valueOf(-1));
        u.setEmail(invalid);
        u.setPassword(invalid);
        u.setResourses(null);
        u.setUsername(invalid);
    }

    private User retrieveUserByAccountID(Long accountId) {
        User u = new User();
        //u.setAccountID(accountId);
        EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory("clavius-account");
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        // Interroga il DB chiedendo l'accountID dell'utente con la password indicata
        // se non esiste nessun utente con tali credenziali ritorna -1
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> cq = cb.createQuery(User.class);
        Root<User> pet = cq.from(User.class);

        Predicate p1 = cb.equal(pet.get("accountID"), accountId);

        cq.where(p1);

        TypedQuery<User> q = entityManager.createQuery(cq);
        List<User> results = q.getResultList();

        log.info(results);
        if (results.size() > 0) {
            u = results.get(0);
        } else {
            nobodyloggedin(u);

        }
        return u;
    }
}

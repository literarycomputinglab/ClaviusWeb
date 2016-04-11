/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.ilc.lc.claviusweb;

import com.google.gson.Gson;
import it.cnr.ilc.lc.claviusweb.entity.Annotation;
import it.cnr.ilc.lc.claviusweb.listener.PersistenceListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.RollbackException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;

/**
 *
 * @author simone
 */
@WebServlet(name = "ClaviusSearch", urlPatterns = {"/ClaviusSearch/*"})
public class ClaviusSearch extends HttpServlet {

    private static Logger log = LogManager.getLogger(ClaviusSearch.class);
    private FullTextEntityManager fullTextEntityManager = null;

    @Override
    public void init(ServletConfig config) throws ServletException {
        log.info("init() puname: " + config.getInitParameter("puname"));
        EntityManager entityManager = null;

        try {
            entityManager = PersistenceListener.getEntityManager();

        } catch (Exception e) {
            log.error(e.getMessage());
            return;
        }

        if (fullTextEntityManager == null) {
            log.info("Hibernate search init()");

            fullTextEntityManager = org.hibernate.search.jpa.Search.getFullTextEntityManager(entityManager);
            log.info("fullTextEntityManager is open? " + fullTextEntityManager.isOpen());

            try {
                fullTextEntityManager.createIndexer().startAndWait();
                fullTextEntityManager.flushToIndexes();
                log.info("Lucene reindexed");
            } catch (InterruptedException ex) {
                log.error("Error creating lucene indexes: " + ex.getMessage());
            }

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        log.info("closing Full Text Entity Manager");
                        if (null != fullTextEntityManager) {

                            if (fullTextEntityManager.isOpen()) {
                                fullTextEntityManager.close();
                                log.info("closed Full Text Entity Manager");
                          }
                        }
                    } catch (IllegalStateException e) {
                        log.warn("Closing fullTextEntityManager: " + e.getMessage());
                    }
               }
            });
            log.info("Hibernate search initialized");

        }
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
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

        Gson gson = new Gson();
        String command = request.getPathInfo().substring(1);
        String json = readPost(request);
        log.info("da readPost() " + json);
        ClaviusQuery cq = gson.fromJson(json, ClaviusQuery.class);
        List<Annotation> loa = searchQueryParse(cq.luceneQuery);

        log.info(loa);

        response.getWriter().append(annotationToJson(loa));

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

    public class ClaviusQuery {

        String luceneQuery;

    }

    private List<Annotation> searchQueryParse(String query) {

        List result = null;
        EntityManager entityManager = null;

        try {
            entityManager = PersistenceListener.getEntityManager();

        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }

        try {

            entityManager.getTransaction().begin();

            log.info("searchQueryParse(" + query + ")");

            QueryParser parser = new QueryParser(
                    "concept",
                    fullTextEntityManager.getSearchFactory().getAnalyzer(Annotation.class)
            );
            log.debug("parser=(" + parser + ")");

            org.apache.lucene.search.Query luceneQuery = parser.parse(query);
            log.info("luceneQuery " + luceneQuery.toString("concept"));
            FullTextQuery fullTextQuery
                    = fullTextEntityManager.createFullTextQuery(luceneQuery, Annotation.class);

            result = fullTextQuery.getResultList();
            log.info("result " + result);
            entityManager.getTransaction().commit();

        } catch (ParseException | IllegalArgumentException | IllegalStateException | RollbackException | NullPointerException ex) {
            log.error(ex);
            entityManager.getTransaction().rollback();

        } finally {
        }

        return result;
    }

    private String annotationToJson(List<Annotation> loa) {

        Gson gson = new Gson();

        // convert java resourceObject to JSON format,
        // and returned as JSON formatted string
        String json = gson.toJson(loa);

        return json;
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.ilc.lc.claviusweb;

import com.google.gson.Gson;
import it.cnr.ilc.lc.claviusweb.entity.Annotation;
import it.cnr.ilc.lc.claviusweb.fulltextsearch.ClaviusHighlighter;
import it.cnr.ilc.lc.claviusweb.listener.PersistenceListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
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
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.pattern.PatternReplaceCharFilterFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
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

        List<Annotation> ret = new ArrayList<>();
        if (null != query) {
            if (query.contains(":")) {
                //concept
                log.info("Request for Concept research: " + query);
                ret = conceptSearch(query);
            } else {
                //full
                try {
                    log.info("Request for FullText research: " + query);
                    ret = fullTextSearch(query);
                } catch (Exception e) {
                    log.error(e);
                }
            }
        }

        return ret;
    }

    private List<Annotation> conceptSearch(String query) {

        List result = null;
        EntityManager entityManager = null;

        try {
            entityManager = PersistenceListener.getEntityManager();

        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }

        // synchronized (entityManager) {
        entityManager.getTransaction().begin();
        fullTextEntityManager = org.hibernate.search.jpa.Search.getFullTextEntityManager(entityManager);
        try {
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

        } catch (ParseException | IllegalArgumentException | IllegalStateException | RollbackException | NullPointerException ex) {
            log.error(ex + " Error in query " + query);

        } finally {
            entityManager.close();
        }
        // }

        return result;
    }

    private static List<Annotation> fullTextSearch(String term) throws IOException, ParseException, InvalidTokenOffsetsException {

        log.info("searchWithHighlighter (" + term + ")");
        Directory indexDirectory
                = FSDirectory.open(Paths.get("/var/lucene/clavius/indexes/it.cnr.ilc.lc.claviusweb.entity.PlainText"));
        DirectoryReader ireader = DirectoryReader.open(indexDirectory);

        IndexSearcher searcher = new IndexSearcher(ireader);

        Analyzer fullTextAnalyzer = CustomAnalyzer.builder().addCharFilter("patternReplace", "pattern","[\\-\\(\\)\\[\\],\\.;:]","replacement","")
                .withTokenizer("whitespace")
                .build();

        QueryParser parser = new QueryParser("content", fullTextAnalyzer);
        Query query = parser.parse(term);

        TopDocs hits = searcher.search(query, 30);

        SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter();
        //Highlighter highlighter = new Highlighter(htmlFormatter, new QueryScorer(query));
        ClaviusHighlighter highlighter = new ClaviusHighlighter(htmlFormatter, new QueryScorer(query));
        highlighter.setTextFragmenter(new SimpleFragmenter());
        List<Annotation> result = new ArrayList<>();
        log.info("hits.totalHits=(" + hits.totalHits + ")");
        for (int i = 0; i < hits.totalHits; i++) {
            int id = hits.scoreDocs[i].doc;
            Document doc = searcher.doc(id);
            String idDoc = doc.get("idDoc");
            
            //String text = doc.get("content");
            TokenStream tokenStream = TokenSources.getAnyTokenStream(searcher.getIndexReader(), id, "content", fullTextAnalyzer);

            List<Annotation> frag = highlighter.getBestTextClaviusFragments(tokenStream, idDoc, false, 10);//highlighter.getBestFragments(tokenStream, text, 3, "...");
            for (int j = 0; j < frag.size(); j++) {
                log.info("idDoc: " + idDoc + ", Annotation[" + j + "] " + frag.get(j).toString());
            }
            result.addAll(frag);
        }
        return result;
    }

    private List<Annotation> searchQueryParse2(String query) {

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
            log.error(ex + " Error in query " + query);
            entityManager.getTransaction().rollback();

        } finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
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

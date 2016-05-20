package it.cnr.ilc.lc.claviusweb;

import com.google.gson.Gson;
import it.cnr.ilc.lc.claviusweb.entity.Annotation;
import it.cnr.ilc.lc.claviusweb.entity.PlainText;
import it.cnr.ilc.lc.claviusweb.entity.TEADocument;
import it.cnr.ilc.lc.claviusweb.listener.PersistenceListener;
import it.cnr.ilc.lc.claviusweb.utilities.WikiDataHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.rest.graphdb.RestGraphDatabase;

/**
 *
 * @author oakgen
 * @author simone
 * @author angelo
 */
@WebServlet(name = "ClaviusGraph", urlPatterns = {"/ClaviusGraph/*"})
public class ClaviusGraph extends HttpServlet {

    private static GraphDatabaseService db;
    private static Logger log = LogManager.getLogger(ClaviusGraph.class);

    private FullTextEntityManager fullTextEntityManager = null;
    private static final int ctxLen = 100;
    private static final int stringLogLenght = 50;
    private static Properties conceptsMap;

    @Override
    public void init(ServletConfig config) throws ServletException {

        EntityManager entityManager = null;
        readProperies();

        if (db == null) {
            log.info("Neo4j init()");
            String url = config.getInitParameter("url");
            String username = config.getInitParameter("username");
            String password = config.getInitParameter("password");
            db = new RestGraphDatabase(url, username, password);

            log.info("Neo4j initialized");
        }

        try {
            entityManager = PersistenceListener.getEntityManager();

        } catch (Exception e) {
            log.error(e.getMessage());
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
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                log.info("Shutting down Neo4j");
                if (null != db) {
                    db.shutdown();
                    log.info("Neo4j stopped");
                } else {
                    log.warn("Neo4j already stopped?");
                }

                log.info("closing FullText Entity Manager");
                try {
                    if (null != fullTextEntityManager) {
                        if (fullTextEntityManager.isOpen()) {
                            fullTextEntityManager.close();
                            log.info("closed FullText Entity Manager");
                        }
                    }
                } catch (IllegalStateException e) {
                    log.warn("Closing fullTextEntityManager: " + e.getMessage());
                }

            }
        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

        String ip = getClientIpAddr(request);

        Gson gson = new Gson();
        String command = request.getPathInfo().substring(1);
        String json = readPost(request);
        if ("create".equals(command)) {
            log.info("[" + ip + "] create " + trimTo(json, stringLogLenght));
            response.getWriter().append(gson.toJson(createNode(gson.fromJson(json, TEADocument.class))));
        } else if ("update".equals(command)) {
            log.info("[" + ip + "] update " + trimTo(json, stringLogLenght));
            response.getWriter().append(gson.toJson(updateNode(gson.fromJson(json, TEADocument.class))));
        } else if ("load".equals(command)) {
            log.info("[" + ip + "] load " + trimTo(json, stringLogLenght));
            response.getWriter().append(gson.toJson(loadNode(gson.fromJson(json, TEADocument.class))));
        } else if ("list".equals(command)) {
            log.info("[" + ip + "] list");
            response.getWriter().append(gson.toJson(listNodes()));
        } else {
            log.error("Unvalid path URI for command " + command + "::" + json + "from ip: " + ip);
            throw new UnsupportedOperationException("Unvalid path URI for command " + command + "::" + json);
        }
        log.info("Response sent to " + ip + " for command (" + command + ")");

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

    private TEADocument createNode(TEADocument document) {
        synchronized (db) {
            try (Transaction tx = db.beginTx()) {
                Node node = db.createNode(() -> "Clavius_1_0_3");
                node.setProperty("name", document.name);
                node.setProperty("code", document.code);
                node.setProperty("graph", document.graph);
                node.setProperty("idDoc", document.idDoc);
                tx.success();
                document.id = node.getId();
                return document;
            }
        }
    }

    private TEADocument updateNode(TEADocument document) {
        long endTime;
        long midTime;
        synchronized (db) {
            long startTime = System.currentTimeMillis();
            try (Transaction tx = db.beginTx()) {
                Node node = db.getNodeById(document.id);
                node.setProperty("name", document.name);
                node.setProperty("code", document.code);
                node.setProperty("graph", document.graph);
                node.setProperty("idDoc", document.idDoc);

                midTime = System.currentTimeMillis();
                log.info("After neo4j load and set  " + (midTime - startTime) + " ms");

                //salvataggio delle annotazioni
                updateLuceneIndex(document);
                endTime = System.currentTimeMillis();
                log.info("Insert in index " + (endTime - midTime) + " ms");
                tx.success();
            }
            return document;
        }

    }

    private TEADocument loadNode(TEADocument document) {
        synchronized (db) {
            try (Transaction tx = db.beginTx()) {
                Node node = db.getNodeById(document.id);
                document.name = (String) node.getProperty("name", "");
                document.code = (String) node.getProperty("code", "");
                document.graph = (String) node.getProperty("graph", "");
                document.idDoc = (String) node.getProperty("idDoc", "");

                return document;
            }
        }
    }

    private List<TEADocument> listNodes() {
        synchronized (db) {
            try (Transaction tx = db.beginTx()) {
                List<TEADocument> documents = new ArrayList<>();
                for (Node node : db.findNodesByLabelAndProperty(() -> "Clavius_1_0_3", null, null)) {
                    TEADocument document = new TEADocument();
                    document.id = node.getId();
                    document.name = (String) node.getProperty("name", "");
                    document.idDoc = (String) node.getProperty("idDoc", "");

                    documents.add(document);
                }
                return documents;
            }
        }
    }

    private void updateLuceneIndex(TEADocument teadoc) {

        EntityManager entityManager = null;

        try {
            entityManager = PersistenceListener.getEntityManager();
        } catch (Exception e) {
            log.error(e.getMessage());
            return;
        }

        synchronized (entityManager) {

            fullTextEntityManager = org.hibernate.search.jpa.Search.getFullTextEntityManager(entityManager);

            entityManager.getTransaction().begin();

            String plainText = teadoc.text;
            String idDoc = teadoc.idDoc;
            List<TEADocument.Triple> triples = teadoc.triples;
            log.info("TEADocument.Triple: " + triples);
            long time1 = System.currentTimeMillis();
            long time2 = 0;
            long time3 = 0;
            try {

                //FIX Gestire la cancellazione con l'entityManager e non direttamente nel DB Mysql. Altrimenti siamo costretti a fare sempre 
                // fullTextEntityManager.createIndexer().startAndWait() per mantenere allineato DB e Indice Lucene
                Query queryDeletePlainText = entityManager.createNativeQuery("DELETE FROM PlainText WHERE idDoc = \"" + teadoc.idDoc + "\"");
                int deleted = queryDeletePlainText.executeUpdate();
                log.info("Delete " + deleted + " Annotation(s)");
                //END TO FIX

                entityManager.persist(createFullTextEntity(teadoc));

                //FIX Gestire la cancellazione con l'entityManager e non direttamente nel DB Mysql. Altrimenti siamo costretti a fare sempre 
                // fullTextEntityManager.createIndexer().startAndWait() per mantenere allineato DB e Indice Lucene
                Query query = entityManager.createNativeQuery("DELETE FROM Annotation WHERE idNeo4j = " + teadoc.id);
                deleted = query.executeUpdate();
                //END TO FIX

                time2 = System.currentTimeMillis();
                log.info("Delete " + deleted + " Annotation(s) in " + (time2 - time1) + " ms");
                int count = 0;
                if (null != triples && null != plainText) {

                    for (TEADocument.Triple triple : triples) {
                        log.debug("createEntity, for each triples: 1");
                        if (isValid(triple)) {
                            Annotation a = new Annotation();
                            log.info("createEntity, for each triples: 1 plainText.length(): " + plainText.length());
                            a.setLeftContext(plainText.substring(triple.start > ctxLen ? triple.start - ctxLen : 0, triple.start));
                            log.debug("createEntity, for each triples: 2");
                            a.setRightContext(plainText.substring(triple.end, triple.end + ctxLen < plainText.length() ? triple.end + ctxLen : plainText.length()));
                            log.info("createEntity, for each triples: 3 " + idDoc);
                            a.setIdDoc(idDoc);
                            //SEARCH FIRST IN CLAVIUS LEXICON AND THEN IN WIKIDATA (IF NO RESULTS FROM CLAVIUS LEXICON)
                            log.info("createEntity, for each triples: 4 (" + conceptsMap.getProperty(triple.object) + ")");
                            if (conceptsMap.containsKey(triple.object)) {
                                a.setConcept(conceptsMap.getProperty(triple.object)); //@FIX triple.object sara' la chiave di accesso alla mappa dei concetti
                            } else {
                                a.setConcept(WikiDataHandler.getInstance().queryStringBuilder(triple.object));
                            }

                            log.debug("createEntity, for each triples: 5");
                            a.setType(conceptsMap.getProperty(triple.object).split(" ")[0]);
                            log.debug("createEntity, for each triples: 6");
                            a.setResourceObject(triple.object);
                            log.debug("createEntity, for each triples: 7");
                            a.setIdNeo4j(teadoc.id);
                            log.debug("createEntity, for each triples: 8");
                            a.setMatched(plainText.substring(triple.start, triple.end));
                            log.info("createEntity before persist 9");
                            entityManager.persist(a);
                            log.info("createEntity: " + a + " persisted");
                            count++;
                        }
                    }
                } else {
                    log.info("No triples in json request");
                }
                entityManager.getTransaction().commit();
                time3 = System.currentTimeMillis();
                log.info("Created " + count + " Annotation(s) in " + (time3 - time2) + " ms");

            } catch (StringIndexOutOfBoundsException e) {
                log.error("plainText " + plainText + ":\n" + e.getMessage());
            } catch (Exception ex) {
                log.error("PPPPPPPPPRRRRRRRRRRRRRRR: ", ex);

            } finally {
                try {
                    fullTextEntityManager.createIndexer().startAndWait();
                } catch (InterruptedException ex) {
                    log.error(ex.getMessage());
                }
                fullTextEntityManager.flushToIndexes();
                long time4 = System.currentTimeMillis();
                log.info("Lucene index reindexing in " + (time4 - time3) + " ms");

                if (entityManager.getTransaction().isActive()) {
                    log.warn("Connection is already active, rolling back!");
                    entityManager.getTransaction().rollback();
                }
                try {
                    entityManager.close();
                } catch (Exception e) {
                    log.error(e.getMessage());
                }
            }

        }
    }

    private PlainText createFullTextEntity(TEADocument teadoc) {

        String content = teadoc.text;
        String idDoc = teadoc.idDoc;
        String extra = teadoc.name;

        PlainText ft = new PlainText();

        ft.setIdDoc(idDoc);
        ft.setContent(content);
        ft.setExtra(extra);

        return ft;
    }

    private void readProperies() {

        InputStream input = null;

        try {
            input = ClaviusGraph.class
                    .getResourceAsStream("/concepts-map.properties");
            // load a properties file
            conceptsMap = new Properties();
            conceptsMap.load(input);
        } catch (IOException ex) {
            log.error(ex.getMessage());
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    log.error(e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
//"start":4,"end":19,"subject":"j","predicate":"its:termInfoRef","object":"http://claviusontheweb.it/lexicon/math/trapezium"}

    private boolean isValid(TEADocument.Triple triple) {
        boolean ret = false;
        if (null != triple) {
            if (triple.end != null
                    && triple.object != null
                    && triple.predicate != null
                    && triple.start != null
                    && triple.subject != null) {
                if (conceptsMap.containsKey(triple.object)) {
                    ret = true;
                } else {
                    log.warn(triple.object + " is not a concept");
                }
            } else {
                log.warn("triple has some null component(s)");
            }
        } else {
            log.warn("triple is null!");
        }
        return ret;
    }

    private String trimTo(String s, int limit) {
        String ret = "";
        if (null != s) {
            if (s.length() < limit) {
                ret = s;
            } else {
                ret = s.substring(0, limit) + "...";
            }
        }
        return ret;
    }

    private String getClientIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}

package it.cnr.ilc.lc.claviusweb;

import com.google.gson.Gson;
import it.cnr.ilc.lc.claviusweb.entity.Annotation;
import it.cnr.ilc.lc.claviusweb.entity.TEADocument;
import it.cnr.ilc.lc.claviusweb.listener.PersistenceListener;
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
 */
@WebServlet(name = "ClaviusGraph", urlPatterns = {"/ClaviusGraph/*"})
public class ClaviusGraph extends HttpServlet {

    private static GraphDatabaseService db;
    private static Logger log = LogManager.getLogger(ClaviusGraph.class);

    private FullTextEntityManager fullTextEntityManager = null;
    private static final int ctxLen = 30;
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

        Gson gson = new Gson();
        String command = request.getPathInfo().substring(1);
        String json = readPost(request);
        if ("create".equals(command)) {
            log.info("create " + json);
            response.getWriter().append(gson.toJson(createNode(gson.fromJson(json, TEADocument.class))));
        } else if ("update".equals(command)) {
            log.info("update " + json);
            response.getWriter().append(gson.toJson(updateNode(gson.fromJson(json, TEADocument.class))));
        } else if ("load".equals(command)) {
            log.info("load " + json);
            response.getWriter().append(gson.toJson(loadNode(gson.fromJson(json, TEADocument.class))));
        } else if ("list".equals(command)) {
            log.info("list");
            response.getWriter().append(gson.toJson(listNodes()));
        } else {
            log.error("Unvalid path URI for command " + command + "::" + json);
            throw new UnsupportedOperationException("Unvalid path URI for command " + command + "::" + json);
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

    private TEADocument createNode(TEADocument document) {
        synchronized (db) {
            try (Transaction tx = db.beginTx()) {
                Node node = db.createNode(() -> "Clavius");
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
                for (Node node : db.findNodesByLabelAndProperty(() -> "Clavius", null, null)) {
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

        long startTime = System.currentTimeMillis();

        EntityManager entityManager = null;

        try {
            entityManager = PersistenceListener.getEntityManager();
        } catch (Exception e) {
            log.error(e.getMessage());
            return;
        }
        synchronized (entityManager) {
            entityManager.getTransaction().begin();

            String plainText = teadoc.text;
            String idDoc = teadoc.idDoc;
            List<TEADocument.Triple> triples = teadoc.triples;
            long time1 = System.currentTimeMillis();

            Query query = entityManager.createNativeQuery("DELETE FROM Annotation WHERE idNeo4j = " + teadoc.id);
            int deleted = query.executeUpdate();
            long time2 = System.currentTimeMillis();
            log.info("Delete " + deleted + " Annotation(s) in " + (time2 - time1) + " ms");
            int count = 0;
            if (null != triples) {

                for (TEADocument.Triple triple : triples) {
                    Annotation a = new Annotation();
                    a.setLeftContext(plainText.substring(triple.start > ctxLen ? triple.start - ctxLen : 0, triple.start));
                    a.setRightContext(plainText.substring(triple.end, triple.end + ctxLen < plainText.length() ? triple.end + ctxLen : plainText.length()));
                    a.setIdDoc(Long.valueOf(idDoc));
                    a.setConcept(conceptsMap.getProperty(triple.object.substring(triple.object.lastIndexOf("/") + 1))); //@FIX triple.object sara' la chiave di accesso alla mappa dei concetti
                    a.setType(triple.object.substring(triple.object.lastIndexOf("/") + 1));
                    a.setResourceObject(triple.object);
                    a.setIdNeo4j(teadoc.id);
                    a.setMatched(plainText.substring(triple.start, triple.end));
                    entityManager.persist(a);
                    log.info("createEntity: " + a);
                    count++;
                }
            } else {
                log.info("No triples in json request");
            }
            long time3 = System.currentTimeMillis();
            log.info("Created " + count + " Annotation(s) in " + (time3 - time2) + " ms");

            /*try {
            fullTextEntityManager.createIndexer().startAndWait();
        } catch (InterruptedException ex) {
            log.error(ex.getMessage());
        }*/
            //fullTextEntityManager.flushToIndexes();
            entityManager.getTransaction().commit();
            long time4 = System.currentTimeMillis();
            log.info("Lucene index commit in " + (time4 - time3) + " ms");

        }
    }

    private void readProperies() {

        InputStream input = null;

        try {
            input = ClaviusGraph.class.getResourceAsStream("/concepts-map.properties");
            // load a properties file
            conceptsMap = new Properties();
            conceptsMap.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}

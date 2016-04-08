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
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
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

        readProperies();

        if (db == null) {
            log.info("Neo4j init()");
            String url = config.getInitParameter("url");
            String username = config.getInitParameter("username");
            String password = config.getInitParameter("password");
            db = new RestGraphDatabase(url, username, password);

            log.info("Neo4j initialized");
        }

        EntityManager entityManager = PersistenceListener.getEntityManager();

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
                db.shutdown();
                log.info("closing Entity Manager");
                fullTextEntityManager.close();
                log.info("closed Entity Manager");

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
                node.setProperty("idLetter", document.idLetter);
                tx.success();
                document.id = node.getId();
                return document;
            }
        }
    }

    private TEADocument updateNode(TEADocument document) {
        synchronized (db) {
            try (Transaction tx = db.beginTx()) {
                Node node = db.getNodeById(document.id);
                node.setProperty("name", document.name);
                node.setProperty("code", document.code);
                node.setProperty("graph", document.graph);
                node.setProperty("idLetter", document.idLetter);

                tx.success();

                //salvataggio delle annotazioni
                updateLuceneIndex(document);

                return document;
            }
        }
    }

    private TEADocument loadNode(TEADocument document) {
        synchronized (db) {
            try (Transaction tx = db.beginTx()) {
                Node node = db.getNodeById(document.id);
                document.name = (String) node.getProperty("name", "");
                document.code = (String) node.getProperty("code", "");
                document.graph = (String) node.getProperty("graph", "");
                document.idLetter = (String) node.getProperty("idLetter", "");

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
                    document.idLetter = (String) node.getProperty("idLetter", "");

                    documents.add(document);
                }
                return documents;
            }
        }
    }

    private void updateLuceneIndex(TEADocument teadoc) {

        EntityManager entityManager = PersistenceListener.getEntityManager();

        entityManager.getTransaction().begin();

        String plainText = teadoc.text;
        String idLetter = teadoc.idLetter;
        List<TEADocument.Triple> triples = teadoc.triples;

        Query query = entityManager.createNativeQuery("DELETE FROM Annotation WHERE idNeo4j = " + teadoc.id);
        log.info("deleted " + query.executeUpdate() + " row(s)");

        if (null != triples) {

            for (TEADocument.Triple triple : triples) {
                Annotation a = new Annotation();
                a.setLeftContext(plainText.substring(triple.start > ctxLen ? triple.start - ctxLen : 0, triple.start));
                a.setRightContext(plainText.substring(triple.end, triple.end + ctxLen < plainText.length() ? triple.end + ctxLen : plainText.length()));
                a.setIdLetter(Long.valueOf(idLetter));
                a.setConcept(conceptsMap.getProperty(triple.object.substring(triple.object.lastIndexOf("/") + 1))); //@FIX triple.object sara' la chiave di accesso alla mappa dei concetti
                a.setType(triple.object.substring(triple.object.lastIndexOf("/") + 1));
                a.setResourceObject(triple.object);
                a.setIdNeo4j(teadoc.id);
                a.setMatched(plainText.substring(triple.start, triple.end));
                entityManager.persist(a);
                log.info("createEntity: " + a);
            }
        } else {
            log.info("No triples in json request");
        }

        try {
            fullTextEntityManager.createIndexer().startAndWait();
        } catch (InterruptedException ex) {
            log.error(ex.getMessage());
        }
        fullTextEntityManager.flushToIndexes();
        entityManager.getTransaction().commit();
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

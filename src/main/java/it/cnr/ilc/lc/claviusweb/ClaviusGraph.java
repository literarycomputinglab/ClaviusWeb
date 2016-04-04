package it.cnr.ilc.lc.claviusweb;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    @Override
    public void init(ServletConfig config) throws ServletException {
        if (db == null) {
            log.info("Neo4j init()");
            String url = config.getInitParameter("url");
            String username = config.getInitParameter("username");
            String password = config.getInitParameter("password");
            db = new RestGraphDatabase(url, username, password);
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    db.shutdown();
                }
            });
            log.info("Neo4j initialized");

        }
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
            response.getWriter().append(gson.toJson(createNode(gson.fromJson(json, Document.class))));
        } else if ("update".equals(command)) {
            log.info("update " + json);
            response.getWriter().append(gson.toJson(updateNode(gson.fromJson(json, Document.class))));
        } else if ("load".equals(command)) {
            log.info("load " + json);
            response.getWriter().append(gson.toJson(loadNode(gson.fromJson(json, Document.class))));
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

    private Document createNode(Document document) {
        synchronized (db) {
            try (Transaction tx = db.beginTx()) {
                Node node = db.createNode(() -> "Clavius");
                node.setProperty("name", document.name);
                node.setProperty("code", document.code);
                node.setProperty("graph", document.graph);
                tx.success();
                document.id = node.getId();
                return document;
            }
        }
    }

    private Document updateNode(Document document) {
        synchronized (db) {
            try (Transaction tx = db.beginTx()) {
                Node node = db.getNodeById(document.id);
                node.setProperty("name", document.name);
                node.setProperty("code", document.code);
                node.setProperty("graph", document.graph);
                tx.success();
                return document;
            }
        }
    }

    private Document loadNode(Document document) {
        synchronized (db) {
            try (Transaction tx = db.beginTx()) {
                Node node = db.getNodeById(document.id);
                document.name = (String) node.getProperty("name", "");
                document.code = (String) node.getProperty("code", "");
                document.graph = (String) node.getProperty("graph", "");
                return document;
            }
        }
    }

    private List<Document> listNodes() {
        synchronized (db) {
            try (Transaction tx = db.beginTx()) {
                List<Document> documents = new ArrayList<>();
                for (Node node : db.findNodesByLabelAndProperty(() -> "Clavius", null, null)) {
                    Document document = new Document();
                    document.id = node.getId();
                    document.name = (String) node.getProperty("name", "");
                    documents.add(document);
                }
                return documents;
            }
        }
    }

    private class Document {

        private Long id;
        private String name;
        private String code;
        private String graph;

    }

}

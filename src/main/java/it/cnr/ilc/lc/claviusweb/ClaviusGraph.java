package it.cnr.ilc.lc.claviusweb;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Transaction;
import org.neo4j.rest.graphdb.RestGraphDatabase;

/**
 *
 * @author oakgen
 */
@WebServlet(name = "ClaviusGraph", urlPatterns = {"/ClaviusGraph/*"})
public class ClaviusGraph extends HttpServlet {

    private static GraphDatabaseService db;

    @Override
    public void init(ServletConfig config) throws ServletException {
        if (db == null) {
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
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String command = request.getPathInfo().substring(1);
        if ("create".equals(command)) {
            System.out.println("create name: " + request.getParameter("name") + " content: " + request.getParameter("content"));
            response.getWriter().append(createNode(request.getParameter("name"), request.getParameter("content")));
        } else if ("update".equals(command)) {
            System.out.println("update id: " + request.getParameter("id") + " name: " + request.getParameter("name") + " content: " + request.getParameter("content"));
            response.getWriter().append(updateNode(Long.valueOf(request.getParameter("id")), request.getParameter("name"), request.getParameter("content")));
        } else if ("load".equals(command)) {
            System.out.println("load id: " + request.getParameter("id"));
            response.getWriter().append(loadNode(Long.valueOf(request.getParameter("id"))));
        } else if ("list".equals(command)) {
            System.out.println("list");
            response.getWriter().append(listNodes());
        }
    }

    private String createNode(String name, String content) {
        synchronized (db) {
            try (Transaction tx = db.beginTx()) {
                Node node = db.createNode(() -> "Clavius");
                node.setProperty("name", name);
                node.setProperty("content", content);
                tx.success();
                return "" + node.getId();
            }
        }
    }

    private String updateNode(Long id, String name, String content) {
        synchronized (db) {
            try (Transaction tx = db.beginTx()) {
                Node node = db.getNodeById(id);
                node.setProperty("name", name);
                node.setProperty("content", content);
                tx.success();
                return Boolean.TRUE.toString();
            } catch (NotFoundException ne) {
                return Boolean.FALSE.toString();
            }
        }
    }

    private String loadNode(Long id) {
        synchronized (db) {
            try (Transaction tx = db.beginTx()) {
                Node node = db.getNodeById(id);
                return (String) node.getProperty("content");
            } catch (NotFoundException ne) {
                return "";
            }
        }
    }

    private String listNodes() {
        synchronized (db) {
            try (Transaction tx = db.beginTx()) {
                List<Map<String, Object>> list = new ArrayList<>();
                Map<String, Object> map;
                for (Node node : db.findNodesByLabelAndProperty(() -> "Clavius", null, null)) {
                    map = new HashMap<>();
                    map.put("id", node.getId());
                    map.put("name", node.getProperty("name"));
                    list.add(map);
                }
                return new Gson().toJson(list);
            }
        }
    }
}

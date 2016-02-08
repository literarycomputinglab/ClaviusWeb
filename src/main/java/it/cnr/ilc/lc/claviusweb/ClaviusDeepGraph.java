/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.ilc.lc.claviusweb;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Type;
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
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.rest.graphdb.RestGraphDatabase;

/**
 *
 * @author simone
 */
@WebServlet(name = "ClaviusDeepGraph", urlPatterns = {"/ClaviusDeepGraph/*"})

public class ClaviusDeepGraph extends HttpServlet {

    private static GraphDatabaseService db;

    private static enum RelTypes implements RelationshipType {
        LINKS
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        if (db == null) {
            String url = config.getInitParameter("url");
            String username = config.getInitParameter("username");
            String password = config.getInitParameter("password");
            System.err.println("url " + url + " username: " + username + " password: " + password);
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
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String command = request.getPathInfo().substring(1);
        if ("create".equals(command)) {
            System.out.println("create name: " + request.getParameter("name") + " content: " + request.getParameter("content"));
            response.getWriter().append(createGraph(request.getParameter("name"), request.getParameter("content")));
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

    /**
     *
     * @param name
     * @param graph rappresenta il campo "graph" del json che arriva dalla gui
     * (a sua volta un json che descrive il grafo da memorizzare in neo4j)
     * @return
     */
    private String createGraph(String name, String graph) {

        Gson gson = new Gson();
        Type mapType = new TypeToken<Map<String, List<Map<String, String>>>>() {
        }.getType();
        Map<String, List<Map<String, String>>> map = gson.fromJson(new StringReader(graph), mapType);
        System.err.println("map : " + map);
        return "";
               /*
        synchronized (db) {
            try (Transaction tx = db.beginTx()) {

                Node node = db.createNode(() -> "Clavius");
                Node node2 = db.createNode(() -> "Clavius");
                node.setProperty("name", name);
                node.setProperty("content", graph);
                node2.setProperty("name", name);
                node2.setProperty("content", graph + " 2");
                node.createRelationshipTo(node2, RelTypes.LINKS);
                tx.success();

                return "" + node.getId();
            }
        }
         */
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

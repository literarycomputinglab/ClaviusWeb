/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.ilc.lc.claviusweb.utilities;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.Var;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author simone
 */
public class WikiDataHandler {

    private static Logger log = LogManager.getLogger(WikiDataHandler.class);

    private WikiDataHandler() {
    }

    public static WikiDataHandler getInstance() {

        return new WikiDataHandler();
    }

    public static void test() {

        // queryStatic(); //OK
        //queryBinding("<http://www.wikidata.org/entity/Q405>"); //KIO
        //querySelectBuilder("<http://www.wikidata.org/entity/Q405>"); //KIO
        //queryStringBuilder("<http://www.wikidata.org/entity/Q405>"); //OK 
    }

    private static void queryStatic() {

        //        String queryString
//                = "PREFIX category: <http://dbpedia.org/resource/Category:>\n"
//                + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
//                + "PREFIX skos: <http://www.w3.org/2004/02/skos/core#>\n"
//                + "PREFIX geo: <http://www.georss.org/georss/>\n"
//                + "PREFIX dct: <http://purl.org/dc/terms/>\n"
//                + "PREFIX dbo: <http://dbpedia.org/ontology/>\n"
//                + "\n"
//                + "SELECT DISTINCT ?m ?n ?p ?c ?d WHERE {\n"
//                + " ?m rdfs:label ?n.\n"
//                + " ?m dct:subject ?c.\n"
//                + " ?c skos:broader category:Churches_in_Paris.\n"
//                + " ?m dbo:abstract ?d.\n"
//                + " ?m geo:point ?p\n"
//                + " }";
        String queryString
                = "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n"
                + "\n"
                + "SELECT DISTINCT ?class\n"
                + "WHERE\n"
                + "{\n"
                + "	<http://www.wikidata.org/entity/Q405> wdt:P31 ?sclass .\n"
                + "    ?sclass wdt:P279* ?class .\n"
                + "}";

        // now creating query object
        Query query = QueryFactory.create(queryString);

        System.err.println("1A query\n" + query.toString());

// initializing queryExecution factory with remote service.
// **this actually was the main problem I couldn't figure out.**
        QueryExecution qexec = QueryExecutionFactory.sparqlService("http://query.wikidata.org/sparql", query);

//after it goes standard query execution and result processing which can
// be found in almost any Jena/SPARQL tutorial.
        try {
            ResultSet results = qexec.execSelect();
            System.err.println("result set " + results + " for " + query.getGraphURIs());

            if (!results.hasNext()) {
                System.err.println("No results found!");
            }
            while (results.hasNext()) {
                QuerySolution binding = results.nextSolution();
                // System.err.println("binding " + binding.toString());
                Resource obj = binding.getResource("class");
                System.out.println("sclass: " + obj.getURI());

//                for (Iterator iterator = binding.varNames(); iterator.hasNext();) {
//                    String next = (String) iterator.next();
//                    System.err.println("var: " + next);
//                }
            }

        } finally {
            qexec.close();
        }
    }

    public String queryStringBuilderOld(String URI) {
        String _uri;

        if (!URI.startsWith("<") && !URI.endsWith(">")) {
            _uri = "<" + URI + ">";
        } else {
            _uri = URI;
        }

        log.info("URI " + URI);
        String ret;
        StringBuilder sb = new StringBuilder();

        CharSequence head = "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n"
                + "SELECT  ?class\n"
                + "WHERE\n"
                + "{ ";

        CharSequence tail = " wdt:P31 ?sclass .\n"
                + "    ?sclass (wdt:P279)* ?class .\n"
                + "}";

        // now creating query object
        Query query = QueryFactory.create(head + _uri + tail);

        log.info("query\n" + query.toString());

// initializing queryExecution factory with remote service.
// **this actually was the main problem I couldn't figure out.**
        QueryExecution qexec = QueryExecutionFactory.sparqlService("http://query.wikidata.org/sparql", query);

//after it goes standard query execution and result processing which can
// be found in almost any Jena/SPARQL tutorial.
        try {
            ResultSet results = qexec.execSelect();
            //System.err.println("result set " + results + " for " + query.getGraphURIs());

            if (!results.hasNext()) {
                log.info("No results found!");
            }

            while (results.hasNext()) {
                QuerySolution binding = results.nextSolution();
                // System.err.println("binding " + binding.toString());
                Resource obj = binding.getResource("class");
                log.debug("sclass: " + obj.getURI());

                sb.append(obj.getURI());
                if (results.hasNext()) {
                    sb.append(" ");
                }

            }

        } finally {
            qexec.close();
        }
        ret = sb.toString();

        log.info("Found " + ret + " URI(s)");

        return ret;
    }

    public String queryStringBuilder(String URI) {
        String _uri;

        if (!URI.startsWith("<") && !URI.endsWith(">")) {
            _uri = "<" + URI + ">";
        } else {
            _uri = URI;
        }

        log.info("URI " + URI);
        List<String> ret = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        CharSequence head = "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n"
                + "SELECT  ?class\n"
                + "WHERE\n"
                + "{ ";

        CharSequence tail = " wdt:P31 ?sclass .\n"
                + "    ?sclass (wdt:P279)* ?class .\n"
                + "}";

        // now creating query object
        Query query = QueryFactory.create(head + _uri + tail);

        log.info("query\n" + query.toString());

// initializing queryExecution factory with remote service.
// **this actually was the main problem I couldn't figure out.**
        QueryExecution qexec = QueryExecutionFactory.sparqlService("http://query.wikidata.org/sparql", query);

//after it goes standard query execution and result processing which can
// be found in almost any Jena/SPARQL tutorial.
        try {
            ResultSet results = qexec.execSelect();
            //System.err.println("result set " + results + " for " + query.getGraphURIs());

            if (!results.hasNext()) {
                log.info("No results found!");
            }

            while (results.hasNext()) {
                QuerySolution binding = results.nextSolution();
                // System.err.println("binding " + binding.toString());
                Resource obj = binding.getResource("class");
                log.debug("sclass: " + obj.getURI());

                ret.add(obj.getURI());
                
            }

        } finally {
            qexec.close();
        }

        log.info("Found " + ret.size() + " URI(s)");

        Set<String> set = new LinkedHashSet<>(ret);
        
        
        for (Iterator<String> it = set.iterator(); it.hasNext();) {
            
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(" ");
            }
        }
        
        return sb.toString();
    }
    

//    String queryString
//                = "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n"
//                + "\n"
//                + "SELECT DISTINCT ?class\n"
//                + "WHERE\n"
//                + "{\n"
//                + "	<http://www.wikidata.org/entity/Q405> wdt:P31 ?sclass .\n"
//                + "    ?sclass wdt:P279* ?class .\n"
//                + "}";
    private static void querySelectBuilder(String URI) {

        SelectBuilder sb = new SelectBuilder().addPrefix("wdt:", "http://www.wikidata.org/prop/direct/")
                .addVar("?class").addVar("?s")
                .addWhere("?s", "wdt:P31", "?sclass").setDistinct(true)
                .addWhere("?sclass", "(wdt:P279)*", "?class");

        sb.setVar(Var.alloc("?s"), NodeFactory.createURI("http://www.wikidata.org/entity/Q405"));
        Query query = sb.build();

        System.err.println("1A query\n" + sb.toString());
        System.err.println("1B query\n" + query.toString());

        QueryExecution qexec = QueryExecutionFactory.sparqlService("http://query.wikidata.org/sparql", query);

        System.err.println("2");

//after it goes standard query execution and result processing which can
// be found in almost any Jena/SPARQL tutorial.
        try {
            ResultSet results = qexec.execSelect();
            System.err.println("result set " + results + " for " + query.getGraphURIs());

            if (!results.hasNext()) {
                System.err.println("No results found!");
            }
            while (results.hasNext()) {
                QuerySolution binding = results.nextSolution();
                // System.err.println("binding " + binding.toString());
                Resource obj = binding.getResource("class");
                System.out.println("sclass: " + obj.getURI());

//                for (Iterator iterator = binding.varNames(); iterator.hasNext();) {
//                    String next = (String) iterator.next();
//                    System.err.println("var: " + next);
//                }
            }

        } finally {
            qexec.close();
        }

    }

    private static void queryBinding(String URI) {
        String queryString
                = "PREFIX wdt: <http://www.wikidata.org/prop/direct/>\n"
                + "\n"
                + "SELECT DISTINCT ?class\n"
                + "WHERE\n"
                + "{\n"
                + "     ?entityUri wdt:P31 ?sclass .\n"
                + "     ?sclass wdt:P279* ?class .\n"
                + "}";

        Query query = QueryFactory.create(queryString);

        QuerySolutionMap initialBinding = new QuerySolutionMap();
        initialBinding.add("entityUri", ResourceFactory.createResource(URI));

        QueryExecution qexec = QueryExecutionFactory.create(query, DatasetFactory.create("http://query.wikidata.org/sparql"), initialBinding);
        System.err.println("query " + qexec.getQuery().toString());

//after it goes standard query execution and result processing which can
// be found in almost any Jena/SPARQL tutorial.
        try {
            ResultSet results = qexec.execSelect();
            System.err.println("result set " + results + " for " + query.getGraphURIs());

            if (!results.hasNext()) {
                System.err.println("No results found!");
            }
            while (results.hasNext()) {
                QuerySolution binding = results.nextSolution();
                // System.err.println("binding " + binding.toString());
                Resource obj = binding.getResource("class");
                System.out.println("class: " + obj.getURI());

//                for (Iterator iterator = binding.varNames(); iterator.hasNext();) {
//                    String next = (String) iterator.next();
//                    System.err.println("var: " + next);
//                }
            }

        } finally {
            qexec.close();
        }
    }

    public static void main(String[] args) {
        WikiDataHandler handler = WikiDataHandler.getInstance();
        System.out.println(handler.queryStringBuilderOld("http://www.wikidata.org/entity/Q279881"));
        System.out.println(handler.queryStringBuilder("http://www.wikidata.org/entity/Q279881"));
    }

}

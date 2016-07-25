/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.ilc.lc.claviusweb;

import com.google.gson.Gson;
import it.cnr.ilc.lc.claviusweb.entity.Annotation;
import it.cnr.ilc.lc.claviusweb.entity.Concept;
import it.cnr.ilc.lc.claviusweb.fulltextsearch.ClaviusHighlighter;
import it.cnr.ilc.lc.claviusweb.listener.PersistenceListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.RollbackException;
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
 */
@WebServlet(name = "ClaviousAccount", urlPatterns = {"/ClaviousAccount/*"})
public class ClaviousAccount extends HttpServlet {

    private static Logger log = LogManager.getLogger(ClaviousAccount.class);

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
        if ("login".equals(command)) {
            String username = "";
            String password = "";
            Long accountId = retrieve(username, password);
            if (accountId != null) {
                HttpSession session = request.getSession(true);
                session.setAttribute("accountId", accountId);
                // mandare indietro YES
            } else {
                // mandare indietro NO
            }
        } else if ("logout".equals(command)) {
            HttpSession session = request.getSession(false);
            if (session == null) {
                // errore ci chiedono logout ma la sessione non esiste
            } else {
                session.invalidate();
            }
        } else if ("getAccountId".equals(command)) {
            HttpSession session = request.getSession(false);
            if (session == null) {
                // nessuno p loggato
            } else {
                Long accountId = (Long) session.getAttribute("accountId");
                if (accountId == null) {
                    // nessuno è loggato
                }
                // mando gson con accountId
            }
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

    private Long retrieve(String username, String password) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}

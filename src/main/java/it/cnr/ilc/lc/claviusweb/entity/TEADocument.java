/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.ilc.lc.claviusweb.entity;

import java.util.List;

/**
 *
 * @author simone
 */
public class TEADocument {

    public Long id;
    public String name;
    public String code;
    public Object graph;
    public String text;
    public String idDoc;
    public List<Triple> triples;

    public static class Triple {

        public Integer start;
        public Integer end;
        public String subject;
        public String predicate;
        public String object;

        public Triple(Integer start, Integer end, String subject, String predicate, String object) {
            this.start = start;
            this.end = end;
            this.subject = subject;
            this.predicate = predicate;
            this.object = object;
        }

        @Override
        public String toString() {
            return "Triple: " + start + " " + end + " " + object;
        }

    }

    @Override
    public String toString() {
        return id + " " + name + " " + triples.toString();
    }

}

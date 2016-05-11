/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.ilc.lc.claviusweb.entity;

import com.google.gson.Gson;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author simone
 */
public class Concept {

    public String uri;
    public Integer count = 99;

    @Override
    public String toString() {
        return "Concept: " + uri + " " + count;
    }

    public static void main(String[] args) {
        String s = "[{\"uri\":\"http://blablabla.com\"},{\"uri\":\"http://blulbulbu.org\"}]";

        List<Concept> cs = Arrays.asList(new Gson().fromJson(s, Concept[].class));
        System.err.println("ConceptSet: " + cs);

        String json = new Gson().toJson(cs.toArray(), Concept[].class);

        System.err.println("Json: " + json);
    }
}

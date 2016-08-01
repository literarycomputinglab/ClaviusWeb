/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.ilc.lc.claviusweb.tests;

import com.google.gson.Gson;
import it.cnr.ilc.lc.claviusweb.ClaviusAccount;
import it.cnr.ilc.lc.claviusweb.entity.User;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author angelo
 */
public class Test {

    private static Logger log = LogManager.getLogger(Test.class);

    public String test(Object o) {
        String ret = "";

        try {

            if (((String) o).equals("lancia")) {
                throw new Exception("la stringa " + (String) o + " è stata lanciata");
            }
        } catch (Exception e) {
            log.error("eccezione", e);

        }

        ret = "ritorno";

        return ret;
    }

    public static void main(String[] args) {

//        String userJson = "{\"username\":\"paolo12\",\"password\":\"pippo\",\"email\":\"pluto@gmail.com\",\"resourses\":[1,2,3],\"accountID\":-1}";
//                "{\"username\":\"paolo12\", \"password\":\"pippo\", \"email\":\"pluto@gmail.com\", \"resources\":[1,2,3]}";
//        log.info(userJson);
//
//        Gson gson = new Gson();
//        User u = gson.fromJson(userJson, User.class);
//        u.setAccountID(Long.valueOf(u.hashCode()));
//
//        log.info("test:" + u.toString());
//        List<Integer> l = new ArrayList<>();
//        l.add(1);
//        l.add(2);
//        l.add(3);
//        log.info(l);
//        
//        u.setResourses(l);
//        
//        String userJsonResponse = gson.toJson(u);
//
//        log.info(userJsonResponse);
        Test t = new Test();

        log.info(t.test("lancia"));

    }
}

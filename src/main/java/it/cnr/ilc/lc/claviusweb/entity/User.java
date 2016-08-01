/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.ilc.lc.claviusweb.entity;

import java.io.Serializable;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

/**
 *
 * @author angelo
 */
@Entity
public class User implements Serializable {

    @Id
    @GeneratedValue
    private Long id;

    @Column(length = 255)
    private String username;

    @Column(length = 255)
    private String password;

    @Column(length = 255)
    private String email;

    //@OneToMany(cascade = CascadeType.ALL)
    @ElementCollection
    @CollectionTable(name = "resources", joinColumns = @JoinColumn(name = "userID"))
    @Column(name = "resource")
    private List<Integer> resources;

    @Column(length = 255)
    private Long accountID;

    public User() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<Integer> getResourses() {
        return resources;
    }

    public void setResourses(List<Integer> resourses) {
        this.resources = resourses;
    }

    public Long getAccountID() {
        return accountID;
    }

    public void setAccountID(Long accountID) {
        this.accountID = accountID;
    }

    @Override
    public String toString() {
        return (String.format("User=(%s) : [%s - %s] [%d], Resources:", 
                (this.username != null) ? this.username : "", 
                (this.email != null) ? this.email : "", 
                (this.password != null) ? this.password : "", 
                (this.accountID != null) ? this.accountID : Long.MIN_VALUE)).concat((this.resources != null) ? this.resources.toString() : "[]");

    }

}

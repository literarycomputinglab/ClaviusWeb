/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.ilc.lc.claviusweb.entity;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.Facet;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;

/**
 *
 * @author angelo
 */
@Entity
@Indexed
public class Annotation implements Serializable {

    @Id
    @GeneratedValue
    private Long id;

    @Field
    private Long idNeo4j;

    @Field
    private Long idLetter;

    @Field
    private Long pageNum;

    @Field
    @Column(length = 1024)
    private String leftContext;

    @Column(length = 1024)
    @Field(analyzer = @Analyzer(impl = WhitespaceAnalyzer.class))
    private String matched;

    @Field
    @Column(length = 1024)
    private String rightContext;

    @Field(analyze = Analyze.NO)
    @Facet
    private String type; //denotes

    @Field(analyze = Analyze.NO)
    @Facet
    private String resourceObject; //object della tripla

    @Field(analyzer = @Analyzer(impl = WhitespaceAnalyzer.class))
    private String concept;

    
    public String getMatched() {
        return matched;
    }

    public void setMatched(String matched) {
        this.matched = matched;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getIdNeo4j() {
        return idNeo4j;
    }

    public void setIdNeo4j(Long idNeo4j) {
        this.idNeo4j = idNeo4j;
    }

    public Long getIdLetter() {
        return idLetter;
    }

    public void setIdLetter(Long idLetter) {
        this.idLetter = idLetter;
    }

    public Long getPageNum() {
        return pageNum;
    }

    public void setPageNum(Long pageNum) {
        this.pageNum = pageNum;
    }

    public String getLeftContext() {
        return leftContext;
    }

    public void setLeftContext(String leftContext) {
        this.leftContext = leftContext;
    }

    public String getRightContext() {
        return rightContext;
    }

    public void setRightContext(String rightContext) {
        this.rightContext = rightContext;
    }

    public String getResourceObject() {
        return resourceObject;
    }

    public void setResourceObject(String resourceObject) {
        this.resourceObject = resourceObject;
    }

    public String getConcept() {
        return concept;
    }

    public void setConcept(String concept) {
        this.concept = concept;
    }

    @Override
    public String toString() {
        return String.format("Letter=(%d) [%d]:[%s] [%s - %s] [%s], %s, %s", this.idLetter, this.id, this.leftContext, this.type, this.matched, this.rightContext, this.concept, this.resourceObject);
    }

}

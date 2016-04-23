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
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.pattern.PatternReplaceCharFilterFactory;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.AnalyzerDef;
import org.hibernate.search.annotations.CharFilterDef;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.annotations.TermVector;
import org.hibernate.search.annotations.TokenizerDef;

/**
 *
 * @author simone
 */
@Entity
@Indexed
@AnalyzerDef(name = "claviusPlainTextAnalyer", charFilters
        = {
            @CharFilterDef(factory = PatternReplaceCharFilterFactory.class, params
                    = {
                @Parameter(name = "pattern", value = "[\\-\\(\\)\\[\\],\\.;:]"),
                @Parameter(name = "replacement", value = "")
            })
        }, tokenizer = @TokenizerDef(factory = WhitespaceTokenizerFactory.class))

public class PlainText implements Serializable {

    @Id
    @GeneratedValue
    private Long id;

    @Field(analyzer = @Analyzer(definition = "claviusPlainTextAnalyer"), store = Store.YES, analyze = Analyze.YES, termVector = TermVector.WITH_POSITION_OFFSETS)
    @Column(length = 65536)
    private String content;

    @Field(analyze = Analyze.NO, store = Store.YES)
    private String idDoc;

    @Field(analyzer = @Analyzer(definition = "claviusPlainTextAnalyer"))
    @Column(length = 4096)
    private String extra = "default extra text";

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getIdDoc() {
        return idDoc;
    }

    public void setIdDoc(String idDoc) {
        this.idDoc = idDoc;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

}

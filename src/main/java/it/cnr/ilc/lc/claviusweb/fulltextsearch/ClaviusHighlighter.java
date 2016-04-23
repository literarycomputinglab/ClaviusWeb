/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.ilc.lc.claviusweb.fulltextsearch;

import it.cnr.ilc.lc.claviusweb.entity.Annotation;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.search.highlight.Encoder;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.Scorer;

/**
 *
 * @author simone
 */
public class ClaviusHighlighter extends Highlighter {

    private static Logger log = LogManager.getLogger(ClaviusHighlighter.class);

    
    private static final int ctxLenght = 20;
    
    Formatter formatter;

    public ClaviusHighlighter(Scorer fragmentScorer) {
        super(fragmentScorer);
    }

    public ClaviusHighlighter(Formatter formatter, Scorer fragmentScorer) {
        super(formatter, fragmentScorer);
        this.formatter = formatter;

    }

    public ClaviusHighlighter(Formatter formatter, Encoder encoder, Scorer fragmentScorer) {
        super(formatter, encoder, fragmentScorer);
        this.formatter = formatter;

    }

    public final List<Annotation> getBestTextClaviusFragments(
            TokenStream tokenStream,
            String idDoc,
            boolean mergeContiguousFragments,
            int maxNumFragments)
            throws IOException, InvalidTokenOffsetsException {

        List<Annotation> ret = new ArrayList<>();

        ArrayList<ClaviusTextFragment> docFrags = new ArrayList<>();
        StringBuilder newText = new StringBuilder();

        Scorer fragmentScorer = getFragmentScorer();
        Fragmenter textFragmenter = getTextFragmenter();
        int maxDocCharsToAnalyze = getMaxDocCharsToAnalyze();
        Encoder encoder = getEncoder();

        CharTermAttribute termAtt = tokenStream.addAttribute(CharTermAttribute.class);
        OffsetAttribute offsetAtt = tokenStream.addAttribute(OffsetAttribute.class);
        ClaviusTextFragment currentFrag = new ClaviusTextFragment(newText, newText.length(), docFrags.size());

        if (fragmentScorer instanceof QueryScorer) {
            ((QueryScorer) fragmentScorer).setMaxDocCharsToAnalyze(Integer.MAX_VALUE);
        }

        TokenStream newStream = fragmentScorer.init(tokenStream);
        if (newStream != null) {
            tokenStream = newStream;
        }
        fragmentScorer.startFragment(currentFrag);
        docFrags.add(currentFrag);

//        FragmentQueue fragQueue = new FragmentQueue(maxNumFragments);
        try {

            String tokenText;
            int startOffset;
            int endOffset;
            int lastEndOffset = 0;
            //textFragmenter.start(text, tokenStream);

            ClaviusTokenGroup tokenGroup = new ClaviusTokenGroup(tokenStream);

            tokenStream.reset();
            //log.info("tokenGroup.getNumTokens() A: " + tokenGroup.getNumTokens());

            for (boolean next = tokenStream.incrementToken(); next && (offsetAtt.startOffset() < maxDocCharsToAnalyze);
                    next = tokenStream.incrementToken()) {

//                if ((offsetAtt.endOffset() > text.length())
//                        || (offsetAtt.startOffset() > text.length())) {
//                    throw new InvalidTokenOffsetsException("Token " + termAtt.toString()
//                            + " exceeds length of provided text sized " + text.length());
//                }
                //  log.info("newText: A (" + newText.toString() + "), fragmentScorer.getTokenScore()("+fragmentScorer.getTokenScore()+")");
                tokenGroup.addToken(fragmentScorer.getTokenScore());

            }// END FOR
             //log.info("tokenGroup.getNumTokens() B: " + tokenGroup.getNumTokens());

            for (int i = 0; i < tokenGroup.getNumTokens(); i++) {
                //log.info("tokenGroup[" + i + "]: token: " + tokenGroup.getToken(i) + ", score: " + tokenGroup.getScore(i));
                if (tokenGroup.getScore(i) > 0) {
                    Annotation a = new Annotation();
                    a.setMatched(tokenGroup.getToken(i).toString());
                    a.setIdDoc(idDoc);
                    //contesto sinistro
                    Token[] t = Arrays.copyOfRange(tokenGroup.getTokens(), (i > ctxLenght) ? i - ctxLenght : 0, i);
                    StringBuilder sb = new StringBuilder();
                    for (int j = 0; j < t.length; j++) {
                        sb.append(t[j].toString());
                        if (j < t.length - 1) {
                            sb.append(" ");
                        }
                    }
                    a.setLeftContext(sb.toString());
                    sb.setLength(0);
                    //contesto destro
                    t = Arrays.copyOfRange(tokenGroup.getTokens(), i + 1, (i + ctxLenght +1 < tokenGroup.getNumTokens() ? i + ctxLenght+1 : tokenGroup.getNumTokens()));
                    sb = new StringBuilder();
                    for (int j = 0; j < t.length; j++) {
                        sb.append(t[j].toString());
                        if (j < t.length - 1) {
                            sb.append(" ");
                        }
                    }
                    a.setRightContext(sb.toString());
                    
                    a.setConcept("");
                    a.setType("");
                    a.setIdNeo4j(-1l);
                    a.setPageNum(-1l);
                    a.setResourceObject("");
                    a.setId(-1l);
                    
                    ret.add(a);
                }
            }

            return ret;

        } finally {
            if (tokenStream != null) {
                try {
                    tokenStream.end();
                    tokenStream.close();
                } catch (Exception e) {
                }
            }
        }
    }

}

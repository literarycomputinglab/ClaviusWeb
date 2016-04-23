/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.ilc.lc.claviusweb.fulltextsearch;

import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.search.highlight.TokenGroup;

/**
 *
 * @author simone
 */
public class ClaviusTokenGroup extends TokenGroup {

    private static final int MAX_NUM_TOKENS_PER_GROUP = 5000;

    private Token[] tokens = new Token[MAX_NUM_TOKENS_PER_GROUP];
    private float[] scores = new float[MAX_NUM_TOKENS_PER_GROUP];
    private int numTokens = 0;
    private int startOffset = 0;
    private int endOffset = 0;
    private float tot;
    private int matchStartOffset;
    private int matchEndOffset;

    private OffsetAttribute offsetAtt;
    private CharTermAttribute termAtt;

    public ClaviusTokenGroup(TokenStream tokenStream) {
        super(tokenStream);
        this.offsetAtt = tokenStream.addAttribute(OffsetAttribute.class);
        this.termAtt = tokenStream.addAttribute(CharTermAttribute.class);
    }

    void addToken(float score) {
        if (numTokens < MAX_NUM_TOKENS_PER_GROUP) {
            final int termStartOffset = offsetAtt.startOffset();
            final int termEndOffset = offsetAtt.endOffset();
            if (numTokens == 0) {
                startOffset = matchStartOffset = termStartOffset;
                endOffset = matchEndOffset = termEndOffset;
                tot += score;
            } else {
                startOffset = Math.min(startOffset, termStartOffset);
                endOffset = Math.max(endOffset, termEndOffset);
                if (score > 0) {
                    if (tot == 0) {
                        matchStartOffset = termStartOffset;
                        matchEndOffset = termEndOffset;
                    } else {
                        matchStartOffset = Math.min(matchStartOffset, termStartOffset);
                        matchEndOffset = Math.max(matchEndOffset, termEndOffset);
                    }
                    tot += score;
                }
            }
            Token token = new Token();
            token.setOffset(termStartOffset, termEndOffset);
            token.setEmpty().append(termAtt);
            tokens[numTokens] = token;
            scores[numTokens] = score;
            numTokens++;
        }
    }

    boolean isDistinct() {
        return offsetAtt.startOffset() >= endOffset;
    }

    void clear() {
        numTokens = 0;
        tot = 0;
    }

    /**
     * @param index a value between 0 and numTokens -1
     * @return the "n"th token
     */
    public Token getToken(int index) {
        return tokens[index];
    }

    public int  numTokens() {
        return tokens.length;
    }  

    
    public Token[] getTokens() {
        return tokens;
    }
    
    
    /**
     *
     * @param index a value between 0 and numTokens -1
     * @return the "n"th score
     */
    public float getScore(int index) {
        return scores[index];
    }

    /**
     * @return the earliest start offset in the original text of a matching
     * token in this group (score &gt; 0), or if there are none then the
     * earliest offset of any token in the group.
     */
    public int getStartOffset() {
        return matchStartOffset;
    }

    /**
     * @return the latest end offset in the original text of a matching token in
     * this group (score &gt; 0), or if there are none then
     * {@link #getEndOffset()}.
     */
    public int getEndOffset() {
        return matchEndOffset;
    }

    /**
     * @return the number of tokens in this group
     */
    public int getNumTokens() {
        return numTokens;
    }

    /**
     * @return all tokens' scores summed up
     */
    public float getTotalScore() {
        return tot;
    }

}

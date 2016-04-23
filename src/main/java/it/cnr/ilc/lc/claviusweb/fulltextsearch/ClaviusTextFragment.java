/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.cnr.ilc.lc.claviusweb.fulltextsearch;

import org.apache.lucene.search.highlight.TextFragment;

/**
 *
 * @author simone
 */
public class ClaviusTextFragment extends TextFragment {

    CharSequence markedUpText;
    int fragNum;
    int textStartPos;
    int textEndPos;
    float score;

    public ClaviusTextFragment(CharSequence markedUpText, int textStartPos, int fragNum) {
        super(markedUpText, textStartPos, fragNum);
        this.markedUpText = markedUpText;
        this.textStartPos = textStartPos;
        this.fragNum = fragNum;
    }

    void setScore(float score) {
        this.score = score;
    }

    public float getScore() {
        return score;
    }

    /**
     * @param frag2 Fragment to be merged into this one
     */
    public void merge(ClaviusTextFragment frag2) {
        textEndPos = frag2.textEndPos;
        score = Math.max(score, frag2.score);
    }

    /**
     * @return true if this fragment follows the one passed
     */
    public boolean follows(ClaviusTextFragment fragment) {
        return textStartPos == fragment.textEndPos;
    }

    /**
     * @return the fragment sequence number
     */
    public int getFragNum() {
        return fragNum;
    }

    /* Returns the marked-up text for this text fragment
     */
    @Override
    public String toString() {
        return markedUpText.subSequence(textStartPos, textEndPos).toString();
    }

}

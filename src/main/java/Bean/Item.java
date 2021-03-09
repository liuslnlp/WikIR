package Bean;

import com.alibaba.fastjson.annotation.JSONField;

public class Item {
    @JSONField(name = "doc")
    private String doc;

    @JSONField(name = "score")
    private float score;

    public Item(String doc, float score) {
        super();
        this.doc = doc;
        this.score = score;
    }

    public String getDoc() {
        return doc;
    }

    public void setDoc(String doc) {
        this.doc = doc;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }
}


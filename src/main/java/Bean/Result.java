package Bean;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.ArrayList;
import java.util.List;

public class Result {
    @JSONField(name = "lineno")
    private int lineno;

    @JSONField(name = "docs")
    private List<Item> docs = new ArrayList<>();

    public Result(int lineno) {
        super();
        this.lineno = lineno;
    }

    public void addItem(String doc, float score) {
        docs.add(new Item(doc, score));
    }

    public int getLineno() {
        return lineno;
    }

    public void setLineno(int lineno) {
        this.lineno = lineno;
    }

    public List<Item> getDocs() {
        return docs;
    }

    public void setDocs(List<Item> docs) {
        this.docs = docs;
    }
}
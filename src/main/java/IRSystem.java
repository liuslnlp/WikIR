import Bean.Result;
import com.alibaba.fastjson.JSON;
import com.csvreader.CsvReader;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class Line {
    int lineno;
    String line;

    public Line(int lineno, String line) {
        this.lineno = lineno;
        this.line = line;
    }

    public int getLineno() {
        return lineno;
    }

    public String getLine() {
        return line;
    }

}

public class IRSystem {
    final private String directoryPath;
    final private String srcFilePath;
    final private Analyzer analyzer = new StandardAnalyzer();
    final private Similarity similarity = new BM25Similarity();

    IRSystem(String dicPath, String srcPath) {
        directoryPath = dicPath;
        srcFilePath = srcPath;
    }

    public void createIndex() throws Exception {
        Directory directory = MMapDirectory.open(Paths.get(directoryPath));
        IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
        indexWriterConfig.setSimilarity(similarity);
        indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter indexWriter = new IndexWriter(directory, indexWriterConfig);
        CsvReader csvReader = new CsvReader(srcFilePath);
        csvReader.readHeaders();
        while (csvReader.readRecord()) {
            String title = csvReader.get("title");
            String text = csvReader.get("text");
            Document document = new Document();
            document.add(new TextField("title", title, Field.Store.YES));
            document.add(new TextField("text", text, Field.Store.YES));
            indexWriter.addDocument(document);
        }
        int numDocs = indexWriter.getDocStats().numDocs;
        System.out.println("A total of " + numDocs + " objects are indexed.");
        indexWriter.close();
    }

    private List<Line> readLines(String filename) throws Exception {
        FileInputStream inputStream = new FileInputStream(filename);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String str;
        List<Line> lines = new ArrayList<>();
        int lineno = 0;
        while ((str = bufferedReader.readLine()) != null) {
            lines.add(new Line(lineno, str));
            lineno++;
        }
        inputStream.close();
        bufferedReader.close();
        return lines;
    }

    private void writeJSON(List<Result> results, String OutputFile) throws Exception {
        String jsonOutput = JSON.toJSONString(results);
        BufferedWriter out = new BufferedWriter(new FileWriter(OutputFile));
        out.write(jsonOutput);
        out.close();
    }

    public void searchParallel(String queryFile, String OutputFile, int nBest) throws Exception {
        Directory directory = MMapDirectory.open(Paths.get(directoryPath));
        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        indexSearcher.setSimilarity(similarity);
        List<Line> lines = readLines(queryFile);
        List<Result> results = lines.parallelStream().map((Line s) -> {
            Result result;
            try {
                result = search(s.getLineno(), s.getLine(), nBest, indexSearcher);
            } catch (Exception e) {
                e.printStackTrace();
                result = new Result(s.getLineno());
            }
            return result;
        }).collect(Collectors.toList());
        writeJSON(results, OutputFile);
        indexReader.close();
    }

    private Result search(int lineno, String line, int nBest, IndexSearcher searcher) throws Exception {
        QueryParser parser = new QueryParser("text", analyzer);
        String str = QueryParser.escape(line);
        Query query = parser.parse(str);
        TopDocs topDocs = searcher.search(query, nBest);
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;
        Result result = new Result(lineno);
        for (ScoreDoc s : scoreDocs) {
            Document doc = searcher.doc(s.doc);
            String text = doc.get("text");
            float score = s.score;
            result.addItem(text, score);
        }
        return result;
    }

    public void searchSerial(String queryFile, String OutputFile, int nBest) throws Exception {
        Directory directory = MMapDirectory.open(Paths.get(directoryPath));
        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        indexSearcher.setSimilarity(similarity);
        FileInputStream inputStream = new FileInputStream(queryFile);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        List<Result> results = new ArrayList<>();
        String str;
        int lineno = 0;
        while ((str = bufferedReader.readLine()) != null) {
            Result result = search(lineno, str, nBest, indexSearcher);
            results.add(result);
            if ((lineno + 1) % 100000 == 0) {
                System.out.println((lineno + 1) + " records have been processed");
            }
            lineno = lineno + 1;
        }
        writeJSON(results, OutputFile);
        indexReader.close();
        inputStream.close();
        bufferedReader.close();
    }
}

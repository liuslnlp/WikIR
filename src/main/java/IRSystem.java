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
import org.apache.lucene.queryparser.classic.ParseException;
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
import java.util.concurrent.atomic.AtomicInteger;
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

    private Directory getDirectory() throws IOException {
        return MMapDirectory.open(Paths.get(directoryPath));
    }

    public void createIndex() throws Exception {
        Directory directory = getDirectory();
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

    public void searchParallel(String queryFile, String outputFile, int nBest, int chunkSize) throws Exception {
        Directory directory = getDirectory();
        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        indexSearcher.setSimilarity(similarity);
        List<Line> lines = readLines(queryFile);
        final AtomicInteger counter = new AtomicInteger();
        for (int i = 0; i < lines.size(); i += chunkSize) {
            String chunkOutputFile = outputFile + ".chunk_" + i;
            searchOneChunk(lines.subList(i, i + chunkSize), chunkOutputFile, nBest, indexSearcher, counter, lines.size());
        }
        indexReader.close();
    }

    public void searchParallel(String queryFile, String outputFile, int nBest) throws Exception {
        Directory directory = getDirectory();
        IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
        indexSearcher.setSimilarity(similarity);
        List<Line> lines = readLines(queryFile);
        final AtomicInteger counter = new AtomicInteger();
        searchOneChunk(lines, outputFile, nBest, indexSearcher, counter, lines.size());
        indexReader.close();
    }

    private void searchOneChunk(List<Line> lines, String outputFile, int nBest, IndexSearcher indexSearcher, final AtomicInteger counter, int elementsCount) throws Exception {
        int fivePercent = elementsCount / 20;
        List<Result> results = lines.parallelStream().map((Line s) -> {
            return search(s.getLineno(), s.getLine(), nBest, indexSearcher);
        }).peek(stat -> {
            if (counter.incrementAndGet() % fivePercent == 0) {
                String info = "[" + (5 * (counter.get() / fivePercent)) + "%] " + counter.get() + " elements on " + elementsCount + " treated.";
                System.out.println(info);
            }
        }).collect(Collectors.toList());
        writeJSON(results, outputFile);
    }

    private Result search(int lineno, String line, int nBest, IndexSearcher searcher) {
        QueryParser parser = new QueryParser("text", analyzer);
        String str = QueryParser.escape(line);
        Query query = null;
        Result result = new Result(lineno);
        try {
            query = parser.parse(str);
        } catch (ParseException e) {
            e.printStackTrace();
            return result;
        }
        TopDocs topDocs = null;
        try {
            topDocs = searcher.search(query, nBest);
        } catch (IOException e) {
            e.printStackTrace();
            return result;
        }
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;

        for (ScoreDoc s : scoreDocs) {
            Document doc = null;
            try {
                doc = searcher.doc(s.doc);
            } catch (IOException e) {
                e.printStackTrace();
                return result;
            }
            String text = doc.get("text");
            float score = s.score;
            result.addItem(text, score);
        }
        return result;
    }

    public void searchSerial(String queryFile, String outputFile, int nBest) throws Exception {
        searchSerial(queryFile, outputFile, nBest, 100000);
    }

    public void searchSerial(String queryFile, String outputFile, int nBest, int logSteps) throws Exception {
        Directory directory = getDirectory();
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
            if ((lineno + 1) % logSteps == 0) {
                System.out.println((lineno + 1) + " records have been processed");
            }
            lineno = lineno + 1;
        }
        writeJSON(results, outputFile);
        indexReader.close();
        inputStream.close();
        bufferedReader.close();
    }
}

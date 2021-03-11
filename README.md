# WikIR

WikIR is an information retrieval system implemented by Java and Lucene. Given a query, WikIR will retrieve several
documents with the highest similarity from a large-scale knowledge base.

## Build the project

Make sure your Java version is greater than 8 and Maven is installed. Enter the project root directory and run

```shell
mvn package
```

Then you will get `target/WikIR.jar`, and you can run `demo.sh` to test it.

## Getting Started

* Run `download.py` to get the dump of Wikipedia articles (~2GB) from 2nd March, 2018. You can also use your own data,
  please refer to `data/wikipedia_demo.csv` for the file format.
* Put all the queries into one file line by line (refer to `data/query_demo.txt`).
* Run `target/WikIR.jar` to get the query results (refer to `demo.sh`).

```shell
usage: WikIR
 -b,--buildIndex         Whether build index.
 -c,--chunkSize <arg>    The size of each block of the output file. If not
                         specified, the retrieval results will not be
                         chunked.
 -d,--indexDir <arg>     Index file directory.
 -i,--queryFile <arg>    Query file.
 -n,--nBest <arg>        The number of documents returned by each query.
 -o,--resultPath <arg>   Query results output path.
 -p,--parallel           Whether to search in parallel.
 -s,--srcFile <arg>      Wikipedia dump file.
```

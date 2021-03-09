directoryPath=data/wikipedia_index
srcFilePath=data/wikipedia_demo.csv
queryFilePath=data/query_demo.txt
queryOutputPath=data/result.json
nBest=10

java -jar target/WikIR.jar -d $directoryPath -s $srcFilePath -i $queryFilePath -o $queryOutputPath -n $nBest -p -b
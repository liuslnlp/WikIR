import org.apache.commons.cli.*;

import java.util.Date;

public class App {
    private static void printArgs(CommandLine cmd) {
        Option[] opts = cmd.getOptions();
        if (opts != null) {
            for (Option opt1 : opts) {
                String name = opt1.getLongOpt();
                String value = cmd.getOptionValue(name);
                System.out.println(name + ": " + value);
            }
        }
    }

    private static CommandLine getCLIOptions(String[] args) {
        Options options = new Options();
        options.addOption(Option.builder("d").longOpt("indexDir").type(String.class).desc("Index file directory.").hasArg().required().build());
        options.addOption(Option.builder("s").longOpt("srcFile").type(String.class).desc("Wikipedia dump file.").hasArg().required().build());
        options.addOption(Option.builder("i").longOpt("queryFile").type(String.class).desc("Query file.").hasArg().required().build());
        options.addOption(Option.builder("o").longOpt("resultPath").type(String.class).desc("Query results output path.").hasArg().required().build());
        options.addOption(Option.builder("n").longOpt("nBest").type(Short.TYPE).desc("The number of documents returned by each query.").hasArg().required().build());
        options.addOption(Option.builder("c").longOpt("chunkSize").type(Integer.TYPE).desc("The size of each block of the output file. If not specified, the retrieval results will not be chunked.").hasArg().build());
        options.addOption(Option.builder("b").longOpt("buildIndex").type(String.class).desc("Whether build index.").hasArg(false).build());
        options.addOption(Option.builder("p").longOpt("parallel").type(String.class).desc("Whether to search in parallel.").hasArg(false).build());
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            HelpFormatter helpFormatter = new HelpFormatter();
            helpFormatter.printHelp("WikIR", options);
            System.exit(0);
        }
        return cmd;
    }

    public static void main(String[] args) throws Exception {
        CommandLine cmd = getCLIOptions(args);
        printArgs(cmd);
        String indexDir = cmd.getOptionValue("indexDir");
        String srcFile = cmd.getOptionValue("srcFile");
        String queryFile = cmd.getOptionValue("queryFile");
        String resultPath = cmd.getOptionValue("resultPath");
        int nBest;
        try {
            nBest = Integer.parseInt(cmd.getOptionValue("nBest"));
        } catch (NumberFormatException e) {
            System.out.println("Reset nBest to 5.");
            nBest = 5;
        }
        IRSystem irSys = new IRSystem(indexDir, srcFile);
        if (cmd.hasOption("b")) {
            System.out.println("Building index ...");
            Date before = new Date();
            irSys.createIndex();
            Date after = new Date();
            System.out.println("Search time is " + (after.getTime() - before.getTime()) / 1000 + "s");
        }
        Date before = new Date();
        if (cmd.hasOption("p")) {
            System.out.println("Parallel searching ...");
            if (cmd.hasOption("c")) {
                int chunkSize;
                try {
                    chunkSize = Integer.parseInt(cmd.getOptionValue("chunkSize"));
                } catch (NumberFormatException e) {
                    System.out.println("Reset chunkSize to 50000.");
                    chunkSize = 50000;
                }
                irSys.searchParallel(queryFile, resultPath, nBest, chunkSize);
            }
            irSys.searchParallel(queryFile, resultPath, nBest);
        } else {
            System.out.println("Searching ...");
            irSys.searchSerial(queryFile, resultPath, nBest);
        }
        Date after = new Date();
        System.out.println("Search time is " + (after.getTime() - before.getTime()) / 1000 + "s");
    }
}

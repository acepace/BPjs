package il.ac.bgu.cs.bp.bpjs.mains;
import il.ac.bgu.cs.bp.bpjs.analysis.BProgramStateVisitedStateStore;
import il.ac.bgu.cs.bp.bpjs.analysis.DfsBProgramVerifier;
import il.ac.bgu.cs.bp.bpjs.analysis.Node;
import il.ac.bgu.cs.bp.bpjs.analysis.VerificationResult;
import il.ac.bgu.cs.bp.bpjs.execution.BProgramRunner;
import il.ac.bgu.cs.bp.bpjs.execution.listeners.PrintBProgramRunnerListener;
import il.ac.bgu.cs.bp.bpjs.model.BProgram;
import il.ac.bgu.cs.bp.bpjs.model.BThreadSyncSnapshot;
import il.ac.bgu.cs.bp.bpjs.model.eventselection.EventSelectionStrategy;
import il.ac.bgu.cs.bp.bpjs.model.eventselection.LoggingEventSelectionStrategyDecorator;
import il.ac.bgu.cs.bp.bpjs.model.eventselection.SimpleEventSelectionStrategy;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Scriptable;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.*;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@CommandLine.Command(name = "BPjs Execution Runtime\n", mixinStandardHelpOptions = true, version = "BPJs version 0.9.1",
header="This program takes a series of BPjs files, and executes them \n" +
        "as a single BProgram.\n",footer="BProgram log and event sequence are written to stdout.")
public class BPJsPicoliRunner implements Runnable {
    @Option(names = { "-v", "--verbose" }, description = "Verbose mode, will log to console steps")
    private boolean verbose = false;




    @Option(names = { "-@", "--stdin" }, description = "Receives input from stdin")
    private boolean stdin = false;

    @Option(names = {"-P", "--param"})
    private Map<String, Object> parameters = new HashMap<>();

    @Parameters(arity = "0..*", paramLabel = "FILE", description = "File(s) to process. At least one")
    private File[] inputFiles;


    @Option(names = { "--verify" }, description = "Verification mode.")
    private boolean verification = false;

    @Option(names = { "-d", "--deadlock" }, description = "Verification mode, @|bold disables @| deadlock checking")
    private boolean deadlock = true;

    @Option(names = {"--hash" }, description = "Verification mode Use hash based node comparison")
    private boolean hashCompare = false;


    public void run() {
        BProgram bpp = new BProgram("BPjs") {
            @Override
            protected void setupProgramScope(Scriptable scope) {
                if (inputFiles != null) {
                    for (File file : inputFiles) {

                        try (InputStream in = Files.newInputStream(file.toPath())) {
                            evaluate(in, file.getName());
                        } catch (EvaluatorException ee) {
                            logScriptExceptionAndQuit(ee, file.getName());
                        } catch (IOException ex) {
                            println("Exception while processing " + file.getName() + ": " + ex.getMessage());
                            Logger.getLogger(BPJsPicoliRunner.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                if (stdin) {
                    println(" [READ] stdin");
                    try {
                        evaluate(System.in, "stdin");
                    } catch (EvaluatorException ee) {
                        logScriptExceptionAndQuit(ee, "stdin");
                    }
                }
            }

            private void logScriptExceptionAndQuit(EvaluatorException ee, String arg) {
                println("Error in source %s:", arg);
                println(ee.details());
                println("line: " + ee.lineNumber() + ":" + ee.columnNumber());
                println("source: " + ee.lineSource());
                System.exit(-3);
            }
        };

        parameters.forEach(bpp::putInGlobalScope);
        SimpleEventSelectionStrategy sess = new SimpleEventSelectionStrategy();
        EventSelectionStrategy ess = verbose ? new LoggingEventSelectionStrategyDecorator(sess) : sess;

        bpp.setEventSelectionStrategy(ess);


        if (verification) {
            DfsBProgramVerifier sut = new DfsBProgramVerifier();
            sut.setDetectDeadlocks(deadlock);
            sut.setVisitedNodeStore(new BProgramStateVisitedStateStore(hashCompare));
            try {
                VerificationResult res =sut.verify(bpp);
                if (res.isVerifiedSuccessfully()) {
                    println("Program successfully verified");
                } else {
                    printCounterExample(res);
                }
            } catch (Exception e) {
                println("Error while verifying:", e.getMessage());
                e.printStackTrace();
                System.exit(-3);
            }
        } else {
            BProgramRunner bpr = new BProgramRunner(bpp);
            if (verbose) {
                bpr.addListener(new PrintBProgramRunnerListener());
            }
            bpr.run();
        }
    }

    public static void main(String[] args) {
        CommandLine.run(new BPJsPicoliRunner(), System.out, args);
    }

    private static void println(String template, String... params) {
        print(template + "\n", params);
    }

    private static void print(String template, String... params) {
        if (params.length == 0) {
            System.out.print("# " + template);
        } else {
            System.out.printf("# " + template, (Object[]) params);
        }
    }

    private static void printCounterExample(VerificationResult res) {
        System.out.println("Found a counterexample");
        final List<Node> trace = res.getCounterExampleTrace();
        trace.forEach(nd -> System.out.println(" " + nd.getLastEvent()));

        Node last = trace.get(trace.size() - 1);
        System.out.println("selectableEvents: " + last.getSelectableEvents());
        last.getSystemState().getBThreadSnapshots().stream()
                .sorted(Comparator.comparing(BThreadSyncSnapshot::getName))
                .forEach(s -> {
                    System.out.println(s.getName());
                    System.out.println(s.getBSyncStatement());
                    System.out.println();
                });
    }
}
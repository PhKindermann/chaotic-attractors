package model;

import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.decorators.AbstractVertexShapeTransformer;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel;
import io.SwappingMatrixReader;
import org.apache.commons.lang.StringUtils;
import org.javatuples.Pair;
import util.DefaultVisualizationModelWithoutReiterating;
import util.NoRealizationException;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class MainClass {

    private static final String PATH_TO_EXAMPLES = "../examples/";
    private static final String PATH_TO_SVG_TARGET_DIR = "our_java_drawings";
    private static final String PATH_TO_PYTHON_FILE_DIR = "..";
    private static final String PYTHON_FILE_NAME = "svgExporter.py";
    private static final String PATH_SWAPPING_DIAGRAMS = "swapping-diagrams";


    private static final long SEED = 123;
    public static final Random RANDOM = new Random(SEED);


    public static void main(String[] args) throws Exception {
        /*
        Note to the reader coming from our IWOCA submission:
        The donkey's construction for k=4 is the counterexample to disprove our older conjecture
        that all non-separable even lists are feasible (GD'19).
         */
        testDonkeysConstruction();


//        LineSwapper example0 = getInfeasibleEvenExample();
//        drawSwappingDiagram(example0,
//                example0.computeOneRealization().getSwappingDiagramOfMinimumHeight(example0),
//                "../java-code/target/swapping-diagrams/example.svg");
//        visualizeOneRealization(example0);
    }

    /**
     * Computes a solution with minimum height
     */
    private static void computeAllExamplesFromChaoticGDPaper() {
        long startTime = System.currentTimeMillis();
        long computationTime = 0;
        File file = new File(PATH_TO_EXAMPLES);
        for (File exampleFile : file.listFiles()) {
            if (exampleFile.getPath().endsWith(".json") && exampleFile.getPath().contains("5x5")) {
                computationTime = computeTangle(exampleFile, computationTime);
            }
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Needed " + (endTime - startTime) + " ms for reading json files, computing diagrams, writing svg files.");
        System.out.println("The pure Java computation time was " + computationTime + " ms, which is " +
                (100 * computationTime / (endTime - startTime)) + " % of the total time.");
        for (File exampleFile : file.listFiles()) {
            if (exampleFile.getPath().endsWith(".json") && exampleFile.getPath().contains("6x6")) {
                computationTime = computeTangle(exampleFile, computationTime);
            }
        }
        endTime = System.currentTimeMillis();
        System.out.println("Needed " + (endTime - startTime) + " ms for reading json files, computing diagrams, writing svg files.");
        System.out.println("The pure Java computation time was " + computationTime + " ms, which is " +
                (100 * computationTime / (endTime - startTime)) + " % of the total time.");
        for (File exampleFile : file.listFiles()) {
            if (exampleFile.getPath().endsWith(".json") && exampleFile.getPath().contains("7x7")) {
                computationTime = computeTangle(exampleFile, computationTime);
            }
        }
        endTime = System.currentTimeMillis();
        System.out.println("Needed " + (endTime - startTime) + " ms for reading json files, computing diagrams, writing svg files.");
        System.out.println("The pure Java computation time was " + computationTime + " ms, which is " +
                (100 * computationTime / (endTime - startTime)) + " % of the total time.");
    }

    private static long computeTangle(File exampleFile, long currentComputationTime) {
        LineSwapper lineSwapper = SwappingMatrixReader.loadFile(exampleFile.getAbsolutePath());
        long computationStartTime = System.currentTimeMillis();
        RealizationGraph realizationGraphOfMinimumHeight = lineSwapper.computeOneMinHeightRealization();
        long computationEndTime = System.currentTimeMillis();
        currentComputationTime += (computationEndTime - computationStartTime);
        //if there was a solution, draw it, otherwise say that there is no solution
        if (realizationGraphOfMinimumHeight == null) {
            System.out.println("No solution found for " + exampleFile.getPath() + " (infeasible instance).");
        }
        else {
            SwappingDiagram swappingDiagramOfMinimumHeight = realizationGraphOfMinimumHeight.
                    getSwappingDiagramOfMinimumHeight(lineSwapper);
            //our solution as string which we pass completely to python as its first argument
            String outputPathPythonParam = "" + PATH_TO_SVG_TARGET_DIR + File.separator +
                    exampleFile.getName().substring(0, exampleFile.getName().length() - 5) + ".svg";
            drawSwappingDiagram(lineSwapper, swappingDiagramOfMinimumHeight, outputPathPythonParam);
            System.out.println("Completed drawing of " + exampleFile.getPath() + ".");
        }
        return currentComputationTime;
    }

    private static void drawSwappingDiagram(LineSwapper lineSwapper, SwappingDiagram swappingDiagram,
                                            String targetPath) {
        try {
            File targetFile = new File(".." + File.separator + targetPath);
            targetFile.getParentFile().mkdirs();
            if (!targetFile.exists()) {
                targetFile.createNewFile();
            }

            int nPythonParam = lineSwapper.getNumberOfLines();
            int len_LPythonParam = lineSwapper.getNumberOfSwaps();
            String LPythonParam = lineSwapper.getInputMatrixAsString();
            String solutionPythonParam = swappingDiagram.toString();
            targetPath = StringUtils.deleteWhitespace(targetPath);
            LPythonParam = StringUtils.deleteWhitespace(LPythonParam);
            solutionPythonParam = StringUtils.deleteWhitespace(solutionPythonParam);
            Process p = Runtime.getRuntime().exec("python " + PYTHON_FILE_NAME + " " + targetPath + " " + nPythonParam + " " +
                            len_LPythonParam + " " + LPythonParam + " " + solutionPythonParam,
                    null, new File(PATH_TO_PYTHON_FILE_DIR));
            p.waitFor();

            String line;

            BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            while((line = error.readLine()) != null){
                System.out.println(line);
            }
            error.close();

            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while((line=input.readLine()) != null){
                System.out.println(line);
            }

            input.close();

            OutputStream outputStream = p.getOutputStream();
            PrintStream printStream = new PrintStream(outputStream);
            printStream.println();
            printStream.flush();
            printStream.close();
            System.out.println(targetPath + ": Converted to a swapping diagram with " +
                    swappingDiagram.getLayers().size() + " layers.");
        } catch (IOException e) {
            System.err.println("Could not execute "+ PATH_TO_PYTHON_FILE_DIR + File.separator + PYTHON_FILE_NAME +". Path correct? Python file modified?");
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public static void visualizeAllRealizations(LineSwapper lineSwapper) {
        Collection<RealizationGraph> allRealizations = lineSwapper.computeAllRealizations();

        List<AbstractLayout<Pair<OrderedSwap, Integer>, Integer>> allLayouts = new ArrayList<>(allRealizations.size());
        Dimension size = new Dimension(600, 600);
        for (RealizationGraph realization : allRealizations) {
            MyDAGLayout<Pair<OrderedSwap, Integer>, Integer> drawing = new MyDAGLayout<>(realization);
            drawing.setSize(size);
            drawing.initialize();
//          while (!drawing.done()) {
//              drawing.step();
//          }
            allLayouts.add(drawing);
        }
        visualize(allLayouts, size);

    }

    public static void visualizeOneRealization(LineSwapper lineSwapper) {
        RealizationGraph realization = lineSwapper.computeOneRealization();
        if (realization == null) {
            System.out.println("This graph has no realization!");
        }

        List<AbstractLayout<Pair<OrderedSwap, Integer>, Integer>> allLayouts = new ArrayList<>(1);
        Dimension size = new Dimension(600, 600);
        MyDAGLayout<Pair<OrderedSwap, Integer>, Integer> drawing = new MyDAGLayout<>(realization);
        drawing.setSize(size);
        drawing.initialize();
//          while (!drawing.done()) {
//              drawing.step();
//          }
        allLayouts.add(drawing);
        visualize(allLayouts, size);

    }

    private static LineSwapper getInfeasibleEvenExample() {
        int numberOfLines = 12;
        LinkedHashMap<UnorderedSwap, Integer> swap2occurrenceCount = new LinkedHashMap<>();
        Line[] line = new Line[numberOfLines];
        for (int i = 0; i < line.length; ++i) {
            line[i] = new Line(i);
        }

        //swaps between blacks
        for (int i = 0; i < 4; i++) {
            for (int j = i + 1; j < 5; j++) {
                swap2occurrenceCount.put(new UnorderedSwap(line[i], line[j]), 2);
            }
        }

        //swaps between orange and rest
        for (int i = 0; i < 12; i++) {
            if (i != 5 && i != 6 && i != 7) {
                swap2occurrenceCount.put(new UnorderedSwap(line[5], line[i]), 2);
            }
        }

        //swaps between red and black
        swap2occurrenceCount.put(new UnorderedSwap(line[6], line[1]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[6], line[3]), 2);

        //swaps between blue and black
        swap2occurrenceCount.put(new UnorderedSwap(line[7], line[0]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[7], line[2]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[7], line[4]), 2);

        //swaps between red and blue
        swap2occurrenceCount.put(new UnorderedSwap(line[6], line[7]), 2);

        //swaps between red/blue and green
        for (int i = 8; i < 12; i++) {
            swap2occurrenceCount.put(new UnorderedSwap(line[6], line[i]), 2);
            swap2occurrenceCount.put(new UnorderedSwap(line[7], line[i]), 2);
        }

        //swaps between greens
        for (int i = 8; i < 11; i++) {
            for (int j = i + 1; j < 12; j++) {
                swap2occurrenceCount.put(new UnorderedSwap(line[i], line[j]), 2);
            }
        }

        //swaps between black and green
        swap2occurrenceCount.put(new UnorderedSwap(line[0], line[8]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[1], line[8]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[1], line[9]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[2], line[9]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[2], line[10]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[3], line[10]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[3], line[11]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[4], line[11]), 2);

        return new LineSwapper(swap2occurrenceCount, numberOfLines);
    }

    private static LineSwapper getInfeasibleEvenExampleSlightlySmaller(boolean variantAOrB) {
        int numberOfLines = 10;
        LinkedHashMap<UnorderedSwap, Integer> swap2occurrenceCount = new LinkedHashMap<>();
        Line[] line = new Line[numberOfLines];
        for (int i = 0; i < line.length; ++i) {
            line[i] = new Line(i);
        }

        //swaps between blacks
        for (int i = 0; i < 3; i++) {
            for (int j = i + 1; j < 4; j++) {
                swap2occurrenceCount.put(new UnorderedSwap(line[i], line[j]), 2);
            }
        }

        //swaps between orange and rest
        for (int i = 0; i < 10; i++) {
            if (i != 4 && i != 5 && i != 6) {
                swap2occurrenceCount.put(new UnorderedSwap(line[4], line[i]), 2);
            }
        }

        //swaps between red and black
        swap2occurrenceCount.put(new UnorderedSwap(line[variantAOrB ? 5 : 6], line[0]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[variantAOrB ? 5 : 6], line[2]), 2);

        //swaps between blue and black
        swap2occurrenceCount.put(new UnorderedSwap(line[variantAOrB ? 6 : 5], line[1]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[variantAOrB ? 6 : 5], line[3]), 2);

        //swaps between red and blue
        swap2occurrenceCount.put(new UnorderedSwap(line[5], line[6]), 2);

        //swaps between red/blue and green
        for (int i = 7; i < 10; i++) {
            swap2occurrenceCount.put(new UnorderedSwap(line[5], line[i]), 2);
            swap2occurrenceCount.put(new UnorderedSwap(line[6], line[i]), 2);
        }

        //swaps between greens
        for (int i = 7; i < 9; i++) {
            for (int j = i + 1; j < 10; j++) {
                swap2occurrenceCount.put(new UnorderedSwap(line[i], line[j]), 2);
            }
        }

        //swaps between black and green
        swap2occurrenceCount.put(new UnorderedSwap(line[0], line[7]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[1], line[7]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[1], line[8]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[2], line[8]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[2], line[9]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[3], line[9]), 2);

        return new LineSwapper(swap2occurrenceCount, numberOfLines);
    }

    private static LineSwapper getSmallExample0() {
        int numberOfLines = 5;
        LinkedHashMap<UnorderedSwap, Integer> swap2occurrenceCount = new LinkedHashMap<>();
        Line[] line = new Line[numberOfLines];
        for (int i = 0; i < line.length; ++i) {
            line[i] = new Line(i);
        }

        swap2occurrenceCount.put(new UnorderedSwap(line[0], line[1]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[1], line[2]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[1], line[4]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[3], line[4]), 2);
        return new LineSwapper(swap2occurrenceCount, numberOfLines);
    }

    private static LineSwapper getSmallExample1() {
        int numberOfLines = 4;
        LinkedHashMap<UnorderedSwap, Integer> swap2occurrenceCount = new LinkedHashMap<>();
        Line[] line = new Line[numberOfLines];
        for (int i = 0; i < line.length; ++i) {
            line[i] = new Line(i);
        }

        swap2occurrenceCount.put(new UnorderedSwap(line[0], line[1]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[0], line[2]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[0], line[3]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[2], line[3]), 2);
        return new LineSwapper(swap2occurrenceCount, numberOfLines);
    }


    private static LineSwapper get02Example0() {
        int numberOfLines = 8;
        LinkedHashMap<UnorderedSwap, Integer> swap2occurrenceCount = new LinkedHashMap<>();
        Line[] line = new Line[numberOfLines];
        for (int i = 0; i < line.length; ++i) {
            line[i] = new Line(i);
        }

        swap2occurrenceCount.put(new UnorderedSwap(line[0], line[1]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[0], line[2]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[0], line[4]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[0], line[5]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[0], line[7]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[3], line[4]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[3], line[5]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[3], line[7]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[6], line[7]), 2);
        return new LineSwapper(swap2occurrenceCount, numberOfLines);
    }

    private static LineSwapper getClassicRedBlueExample() {
        int numberOfLines = 8;
        LinkedHashMap<UnorderedSwap, Integer> swap2occurrenceCount = new LinkedHashMap<>();
        Line[] line = new Line[numberOfLines];
        for (int i = 0; i < line.length; ++i) {
            line[i] = new Line(i);
        }

        //red-blue swaps
        swap2occurrenceCount.put(new UnorderedSwap(line[3], line[4]), 4);
        //left-side-elements with right-side-elements
        swap2occurrenceCount.put(new UnorderedSwap(line[2], line[5]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[1], line[6]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[0], line[7]), 2);
        //left-side-elements with red/blue
        swap2occurrenceCount.put(new UnorderedSwap(line[2], line[4]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[1], line[3]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[0], line[4]), 2);
        //right-side-elements with red/blue
        swap2occurrenceCount.put(new UnorderedSwap(line[5], line[3]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[6], line[4]), 2);
        swap2occurrenceCount.put(new UnorderedSwap(line[7], line[3]), 2);
        //left-side-elements-one-time
        swap2occurrenceCount.put(new UnorderedSwap(line[0], line[1]), 1);
        swap2occurrenceCount.put(new UnorderedSwap(line[0], line[2]), 1);
        swap2occurrenceCount.put(new UnorderedSwap(line[1], line[2]), 1);
        //right-side-elements-one-time
        swap2occurrenceCount.put(new UnorderedSwap(line[7], line[6]), 1);
        swap2occurrenceCount.put(new UnorderedSwap(line[7], line[5]), 1);
        swap2occurrenceCount.put(new UnorderedSwap(line[6], line[5]), 1);
        return new LineSwapper(swap2occurrenceCount, numberOfLines);
    }

    /**
     *
     * @param m
     *      Parameter from donkey's construction; we have 2^m wires.
     * @param k
     *      Swap multiplicity for the pairs of wires that swap (should be an even number)
     * @return
     *      Line swapping instance specifying that set of swaps
     */
    private static LineSwapper getDonkeysInstance(int m, int k) {
        //init wires
        int numberOfLines = (int) Math.pow(2.0, m);
        LinkedHashMap<UnorderedSwap, Integer> swap2occurrenceCount = new LinkedHashMap<>();
        Line[] line = new Line[numberOfLines];
        for (int i = 0; i < line.length; ++i) {
            line[i] = new Line(i);
        }

        //add swaps. Here is donkey's description:
        /*
        Donkey writes one of most promising list sequences, (L_m), explicitly. Given a natural number m,
        we have 2^m wires numbered from 0 to 2^m-1. Wires i<j do not swap in L_m if each 1 of the binary representation
        of i also belongs to the binary representation of j (that is, the bitwise OR of i and j equals j), otherwise
        i and j swap twice. For instance, the wire 1=001_2 swaps with the wires 2=010_2, 4=100_2, and so forth.
        The wire 2=0010_2 swaps with the wires 4=0100_2, 5=0101_2, 8=1000_2 and so forth.
        That is for m=2 the list L_2 consists of the only double swap 12, for m=3 the list L_3 consists
        of double swaps 12, 14, 16, 24, 25, 34, 35, 36, and 56.

        Donkey expect that the feasibility of lists of the sequence (L_m) can be checked up to m=8 by the programs.
        If all these lists will be feasible and their realization tangle patterns can be generalized to all natural m
        then it will imply that f(n)\le 4 for all natural n. But if for some natural m the list L_m will be
        infeasible then Conjecture will be refuted.
         */

        //go through all pairs of wires and check if they should swap 0 or 2 times
        for (int i = 0; i < numberOfLines - 1; i++) {
            for (int j = i + 1; j < numberOfLines; j++) {
                //check bitwise OR
                if ((i | j) != j) {
                    swap2occurrenceCount.put(new UnorderedSwap(line[i], line[j]), k);
                }
            }
        }
        System.out.println("Donkey's instance for m = " + m + " is non-separable is " +
                isNonSeparable(swap2occurrenceCount) + ".");
        return new LineSwapper(swap2occurrenceCount, numberOfLines);
    }

    /**
     * Runs forever. Must be terminated from the outside
     */
    private static void testDonkeysConstruction() {
        int k = 2; //swap multiplicity (doubled each time something fails)
        for (int m = 1; true; m++) {
            boolean hasSolution = false;
            while (!hasSolution) {
                System.out.println("I am testing Donkey's instance for m = " + m + " and swap multiplicity " + k + ".");
                hasSolution = testDonkeysConstruction(m, k);
                if (!hasSolution) {
                    k += 2;
                }
                System.out.println("-----");
                System.out.println();
            }
        }
    }

    /**
     *
     * @param m
     *      Parameter from donkey's construction; we have 2^m wires.
     * @param k
     *      Swap multiplicity for the pairs of wires that swap (should be an even number)
     * @return
     *      success in finding a tangle
     */
    private static boolean testDonkeysConstruction(int m, int k) {
        LineSwapper donkeysInstance = getDonkeysInstance(m, k);
        System.out.println("Constructed swapping list using " + donkeysInstance.getNumberOfLines() + " wires and " +
                donkeysInstance.getNumberOfSwaps() + " swaps.");


        RealizationGraph realizationGraph = donkeysInstance.computeOneMinHeightRealization();

        if (realizationGraph == null) {
            System.out.println("!!! FOUND NO REALIZATION !!!");
            return false;
        }

        SwappingDiagram swappingDiagram = realizationGraph.getSwappingDiagramOfMinimumHeight(donkeysInstance);
        System.out.println("Found a realization with " + swappingDiagram.getLayers().size() + " layers.");
        drawSwappingDiagram(donkeysInstance, swappingDiagram,
                PATH_SWAPPING_DIAGRAMS + File.separator + "donkey-m" + m + "-k" + k + ".svg");
        return true;
    }


    private static void checkForeverRandomNonSeparable02Lists(int numberOfWires, Random02ListGenerator.Method method,
                                                              int numberOfLinearOrders) throws NoRealizationException {
        System.out.println("Checking random 0-2-lists for " + numberOfWires + " wires. For generating random " +
                "non-separable 0-2-list, the method is " + method + " (numberOfLinearOrders=" + numberOfLinearOrders + ").");
        int counter = 0;
        while (true) {
            checkRandomNonSeparable02List(numberOfWires, method, numberOfLinearOrders);
            System.out.println(++counter + " completed");
        }
    }

    private static LinkedHashSet<LineSwapper> alreadyCheckedRandomly = new LinkedHashSet<>();

    public static void checkRandomNonSeparable02List(int numberOfWires, Random02ListGenerator.Method method,
                                                     int numberOfLinearOrders) throws NoRealizationException {
        while (true) {
            LinkedHashMap<UnorderedSwap, Integer> candidateList =
                    Random02ListGenerator.getRandomNonSeparable02List(method, numberOfWires, numberOfLinearOrders);
            LineSwapper lineSwapper = new LineSwapper(candidateList, numberOfWires);
            if (!alreadyCheckedRandomly.contains(lineSwapper)) {
//                Collection<RealizationGraph> allRealizations = lineSwapper.computeAllRealizations();
//                int count = allRealizations.size();
                if (lineSwapper.hasRealization()) {
//                if (count > 0) {
//                    visualizeAllRealizations(lineSwapper);
                    System.out.println(
                            "The following 0-2-*-swap-set can be realized on " + numberOfWires + " " + "lines: " +
//                            "The following 0-2-*-swap-set has " +  count + " realizations (n = " + numberOfWires + ")" +
//                                    ": " +
                                    candidateList);
                } else {
                    System.out.println("NO REALIZATION FOUND FOR " + candidateList);
                    throw new NoRealizationException();
                }
                alreadyCheckedRandomly.add(lineSwapper);
                break;
            }
            else {
                System.out.println("This has already been checked; skip: " + candidateList);
            }
        }
    }

    public static void countAllNonSeparable02Lists() {
        for (int numberOfLines = 2; true; ++numberOfLines) {
            System.out.println("Checking " + numberOfLines + " lines.");
            step.set(0L);
            totalSteps = (long) Math.pow(2.0, numberOfLines * (numberOfLines - 1) / 2.0);
            stepWidth = Math.max(1L, (long) ((double) percentSteps * 0.01 * (double) totalSteps));
            countNonSeparable.set(0);
            countSeparable.set(0);
            LinkedHashMap<UnorderedSwap, Integer> swap2occurrenceCount = new LinkedHashMap<>();
            countAllNonSeparable02ListsRecursively(numberOfLines, 0, 1, swap2occurrenceCount);
            System.out.println(numberOfLines + " lines: non-separable lists = " + countNonSeparable.get() + ", " +
                    "separable lists = " + countSeparable.get() + "; (total = " + totalSteps + ")");
            System.out.println();
        }
    }

    private static AtomicLong step = new AtomicLong(0);
    private static long totalSteps = 0;
    private static long percentSteps = 10;
    private static long stepWidth = 0;

    private static AtomicLong countSeparable = new AtomicLong(0);
    private static AtomicLong countNonSeparable = new AtomicLong(0);


    private static void countAllNonSeparable02ListsRecursively(int numberOfLines, int i, int j,
                                                               LinkedHashMap<UnorderedSwap, Integer> swap2occurrenceCount) {
        UnorderedSwap currentSwap = new UnorderedSwap(new Line(i), new Line(j));
        boolean endReached = false;
        if (++j >= numberOfLines) {
            if (++i >= numberOfLines - 1) {
                endReached = true;
            }
            else {
                j = i + 1;
            }
        }
        //without swap ij
        LinkedHashMap<UnorderedSwap, Integer> swap2occurrenceCountWithout = new LinkedHashMap<>(swap2occurrenceCount);
        swap2occurrenceCountWithout.remove(currentSwap);
        //with swap ij
        LinkedHashMap<UnorderedSwap, Integer> swap2occurrenceCountWith = new LinkedHashMap<>(swap2occurrenceCount);
        swap2occurrenceCountWith.put(currentSwap, 2);

        //execute in parallel
        ArrayList<LinkedHashMap<UnorderedSwap, Integer>> withAndWithout = new ArrayList<>(2);
        withAndWithout.add(swap2occurrenceCountWithout);
        withAndWithout.add(swap2occurrenceCountWith);
        boolean finalEndReached = endReached;
        int finalI = i;
        int finalJ = j;
        withAndWithout.parallelStream().forEach((swapList) -> {
            if (finalEndReached) {
                //show progress
                long currentStep = step.getAndAdd(1);
                if (currentStep % stepWidth == 0) {
                    System.out.println((currentStep * 100 / totalSteps) + " %");
                }
                //evaluate
                if (isNonSeparable(swapList)) {
                    countNonSeparable.incrementAndGet();
                }
                else {
                    countSeparable.incrementAndGet();
                }
            }
            else {
                countAllNonSeparable02ListsRecursively(numberOfLines, finalI, finalJ, swapList);
            }
        });
    }

    public static void checkAllNonSeparable02Lists() throws NoRealizationException {
        for (int numberOfLines = 2; true; ++numberOfLines) {
            System.out.println("Checking " + numberOfLines + " lines.");
            step.set(0L);
            totalSteps = (long) Math.pow(2.0, numberOfLines * (numberOfLines - 1) / 2.0);
            stepWidth = Math.max(1L, (long) ((double) percentSteps * 0.01 * (double) totalSteps));
            LinkedHashMap<UnorderedSwap, Integer> swap2occurrenceCount = new LinkedHashMap<>();
            checkAllNonSeparable02ListsRecursively(numberOfLines, 0, 1, swap2occurrenceCount);
            System.out.println("All non-separable even list up to " + numberOfLines + " lines are realizable");
            System.out.println();
        }
    }

    private static void checkAllNonSeparable02ListsRecursively(int numberOfLines, int i, int j,
                                                               LinkedHashMap<UnorderedSwap, Integer> swap2occurrenceCount) throws NoRealizationException {

        UnorderedSwap currentSwap = new UnorderedSwap(new Line(i), new Line(j));
        boolean endReached = false;
        if (++j >= numberOfLines) {
            if (++i >= numberOfLines - 1) {
                endReached = true;
            }
            else {
                j = i + 1;
            }
        }
        //without swap ij
        LinkedHashMap<UnorderedSwap, Integer> swap2occurrenceCountWithout = new LinkedHashMap<>(swap2occurrenceCount);
        swap2occurrenceCountWithout.remove(currentSwap);
        //with swap ij
        LinkedHashMap<UnorderedSwap, Integer> swap2occurrenceCountWith = new LinkedHashMap<>(swap2occurrenceCount);
        swap2occurrenceCountWith.put(currentSwap, 2);

        //execute in parallel
        ArrayList<LinkedHashMap<UnorderedSwap, Integer>> withAndWithout = new ArrayList<>(2);
        withAndWithout.add(swap2occurrenceCountWithout);
        withAndWithout.add(swap2occurrenceCountWith);
        boolean finalEndReached = endReached;
        int finalI = i;
        int finalJ = j;
        boolean success = withAndWithout.parallelStream().map((swapList) -> {
            if (finalEndReached) {
                //show progress
                long currentStep = step.getAndAdd(1);
                if (currentStep % stepWidth == 0) {
                    System.out.println((currentStep * 100 / totalSteps) + " %");
                }
                //evaluate
                if (isNonSeparable(swapList)) {
                    LineSwapper lineSwapper = new LineSwapper(swapList, numberOfLines);
                    if (lineSwapper.hasRealization()) {
//                        System.out.println("The following 0-2-*-swap-set can be realized on " + numberOfLines +" " +
//                                "lines: " + swapList);
                    } else {
                        System.out.println("NO REALIZATION FOUND FOR " + swapList);
                        return false;
                    }
                }
            }
            else {
                try {
                    checkAllNonSeparable02ListsRecursively(numberOfLines, finalI, finalJ, swapList);
                } catch (NoRealizationException e) {
                    return false;
                }
            }
            return true;
        }).reduce(true, (Boolean prev, Boolean curr) -> prev && curr);

        if (!success) {
            throw new NoRealizationException();
        }
    }

    public static boolean isNonSeparable(Map<UnorderedSwap, Integer> swap2occurrenceCount) {
        for (UnorderedSwap swap : swap2occurrenceCount.keySet()) {
            if (swap2occurrenceCount.get(swap) <= 0) {
                continue;
            }

            Line line0 = swap.getValue0();
            Line line1 = swap.getValue1();
            for (int k = line0.getLineNumber() + 1; k < line1.getLineNumber(); ++k) {
                Line lineK = new Line(k);
                UnorderedSwap swapLeft = new UnorderedSwap(line0, lineK);
                UnorderedSwap swapRight = new UnorderedSwap(lineK, line1);
                if ((!swap2occurrenceCount.containsKey(swapLeft) || swap2occurrenceCount.get(swapLeft) <= 0) &&
                        (!swap2occurrenceCount.containsKey(swapRight) || swap2occurrenceCount.get(swapRight) <= 0)) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void findRealizations(boolean countOnly) {
        int maxTotal = 6;
        for (int i = 0; i <= maxTotal; ++i) {
            for (int j = 0; j <= maxTotal; ++j) {
                for (int k = 0; k <= maxTotal; ++k) {
                    if (countOnly) {
                        System.out.println(i+", "+j+", "+k+": "+SwapTripletRegistry.getRealizations(i, j, k).size());
                    }
                    else {
                        System.out.println(i+", "+j+", "+k+": "+SwapTripletRegistry.getRealizations(i, j, k));
                    }
                }
            }
        }
    }

    public static void parityCheck() {
        int maxTotal = 1300;
        for (int i = 0; i <= maxTotal; ++i) {
            for (int j = 0; j <= maxTotal; ++j) {
                for (int k = 0; k <= maxTotal; ++k) {
                    boolean parityRealizable = true;
                    if (i % 2 == k % 2 && i % 2 != j % 2) {
                        parityRealizable = false;
                    }
                    if (!SwapTripletRegistry.isRealizable(i, j, k) && parityRealizable) {
                        System.out.println("Found configuration that is contradiction free in terms of parity, but not realizable!!!");
                        System.out.println("It is ("+i+", "+j+", "+k+"): "+SwapTripletRegistry.getRealizations(i, j, k));
                    }
                }
            }
        }
    }

    private static void visualize(List<AbstractLayout<Pair<OrderedSwap, Integer>, Integer>> allLayouts, Dimension size) {
        final VisualizationViewer<Pair<OrderedSwap, Integer>, Integer> vv1 = new VisualizationViewer<>(
                new DefaultVisualizationModelWithoutReiterating<>(allLayouts.get(0), size));
        vv1.getRenderContext().setEdgeShapeTransformer(EdgeShape.line(allLayouts.get(0).getGraph()));
        vv1.getRenderContext().setVertexShapeTransformer(new AbstractVertexShapeTransformer<Pair<OrderedSwap, Integer>>() {
            @Override
            public Shape apply(Pair<OrderedSwap, Integer> input) {
                return factory.getEllipse(input);
            }
        });
        vv1.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
        vv1.getRenderer().getVertexLabelRenderer().setPosition(VertexLabel.Position.N);
        GraphZoomScrollPane scrollPaneOld = new GraphZoomScrollPane(vv1);
        vv1.setGraphMouse(new DefaultModalGraphMouse<String,Number>());
        //second layout
        AbstractLayout<Pair<OrderedSwap, Integer>, Integer> nextLayout = allLayouts.get(0); //catch case that only 1 was drawn
        if(allLayouts.size()>1){ //case that more than 1 was drawn
            nextLayout = allLayouts.get(1);
        }
        final VisualizationViewer<Pair<OrderedSwap, Integer>, Integer> vv2 = new VisualizationViewer<>(
                new DefaultVisualizationModelWithoutReiterating<>(nextLayout, size));
        vv2.getRenderContext().setEdgeShapeTransformer(EdgeShape.line(nextLayout.getGraph()));
        vv2.getRenderContext().setVertexShapeTransformer(new AbstractVertexShapeTransformer<Pair<OrderedSwap, Integer>>() {
            @Override
            public Shape apply(Pair<OrderedSwap, Integer> input) {
                return factory.getEllipse(input);
            }
        });
        vv2.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
        vv2.getRenderer().getVertexLabelRenderer().setPosition(VertexLabel.Position.N);
        GraphZoomScrollPane scrollPaneNeu = new GraphZoomScrollPane(vv2);
        vv2.setGraphMouse(new DefaultModalGraphMouse<String,Number>());
        JFrame frame = new JFrame("These are the acyclic swap dependency graphs");
        String[] options = new String[allLayouts.size()];
        for(int i=0; i<allLayouts.size(); i++) options[i] = "Realization "+i;
        final JComboBox<String> selectDrawing1 = new JComboBox<>(options);
        selectDrawing1.setSelectedIndex(0);
        selectDrawing1.addActionListener(e -> {
            vv2.setGraphLayout(allLayouts.get(selectDrawing1.getSelectedIndex()));
            vv2.getRenderContext().setEdgeShapeTransformer(
                    EdgeShape.line(allLayouts.get(selectDrawing1.getSelectedIndex()).getGraph()));
        });
        JButton reset1 = new JButton("Reset position and zoom");
        reset1.addActionListener(e -> {
            vv1.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT).setToIdentity();
            vv1.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW).setToIdentity();
        });
        JPanel pane1 = new JPanel();
        pane1.setLayout(new BoxLayout(pane1, BoxLayout.Y_AXIS));
        pane1.add(selectDrawing1);
        pane1.add(reset1);
        pane1.add(scrollPaneOld);
        final JComboBox<String> selectDrawing2 = new JComboBox<>(options);
        selectDrawing2.setSelectedIndex(0);
        if(allLayouts.size()>1){ //again catch if it's only 1 alg
            selectDrawing2.setSelectedIndex(1);
        }
        selectDrawing2.addActionListener(e -> {
            vv2.setGraphLayout(allLayouts.get(selectDrawing2.getSelectedIndex()));
            vv2.getRenderContext().setEdgeShapeTransformer(
                    EdgeShape.line(allLayouts.get(selectDrawing2.getSelectedIndex()).getGraph()));
        });
        JButton reset2 = new JButton("Reset position and zoom");
        reset2.addActionListener(e -> {
            vv2.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT).setToIdentity();
            vv2.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.VIEW).setToIdentity();
        });
        JPanel pane2 = new JPanel();
        pane2.setLayout(new BoxLayout(pane2, BoxLayout.Y_AXIS));
        pane2.add(selectDrawing2);
        pane2.add(reset2);
        pane2.add(scrollPaneNeu);
        frame.setLayout(new FlowLayout());
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        scrollPaneOld.setBorder(new TitledBorder(null, "Layout 1", TitledBorder.CENTER, TitledBorder.TOP));
        scrollPaneNeu.setBorder(new TitledBorder(null, "Layout 2", TitledBorder.CENTER, TitledBorder.TOP));
        frame.getContentPane().add(pane1);
        frame.getContentPane().add(pane2);
        frame.pack();
        frame.setVisible(true);
    }
}

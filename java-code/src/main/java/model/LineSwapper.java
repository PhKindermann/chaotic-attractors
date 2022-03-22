package model;

import edu.uci.ics.jung.algorithms.cluster.WeakComponentClusterer;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.UndirectedGraph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import org.javatuples.Pair;
import org.javatuples.Triplet;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LineSwapper {
    private int numberOfLines;
    private int[][] inputMatrix;

    private LinkedHashMap<Integer, LinkedList<ArrayList<RealizationGraph>>> allTripletRealizations;
    private int numberOfTriplets;

    private Collection<RealizationGraph> allRealizations;

    public LineSwapper(LinkedHashMap<UnorderedSwap, Integer> swap2occurrenceCount, int numberOfLines) {
        this.numberOfLines = numberOfLines;
        setInputMatrix(swap2occurrenceCount, numberOfLines);
    }

    private void setInputMatrix(LinkedHashMap<UnorderedSwap, Integer> swap2occurrenceCount, int numberOfLines) {
        this.inputMatrix = new int[numberOfLines][numberOfLines];
        for (int i = 0; i < numberOfLines - 1; i++) {
            for (int j = i + 1; j < numberOfLines; j++) {
                UnorderedSwap swap = new UnorderedSwap(new Line(i), new Line(j));
                if (swap2occurrenceCount.containsKey(swap)) {
                    this.inputMatrix[i][j] = swap2occurrenceCount.get(swap);
                    this.inputMatrix[j][i] = swap2occurrenceCount.get(swap);
                }
            }
        }
    }

    public LineSwapper(Collection<UnorderedSwap> swaps, int numberOfLines) {
        this.numberOfLines = numberOfLines;
        LinkedHashMap<UnorderedSwap, Integer> swap2occurrenceCount = new LinkedHashMap<>();
        for (UnorderedSwap swap : swaps) {
            swap2occurrenceCount.merge(swap, 1, (a, b) -> a + b);
        }
        setInputMatrix(swap2occurrenceCount, numberOfLines);
    }

    public int getNumberOfLines() {
        return numberOfLines;
    }

    public int getNumberOfSwaps(UnorderedSwap swap) {
        //use only absolute values for the matrices
        return Math.abs(inputMatrix[swap.getLineWithSmallerNumber().getLineNumber()][swap.getLineWithGreaterNumber().getLineNumber()]);
    }

    public int getNumberOfSwaps() {
        int sum = 0;
        for (int i = 0; i < numberOfLines - 1; i++) {
            for (int j = i + 1; j < numberOfLines; j++) {
                //use only absolute values for the matrices
                sum += Math.abs(this.inputMatrix[i][j]);
            }
        }
        return sum;
    }

    public int[][] getInputMatrix() {
        return inputMatrix;
    }

    public String getInputMatrixAsString() {
        String string = "[";
        boolean outerCommaNeeded = false;
        for (int[] line : inputMatrix) {
            if (outerCommaNeeded) {
                string += ", ";
            }
            string += "[";
            boolean commaNeeded = false;
            for (Integer cell : line) {
                if (commaNeeded) {
                    string += ", ";
                }
                string += cell;
                commaNeeded = true;
            }
            string += "]";
            outerCommaNeeded = true;
        }
        return string + "]";
    }

    public void setInputMatrix(Integer[][] inputMatrix) {
        this.inputMatrix = new int[inputMatrix.length][inputMatrix.length];
        for (int i = 0; i < inputMatrix.length; i++) {
            for (int j = 0; j < inputMatrix.length; j++) {
                this.inputMatrix[i][j] = inputMatrix[i][j];
            }
        }
    }

    public List<Line> getFinalPermutation() {
        Line[] finalPositions = new Line[numberOfLines];
        for (int i = 0; i < numberOfLines; ++i) {
            int position = i;
            for (int j = 0; j < i; ++j) {
                //use only absolute values for the matrices
                position -= Math.abs(this.inputMatrix[j][i]) % 2;
            }
            for (int j = i + 1; j < numberOfLines; ++j) {
                //use only absolute values for the matrices
                position += Math.abs(this.inputMatrix[i][j]) % 2;
            }
            //fail: two lines end up at the same position => infeasible
            if (finalPositions[position] != null) {
                return null;
            }
            finalPositions[position] = new Line(i);
        }
        return Arrays.asList(finalPositions);
    }

    public boolean hasRealization() {
        return computeOneRealization() != null;
    }

    /**
     *
     * @return null if no realization exists
     */
    public RealizationGraph computeOneMinHeightRealization() {
        int currentMinHeight = Integer.MAX_VALUE;
        RealizationGraph currentMinHeightRealization = null;

        while (true) {
            RealizationGraph intermediateOutput = computeOneRealization(currentMinHeight -
                    (currentMinHeight == Integer.MAX_VALUE ? 0 : 1));
            if (intermediateOutput == null) {
                break;
            }
            currentMinHeightRealization = intermediateOutput;
            currentMinHeight = currentMinHeightRealization.getHeight(this);
        }

        return currentMinHeightRealization;
    }

    /**
     *
     * @return null if no realization exists
     */
    public RealizationGraph computeOneRealization() {
        return computeOneRealization(Integer.MAX_VALUE);
    }

    /**
     *
     * @return null if no realization exists
     */
    public RealizationGraph computeOneRealization(int maxHeight) {
        if (maxHeight < 0) {
            return null;
        }

        computeAllTripletRealizations();

        //preparing
        /*
         * List with two indices: one entry per triplet in order non-decreasing in the number of realizations
         * The first value of the pair is the currently tested realization in the collection of realizations of this triplet
         * The second value of the pair is the list of realizations of this triplet
         * (from model.LineSwapper#allTripletRealizations)
         *
         * This is done because the computeOneRealization works as follows:
         * Try to combine the triplets in one way, if it fails, take from the triplet-realizations-collections the next one
         * and try to combine.
         * If successful add the next triplet and so on.
         */
        ArrayList<Pair<Integer, List<RealizationGraph>>> currentlyTried = new ArrayList<>(numberOfTriplets);

        for (int numberOfRealizations : allTripletRealizations.keySet().stream().sorted().collect(Collectors.toList())) {
            for (ArrayList<RealizationGraph> collectionForOneTriplet :
                    allTripletRealizations.get(numberOfRealizations)) {
                currentlyTried.add(new Pair<>(0, collectionForOneTriplet));
            }
        }

        //trying to combine triplets, maintain only one acyclic realization which is built up
        RealizationGraph realization = new RealizationGraph();
        int tripletNumberToBeAdded = 0;
        while (tripletNumberToBeAdded < numberOfTriplets) {
            Pair<Integer, List<RealizationGraph>> tripletEntry = currentlyTried.get(tripletNumberToBeAdded);
            int numberInRealizationCollection = tripletEntry.getValue0();
            List<RealizationGraph> allTripletRealizations = tripletEntry.getValue1();
            if (allTripletRealizations.isEmpty()) {
                //not even for that triplet there is a realization
                return null;
            }
            RealizationGraph tripletRealization = allTripletRealizations.get(numberInRealizationCollection);
            RealizationGraph possibleRealization = combineDependencies(realization, tripletRealization);
            //success
            if (possibleRealization != null && !CycleFinder.containsCycle(possibleRealization)
                    && (maxHeight == Integer.MAX_VALUE || possibleRealization.getHeight(this) <= maxHeight)) {
                //try first and remove it from the list of "open" realizations in the stack
//                WeakComponentClusterer<Pair<OrderedSwap, Integer>, Integer> wcc = new WeakComponentClusterer<>();
//                Set<Set<Pair<OrderedSwap, Integer>>> components = wcc.apply(possibleRealization);
//                int numberOfComponents = components.size();
                realization = possibleRealization;
                ++tripletNumberToBeAdded;
            }
            //fail
            else{
                boolean recompute = false;
                //backtrack triplet entries
                Pair<Integer, List<RealizationGraph>> currentTripletEntry = tripletEntry;
                while (true) {

                    //all realizations of this triplet already tested -> go to prev and try another realization
                    if (currentTripletEntry.getValue0() ==
                            currentTripletEntry.getValue1().size() - 1) {
                        currentlyTried.set(tripletNumberToBeAdded,
                                new Pair<>(0, currentTripletEntry.getValue1()));
                        --tripletNumberToBeAdded;
                        //total fail -> no realization
                        if (tripletNumberToBeAdded < 0) {
                            return null;
                        }
                        currentTripletEntry = currentlyTried.get(tripletNumberToBeAdded);
                        recompute = true;
                    }
                    //not all realizations of this triplet already tested
                    else {
                        currentlyTried.set(tripletNumberToBeAdded,
                                new Pair<>(currentTripletEntry.getValue0() + 1,
                                        currentTripletEntry.getValue1()));
                        if (recompute) {
                            realization = new RealizationGraph();
                            tripletNumberToBeAdded = 0;
                        }
                        break;
                    }
                }
            }
        }

        return realization;
    }

    public Collection<RealizationGraph> computeAllRealizations() {
        if (allRealizations == null) {
            //start with the triplets
            computeAllTripletRealizations();
            //now combine them beginning with the ones with only few realizations
            allRealizations = new LinkedList<>();
            allRealizations.add(new RealizationGraph());
            for (int numberOfRealizations : allTripletRealizations.keySet().stream().sorted().collect(Collectors.toList())) {
                for (Collection<RealizationGraph> tripletRealizations : allTripletRealizations.get(numberOfRealizations)) {
                    allRealizations = computeAllRealizations(allRealizations, tripletRealizations);
//                System.out.println("Added Triplet with "+numberOfRealizations+" realizations: " +
//                        "Currently "+allRealizations.size()+" realizations.");
                }
            }
            SwapTripletRegistry.getRealizations(0, 0, 0);
        }
        return allRealizations;
    }

    private void computeAllTripletRealizations() {
        allTripletRealizations = new LinkedHashMap<>();
        numberOfTriplets = 0;
        for (int i = 0; i < numberOfLines - 2; ++i) {
            Line lineI = new Line(i);
            for (int j = i + 1; j < numberOfLines - 1; ++j) {
                Line lineJ = new Line(j);
                for (int k = j + 1; k < numberOfLines; ++k) {
                    Line lineK = new Line(k);
                    //use only absolute values for the matrices
                    LineTriplet triplet = new LineTriplet(lineI, lineJ, lineK,
                            Math.abs(inputMatrix[i][j]), Math.abs(inputMatrix[i][k]), Math.abs(inputMatrix[j][k]));
                    ArrayList<RealizationGraph> executionSequences = computeAllRealizations(triplet);
                    if (!allTripletRealizations.containsKey(executionSequences.size())) {
                        allTripletRealizations.put(executionSequences.size(), new LinkedList<>());
                    }
                    allTripletRealizations.get(executionSequences.size()).add(executionSequences);
                    ++numberOfTriplets;
//                    System.out.println("Computed Triplet("+i+", "+j+", "+k+"): Found "+executionSequences.size()+" realizations.");
                }
            }
        }
    }

    private Collection<RealizationGraph> computeAllRealizations(
            Collection<RealizationGraph> dependencyGraphs0,
            Collection<RealizationGraph> dependencyGraphs1) {
        //compute all combinations
        LinkedList<RealizationGraph> allCombinations = new LinkedList<>();
        for (RealizationGraph graph0realization : dependencyGraphs0) {
            for (RealizationGraph graph1realization : dependencyGraphs1) {
                RealizationGraph combinedDependencies =
                        combineDependencies(graph0realization, graph1realization);
                if (!containsThisGraph(allCombinations, combinedDependencies)) {
                    allCombinations.add(combinedDependencies);
                }
            }
        }
        //filter the non-realizable combinations out, i.e. the realizations with cycles
        for (RealizationGraph combination : new LinkedList<>(allCombinations)) {
            if (CycleFinder.containsCycle(combination)) {
                allCombinations.remove(combination);
//                System.out.println("Found cycle! (#vertices = "+combination.getVertexCount()+")");
//            }
//            else {
//               System.out.println("Found cycle-free combination! (#vertices = "+combination.getVertexCount()+")");
            }
        }

        return allCombinations;
    }
    private boolean containsThisGraph(Collection<RealizationGraph> collection,
                                      RealizationGraph graph) {
        for (RealizationGraph collectionGraph : collection) {
            boolean isIdentical = true;
            if (collectionGraph.getVertexCount() != graph.getVertexCount()
                    || collectionGraph.getEdgeCount() != graph.getEdgeCount()) {
                //isIdentical = false;
                continue;
            }
            for (Pair<OrderedSwap, Integer> v : collectionGraph.getVertices()) {
                if (!graph.containsVertex(v)) {
                    isIdentical = false;
                    break;
                }
                if (collectionGraph.getSuccessorCount(v) != graph.getSuccessorCount(v)) {
                    isIdentical = false;
                    break;
                }
                for (Pair<OrderedSwap, Integer> successor : collectionGraph.getSuccessors(v)) {
                    if (!graph.getSuccessors(v).contains(successor)){
                        isIdentical = false;
                        break;
                    }
                }
                if (!isIdentical) {
                    break;
                }
            }
            if (isIdentical) {
                return true;
            }
        }
        return false;
    }

    private RealizationGraph combineDependencies(
            RealizationGraph graph0,
            RealizationGraph graph1) {
        RealizationGraph combinationGraph = new RealizationGraph();
        //add all vertices
        for (Pair<OrderedSwap, Integer> swap : graph0.getVertices()) {
            combinationGraph.addVertex(swap);
        }
        for (Pair<OrderedSwap, Integer> swap : graph1.getVertices()) {
            if (!combinationGraph.containsVertex(swap)) {
                combinationGraph.addVertex(swap);
            }
        }
        //add edges
        int edgeCounter = 0;
        for (Integer e : graph0.getEdges()) {
            Pair<OrderedSwap, Integer> source = graph0.getSource(e);
            Pair<OrderedSwap, Integer> target = graph0.getDest(e);
            if (!combinationGraph.isPredecessor(source, target)) {
                combinationGraph.addEdge(edgeCounter++, source, target);
            }
        }
        for (Integer e : graph1.getEdges()) {
            Pair<OrderedSwap, Integer> source = graph1.getSource(e);
            Pair<OrderedSwap, Integer> target = graph1.getDest(e);
            if (!combinationGraph.isPredecessor(source, target)) {
                combinationGraph.addEdge(edgeCounter++, source, target);
            }
        }
        return combinationGraph;
    }

    private static ArrayList<RealizationGraph> computeAllRealizations(LineTriplet triplet) {
        Collection<List<SwapTripletRegistry.TripletSwap>> generalRealizations = SwapTripletRegistry.getRealizations(
                triplet.getNumberOf01Swaps(), triplet.getNumberOf02Swaps(), triplet.getNumberOf12Swaps());
        ArrayList<RealizationGraph> concreteRealizations =
                new ArrayList<>(generalRealizations.size());
        for (List<SwapTripletRegistry.TripletSwap> realization : generalRealizations) {
            RealizationGraph executionSequence = new RealizationGraph();
            int edgeCounter = 0;
            int swapCounter01 = 0;
            int swapCounter02 = 0;
            int swapCounter12 = 0;
            int swapCounter10 = 0;
            int swapCounter20 = 0;
            int swapCounter21 = 0;
            Pair<OrderedSwap, Integer> prevSwap = null;
            for (SwapTripletRegistry.TripletSwap tripletSwap : realization) {
                Pair<OrderedSwap, Integer> swap = null;
                switch (tripletSwap) {
                    case SWAP_01:
                        swap = new Pair<>(new OrderedSwap(triplet.getValue0(), triplet.getValue1()), swapCounter01++);
                        break;
                    case SWAP_10:
                        swap = new Pair<>(new OrderedSwap(triplet.getValue1(), triplet.getValue0()), swapCounter10++);
                        break;
                    case SWAP_02:
                        swap = new Pair<>(new OrderedSwap(triplet.getValue0(), triplet.getValue2()), swapCounter02++);
                        break;
                    case SWAP_20:
                        swap = new Pair<>(new OrderedSwap(triplet.getValue2(), triplet.getValue0()), swapCounter20++);
                        break;
                    case SWAP_12:
                        swap = new Pair<>(new OrderedSwap(triplet.getValue1(), triplet.getValue2()), swapCounter12++);
                        break;
                    case SWAP_21:
                        swap = new Pair<>(new OrderedSwap(triplet.getValue2(), triplet.getValue1()), swapCounter21++);
                        break;
                }
                executionSequence.addVertex(swap);
                if (prevSwap != null) {
                    executionSequence.addEdge(edgeCounter++, prevSwap, swap);
                }
                prevSwap = swap;
            }
            concreteRealizations.add(executionSequence);
        }

        return concreteRealizations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LineSwapper that = (LineSwapper) o;
        boolean allRowsAreEqual = true;
        if (numberOfLines != that.numberOfLines) return false;
        if (inputMatrix.length != that.inputMatrix.length) return false;
        for (int i = 0; i < inputMatrix.length; i++) {
            if (!Arrays.equals(inputMatrix[i], that.inputMatrix[i])) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(numberOfLines);
        for (int[] row : inputMatrix) {
            result = 31 * result + Arrays.hashCode(row);
        }
        return result;
    }

    @Override
    public String toString() {
        return "LineSwapper{" + "inputMatrix=" + Arrays.stream(inputMatrix).map(Arrays::toString).collect(Collectors.joining()) + '}';
    }















    //////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////
    // Unsuccessful tries for finding an algorithm to find a realization for all 0-2-lists. //
    //////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////



    //////////////////////////////////////////////////////////////////////////////////////////////
    // newer method: by dividing into undivisible 0-2-lists and split them into 3 simple lists  //
    //////////////////////////////////////////////////////////////////////////////////////////////





    public boolean hasRealizationBy02Method() {
        return computeOneRealizationBy02Method() != null;
    }

    private Set<UnorderedSwap> getAllSwapsAsSet() {
        Set<UnorderedSwap> allSwaps = new LinkedHashSet<>();
        for (int i = 0; i < numberOfLines - 1; ++i) {
            Line lineI = new Line(i);
            for (int j = i + 1; j < numberOfLines; ++j) {
                Line lineJ = new Line(j);

                if (inputMatrix[i][j] != 0) {
                    allSwaps.add(new UnorderedSwap(lineI, lineJ));
                }
            }
        }
        return allSwaps;
    }

    private Map<UnorderedSwap, Integer> getAllSwapsAsMap() {
        Map<UnorderedSwap, Integer> swap2occurrenceCount = new LinkedHashMap<>();
        for (int i = 0; i < numberOfLines - 1; ++i) {
            Line lineI = new Line(i);
            for (int j = i + 1; j < numberOfLines; ++j) {
                Line lineJ = new Line(j);

                if (inputMatrix[i][j] != 0) {
                    //use only absolute values for the matrices
                    swap2occurrenceCount.put(new UnorderedSwap(lineI, lineJ), Math.abs(inputMatrix[i][j]));
                }
            }
        }
        return swap2occurrenceCount;
    }

    public RealizationGraph computeOneRealizationBy02Method() {
        //build requirement graph, i.e., a swap depends on another swap if it would become separated otherwise
        // (umbrella configuration). Hence, we make them require on the intermediate swaps "below" itself. If we can
        // choose between two underlying swaps, we take the left one (todo: maybe that choice should be done more
        // carefully)
        DirectedGraph<UnorderedSwap, Integer> requirementGraph = buildRequirementGraph();

        //we now subdivide the total list into smaller minimal lists s.t. all swaps contained are required by each other
        Set<UnorderedSwap> unassignedSwaps = getAllSwapsAsSet();
        List<LineSwapper> minimalSwappers = new ArrayList<>();
        while (!unassignedSwaps.isEmpty()) {
            List<UnorderedSwap> swapsOfThisSublist = new ArrayList<>();
            for (UnorderedSwap swap : requirementGraph.getVertices()) {
                //start at a source
                if (requirementGraph.getInEdges(swap).isEmpty()) {
                    addDependenciesRecursively(swap, swapsOfThisSublist, unassignedSwaps, requirementGraph);
                    break;
                }
            }
            //little consistency check
            if (!MainClass.isNonSeparable(this.getAllSwapsAsMap())) {
                System.out.println("FOUND A SEPARABLE LIST THAT SHOULD BE NON-SEPARABLE: " + this);
            }
            minimalSwappers.add(new LineSwapper(swapsOfThisSublist, numberOfLines));
        }

        //execute for all minimal lists independently
        List<RealizationGraph> realizationsOfMinimalLists = new ArrayList<>();
        for (LineSwapper minimalSwapper : minimalSwappers) {
            RealizationGraph realizationGraph = minimalSwapper.computeOneRealizationBy02MethodForMinimalList();
            if (realizationGraph == null) {
                return null;
            }
            realizationsOfMinimalLists.add(realizationGraph);
        }
        return new RealizationGraph();
    }

    private void addDependenciesRecursively(UnorderedSwap swap, List<UnorderedSwap> swapsOfThisSublist,
                                            Set<UnorderedSwap> unassignedSwaps,
                                            DirectedGraph<UnorderedSwap, Integer> requirementGraph) {
        if (!unassignedSwaps.contains(swap)) {
            return;
        }
        swapsOfThisSublist.add(swap);
        swapsOfThisSublist.add(swap); //add it twice such that the final list becomes a 0-2-list
        unassignedSwaps.remove(swap);
        for (Integer edge : requirementGraph.getIncidentEdges(swap)) {
            if (requirementGraph.containsEdge(edge)) { //it may happen that in the meantime the edge has been removed
                addDependenciesRecursively(requirementGraph.getOpposite(swap, edge), swapsOfThisSublist,
                        unassignedSwaps, requirementGraph);
            }
        }
        requirementGraph.removeVertex(swap);
    }

    private RealizationGraph computeOneRealizationBy02MethodForMinimalList() {
        if (this.computeOneRealization() == null) {
            System.out.println("NO REALIZATION FOUND IN THE CLASSICAL WAY");
        }
        else {
            System.out.println("Found realization in the classical way.");
        }

        //find all double bow triplets:
        List<Triplet<Line, Line, Line>> doubleBows = new ArrayList<>();
        for (int i = 0; i < numberOfLines - 2; ++i) {
            Line lineI = new Line(i);
            for (int j = i + 1; j < numberOfLines - 1; ++j) {
                Line lineJ = new Line(j);
                for (int k = j + 1; k < numberOfLines; ++k) {
                    Line lineK = new Line(k);

                    UnorderedSwap swapIJ = new UnorderedSwap(lineI, lineJ);
                    UnorderedSwap swapIK = new UnorderedSwap(lineI, lineK);
                    UnorderedSwap swapJK = new UnorderedSwap(lineJ, lineK);

                    if (getNumberOfSwaps(swapIJ) == 2 && getNumberOfSwaps(swapIK) == 0 && getNumberOfSwaps(swapJK) == 2) {
                        doubleBows.add(new Triplet<>(lineI, lineJ, lineK));
                    }
                }
            }
        }

        //find chains of bows
        DirectedGraph<UnorderedSwap, Integer> bowChains = new DirectedSparseGraph<>();
        int edgeCounter = 0;
        for (Triplet<Line, Line, Line> doubleBow : doubleBows) {
            UnorderedSwap firstBow = new UnorderedSwap(doubleBow.getValue0(), doubleBow.getValue1());
            UnorderedSwap secondBow = new UnorderedSwap(doubleBow.getValue1(), doubleBow.getValue2());
            if (!bowChains.containsVertex(firstBow)) {
                bowChains.addVertex(firstBow);
            }
            if (!bowChains.containsVertex(secondBow)) {
                bowChains.addVertex(secondBow);
            }
            bowChains.addEdge(edgeCounter++, firstBow, secondBow);
        }
        //partition all bows into 2 sets s.t. no two bows in the same set span a double bow
        //this is done using a bfs in the dependency graph of bows, i.e., bowChains
        List<Pair<Set<UnorderedSwap>, Set<UnorderedSwap>>> takenOutSets = new ArrayList<>();
        for (UnorderedSwap vertex : bowChains.getVertices()) {
            //start at a source
            if (bowChains.getInEdges(vertex).isEmpty() && !isContained(vertex, takenOutSets)) {
                Set<UnorderedSwap> takenOutSet0 = new LinkedHashSet<>();
                Set<UnorderedSwap> takenOutSet1 = new LinkedHashSet<>();
                assignAlternating(bowChains, vertex, takenOutSet0, takenOutSet1);
                takenOutSets.add(new Pair<>(takenOutSet0, takenOutSet1));
            }
        }

        //now compute 3 new line swappers that comprise
        // A) all swaps once without takenOutSet0 and all swaps becoming a forbidden configuration then
        // C) all swaps once without takenOutSet1 and all swaps becoming a forbidden configuration then
        // B) the remaining swaps
        Set<UnorderedSwap> swapsA = new LinkedHashSet<>();
        Set<UnorderedSwap> swapsB = new LinkedHashSet<>();
        Set<UnorderedSwap> swapsC = new LinkedHashSet<>();

        //try all combinations of taken out sets
        Set<UnorderedSwap> redSet = new LinkedHashSet<>();
        Set<UnorderedSwap> blueSet = new LinkedHashSet<>();
        boolean[] takeFirst = new boolean[takenOutSets.size()];
        while (true) {
            //combine sets
            redSet = new LinkedHashSet<>();
            blueSet = new LinkedHashSet<>();
            for (int i = 0; i < takeFirst.length; i++) {
                if (takeFirst[i]) {
                    redSet.addAll(takenOutSets.get(i).getValue0());
                    blueSet.addAll(takenOutSets.get(i).getValue1());
                }
                else {
                    redSet.addAll(takenOutSets.get(i).getValue1());
                    blueSet.addAll(takenOutSets.get(i).getValue0());
                }
            }
            //compute remaining sets, i.e., the potential start and end of the tangle
            //check that what depends on them does not overlap
            //first add all except for the corresponding taken out once
            for (int i = 0; i < numberOfLines - 1; i++) {
                for (int j = i + 1; j < numberOfLines; j++) {
                    UnorderedSwap swap = new UnorderedSwap(new Line(i), new Line(j));
                    if (this.inputMatrix[i][j] != 0) {
                        if (!blueSet.contains(swap)) {
                            swapsA.add(swap);
                        }
                        if (!redSet.contains(swap)) {
                            swapsC.add(swap);
                        }
                    }
                }
            }
            //now remove further swaps to prevent that non-allowed triplets occur
            takeOutSwapsAgainstForbiddenTripletConfigs(swapsA, redSet);
            takeOutSwapsAgainstForbiddenTripletConfigs(swapsC, blueSet);
            //make sure that the taken out sets have no overlap
            boolean hasOverlap = false;
            for (UnorderedSwap swap : redSet) {
                if (blueSet.contains(swap)) {
                    hasOverlap = true;
                    break;
                }
            }
            if (!hasOverlap) {
                //we found a good configuration!
                break; //we can continue with the rest of the algorithm
            }
            //otherwise go to the next combination
            boolean endReached = getNextCombination(takeFirst);
            if (endReached) {
                System.out.println("Bad configuration!! No combination of red and blue edges without overlap found.");
            }
        }
        swapsB.addAll(redSet);
        swapsB.addAll(blueSet);

        //now create the 3 line swappers and compute a realization
        LineSwapper swapperA = new LineSwapper(swapsA, numberOfLines);
        RealizationGraph realizationA = swapperA.computeOneRealization();
        List<Line> permutationAfterA = swapperA.getFinalPermutation();
        LineSwapper swapperB = new LineSwapper(permute(swapsB, permutationAfterA), numberOfLines);
        RealizationGraph realizationB = swapperB.computeOneRealization();
        //todo: swapper c with permuted config
        LineSwapper swapperC = new LineSwapper(swapsC, numberOfLines);
        RealizationGraph realizationC = swapperC.computeOneRealization();

        if (realizationA == null || realizationB == null || realizationC == null) {
            return null;
        }
        return new RealizationGraph(); //todo return "real" realization graph
    }

    private boolean getNextCombination(boolean[] takeFirst) {
        for (int i = 0; i < takeFirst.length; i++) {
            if (takeFirst[i] == false) {
                takeFirst[i] = true;
                return false;
            }
            else {
                takeFirst[i] = false;
            }
        }
        return true;
    }

    private boolean isContained(UnorderedSwap swap, List<Pair<Set<UnorderedSwap>, Set<UnorderedSwap>>> takenOutSets) {
        for (Pair<Set<UnorderedSwap>, Set<UnorderedSwap>> pairOfSets : takenOutSets) {
            for (int i = 0; i < 2; i++) {
                for (UnorderedSwap containedSwap : ((Set<UnorderedSwap>) pairOfSets.getValue(i))) {
                    if (containedSwap.equals(swap)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private DirectedGraph<UnorderedSwap, Integer> buildRequirementGraph() {
        DirectedGraph<UnorderedSwap, Integer> requirementGraph = new DirectedSparseGraph<>();
        //init all vertices
        for (UnorderedSwap swap : getAllSwapsAsSet()) {
            requirementGraph.addVertex(swap);
        }
        //find dependency edges
        int edgeCounter = 0;
        //for the triplets where there are 2 bows below a large bow, we have 2 possibilities -> wait with the decision
        // which to take until all others have been handled where we don't have a choice
        List<Triplet<UnorderedSwap, UnorderedSwap, UnorderedSwap>> fullTriplets = new LinkedList<>();
        for (int i = 0; i < numberOfLines - 2; ++i) {
            Line lineI = new Line(i);
            for (int j = i + 1; j < numberOfLines - 1; ++j) {
                Line lineJ = new Line(j);
                for (int k = j + 1; k < numberOfLines; ++k) {
                    Line lineK = new Line(k);

                    UnorderedSwap swapIJ = new UnorderedSwap(lineI, lineJ);
                    UnorderedSwap swapIK = new UnorderedSwap(lineI, lineK);
                    UnorderedSwap swapJK = new UnorderedSwap(lineJ, lineK);

                    //we need only to care about non-separability when the long middle swap i--k exists
                    if (getNumberOfSwaps(swapIK) > 0) {
                        //full triplet
                        if (getNumberOfSwaps(swapIJ) > 0 && getNumberOfSwaps(swapJK) > 0) {
                            fullTriplets.add(new Triplet<>(swapIJ, swapIK, swapJK));
                        }
                        //non-full triplet (there we don't have a choice)
                        else {
                            requirementGraph.addEdge(edgeCounter++, swapIK, getNumberOfSwaps(swapIJ) > 0 ? swapIJ : swapJK);
                        }
                    }
                }
            }
        }

        //now in the end we go through all full triplets and have a look if we one of the 2 inner swaps are in the
        // same component already.
        // In this case, we can assign the dependency of the long edge to this what we keep anyways.
        // Otherwise pick just one (in our case the right one)
        WeakComponentClusterer<UnorderedSwap, Integer> componentClusterer = new WeakComponentClusterer<>();
        Set<Set<UnorderedSwap>> components = componentClusterer.apply(requirementGraph);
        boolean hasChanged = true;
        while (hasChanged) {
            hasChanged = false;
            Iterator<Triplet<UnorderedSwap, UnorderedSwap, UnorderedSwap>> fullTripletsIterator = fullTriplets.iterator();
            while (fullTripletsIterator.hasNext()) {
                Triplet<UnorderedSwap, UnorderedSwap, UnorderedSwap> fullTriplet = fullTripletsIterator.next();
                UnorderedSwap leftSwap = fullTriplet.getValue0();
                UnorderedSwap middleSwap = fullTriplet.getValue1();
                UnorderedSwap rightSwap = fullTriplet.getValue2();
                Set<UnorderedSwap> componentOfLeftSwap = findComponentOf(leftSwap, components);
                Set<UnorderedSwap> componentOfMiddleSwap = findComponentOf(middleSwap, components);
                Set<UnorderedSwap> componentOfRightSwap = findComponentOf(rightSwap, components);

                //are already in the same component
                if (componentOfMiddleSwap == componentOfLeftSwap) {
                    requirementGraph.addEdge(edgeCounter++, middleSwap, leftSwap);
                    fullTripletsIterator.remove();
                    continue;
                }
                if (componentOfMiddleSwap == componentOfRightSwap) {
                    requirementGraph.addEdge(edgeCounter++, middleSwap, rightSwap);
                    fullTripletsIterator.remove();
                    continue;
                }
                //both possibilities are in the same component -> then we also have no choice
                if (componentOfLeftSwap == componentOfRightSwap) {
                    requirementGraph.addEdge(edgeCounter++, middleSwap, leftSwap);
                    unifySets(components, componentOfMiddleSwap, componentOfLeftSwap);
                    hasChanged = true;
                    fullTripletsIterator.remove();
                }
            }
        }
        //for the remaining full triplets we have to unify more components. however we should not unify more than
        // necessary. First note that now the 3 swaps of a full triplet are in 3 different components.
        // we can satisfy a full triplet if we merge once a pair of components including the component of the middle
        // part. We can imagine this as a graph problem:
        // Each pair of components is a vertex (a candidate for unification) and for each full triplet we add
        // an edge between the two vertices that are both equally good candidates to satisfy this full triplet (it
        // suffices if one of them is chosen). This a minimal vertex cover problem: for any minimal vertex cover,
        // each full triplet is satisfied but we cannot split anything any more
        UndirectedGraph<Set<Set<UnorderedSwap>>, Triplet<UnorderedSwap, UnorderedSwap, UnorderedSwap>>
                componentUnificationGraph = new UndirectedSparseGraph<>();
        ArrayList<Set<UnorderedSwap>> componentsAsList = new ArrayList<>(components);
        //add vertices
        for (int i = 0; i < componentsAsList.size() - 1; i++) {
            for (int j = i + 1; j < componentsAsList.size(); j++) {
                Set<Set<UnorderedSwap>> pairSet = new LinkedHashSet<>();
                pairSet.add(componentsAsList.get(i));
                pairSet.add(componentsAsList.get(j));
                componentUnificationGraph.addVertex(pairSet);
            }
        }
        //add edges
        for (Triplet<UnorderedSwap, UnorderedSwap, UnorderedSwap> fullTriplet : fullTriplets) {
            UnorderedSwap leftSwap = fullTriplet.getValue0();
            UnorderedSwap middleSwap = fullTriplet.getValue1();
            UnorderedSwap rightSwap = fullTriplet.getValue2();
            Set<UnorderedSwap> componentOfLeftSwap = findComponentOf(leftSwap, components);
            Set<UnorderedSwap> componentOfMiddleSwap = findComponentOf(middleSwap, components);
            Set<UnorderedSwap> componentOfRightSwap = findComponentOf(rightSwap, components);

            Set<Set<UnorderedSwap>> leftPairSet = new LinkedHashSet<>();
            leftPairSet.add(componentOfLeftSwap);
            leftPairSet.add(componentOfMiddleSwap);
            Set<Set<UnorderedSwap>> rightPairSet = new LinkedHashSet<>();
            rightPairSet.add(componentOfRightSwap);
            rightPairSet.add(componentOfMiddleSwap);

            componentUnificationGraph.addEdge(fullTriplet, leftPairSet, rightPairSet);
        }
        //find minimal vertex cover
        //the innermost set is the component, the middle set is the pair of components and the outer set is the
        // minimal vertex cover of pairs of components
        Set<Set<Set<UnorderedSwap>>> minimalVertexCover = getMinimalVertexCover(componentUnificationGraph);
        //do all unifications of the minimal vertex cover
        //first add dependency edges
        for (Set<Set<UnorderedSwap>> pairOfComponents : minimalVertexCover) {
            if (pairOfComponents.size() != 2) {
                System.out.println("INVALID STATE: WRONG SIZE OF A SET, THIS SHOULD BE A PAIR OF 2 COMPONENTS");
            }
            for (Triplet<UnorderedSwap, UnorderedSwap, UnorderedSwap> fullTriplet : componentUnificationGraph
                    .getIncidentEdges(pairOfComponents)) {
                UnorderedSwap leftSwap = fullTriplet.getValue0();
                UnorderedSwap middleSwap = fullTriplet.getValue1();
                UnorderedSwap rightSwap = fullTriplet.getValue2();
                Set<UnorderedSwap> componentOfLeftSwap = findComponentOf(leftSwap, components);
                requirementGraph.addEdge(edgeCounter++, middleSwap, pairOfComponents.contains(componentOfLeftSwap) ?
                        leftSwap : rightSwap);
            }
        }
        //now unify sets
        for (Set<Set<UnorderedSwap>> pairOfComponents : minimalVertexCover) {
            Iterator<Set<UnorderedSwap>> pairIterator = pairOfComponents.iterator();
            Set<UnorderedSwap> component0 = pairIterator.next();
            Set<UnorderedSwap> component1 = pairIterator.next();

            unifySets(components, component0, component1);
        }

        return requirementGraph;
    }

    private static <V, E> Set<V> getMinimalVertexCover(UndirectedGraph<V, E> graph) {
        //start with all vertices in the vertex cover (this is for sure a vertex cover)
        Set<V> vertexCover = new LinkedHashSet<>(graph.getVertices());
        boolean hasChanged = true;
        while (hasChanged) {
            hasChanged = false;
            //go through all vertices and check if we can remove it and the vertex covere remains a vertex cover
            //then we will remove it and have a smaller one
            for (V v : graph.getVertices()) {
                if (vertexCover.contains(v)) {
                    vertexCover.remove(v);
                    if (isVertexCover(vertexCover, graph)) {
                        hasChanged = true;
                        continue;
                    }
                    //we cannot remove it without destroying the vertex cover property -> keep it
                    vertexCover.add(v);
                }
            }
        }
        return vertexCover;
    }

    private static <V, E> boolean isVertexCover(Set<V> vertexCover, UndirectedGraph<V, E> graph) {
        for (E edge : graph.getEdges()) {
            boolean isCovered = false;
            for (V incidentVertex : graph.getIncidentVertices(edge)) {
                if (vertexCover.contains(incidentVertex)) {
                    isCovered = true;
                }
            }
            if (!isCovered) {
                return false;
            }
        }
        return true;
    }

    private void unifySets(Set<Set<UnorderedSwap>> components, Set<UnorderedSwap> comp0, Set<UnorderedSwap> comp1) {
        comp0.addAll(comp1);
        components.remove(comp1);
    }

    private Set<UnorderedSwap> findComponentOf(UnorderedSwap swap, Set<Set<UnorderedSwap>> components) {
        for (Set<UnorderedSwap> component : components) {
            if (component.contains(swap)) {
                return component;
            }
        }
        return null;
    }

    private Collection<UnorderedSwap> permute(Collection<UnorderedSwap> swaps, List<Line> permutation) {
        List<UnorderedSwap> permutedSwaps = new ArrayList<>();
        for (UnorderedSwap swap : swaps) {
            Line permutedLine0 = new Line(permutation.indexOf(swap.getLineWithSmallerNumber()));
            Line permutedLine1 = new Line(permutation.indexOf(swap.getLineWithGreaterNumber()));
            permutedSwaps.add(new UnorderedSwap(permutedLine0, permutedLine1));
        }
        return permutedSwaps;
    }

    private void takeOutSwapsAgainstForbiddenTripletConfigs(Set<UnorderedSwap> swapSet,
                                                            Set<UnorderedSwap> takenOutSet) {
        boolean hasChanged = true;
        while (hasChanged) {
            hasChanged = false;
            for (int i = 0; i < numberOfLines - 2; ++i) {
                Line lineI = new Line(i);
                for (int j = i + 1; j < numberOfLines - 1; ++j) {
                    Line lineJ = new Line(j);
                    for (int k = j + 1; k < numberOfLines; ++k) {
                        Line lineK = new Line(k);

                        UnorderedSwap swapIJ = new UnorderedSwap(lineI, lineJ);
                        UnorderedSwap swapIK = new UnorderedSwap(lineI, lineK);
                        UnorderedSwap swapJK = new UnorderedSwap(lineJ, lineK);

                        //case separable config (umbrella):
                        if (!swapSet.contains(swapIJ) && swapSet.contains(swapIK) && !swapSet.contains(swapJK)) {
                            //remove the middle swap
                            swapSet.remove(swapIK);
                            takenOutSet.add(swapIK);
                            hasChanged = true;
                        }

                        //case non-consistent config (double bow):
                        if (swapSet.contains(swapIJ) && !swapSet.contains(swapIK) && swapSet.contains(swapJK)) {
                            //remove the left swap
                            swapSet.remove(swapIJ);
                            takenOutSet.add(swapIJ);
                            hasChanged = true;
                        }
                    }
                }
            }
        }
    }

    private void assignAlternating(DirectedGraph<UnorderedSwap, Integer> graph, UnorderedSwap vertex,
                                   Set<UnorderedSwap> usedTakenOutSet, Set<UnorderedSwap> otherTakenOutSet) {
        //if it is contained nowhere -> add it
        if (!usedTakenOutSet.contains(vertex) && !otherTakenOutSet.contains(vertex)) {
            usedTakenOutSet.add(vertex);
        }
        else {
            //otherwise stop here because we have reached a chain where we have already been
            return;
        }
        //now find all neighboring bows and add them to the other set
        Collection<Integer> edges = graph.getIncidentEdges(vertex);
        if (edges != null && !edges.isEmpty()) {
            //if they exist, continue recursively
            for (Integer edge : edges) {
                assignAlternating(graph, graph.getOpposite(vertex, edge), otherTakenOutSet, usedTakenOutSet);
            }
        }
        //for all bows that the current bow depends on or that depend on the current bow, add them to the same set
        for (UnorderedSwap otherVertex : graph.getVertices()) {
            if (!vertex.equals(otherVertex) && (dependsOn(vertex, otherVertex) || dependsOn(otherVertex, vertex))) {
                assignAlternating(graph, otherVertex, usedTakenOutSet, otherTakenOutSet);
            }
        }
    }

    /**
     *
     * @param swap0
     * @param swap1
     * @return
     *      Whether swap0 can only be executed (that is here, it is in a non-separable configuration) if swap1 is there.
     *      Hence, it is a requirement that swap1 spans a smaller distance in the initial permutation than swap0.
     */
    private boolean dependsOn(UnorderedSwap swap0, UnorderedSwap swap1) {
        Line swap0Line0 = swap0.getLineWithSmallerNumber();
        Line swap0Line1 = swap0.getLineWithGreaterNumber();
        Line swap1Line0 = swap1.getLineWithSmallerNumber();
        Line swap1Line1 = swap1.getLineWithGreaterNumber();

        //if swap1 spans more than swap0 then we can return false
        if (swap1Line0.getLineNumber() < swap0Line0.getLineNumber() ||
                swap0Line1.getLineNumber() < swap1Line1.getLineNumber()) {
            return false;
        }

        //if 2 lines are the same then swap0 depends on swap1 if there is no other swap connecting to the third wire
        if (swap0Line0.equals(swap1Line0)) {
            UnorderedSwap thirdSwap = new UnorderedSwap(swap1Line1, swap0Line1);
            if (this.getNumberOfSwaps(thirdSwap) == 0) {
                return true;
            }
        }
        if (swap0Line1.equals(swap1Line1)) {
            UnorderedSwap thirdSwap = new UnorderedSwap(swap0Line0, swap1Line0);
            if (this.getNumberOfSwaps(thirdSwap) == 0) {
                return true;
            }
        }

        //otherwise the two swaps may depend on each other indirectly --> go through all wires spanned by swap0
        boolean dependsIndirectly = false;
        for (int i = swap0Line0.getLineNumber() + 1; i < swap0Line1.getLineNumber(); i++) {
            Line middleWire = new Line(i);
            UnorderedSwap swapLeft = new UnorderedSwap(swap0Line0, middleWire);
            UnorderedSwap swapRight = new UnorderedSwap(middleWire, swap0Line1);
            if (this.getNumberOfSwaps(swapLeft) == 0) {
                dependsIndirectly |= dependsOn(swapRight, swap1);
            }
            if (this.getNumberOfSwaps(swapRight) == 0) {
                dependsIndirectly |= dependsOn(swapLeft, swap1);
            }
        }

        return dependsIndirectly;
    }








    ////////////////////////////////////////////////////////////////////////////////////////////
    // older method: by performing any swap that does not lead into a forbidden configuration //
    ////////////////////////////////////////////////////////////////////////////////////////////




    public RealizationGraph computeOneRealizationByOutdated02Method() {
        LinkedHashMap<UnorderedSwap, Integer> remainingSwaps = new LinkedHashMap<>();
        for (int i = 0; i < numberOfLines - 1; i++) {
            for (int j = i + 1; j < numberOfLines; j++) {
                UnorderedSwap swap = new UnorderedSwap(new Line(i), new Line(j));
                if (this.inputMatrix[i][j] != 0) {
                    //use only absolute values for the matrices
                    remainingSwaps.put(swap, Math.abs(this.inputMatrix[i][j]));
                }
            }
        }

        ArrayList<Line> currentTangle =
                IntStream.range(0, numberOfLines).mapToObj(Line::new) .collect(Collectors.toCollection(ArrayList::new));
        RealizationGraph realization = new RealizationGraph();
        Pair<OrderedSwap, Integer> lastVertexInRealization = null;
        int edgeCounter = 0;
        while (!remainingSwaps.isEmpty()) {
            ArrayList<UnorderedSwap> allOpenSwapsOfCurrentNeighbors =
                    getAllOpenSwapsOfCurrentNeighbors(remainingSwaps, currentTangle);
            Set<UnorderedSwap> allForbiddenSwapsAccordingToTriplets =
                    getAllForbiddenSwapsAccordingToTriplets(remainingSwaps);

            //remove the forbidden swaps from the possible swaps
            allOpenSwapsOfCurrentNeighbors.removeIf(allForbiddenSwapsAccordingToTriplets::contains);

            //sort possible swaps by occurrence increasingly (first the ones with 1 swap open
//            allOpenSwapsOfCurrentNeighbors.sort(Comparator.comparingInt(remainingSwaps::get));

            //execute some swap of the remaining possible swaps
            UnorderedSwap swapToBeExecuted = chooseSwapSuchThatUnforbiddenSwapBecomesPossible(
                    allOpenSwapsOfCurrentNeighbors, remainingSwaps, currentTangle, allForbiddenSwapsAccordingToTriplets);
//                swapToBeExecuted = allOpenSwapsOfCurrentNeighbors.get(0);

            //update current swap diagram and realization graph
            executeSwap(currentTangle, swapToBeExecuted);
            Pair<OrderedSwap, Integer> currentVertexInRealization = remainingSwaps.get(swapToBeExecuted) == 2 ?
                    new Pair<>(new OrderedSwap(swapToBeExecuted.getValue0(), swapToBeExecuted.getValue1()), 0) :
                    new Pair<>(new OrderedSwap(swapToBeExecuted.getValue1(), swapToBeExecuted.getValue0()), 0);
            realization.addVertex(currentVertexInRealization);
            if (lastVertexInRealization != null) {
                realization.addEdge(edgeCounter++, lastVertexInRealization, currentVertexInRealization);
            }
            lastVertexInRealization = currentVertexInRealization;

            //update remaining swaps
            remainingSwaps.replace(swapToBeExecuted, remainingSwaps.get(swapToBeExecuted) - 1);
            if (remainingSwaps.get(swapToBeExecuted) == 0) {
                remainingSwaps.remove(swapToBeExecuted); //remove it from the map if no swap remains
            }
        }
        return realization;
    }

    private UnorderedSwap chooseSwapSuchThatUnforbiddenSwapBecomesPossible(ArrayList<UnorderedSwap> allOpenSwapsOfCurrentNeighbors,
                                                                           Map<UnorderedSwap, Integer> remainingSwaps,
                                                                           ArrayList<Line> currentTangle,
                                                                           Set<UnorderedSwap> allForbiddenSwapsAccordingToTriplets) {
        UnorderedSwap bestChoice = null;
        int mostNewSwaps = 0;
        for (UnorderedSwap possibleSwap : allOpenSwapsOfCurrentNeighbors) {
            int newSwaps = 0;

            int indexLeft = Math.min(currentTangle.indexOf(possibleSwap.getValue0()),
                    currentTangle.indexOf(possibleSwap.getValue1()));

            //perform the swap for testing
            executeSwap(currentTangle, possibleSwap);

            if (indexLeft > 0 && givesNewSwap(currentTangle, indexLeft - 1, remainingSwaps, allForbiddenSwapsAccordingToTriplets)) {
                ++newSwaps;
            }
            if (remainingSwaps.get(possibleSwap) > 1 && givesNewSwap(currentTangle, indexLeft, remainingSwaps,
                    allForbiddenSwapsAccordingToTriplets)) {
                ++newSwaps;
            }
            if (indexLeft < currentTangle.size() - 2 && givesNewSwap(currentTangle, indexLeft + 1, remainingSwaps,
                    allForbiddenSwapsAccordingToTriplets)) {
                ++newSwaps;
            }

            //undo the swap for testing
            executeSwap(currentTangle, possibleSwap);

            //check if best choice
            if (newSwaps >= mostNewSwaps) {
                mostNewSwaps = newSwaps;
                bestChoice = possibleSwap;
            }
        }
        return bestChoice;
    }

    private boolean givesNewSwap(ArrayList<Line> currentTangle, int indexLeft, Map<UnorderedSwap, Integer> remainingSwaps,
                                 Set<UnorderedSwap> allForbiddenSwapsAccordingToTriplets) {
        UnorderedSwap swap = new UnorderedSwap(currentTangle.get(indexLeft), currentTangle.get(indexLeft + 1));
        if (remainingSwaps.containsKey(swap) && !allForbiddenSwapsAccordingToTriplets.contains(swap)) {
            return true;
        }
        return false;
    }

    private void executeSwap(ArrayList<Line> currentTangle, UnorderedSwap swapToBeExecuted) {
        int index0  = currentTangle.indexOf(swapToBeExecuted.getValue0());
        int index1  = currentTangle.indexOf(swapToBeExecuted.getValue1());

        currentTangle.set(index0, swapToBeExecuted.getValue1());
        currentTangle.set(index1, swapToBeExecuted.getValue0());
    }

    private ArrayList<UnorderedSwap> getAllOpenSwapsOfCurrentNeighbors(Map<UnorderedSwap, Integer> remainingSwaps,
                                                                       List<Line> currentTangle) {
        ArrayList<UnorderedSwap> allOpenSwapsOfCurrentNeighbors = new ArrayList<>();
        for (int i = 0; i < currentTangle.size() - 1; i++) {
            UnorderedSwap neighboringWires = new UnorderedSwap(currentTangle.get(i), currentTangle.get(i + 1));
            if (remainingSwaps.containsKey(neighboringWires)) {
                allOpenSwapsOfCurrentNeighbors.add(neighboringWires);
            }
        }
        return allOpenSwapsOfCurrentNeighbors;
    }

    private Set<UnorderedSwap> getAllForbiddenSwapsAccordingToTriplets(Map<UnorderedSwap, Integer> remainingSwaps) {
        LinkedHashSet<UnorderedSwap> forbiddenSwaps = new LinkedHashSet<>();

        for (int i = 0; i < numberOfLines - 2; ++i) {
            Line lineI = new Line(i);
            for (int j = i + 1; j < numberOfLines - 1; ++j) {
                Line lineJ = new Line(j);
                for (int k = j + 1; k < numberOfLines; ++k) {
                    Line lineK = new Line(k);

                    UnorderedSwap swapIJ = new UnorderedSwap(lineI, lineJ);
                    UnorderedSwap swapIK = new UnorderedSwap(lineI, lineK);
                    UnorderedSwap swapJK = new UnorderedSwap(lineJ, lineK);
                    int remainingIJ = remainingSwaps.getOrDefault(swapIJ, 0);
                    int remainingIK = remainingSwaps.getOrDefault(swapIK, 0);
                    int remainingJK = remainingSwaps.getOrDefault(swapJK, 0);

                    //go through all cases of triplets where we cannot perform some swap

                    if (remainingIJ == 1 && remainingIK > 0 && remainingJK == 0) {
                        forbiddenSwaps.add(swapIJ);
                    }
                    if (remainingJK == 1 && remainingIK > 0 && remainingIJ == 0) {
                        forbiddenSwaps.add(swapJK);
                    }
                }
            }
        }

        //we also need to check that no wire gets stuck in between two wires it is forbidden to swap with then
        if (numberOfLines >= 4) {
            for (int i = 0; i < numberOfLines - 3; ++i) {
                Line lineI = new Line(i);
                for (int j = i + 1; j < numberOfLines - 2; ++j) {
                    Line lineJ = new Line(j);
                    for (int k = j + 1; k < numberOfLines - 1; ++k) {
                        Line lineK = new Line(k);
                        for (int l = k + 1; l < numberOfLines; ++l) {
                            Line lineL = new Line(l);

                            UnorderedSwap swapIJ = new UnorderedSwap(lineI, lineJ);
                            UnorderedSwap swapIK = new UnorderedSwap(lineI, lineK);
                            UnorderedSwap swapIL = new UnorderedSwap(lineI, lineL);
                            UnorderedSwap swapJK = new UnorderedSwap(lineJ, lineK);
                            UnorderedSwap swapJL = new UnorderedSwap(lineJ, lineL);
                            UnorderedSwap swapKL = new UnorderedSwap(lineK, lineL);
                            int remainingIJ = remainingSwaps.getOrDefault(swapIJ, 0);
                            int remainingIK = remainingSwaps.getOrDefault(swapIK, 0);
                            int remainingIL = remainingSwaps.getOrDefault(swapIL, 0);
                            int remainingJK = remainingSwaps.getOrDefault(swapJK, 0);
                            int remainingJL = remainingSwaps.getOrDefault(swapJL, 0);
                            int remainingKL = remainingSwaps.getOrDefault(swapKL, 0);

                            //keep loop configuration possible, i.e. if i an l have to meet each other inside a
                            // loop built by j and k, we have to wait before we make the second j--k swap until the
                            // i--l swaps have occurred

                            if (remainingIJ == 0 && remainingIL > 0 && remainingJK == 1 && remainingKL == 0) {
                                forbiddenSwaps.add(swapJK);
                            }


                            //keep sequential inner loop possible, i.e., if i--k and j--l need to meet we first have
                            // i--j or k--l but not both
                            if (remainingIJ == 1 && remainingIK > 0 && remainingIL == 0 && remainingJK == 0
                                    && remainingJL > 0 && remainingKL == 2) {
                                forbiddenSwaps.add(swapKL);
                            }
                            if (remainingIJ == 2 && remainingIK > 0 && remainingIL == 0 && remainingJK == 0
                                    && remainingJL > 0 && remainingKL == 1) {
                                forbiddenSwaps.add(swapIJ);
                            }
                        }
                    }
                }
            }
        }

        //we also need to check that no wire gets stuck in between two wires it is forbidden to swap with then
        if (numberOfLines >= 5) {
            for (int i = 0; i < numberOfLines - 4; ++i) {
                Line lineI = new Line(i);
                for (int j = i + 1; j < numberOfLines - 3; ++j) {
                    Line lineJ = new Line(j);
                    for (int k = j + 1; k < numberOfLines - 2; ++k) {
                        Line lineK = new Line(k);
                        for (int l = k + 1; l < numberOfLines - 1; ++l) {
                            Line lineL = new Line(l);
                            for (int m = l + 1; m < numberOfLines; ++m) {
                                Line lineM = new Line(m);

                                UnorderedSwap swapIJ = new UnorderedSwap(lineI, lineJ);
                                UnorderedSwap swapIK = new UnorderedSwap(lineI, lineK);
                                UnorderedSwap swapIL = new UnorderedSwap(lineI, lineL);
                                UnorderedSwap swapIM = new UnorderedSwap(lineI, lineM);
                                UnorderedSwap swapJK = new UnorderedSwap(lineJ, lineK);
                                UnorderedSwap swapJL = new UnorderedSwap(lineJ, lineL);
                                UnorderedSwap swapJM = new UnorderedSwap(lineJ, lineM);
                                UnorderedSwap swapKL = new UnorderedSwap(lineK, lineL);
                                UnorderedSwap swapKM = new UnorderedSwap(lineK, lineM);
                                UnorderedSwap swapLM = new UnorderedSwap(lineL, lineM);
                                int remainingIJ = remainingSwaps.getOrDefault(swapIJ, 0);
                                int remainingIK = remainingSwaps.getOrDefault(swapIK, 0);
                                int remainingIL = remainingSwaps.getOrDefault(swapIL, 0);
                                int remainingIM = remainingSwaps.getOrDefault(swapIM, 0);
                                int remainingJK = remainingSwaps.getOrDefault(swapJK, 0);
                                int remainingJL = remainingSwaps.getOrDefault(swapJL, 0);
                                int remainingJM = remainingSwaps.getOrDefault(swapJM, 0);
                                int remainingKL = remainingSwaps.getOrDefault(swapKL, 0);
                                int remainingKM = remainingSwaps.getOrDefault(swapKM, 0);
                                int remainingLM = remainingSwaps.getOrDefault(swapLM, 0);

                                //go through all cases of triplets where we cannot perform some swap
                                //bc we are then locked in between 2 wires with which we are not allowed to swap

                                if (remainingIK == 2 && remainingIL == 0 && remainingJK == 2 && remainingJL == 1 &&
                                        remainingJM == 0 && remainingKL == 1 && remainingKM == 2) {
                                    forbiddenSwaps.add(swapJK);
                                }
                                if (remainingKM == 2 && remainingJM == 0 && remainingKL == 2 && remainingJL == 1 &&
                                        remainingIL == 0 && remainingJK == 1 && remainingIK == 2) {
                                    forbiddenSwaps.add(swapKL);
                                }
                            }
                        }
                    }
                }
            }
        }

        return forbiddenSwaps;
    }
}

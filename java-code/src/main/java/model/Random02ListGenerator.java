package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.IntStream;

public class Random02ListGenerator {

    public enum Method {
        KEEP_0_AGAINST_INCONSISTENCY,
        KEEP_2_AGAINST_INCONSISTENCY,
        COMBINE_LINEAR_ORDERS;
    }

    public static final int DEFAULT_NUMBER_OF_LINEAR_ORDERS = 6;


    public static LinkedHashMap<UnorderedSwap, Integer> getRandomNonSeparable02List(Method method, int numberOfWires) {
        return getRandomNonSeparable02List(method, numberOfWires, DEFAULT_NUMBER_OF_LINEAR_ORDERS);
    }

    public static LinkedHashMap<UnorderedSwap, Integer> getRandomNonSeparable02List(Method method, int numberOfWires,
                                                                                    int numberOfLinearOrders) {
        switch (method) {
            case KEEP_0_AGAINST_INCONSISTENCY:
                return getRandomNonSeparable02ListStartingAt0s(numberOfWires);
            case KEEP_2_AGAINST_INCONSISTENCY:
                return getRandomNonSeparable02ListStartingAt2s(numberOfWires);
            case COMBINE_LINEAR_ORDERS:
                return getRandomNonSeparable02ByLinearOrders(numberOfWires, numberOfLinearOrders);
        }
        return null;
    }

    public static LinkedHashMap<UnorderedSwap, Integer> getRandomNonSeparable02ListStartingAt0s(int numberOfWires) {
        LinkedHashMap<UnorderedSwap, Integer> random02list = new LinkedHashMap<>();

        //first go through all swaps and set them to 0 or 2 randomly
        for (int i = 0; i < numberOfWires - 1; i++) {
            for (int j = i + 1; j < numberOfWires; j++) {
                UnorderedSwap swap = new UnorderedSwap(new Line(i), new Line(j));
                if (MainClass.RANDOM.nextBoolean()) {
                    random02list.put(swap, 0);
                }
                else {
                    random02list.put(swap, 2);
                }
            }
        }

        //as long as this whole construction remains separable, replace conflicting 2s by 0s
        while (!MainClass.isNonSeparable(random02list)) {
            //find conflicting triplets
            for (int j = 1; j < numberOfWires - 1; j++) {
                for (int i = 0; i < j; i++) {
                    for (int k = j + 1; k < numberOfWires; k++) {
                        UnorderedSwap swapIJ = new UnorderedSwap(new Line(i), new Line(j));
                        UnorderedSwap swapJK = new UnorderedSwap(new Line(j), new Line(k));
                        UnorderedSwap swapIK = new UnorderedSwap(new Line(i), new Line(k));

                        if (random02list.get(swapIJ) == 0 && random02list.get(swapJK) == 0) {
                            random02list.replace(swapIK, 0);
                        }
                    }
                }
            }
        }

        return random02list;
    }

    public static LinkedHashMap<UnorderedSwap, Integer> getRandomNonSeparable02ListStartingAt2s(int numberOfWires) {
        //initial capacity is binomial coefficient -> 2 out of numberOfWires
        ArrayList<UnorderedSwap> sequenceOfSwaps = new ArrayList<>((numberOfWires * (numberOfWires - 1)) / 2);
        LinkedHashMap<UnorderedSwap, Integer> random02list = new LinkedHashMap<>();
        //start with the full 2-list, i.e., all entries are 2s, this is definitely non-separable
        for (int i = 0; i < numberOfWires - 1; i++) {
            for (int j = i + 1; j < numberOfWires; j++) {
                UnorderedSwap swap = new UnorderedSwap(new Line(i), new Line(j));
                sequenceOfSwaps.add(swap);
                random02list.put(swap, 2);
            }
        }
        //now go through all swaps in some random order and try to set it to 0 with prob 50 % and if it remains non-sep.
        Collections.shuffle(sequenceOfSwaps, MainClass.RANDOM);
        for (UnorderedSwap swap : sequenceOfSwaps) {
            if (MainClass.RANDOM.nextBoolean()) {
                //try to set it to 0
                random02list.replace(swap, 0);
                if (!MainClass.isNonSeparable(random02list)) {
                    //this operation makes it separable -> undo
                    random02list.replace(swap, 2);
                }
            }
        }

        return random02list;
    }

    private static LinkedHashMap<UnorderedSwap, Integer> getRandomNonSeparable02ByLinearOrders(int numberOfWires,
                                                                                               int numberOfLinearOrders) {
        List<Line> allWires =
                IntStream.range(0, numberOfWires).mapToObj(Line::new).collect(java.util.stream.Collectors.toList());

        List<List<Line>> allPermutations = new ArrayList<>(numberOfLinearOrders + 1);
        //we always also use the initial default permutation of wires as one of our permutations
        allPermutations.add(new ArrayList<>(allWires));
        //now get numberOfLinearOrders many linear orders, i.e., permutation of the wires
        for (int i = 0; i < numberOfLinearOrders; i++) {
            //first bring the wires in a new random permutation
            Collections.shuffle(allWires, MainClass.RANDOM);
            //now save this permuation
            allPermutations.add(new ArrayList<>(allWires));
        }


        //init a swap lists with 0s
        LinkedHashMap<UnorderedSwap, Integer> random02list = new LinkedHashMap<>();
        for (int i = 0; i < numberOfWires - 1; i++) {
            for (int j = i + 1; j < numberOfWires; j++) {
                UnorderedSwap swap = new UnorderedSwap(new Line(i), new Line(j));
                random02list.put(swap, 0);
            }
        }

        //now go through all pairs of permutations and for each pair of wires check if they are in different order ->
        // if so add 2 swaps
        for (int p0 = 0; p0 < allPermutations.size() - 1; p0++) {
            List<Line> permutation0 = allPermutations.get(p0);
            for (int p1 = p0 + 1; p1 < allPermutations.size(); p1++) {
                List<Line> permutation1 = allPermutations.get(p1);

                //now go through all pairs of wires, i.e. all potential swaps
                for (int i = 0; i < numberOfWires - 1; i++) {
                    Line lineI = new Line(i);
                    int posI0 = permutation0.indexOf(lineI);
                    int posI1 = permutation1.indexOf(lineI);
                    for (int j = i + 1; j < numberOfWires; j++) {
                        Line lineJ = new Line(j);
                        int posJ0 = permutation0.indexOf(lineJ);
                        int posJ1 = permutation1.indexOf(lineJ);

                        if ( (posI0 < posJ0 && posI1 > posJ1) || (posI0 > posJ0 && posI1 < posJ1) ) {
                            //the orders of wire i and j differ in permutation 0 and permutation 1
                            // -> we use 2 swaps
                            UnorderedSwap swap = new UnorderedSwap(lineI, new Line(j));
                            random02list.replace(swap, 2);
                        }
                    }
                }
            }
        }

        return random02list;
    }

}

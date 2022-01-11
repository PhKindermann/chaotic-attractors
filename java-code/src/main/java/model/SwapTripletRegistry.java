package model;

import org.javatuples.Triplet;

import java.util.*;

public class SwapTripletRegistry {

    public static enum TripletSwap {
        SWAP_01, SWAP_02, SWAP_12,
        SWAP_10, SWAP_20, SWAP_21;
    }

    private static class SwapTriplet {

        private int count01swaps;
        private int count02swaps;
        private int count12swaps;

        public List<List<TripletSwap>> realizations;
        private Boolean isRealizable = null;

        public SwapTriplet(int count01swaps, int count02swaps, int count12swaps) {
            this.count01swaps = count01swaps;
            this.count02swaps = count02swaps;
            this.count12swaps = count12swaps;
        }

        public int get01swaps() {
            return count01swaps;
        }

        public int get02swaps() {
            return count02swaps;
        }

        public int get12swaps() {
            return count12swaps;
        }

        public synchronized List<List<TripletSwap>> getRealizations() {
            if (realizations == null) {
                realizations = new ArrayList<>();
                //special case: there is a realization of nothing, i.e. doing nothing
                if (get01swaps() == 0 && get02swaps() == 0 && get12swaps() == 0) {
                    realizations.add(Collections.EMPTY_LIST);
                }
                //base case: perform a possible swap and use data from what's left
                if (get01swaps() > 0) {
                    //changed order because we perform an initial 0-1 swap and now 0 is the new 1 and the other way round
                    //therefore we also lower the number of 0-1 swaps by one
                    for (List<TripletSwap> baseRealization : SwapTripletRegistry.getRealizations(get01swaps() - 1, get12swaps(), get02swaps())) {
                        ArrayList<TripletSwap> newRealization = new ArrayList<>(Collections.singleton(TripletSwap.SWAP_01));
                        newRealization.addAll(baseRealization);
                        interchangeTwoSwaps(newRealization, TripletSwap.SWAP_01, TripletSwap.SWAP_10, 1);
                        interchangeTwoSwaps(newRealization, TripletSwap.SWAP_02, TripletSwap.SWAP_12, 1);
                        interchangeTwoSwaps(newRealization, TripletSwap.SWAP_20, TripletSwap.SWAP_21, 1);
                        realizations.add(newRealization);
                    }
                }
                if (get12swaps() > 0) {
                    //changed order because we perform an initial 1-2 swap and now 1 is the new 2 and the other way round
                    //therefore we also lower the number of 1-2 swaps by one
                    for (List<TripletSwap> baseRealization : SwapTripletRegistry.getRealizations(get02swaps(), get01swaps(), get12swaps() - 1)) {
                        ArrayList<TripletSwap> newRealization = new ArrayList<>(Collections.singleton(TripletSwap.SWAP_12));
                        newRealization.addAll(baseRealization);
                        interchangeTwoSwaps(newRealization, TripletSwap.SWAP_12, TripletSwap.SWAP_21, 1);
                        interchangeTwoSwaps(newRealization, TripletSwap.SWAP_01, TripletSwap.SWAP_02, 1);
                        interchangeTwoSwaps(newRealization, TripletSwap.SWAP_10, TripletSwap.SWAP_20, 1);
                        realizations.add(newRealization);
                    }
                }
            }

            return realizations;
        }

        public boolean isRealizable() {
            if (isRealizable == null) {
                if (get01swaps() == 0 && get02swaps() == 0 && get12swaps() == 0) {
                    isRealizable = true;
                }
                if (get01swaps() > 0 && SwapTripletRegistry.isRealizable(get01swaps() - 1, get12swaps(), get02swaps())) {
                    isRealizable = true;
                }
                if (get12swaps() > 0 && SwapTripletRegistry.isRealizable(get02swaps(), get01swaps(), get12swaps() - 1)) {
                    isRealizable = true;
                }
                if (isRealizable == null) {
                    isRealizable = false;
                }
            }
            return isRealizable;
        }

        private void interchangeTwoSwaps(List<TripletSwap> list, TripletSwap tripletSwapA, TripletSwap tripletSwapB, int startIndex) {
            for (int i = startIndex; i < list.size(); ++i) {
                if (list.get(i) == tripletSwapA) {
                    list.set(i, tripletSwapB);
                }
                else if (list.get(i) == tripletSwapB) {
                    list.set(i, tripletSwapA);
                }
            }
        }
    }


    private static LinkedHashMap<Triplet<Integer, Integer, Integer>, SwapTriplet> OCCURRENCES_2_SWAP_TRIPLET = new LinkedHashMap<>();

    public static boolean isRealizable(int occ01, int occ02, int occ12) {
        return findSwapTriplet(occ01, occ02, occ12).isRealizable();

    }

    public static Collection<List<TripletSwap>> getRealizations(int occ01, int occ02, int occ12) {
        return findSwapTriplet(occ01, occ02, occ12).getRealizations();
    }

    private synchronized static SwapTriplet findSwapTriplet(int occ01, int occ02, int occ12) {
        SwapTriplet swapTriplet = OCCURRENCES_2_SWAP_TRIPLET.get(new Triplet<>(occ01, occ02, occ12));
        if (swapTriplet == null) {
            swapTriplet = new SwapTriplet(occ01, occ02, occ12);
            OCCURRENCES_2_SWAP_TRIPLET.put(new Triplet<>(occ01, occ02, occ12), swapTriplet);
        }
//        if (swapTriplet.count01swaps == 2 && swapTriplet.count02swaps == 2 && swapTriplet.count12swaps == 2) {
//            List<List<TripletSwap>> realizations = swapTriplet.getRealizations();
//            if (realizations.size() > 7) {
//                List<TripletSwap> keep = realizations.get(0);
//                realizations.clear();
//                realizations.add(keep);
//                realizations.remove(5);
//                realizations.remove(2);
//            }
//        }
//        if (swapTriplet.count01swaps == 2 && swapTriplet.count02swaps == 0 && swapTriplet.count12swaps == 2) {
//            List<List<TripletSwap>> realizations = swapTriplet.getRealizations();
//            if (realizations.size() > 1) {
//                List<TripletSwap> keep = realizations.get(0);
//                realizations.clear();
//                realizations.add(keep);
//            }
//        }
        return swapTriplet;
    }
}

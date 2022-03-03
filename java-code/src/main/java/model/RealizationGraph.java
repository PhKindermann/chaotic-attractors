package model;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import org.javatuples.Pair;

import java.util.*;

/**
 * Vertices are swaps of lines + the count of the swap (e.g. if the second value of the pair (i,j) is 5,
 * then this is the 5-th ordered(!) swap of i with j, i.e., the 9-th or 10-th unordered swaps of i and j.
 * The data type of the edges is irrelevant.
 * It is here Integer just because JUNG requires something.
 */
public class RealizationGraph extends DirectedSparseGraph<Pair<OrderedSwap, Integer>, Integer> {


    /**
     * This {@link RealizationGraph} instance must be acyclic - this is not checked!
     *
     * @return
     */
    public int getHeight(LineSwapper correspondingLineSwapper) {
        return this.getSwappingDiagramOfMinimumHeight(correspondingLineSwapper).getLayers().size();
    }

    /**
     * This {@link RealizationGraph} instance must be acyclic - this is not checked!
     *
     * @return
     */
    public SwappingDiagram getSwappingDiagramOfMinimumHeight(LineSwapper correspondingLineSwapper) {
        //map from a swap to its inDegree (for swaps with inDeg > 0)
        Map<Pair<OrderedSwap, Integer>, Integer> swap2PredecessorCount = new LinkedHashMap<>();
        //swaps with inDegree = 0
        List<Pair<OrderedSwap, Integer>> currentSources = new ArrayList<>();

        for (Pair<OrderedSwap, Integer> swap : this.getVertices()) {
            int predecessorCount = this.getPredecessorCount(swap);
            if (predecessorCount == 0) {
                currentSources.add(swap);
            }
            else {
                swap2PredecessorCount.put(swap, predecessorCount);
            }
        }

        SwappingDiagram swappingDiagram = new SwappingDiagram(correspondingLineSwapper.getNumberOfLines(),
                correspondingLineSwapper.getFinalPermutation());
        while (!swap2PredecessorCount.isEmpty()) {
            //remove the vertices without predecessors, i.e., the sources - we do all theses swaps initially and simultaneously
            //this will be the current layer, which is added to the swappingDiagram
            swappingDiagram.getLayers().add(currentSources);
            //lower inDegree for swaps that are successors of the swaps that are added to the current layer
            List<Pair<OrderedSwap, Integer>> newSources = new ArrayList<>();
            for (Pair<OrderedSwap, Integer> swap : currentSources) {
                for (Pair<OrderedSwap, Integer> successor : this.getSuccessors(swap)) {
                    int inDeg = swap2PredecessorCount.get(successor);
                    if (inDeg == 1) {
                        swap2PredecessorCount.remove(successor);
                        newSources.add(successor);
                    }
                    else {
                        swap2PredecessorCount.replace(successor, inDeg - 1);
                    }
                }
            }
            currentSources = newSources;
        }
        if (!currentSources.isEmpty()) {
            swappingDiagram.getLayers().add(currentSources);
        }

        return swappingDiagram;
    }

}

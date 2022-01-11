package model;

import edu.uci.ics.jung.graph.DirectedGraph;

import java.util.LinkedHashSet;

public class CycleFinder {

    /**
     * see https://www.geeksforgeeks.org/detect-cycle-in-a-graph/
     *
     * @param diGraph
     * @param <V>
     * @param <E>
     * @return
     */
    public static <V, E> boolean containsCycle(DirectedGraph<V, E> diGraph) {
        // Mark all the vertices as not visited and
        // not part of recursion stack
        LinkedHashSet<V> visited = new LinkedHashSet<>(diGraph.getVertexCount());
        LinkedHashSet<V> recStack = new LinkedHashSet<>(diGraph.getVertexCount());


        // Call the recursive helper function to
        // detect cycle in different DFS trees
        for (V v : diGraph.getVertices()) {
            if (isCyclicUtil(diGraph, v, visited, recStack)) {
                return true;
            }
        }

        return false;
    }

    private static <V, E> boolean isCyclicUtil(DirectedGraph<V, E> diGraph, V v, LinkedHashSet<V> visited, LinkedHashSet<V> recStack) {
        // Mark the current node as visited and
        // part of recursion stack
        if (recStack.contains(v)) {
            return true;
        }

        if (visited.contains(v)) {
            return false;
        }

        visited.add(v);

        recStack.add(v);

        for (V c : diGraph.getSuccessors(v)) {
            if (isCyclicUtil(diGraph, c, visited, recStack)) {
                return true;
            }
        }

        recStack.remove(v);

        return false;
    }
}

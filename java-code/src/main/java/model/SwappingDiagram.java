package model;

import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.List;

public class SwappingDiagram {
    private int numberOfLines;
    private List<List<Pair<OrderedSwap, Integer>>> layers;
    private List<Line> finalPermutation;

    public SwappingDiagram(int numberOfLines, List<Line> finalPermutation) {
        this.numberOfLines = numberOfLines;
        this.finalPermutation = finalPermutation;
        this.layers = new ArrayList<>();
    }

    public SwappingDiagram(int numberOfLines, List<Line> finalPermutation, List<List<Pair<OrderedSwap, Integer>>> layers) {
        this.numberOfLines = numberOfLines;
        this.finalPermutation = finalPermutation;
        this.layers = layers;
    }

    public int getNumberOfLines() {
        return numberOfLines;
    }

    public List<List<Pair<OrderedSwap, Integer>>> getLayers() {
        return layers;
    }

    @Override
    public String toString() {
        String s = "{\"h\": " + layers.size() + ", \"s\": [";
        boolean outerCommaNeeded = false;
        for (List<Pair<OrderedSwap, Integer>> layer : layers) {
            if (outerCommaNeeded) {
                s += ", ";
            }
            s += "[";
            boolean commaNeeded = false;
            for (Pair<OrderedSwap, Integer> exactSwap : layer) {
                if (commaNeeded) {
                    s += ", ";
                }
                s += exactSwap.getValue0().getUnorderedSwap();
                commaNeeded = true;
            }
            s += "]";
            outerCommaNeeded = true;
        }
        return s + "], \"p\": " + finalPermutation + "}";
    }
}

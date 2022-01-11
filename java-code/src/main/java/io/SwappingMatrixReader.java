package io;

import model.Line;
import model.LineSwapper;
import model.UnorderedSwap;

import java.util.LinkedHashMap;

public class SwappingMatrixReader {

    public static LineSwapper loadFile(String path) {
        SwappingMatrixReader reader = new SwappingMatrixReader(path);
        return reader.loadFile();
    }

    private String path;

    public SwappingMatrixReader(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public LineSwapper loadFile() {
        Integer[][] matrix = DataAsJSonFile.getFileContent(this.path, Integer[][].class);
        LineSwapper lineSwapper = new LineSwapper(getSwap2occurrenceCount(matrix), matrix.length);
        lineSwapper.setInputMatrix(matrix);
        return lineSwapper;
    }

    private LinkedHashMap<UnorderedSwap, Integer> getSwap2occurrenceCount(Integer[][] swapMatrix) {
        LinkedHashMap<UnorderedSwap, Integer> swap2occurrenceCount = new LinkedHashMap<>();
        //only read the upper half above the diagonal - use the absolute values that are read
        //ignore the entries on the diagonal
        for (int i = 0; i < swapMatrix.length; ++i) {
            for (int j = i + 1; j < swapMatrix[i].length; ++j) {
                swap2occurrenceCount.put(new UnorderedSwap(new Line(i), new Line(j)),
                        Math.abs(swapMatrix[i][j]));
            }
        }
        return swap2occurrenceCount;
    }
}

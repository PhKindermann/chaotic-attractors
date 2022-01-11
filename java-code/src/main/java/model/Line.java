package model;

/**
 * @author Johannes
 * @created 2018-10-27.
 */
public class Line {

    private int lineNumber;

    public Line(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Line line = (Line) o;

        return lineNumber == line.lineNumber;

    }

    @Override
    public int hashCode() {
        return lineNumber;
    }

    @Override
    public String toString() {
        return "" + lineNumber;
    }
}

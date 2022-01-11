package model;

/**
 * @author Johannes
 * @created 2018-10-27.
 *
 * Unordered Swaps always have the smaller line number as first element
 */
public class UnorderedSwap extends Swap {

    public UnorderedSwap(Line val0, Line val1) {
        super(val0.getLineNumber() <= val1.getLineNumber() ? val0 : val1,
                val0.getLineNumber() <= val1.getLineNumber() ? val1 : val0);
    }

    public Line getLineWithSmallerNumber() {
        return getValue0();
    }

    public Line getLineWithGreaterNumber() {
        return getValue1();
    }
}

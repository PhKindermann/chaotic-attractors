package model;

/**
 * @author Johannes
 * @created 2018-10-27.
 */
public class OrderedSwap extends Swap {

    public OrderedSwap(Line val0, Line val1) {
        super(val0, val1);
    }

    public OrderedSwap getInverse() {
        return new OrderedSwap(getValue1(), getValue0());
    }

    public UnorderedSwap getUnorderedSwap() {
        return new UnorderedSwap(getValue0(), getValue1());
    }
}

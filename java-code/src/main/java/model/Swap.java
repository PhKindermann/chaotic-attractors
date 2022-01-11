package model;

import org.javatuples.Tuple;
import org.javatuples.valueintf.IValue0;
import org.javatuples.valueintf.IValue1;

/**
 * @author Johannes
 * @created 2018-10-26.
 */
public abstract class Swap extends Tuple implements IValue0<Line>, IValue1<Line> {

    private final Line val0;
    private final Line val1;

    public Swap(Line val0, Line val1) {
        super(val0, val1);
        this.val0 = val0;
        this.val1 = val1;
    }

    @Override
    public int getSize() {
        return 2;
    }

    @Override
    public Line getValue0() {
        return val0;
    }

    @Override
    public Line getValue1() {
        return val1;
    }
}

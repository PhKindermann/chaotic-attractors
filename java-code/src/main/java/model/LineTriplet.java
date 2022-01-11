package model;

import org.javatuples.Tuple;
import org.javatuples.valueintf.IValue0;
import org.javatuples.valueintf.IValue1;
import org.javatuples.valueintf.IValue2;

import java.util.LinkedHashMap;

/**
 * @author Johannes
 * @created 2018-10-26.
 *
 * The elements will be sorted by line number
 */
public class LineTriplet extends Tuple implements IValue0<Line>, IValue1<Line>, IValue2<Line> {

    private final Line val0;
    private final Line val1;
    private final Line val2;

    private LinkedHashMap<UnorderedSwap, Integer> swaps;

    public LineTriplet(Line val0, Line val1, Line val2, int swaps01, int swaps02, int swaps12) {
        //logic horror because super-constructor-call must be first... -.-
        super(
                //smallest param
                val0.getLineNumber() <= val1.getLineNumber() && val0.getLineNumber() <= val2.getLineNumber() ? val0 :
                val1.getLineNumber() <= val2.getLineNumber() ? val1 : val2,
                //middle param
                (val0.getLineNumber() <= val1.getLineNumber() && val0.getLineNumber() > val2.getLineNumber()) ||
                (val0.getLineNumber() > val1.getLineNumber() && val0.getLineNumber() <= val2.getLineNumber()) ? val0 :
                (val1.getLineNumber() <= val0.getLineNumber() && val1.getLineNumber() > val2.getLineNumber()) ||
                (val1.getLineNumber() > val0.getLineNumber() && val1.getLineNumber() <= val2.getLineNumber()) ? val1 : val2,
                //greatest param
                val0.getLineNumber() > val1.getLineNumber() && val0.getLineNumber() > val2.getLineNumber() ? val0 :
                val1.getLineNumber() > val2.getLineNumber() ? val1 : val2
        );
        this.val0 = (Line) super.getValue(0);
        this.val1 = (Line) super.getValue(1);
        this.val2 = (Line) super.getValue(2);

        swaps = new LinkedHashMap<>();
        swaps.put(new UnorderedSwap(this.val0, this.val1), swaps01);
        swaps.put(new UnorderedSwap(this.val0, this.val2), swaps02);
        swaps.put(new UnorderedSwap(this.val1, this.val2), swaps12);
    }

    @Override
    public int getSize() {
        return 3;
    }

    @Override
    public Line getValue0() {
        return val0;
    }

    @Override
    public Line getValue1() {
        return val1;
    }

    @Override
    public Line getValue2() {
        return val2;
    }

    public int getNumberOfSwaps(UnorderedSwap swap) {
        return swaps.get(swap);
    }

    public int getNumberOf01Swaps() {
        return swaps.get(new UnorderedSwap(val0, val1));
    }

    public int getNumberOf02Swaps() {
        return swaps.get(new UnorderedSwap(val0, val2));
    }

    public int getNumberOf12Swaps() {
        return swaps.get(new UnorderedSwap(val1, val2));
    }
}

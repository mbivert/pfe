package entropy;

import choco.kernel.common.logging.ChocoLogging;
import choco.kernel.common.util.objects.BipartiteSet;

/**
 * Created with IntelliJ IDEA.
 * User: fhermeni
 * Date: 07/06/12
 * Time: 12:14
 * To change this template use File | Settings | File Templates.
 */
public class BenchBipartiteSet {

    public static void main(String[] args) {
        BipartiteSet<Integer> s = new BipartiteSet<Integer>();
        System.out.println(ChocoLogging.START_MESSAGE);
        for (int i = 0; i < 1000000; i++) {
            if (i % 2 == 0) {
                s.addLeft(new Integer(i));
            } else {
                s.addRight(new Integer(i));
            }
        }
        long st = 0;
        for (int a = 0; a < 100; a++) {
            st -= System.currentTimeMillis();
            for (int i = 0; i < 1000000; i++) {
                Integer x = new Integer(i);
                s.isLeft(x);
            }
            st += System.currentTimeMillis();
        }
        System.out.println(st + " ms");
    }
}

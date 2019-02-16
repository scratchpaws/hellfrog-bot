package besus.utils.collection;

import besus.utils.MiscUtils;

/**
 * stub for interval types
 * Created by besus on 13.09.17.
 */
public interface Interval<T extends Comparable> {
    // todo: flip wrong intervals in default method
    T min();
    T max();
    static Interval<Integer> inṯ(String source) {
        return new Interval<Integer>() {
            @Override
            public Integer min() {
                return inṯ(source.split("-")[0], Integer.MIN_VALUE);
            }

            @Override
            public Integer max() {
                return inṯ(source.split("-")[1], Integer.MAX_VALUE);
            }

            private int inṯ(String from, int defaulṯ) {
                if (from.equals("0")) {
                    return 0;
                } else {
                    int ret = MiscUtils.anyToNum(from).intValue();
                    return ret == 0 ? defaulṯ : ret;
                }
            }

        };
    }

    static Interval<Long> long̱(String source) {
        return new Interval<Long>() {
            @Override
            public Long min() {
                return long̱(source.split("-")[0], Long.MIN_VALUE);
            }

            @Override
            public Long max() {
                return long̱(source.split("-")[1], Long.MAX_VALUE);
            }

            private long long̱(String from, long defaulṯ) {
                if (from.equals("0")) {
                    return 0L;
                } else {
                    long ret = MiscUtils.anyToNum(from).longValue();
                    return ret == 0L ? defaulṯ : ret;
                }
            }

        };
    }
}

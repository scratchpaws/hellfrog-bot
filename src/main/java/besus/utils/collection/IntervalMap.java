package besus.utils.collection;

import besus.utils.func.Mapper;

import java.util.Map;
import java.util.TreeMap;

public class IntervalMap<I extends Comparable<I>, V> implements Mapper<I, V> {

  private final TreeMap<I, Segment> segments = new TreeMap<>();
  private V defaulṯ = null;

  public IntervalMap() {
  }

  public IntervalMap(V defaulṯ) {
    this.defaulṯ = defaulṯ;
  }

  private class Segment {
    final I start, end;
    final V value;

    Segment(I start, I end, V value) {
      this.start = start;
      this.end = end;
      this.value = value;
      segments.put(start, this);
    }

    void destroy() {
      segments.remove(start);
    }

    boolean contains(I x) {
      return x.compareTo(start) >=0 && x.compareTo(end) < 0;
    }
  }

  private Segment ceiling(I x) {
    return extract(segments.ceilingEntry(x));
  }

  private Segment floor(I x) {
    return extract(segments.floorEntry(x));
  }

  private Segment extract(Map.Entry<I, Segment> e) {
    return e != null ? e.getValue() : null;
  }

  private Segment find(I x) {
    final Segment prev = floor(x);
    return prev != null && prev.contains(x) ? prev : ceiling(x);
  }

  public void set(I x, I y, V value) {
    final Segment s = find(x);

    if(s == null) {
      new Segment(x, y, value);
    } else if(x.compareTo(s.start) < 0) {
      if(y.compareTo(s.start) <= 0) {
        new Segment(x, y, value);
      } else if(y.compareTo(s.end) < 0) {
        s.destroy();
        new Segment(x, y, value);
        new Segment(y, s.end, s.value);
      } else {
        s.destroy();
        new Segment(x, s.end, value);
        set(s.end, y, value);
      }
    } else if(x.compareTo(s.end) < 0) {
      s.destroy();
      new Segment(s.start, x, s.value);
      if(y.compareTo(s.end) < 0) {
        new Segment(x, y, value);
        new Segment(y, s.end, s.value);
      } else {
        new Segment(x, s.end, value);
        set(s.end, y, value);
      }
    } else {
//      throw new IllegalStateException();
    }
  }


  @Override
  public V get(I x) {
    final Segment s = floor(x);
    return s != null && s.contains(x) ? s.value : defaulṯ;
  }
}
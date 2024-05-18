package org.ontime.ontime;

import java.util.Comparator;
import java.util.Map;

public class ValueComparator implements Comparator {
   Map base;

   public ValueComparator(Map base) {
      this.base = base;
   }

   public int compare(Object a, Object b) {
      if ((Long)this.base.get(a) < (Long)this.base.get(b)) {
         return 1;
      } else {
         return (Long)this.base.get(a) == (Long)this.base.get(b) ? 0 : -1;
      }
   }
}

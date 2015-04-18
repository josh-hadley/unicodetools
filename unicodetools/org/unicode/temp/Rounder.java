package org.unicode.temp;
import com.ibm.icu.lang.CharSequences;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;

public class Rounder {
  public StringBuilder round(double lowPositive, double highPositive) {
    lowPositive = Math.nextUp(lowPositive);
    highPositive = Math.nextDown(highPositive);
    StringBuilder digits = new StringBuilder();
    int count = 1;
    while (lowPositive >= 10d || highPositive >= 10d) {
      lowPositive /= 10d;
      highPositive /= 10d;
      ++count;
    }
    while (true) {
      if (count == 0) {
        digits.append('.');
      }
      --count;
      double lowDigit = Math.floor(lowPositive);
      double highDigit = Math.floor(highPositive);
      if (lowDigit != highDigit) {
        // for the last digit, we take our best shot
        // if we don't do this last digit, we don't have enough digits to get within the bounds of low/high
        double median = Math.floor(Math.round((lowPositive + highPositive)/2));
        digits.append((char)('0' + (int)median));
        break;
      }
      digits.append((char)('0' + (int)lowDigit));
      lowPositive -= lowDigit;
      lowPositive *= 10d;
      highPositive -= highDigit;
      highPositive *= 10d;
    }
    for (int i = 0; i < count; ++i) {
      digits.append('0');
    }
    return digits;
  }

  public enum Position {
    DEGREES("°", 1), 
    MINUTES("′", 60), 
    SECONDS("″", 60*60);
    public final String symbol;
    public final double divisor;
    private Position(String symbol, int divisor) {
      this.symbol = symbol;
      this.divisor = divisor;
    }
    public static Position fromSymbol(char ch) {
      for (Position p : Position.values()) {
        if (p.symbol.charAt(0) == ch) {
          return p;
        }
      }
      return null;
    }
  }

  public StringBuilder round60(double lowPositive, double highPositive) {
    lowPositive = Math.nextUp(lowPositive);
    highPositive = Math.nextDown(highPositive);
    Position position = Position.DEGREES;
    StringBuilder digits = new StringBuilder();
    int count = 1;
    while (lowPositive >= 10d || highPositive >= 10d) {
      lowPositive /= 10d;
      highPositive /= 10d;
      ++count;
    }
    while (true) {
      if (count == 0) {
        boolean isSeconds = position == Position.SECONDS;
        digits.append(isSeconds ? "." : position.symbol);
        if (!isSeconds) {
          position = Position.values()[position.ordinal()+1];
          lowPositive *= 60;
          highPositive *= 60;
          while (lowPositive >= 10d || highPositive >= 10d) {
            lowPositive /= 10d;
            highPositive /= 10d;
            ++count;
          }
        }
      }
      --count;
      double lowDigit = Math.floor(lowPositive);
      double highDigit = Math.floor(highPositive);
      if (lowDigit != highDigit) {
        // for the last digit, we take our best shot
        // if we don't do this last digit, we don't have enough digits to get within the bounds of low/high
        double median = Math.floor(Math.round((lowPositive + highPositive)/2));
        digits.append((char)('0' + (int)median));
        break;
      }
      digits.append((char)('0' + (int)lowDigit));
      lowPositive -= lowDigit;
      lowPositive *= 10d;
      highPositive -= highDigit;
      highPositive *= 10d;
    }
    for (int i = 0; i < count; ++i) {
      digits.append('0');
    }
    digits.append(position.symbol);
    return digits;
  }


  static double[] parseLowHigh(CharSequence ss) {
    String s = ss.toString();
    double base = Double.parseDouble(s);
    double low;
    double high;
    int pos = s.indexOf('.');
    if (pos >= 0) {
      double offset = Math.pow(10d, -(double)(s.length()-pos-1));
      low = base - offset/2;
      high = base + offset/2;
    } else {
      low = base - 0.5;
      high = base + 0.5;
    }
    double[] result = {low, high};
    return result;
  }

  static double[] parseLowHigh60(CharSequence s) {
    double value = 0;
    int start = 0;
    int lastIndex = s.length()-1;
    for (int i = 0; i <= lastIndex; ++i) {
      char ch = s.charAt(i);
      if ('0' <= ch && ch <= '9' || ch == '.') {
        continue;
      }
      Position position = Position.fromSymbol(ch);
      CharSequence toParse = s.subSequence(start, i);
      start = i+1;
      if (i != lastIndex) { // not final field
        int item = Integer.parseInt(toParse.toString());
        value += item/position.divisor;
      } else {
        double[] result = parseLowHigh(toParse);
        result[0] = result[0]/position.divisor + value;
        result[1] = result[1]/position.divisor + value;
        return result;
      }
    }
    throw new IllegalArgumentException("Bad degree format for " + s);
  }

  static final DecimalFormat nf = (DecimalFormat) NumberFormat.getInstance(ULocale.ENGLISH);
  static final DecimalFormat nfPlusMinus = (DecimalFormat) NumberFormat.getInstance(ULocale.ENGLISH);
  static {
    nf.setMinimumSignificantDigits(1);
    nf.setMaximumSignificantDigits(9);
    nfPlusMinus.setMinimumSignificantDigits(1);
    nfPlusMinus.setMaximumSignificantDigits(3);
  }

  public static void main(String[] args) {
    tryRounder();
    tryRounder60();
  }


  private static void tryRounder60() {
    System.out.println("\nRounding when convering double degrees to degrees-minutes-seconds\nSource\trange\tdegrees decimal\t rounded & converted \tinterpreted\t rounded & converted \tSame?");
    String[] tests = {
        "12", "12.3", "12.30", "12.34", "12.340", "12.345", "12.3456", "12.34567", "12.3456789", "12.34567890"
    };
    Rounder rounder = new Rounder();
    for (String test : tests) {
      double[] start = parseLowHigh(test);

      double low = start[0];
      double high = start[1];
      StringBuilder rounded = rounder.round60(low, high);
      double[] back = rounder.parseLowHigh60(rounded);
      StringBuilder rounded3 = rounder.round(back[0], back[1]);

      System.out.println(
          '“' + test + '”'
          + "\t" + showPlusMinus(start)
          + "\t" + test + "°"
          + "\t" + rounded
          + "\t" + showPlusMinus(back) + "°"
          + "\t" + rounded3 + "°"
          + (test.equals(rounded3.toString()) ? "" : "\tNO")
          );
    }
  }


  private static void tryRounder() {
    System.out.println("Rounding when convering centimeter values to inches\nSource\tcm interpreted\tcm\tmapped to inches\t rounded & converted \tinches interpreted\tmapped to cm\t rounded & converted \tSame?\t");
    String[] tests = {
        "0.49", "0.5", "0.50", "0.51", "0.510", "0.52",
        "4.9", "5", "5.0", "5.1", "5.10", "5.2",
        "49", "50", "51", "51.0", "52"
    };
    Rounder rounder = new Rounder();
    for (String test : tests) {
      double[] start = parseLowHigh(test);

      double low = start[0];
      double high = start[1];
      StringBuilder rounded = rounder.round(low, high);
      double[] start2 = {low/2.54d, high/2.54d};
      StringBuilder rounded2 = rounder.round(start2[0], start2[1]);
      double[] back = parseLowHigh(rounded2);
      double[] back2 = {back[0]*2.54, back[1]*2.54};
      StringBuilder rounded3 = rounder.round(back2[0], back2[1]);
      System.out.println('“' + test + '”'
          + "\t" + showPlusMinus(start)
          + "\t" + rounded + " cm"
          + "\t" + showPlusMinus(start2) + " in"
          + "\t" + rounded2 + " in"
          + "\t" + showPlusMinus(back) + " in"
          + "\t" + showPlusMinus(back2) + " cm"
          + "\t" + rounded3 + " cm"
          + (rounded.toString().equals(rounded3.toString()) ? "" : "\tNO")
          );
    }
  }

  private static String showPlusMinus(double[] start) {
    double average = (start[0] + start[1])/2;
    return nf.format(average) + " ± " + nfPlusMinus.format(start[1]-average);
  }
}
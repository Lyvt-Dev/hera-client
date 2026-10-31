package gg.hera.ui;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
class TextFitTest {
 private static int width(String s){return s.codePointCount(0,s.length());}
 @Test void respectsFontBudgetAndNeverSplitsSupplementaryCharacters(){
  assertEquals("abc",TextFit.fit("abc",3,TextFitTest::width));
  assertEquals("a😀…",TextFit.fit("a😀bc",3,TextFitTest::width));
  assertEquals("…",TextFit.fit("long",1,TextFitTest::width));
  assertEquals("",TextFit.fit("long",0,TextFitTest::width));
  assertEquals("a b c",TextFit.fit("a\nb\tc",20,TextFitTest::width));
 }
 @Test void hugeServerLabelsHaveBoundedMeasurementWork(){
  var calls=new AtomicInteger();String result=TextFit.fit("x".repeat(200_000),40,s->{calls.incrementAndGet();assertTrue(s.length()<=4097);return width(s);});
  assertEquals(40,width(result));assertTrue(result.endsWith("…"));assertTrue(calls.get()<=15);
 }
 @Test void usesSuppliedFontAndHandlesTinyBudget(){assertEquals("ab…",TextFit.fit("abcdef",6,s->width(s)*2));assertEquals("",TextFit.fit("x",1,s->width(s)*2));}
}

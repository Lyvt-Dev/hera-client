package gg.hera.ui;
import java.util.function.ToIntFunction;
/** Bounded, codepoint-safe label fitting; font measurement supplied by the caller. */
public final class TextFit {
 private TextFit(){}
 public static String fit(String input,int width,ToIntFunction<String> measure){
  if(input==null||input.isEmpty()||width<=0)return "";
  int count=input.codePointCount(0,input.length()),limit=Math.min(count,4096);
  String value=input.substring(0,input.offsetByCodePoints(0,limit)).replace('\n',' ').replace('\r',' ').replace('\t',' ');
  if(count<=limit&&measure.applyAsInt(value)<=width)return value;
  String suffix="…";if(measure.applyAsInt(suffix)>width)return "";
  int low=0,high=limit;
  while(low<high){int mid=(low+high+1)>>>1;String candidate=value.substring(0,value.offsetByCodePoints(0,mid))+suffix;if(measure.applyAsInt(candidate)<=width)low=mid;else high=mid-1;}
  return value.substring(0,value.offsetByCodePoints(0,low))+suffix;
 }
}

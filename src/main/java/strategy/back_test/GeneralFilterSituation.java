package strategy.back_test;

import org.json.JSONObject;
import strategy.OneTrade;
import strategy.situations.RuleSituation;

import java.util.HashSet;
import java.util.Set;

public class GeneralFilterSituation {

      private Set<Integer> rangeDates; // String rangeDates диапазон дат: all -все, 202605 - конкретный месяц, 2026 - год, 202605-202607 - месяцы
     // 2026-2027 - годы
     // если не all, то переводит в сет месяцев

      //private final int toInt; // параметр если надо десятичные перевести в целые числа, если 0 уже целое
      private final RuleSituation ruleSituation;
      private int currentDay;


    public GeneralFilterSituation(JSONObject parameters, RuleSituation ruleSituation){
        setRangeDates(parameters.getString("rangeDates"));

        //rangeDates.forEach(System.out::println);
        //this.toInt = parameters.getInt("toInt");
        this.ruleSituation = ruleSituation;
    }


      public GeneralFilterSituation(String rangeDates, int toInt, RuleSituation ruleSituation){
          setRangeDates(rangeDates);
          //this.toInt = toInt;
          this.ruleSituation = ruleSituation;
      }


      private void setRangeDates(String range){
          if(!range.endsWith("all")){
              rangeDates = new HashSet<>();
              // год 2026
              if(range.length()==4){
                  for (int i=1; i<13; i++){
                      String m = i+"";
                      if(i<10)m = "0"+i;
                      rangeDates.add(Integer.parseInt(range+m));
                  }
              }// 202605 - конкретный месяц
              else if(range.length()==6){
                  rangeDates.add(Integer.parseInt(range));
              }// 2026-2027 - годы
              else if(range.length()==9){
                  String[] rangesM = range.split("-");
                  for (int i=Integer.parseInt(rangesM[0]); i<=Integer.parseInt(rangesM[1]); i++){
                      for (int a=1; a<13; a++){
                          String m = a+"";
                          if(a<10)m = "0"+a;
                          rangeDates.add(Integer.parseInt(i+m));
                      }
                  }
              }// 202605-202607 - месяцы
             else {
                 String[] rangesM = range.split("-");
                  for (int i=Integer.parseInt(rangesM[0]); i<=Integer.parseInt(rangesM[1]); i++){
                      rangeDates.add(i);
                  }
              }
          }
      }
     // tick строка котировки, isStop если список закончился команда на завершение, надо закрыть сделку если она есть
     public final OneTrade getOneTrade(int date,int time, int price,boolean isStop){
         //String[] date = tick.split(",");// 0 - date 20260501, 1 - time 082708, 2 - price 2264.05 || 226405
         // пока здесь фильтр на большие периоды котировок, если завершение списка все равно проходит
         if(!isStop) if(!isAllowPeriod(date))return null;
         //int price = tick[2];
        // if(toInt!=0)price =(int) (Double.parseDouble(tick[2]) * toInt); else price = Integer.parseInt(tick[2]);
         boolean isStartDay = false;
         if(date!=currentDay){
             currentDay = date;
             isStartDay = true;
         }
         if(isStop)isStartDay = true;
         return ruleSituation.trade(isStartDay,price,date,time);
     }

     private boolean isAllowPeriod(int date){
         if(rangeDates==null)return true;
         return rangeDates.contains(date/100);
     }



    public static void main(String[] args) {
        //setRangeDates("2026-2027");
        //rangeDates.stream().sorted().forEach(System.out::println);
    }

}

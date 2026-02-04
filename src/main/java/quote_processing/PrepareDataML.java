package quote_processing;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static quote_processing.FixQuote.*;

public class PrepareDataML {


    

    public static void startPrepare(String startDay,String endDay){


        JSONObject partDaysMin5Sec = getDayMinCandles(startDay,endDay);
        JSONArray partDays = alignmentHoursInDay(getArrayDays(startDay,endDay,partDaysMin5Sec));





        System.out.println(partDays.getJSONObject(10));
        //System.out.println(partDaysMin5Sec.getJSONArray("20120119"));
        System.out.println("partDays size "+partDays.length());
        JSONArray cutPeriods = cutPeriodDays(partDays,7);
        System.out.println("cutPeriods size "+cutPeriods.length());
        //System.out.println(cutPeriods.getJSONArray(0));
        System.out.println("++++++++++++++++++++++++");
        //System.out.println(cutPeriods.getJSONArray(1));
        List<JSONObject> res = cutFinalMin5Sec(cutPeriods,7,partDaysMin5Sec);
        //System.out.println("RES "+res.get(0));
    }
    // [
    // [sizeDays elements { day}]...
    // ]
    static JSONArray cutPeriodDays(JSONArray partDays,int sizeDays){
        JSONArray result = new JSONArray();
        for(int i=0; i< partDays.length()-sizeDays; i++){
                  JSONArray cutArray = new JSONArray();
                  for (int s=0; s<sizeDays; s++){
                      cutArray.put(partDays.getJSONObject(i+s));
                  }
                  result.put(cutArray);
        }
        return result;
    }
    // ДЕНЬ
    // [
    // {
    // day:dayDate,
    // hours:[ 10 elements [ candle hour 4 elements ] ],
    // candleDay: [ candle day 4 elements + 1 volume ]
    // },...
    // ]


    // МИНУТЫ
    // {day: [
    // {
    // minuteDate:time,
    //candleMinute:Integer[4],
    // volumeMinute: int
    // 5secData:[12 elements [(candle 4 elements),volume] ]
    // }
    // ]


    // РЕЗУЛЬТАТ
    // [
    //
    // [ sizeDays-2x(candelDayX4), 10x(candleHourX4), 120x(candleMinX4,volume), 12x(candle5SecsX4,volume)]
    // ]

    static List<JSONObject> cutFinalMin5Sec(JSONArray periodsDays,int sizeDays,JSONObject partDaysMin5Sec){
        int sizeFirstDaysCandle = sizeDays-2;
        int sizeDayHoursCandle = 10;
        int sizeMinutesCandle = 120;
        int size5SecCandle = 12;
        //int[] result = new int[(sizeFirstDaysCandle*4+sizeDayHoursCandle*4)*(sizeMinutesCandle*5)*(size5SecCandle*5)];
        List<JSONObject> result = new ArrayList<>(100000000);
        List<int[][]> resultTest = new ArrayList<>(100000000);
        for(int i=0; i< 1; i++){
            JSONArray periodDay = periodsDays.getJSONArray(i);
            int[] firstDaysCandlesAndDayHours = new int[sizeFirstDaysCandle*4+sizeDayHoursCandle*4];

            JSONObject dayHours = periodDay.getJSONObject(sizeFirstDaysCandle);
            JSONArray candlesHours = dayHours.getJSONArray("hours");
            int closeBeforeDay =((Integer[])candlesHours.get(candlesHours.length()-1))[3];

            JSONObject lastDay = periodDay.getJSONObject(sizeFirstDaysCandle+1);
            JSONArray  minutesLastDay = partDaysMin5Sec.getJSONArray(lastDay.getString("day"));
            int volumeLastDay =  ((Integer[])lastDay.get("candleDay"))[4];
            System.out.println("volumeLastDay "+volumeLastDay);

            for (int d=0; d<sizeFirstDaysCandle; d++){
                JSONObject day = periodDay.getJSONObject(d);
                Integer[] candleDay = (Integer[]) day.get("candleDay");
                for(int cD=0; cD<4; cD++) firstDaysCandlesAndDayHours[cD+(d*4)] = candleDay[cD]-closeBeforeDay;
            }
            System.out.println("size firstDaysCandlesAndDayHours "+firstDaysCandlesAndDayHours.length);

            for (int h=0; h<sizeDayHoursCandle; h++){
                Integer[] candleHour = (Integer[]) candlesHours.get(h);
                for(int cD=0; cD<4; cD++) firstDaysCandlesAndDayHours[(sizeFirstDaysCandle*4)+cD+(h*4)] = candleHour[cD]-closeBeforeDay;
            }


            for(int m=10; m<sizeMinutesCandle; m++){
                int[] minutes = new int[sizeMinutesCandle*5];
                for (int m1=0; m1<m; m1++){
                    JSONObject minute = minutesLastDay.getJSONObject(m1);
                    Integer[] candleMinute = (Integer[]) minute.get("candleMinute");
                    for(int cD=0; cD<4; cD++) minutes[cD+m1*5] = candleMinute[cD]-closeBeforeDay;
                    minutes[4+m1*5] = minute.getInt("volumeMinute")/volumeLastDay;
                }
                /*for(int l=0; l<75; l++) System.out.print(","+minutes[l]);
                System.out.println("+++++++++++++++++++++++++++++++++++");*/
                int[] sec5 = new int[size5SecCandle*5];
                JSONObject minute = minutesLastDay.getJSONObject(m);
                JSONArray sec5Minute = minute.getJSONArray("5secData");
                if(m==10) System.out.println("day "+lastDay.getString("day")+","+minute.getString("minuteDate")+" closeBeforeDay "+closeBeforeDay);
                for (int s=0; s<size5SecCandle; s++){
                    Integer[] candle5sec = (Integer[]) sec5Minute.get(s);
                    //if(m==10) System.out.println("candle5sec "+Arrays.toString(candle5sec));
                    for(int cD=0; cD<4; cD++) {sec5[cD+s*5] = candle5sec[cD] - closeBeforeDay;}
                    sec5[4+s*5] = candle5sec[4]/volumeLastDay; // объем отдельно, как в минутах

                    int[][] dataML = new int[2][];
                    dataML[0] = new int[720];
                    dataML[1] = new int[10];

                    /*for(int add=0; add<firstDaysCandlesAndDayHours.length; add++)dataML[0][add] = firstDaysCandlesAndDayHours[add];
                    for(int add=0; add<minutes.length; add++)dataML[0][add+firstDaysCandlesAndDayHours.length] = minutes[add];
                    for(int add=0; add<sec5.length; add++)dataML[0][add+firstDaysCandlesAndDayHours.length+minutes.length] = sec5[add];*/


                    System.arraycopy(firstDaysCandlesAndDayHours, 0, dataML[0], 0, firstDaysCandlesAndDayHours.length);
                    System.arraycopy(minutes, 0, dataML[0], firstDaysCandlesAndDayHours.length, minutes.length);
                    System.arraycopy(sec5, 0, dataML[0], firstDaysCandlesAndDayHours.length+minutes.length, sec5.length);
                    resultTest.add(dataML);
                    /*JSONObject data = new JSONObject();
                    data.put("dataML",dataML);
                    data.put("minutesLastDay",minutesLastDay);//для расчета стоп лоссов, объемов торгов
                    data.put("closeBeforeDay",closeBeforeDay);
                    result.add(data);*/
                    /*for (int firstDaysCandlesAndDayHour : firstDaysCandlesAndDayHours) resultTest.add(firstDaysCandlesAndDayHour);
                    for (int j : minutes) resultTest.add(j);
                    for (int j : sec5) resultTest.add(j);
                    if(m==10) System.out.println("sec5 "+Arrays.toString(sec5));*/
                }
                /*for (int firstDaysCandlesAndDayHour : firstDaysCandlesAndDayHours) result.add(firstDaysCandlesAndDayHour);
                int b=0;
                for (int j : minutes) {
                    b++;
                    result.add(j);
                }
                if(m==10) System.out.println("b "+b);*/
            }
        }
        System.out.println("result.size "+resultTest.size());
        return result;
    }
    public static void main(String[] args) {
        startPrepare("20120105","20121101");
    }

}

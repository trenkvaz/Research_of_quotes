package quote_processing;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static utilities.ReadWrite.readListStr;

public class FixQuote {



    // {  Объем нужен для вычисления от дневного объема относительного объема в минутах и 5секундах
    // dayDate1:candle[4+1 volume], dayDate2:candle[4+1 volume] ...
    // }
    static JSONObject getDayCandle(String startDay,String endDay,JSONObject getDayMinCandlesFilterExist){
        JSONObject result = new JSONObject();
        List<String> days = readListStr("J:\\static_data_2\\PythonProjects\\MIX_2011_2025_days.txt");
        assert days != null;
        boolean isStart = false;
        Set<String> keyDayMinutes = null;
        if(getDayMinCandlesFilterExist!=null)keyDayMinutes = getDayMinCandlesFilterExist.keySet();


        for(String day:days){
            String[] elementsDay = day.split(",");
            if(!isStart){
                if(elementsDay[0].equals(startDay)||startDay.isEmpty()){isStart = true;} else continue;
            }
            if(elementsDay[0].equals(endDay))break;
            // фильтр на наличие дней с минутами, которых может не быть если минут мало в дне
            if(keyDayMinutes!=null)
            if(!keyDayMinutes.contains(elementsDay[0]))continue;

            Integer[] candleDay = new Integer[5];
            for(int e=0; e<5; e++)candleDay[e]= Integer.parseInt(elementsDay[e+2]);
            result.put(elementsDay[0],candleDay);
            //System.out.println(Arrays.toString(candleDay));

        }
        System.out.println("AMOUNT DAYS "+result.keySet().size());
        return result;
    }

    static JSONObject getTickes(String startDay,String endDay){
        JSONObject result = new JSONObject();
        List<String> tickes = readListStr("J:\\static_data_2\\PythonProjects\\MIX_2011_2025_tickes.txt");
        assert tickes != null;
        String currentKey = "";
        JSONArray jsonArrayTickesInMinute = null;
        int c = 0;
        int maxTickers = 0;
        String keyMaxTickers = "";
        boolean isStart = false;
        for(String tick:tickes){
            c++;
            //if(c==100)break;
            String[] elementsTick = tick.split(",");
            //String dayMinKey = elementsTick[0]+elementsTick[1].substring(0,4);
            String dayMinKey = elementsTick[0];

            if(!isStart){
                if(dayMinKey.equals(startDay)||startDay.isEmpty()){isStart = true;} else continue;
            }
            if(dayMinKey.equals(endDay))break;


            if(!currentKey.equals(dayMinKey)){
                if(jsonArrayTickesInMinute!=null){
                    //System.out.println(currentKey+" "+jsonArrayTickesInMinute);
                    if(jsonArrayTickesInMinute.length()>maxTickers){
                        maxTickers = jsonArrayTickesInMinute.length();
                        keyMaxTickers = currentKey;
                    }
                    result.put(currentKey,jsonArrayTickesInMinute);
                }
                currentKey = dayMinKey;
                jsonArrayTickesInMinute = new JSONArray();
            }

            Integer[] dataTick = new Integer[3];
            for(int e=0; e<3; e++){
                String el = elementsTick[e+2];
                if(el.equals("Buy"))dataTick[e] = 1;
                else if(el.equals("Sell"))dataTick[e] = -1;
                else dataTick[e]= Integer.parseInt(el);
            }
            jsonArrayTickesInMinute.put(dataTick);
            //System.out.println(Arrays.toString(candleDay));
        }
        System.out.println("maxTickers "+maxTickers+" keyMaxTickers "+keyMaxTickers);
        return result;
    }


    // {
    // dayDate+minuteDate: [12 elements [candle4,volume] ]
    // }
    static JSONObject get5secCandle(String startDay,String endDay){
        JSONObject result = new JSONObject();
        List<String> seconds5 = readListStr("J:\\static_data_2\\PythonProjects\\MIX_2011_2025_5sec.txt");
        assert seconds5 != null;
        String currentKey = "";
        JSONArray jsonArray5secInMinute = null;
        JSONObject jsonObject5secInMinute = null;
        int c = 0;
        int maxTickers = 0;
        String keyMaxTickers = "";
        Integer[] beforeData5sec = new Integer[] {0,0,0,0,0};
        boolean isStart = false;
        for(String sec5:seconds5){
            c++;
            //if(c==100)break;
            String[] elementsSec5 = sec5.split(",");
            if(!isStart){
                if(elementsSec5[0].equals(startDay)||startDay.isEmpty()){isStart = true;} else continue;
            }
            if(elementsSec5[0].equals(endDay))break;
            String dayMinKey = elementsSec5[0]+elementsSec5[1].substring(0,4);


            if(!currentKey.equals(dayMinKey)){
                boolean isLog = false;
                //if(currentKey.equals("201201131012"))isLog = true;

                if(jsonObject5secInMinute!=null){
                    //System.out.println(currentKey+" "+jsonArray5secInMinute);
                    if(jsonArray5secInMinute.length()>maxTickers){
                        maxTickers = jsonArray5secInMinute.length();
                        keyMaxTickers = currentKey;
                    }
                    //if(beforeData5sec!=null){
                    //System.out.println(currentKey+"   "+jsonObject5secInMinute);
                        for(int i=0; i<60; i+=5){
                           if(jsonObject5secInMinute.has(String.valueOf(i))){
                               beforeData5sec = (Integer[]) jsonObject5secInMinute.get(String.valueOf(i));
                               Integer[] input = beforeData5sec.clone();
                               jsonArray5secInMinute.put(input);
                               if(isLog)System.out.println(i+" "+ Arrays.toString(beforeData5sec));
                           }
                           else {
                               beforeData5sec[4] = 0;
                               jsonArray5secInMinute.put(beforeData5sec);
                               if(isLog)System.out.println("empty "+i+" "+ Arrays.toString(beforeData5sec));
                           }
                            //System.out.println(elementsSec5[1].substring(0,4)+i+" "+ Arrays.toString(beforeData5sec));
                        }
                    //System.out.println(jsonArray5secInMinute.length());
                   // }



                    result.put(currentKey,jsonArray5secInMinute);
                    beforeData5sec = (Integer[]) jsonArray5secInMinute.get(jsonArray5secInMinute.length()-1);
                }
                currentKey = dayMinKey;
                jsonArray5secInMinute = new JSONArray();
                jsonObject5secInMinute = new JSONObject();
            }

            //if(beforeData5sec!=null)

            Integer[] data5sec = new Integer[5];
            for(int e=0; e<5; e++){data5sec[e]= Integer.parseInt(elementsSec5[e+2]);}
           // if(elementsSec5[1].equals("101000")) System.out.println((Integer.parseInt(elementsSec5[1])%100));
            jsonObject5secInMinute.put(String.valueOf(Integer.parseInt(elementsSec5[1])%100),data5sec);
            //System.out.println(Arrays.toString(candleDay));
        }
        //System.out.println("maxTickers "+maxTickers+" keyMaxTickers "+keyMaxTickers);
        return result;
    }

    //{day: [
    // {
    // minuteDate:time,
    //candleMinute:Integer[4],
    // volumeMinute: int
    // 5secData:[12 elements [(candle 4 elements),volume] ]
    // }
    // ]
    static JSONObject getDayMinCandles(String startDay,String endDay){
        int MIN_AMOUNT_MINUTES_IN_DAY = 240;
        JSONObject result = new JSONObject();
        List<String> minutes = readListStr("J:\\static_data_2\\PythonProjects\\MIX_2011_2025_minutes.txt");
        JSONObject sec5 = get5secCandle(startDay,endDay);
        assert minutes != null;
        String currentDay = "";
        JSONArray jsonArrayMinutesInDay = null;
        JSONObject jsonObjectMinutes = null;
        int c = 0;
        boolean isStart = false;
        for(String minute:minutes){
            String[] elementsMinute = minute.split(",");
            if(!isStart){
                if(elementsMinute[0].equals(startDay)||startDay.isEmpty()){isStart = true;} else continue;
            }
            if(elementsMinute[0].equals(endDay))break;

            if(!currentDay.equals(elementsMinute[0])){

                    if(jsonArrayMinutesInDay!=null){
                        //System.out.println("count minutes in day "+currentDay+"     "+jsonArrayMinutesInDay);
                        //jsonObjectMinutes.put()
                        if(jsonArrayMinutesInDay.length()>=MIN_AMOUNT_MINUTES_IN_DAY) result.put(currentDay,jsonArrayMinutesInDay);
                    }

                    currentDay = elementsMinute[0];
                    jsonArrayMinutesInDay = new JSONArray();
                    //jsonObjectMinutes = new JSONObject();
                }
            assert jsonArrayMinutesInDay != null;
            Integer[] candleMinute = new Integer[4];
            jsonObjectMinutes = new JSONObject();
            for(int e=0; e<4; e++)candleMinute[e]= Integer.parseInt(elementsMinute[e+2]);
            //System.out.println(Arrays.toString(candleMinute));
            jsonObjectMinutes.put("minuteDate",elementsMinute[1]);
            jsonObjectMinutes.put("candleMinute",candleMinute);
            jsonObjectMinutes.put("volumeMinute",Integer.parseInt(elementsMinute[6]));
            String dayMinKey = elementsMinute[0]+elementsMinute[1].substring(0,4);

            if(sec5.has(dayMinKey)) {
                JSONArray jsonArrayTickes = sec5.getJSONArray(dayMinKey);
                //System.out.println(dayMinKey+"      "+jsonArrayTickes);
                jsonObjectMinutes.put("5secData",jsonArrayTickes);

            } else System.out.println("NO 5sec dayMinKey "+dayMinKey);
            jsonArrayMinutesInDay.put(jsonObjectMinutes);
            }

        System.out.println("AMOUNT DAYS WITH MINUTES "+result.keySet().size());
        return  result;
    }


    // [
    // {
    // day:dayDate,
    // hours:[ 10 elements [ candle hour 4 elements ] ],
    // candleDay: [ candle day 4 elements + 1 volume ]
    // },...
    // ]
    // Создание массива дней с добавлением часов
    static JSONArray getArrayDays(String startDay,String endDay,JSONObject getDayMinCandlesFilterExist){
        List<String> hours = readListStr("J:\\static_data_2\\PythonProjects\\MIX_2011_2025_hours.txt");
        JSONObject days = getDayCandle(startDay,endDay,getDayMinCandlesFilterExist);
        assert hours != null;
        String currentDay = "";
        JSONArray jsonArrayDays = new JSONArray();
        JSONObject jsonObjectDay = null;
        JSONArray jsonArrayHoursInDay = null;
        Set<String> setDays = days.keySet();
        int c = 0;
        boolean isStart = false;
        for(String hour:hours){
            String[] elementsHour = hour.split(",");
            if(!isStart){
                if(elementsHour[0].equals(startDay)){isStart = true;} else continue;
            }
            if(elementsHour[0].equals(endDay))break;
            if(!currentDay.equals(elementsHour[0])){
                // поместить накопившийся день в массив.
                if(jsonObjectDay!=null&&setDays.contains(currentDay)){
                    c+=jsonArrayHoursInDay.length();
                    //System.out.println("count hours in day "+currentDay+"     "+jsonArrayHoursInDay.length());
                    jsonObjectDay.put("day",currentDay);
                    jsonObjectDay.put("hours",jsonArrayHoursInDay);
                    Integer[] candleDay = (Integer[]) days.get(currentDay);
                    if(candleDay!=null) {
                        //System.out.println(currentDay+" candleDay "+Arrays.toString(candleDay));
                        jsonObjectDay.put("candleDay",candleDay);
                    }
                    else System.out.println(currentDay+" candleDay null ");

                    jsonArrayDays.put(jsonObjectDay);
                }
                currentDay = elementsHour[0];
                jsonObjectDay = new JSONObject();
                jsonArrayHoursInDay = new JSONArray();

            }

            assert jsonArrayHoursInDay != null;
            Integer[] candleHour = new Integer[4];
            for(int e=0; e<4; e++)candleHour[e]= Integer.parseInt(elementsHour[e+2]);
            //System.out.println(Arrays.toString(candleHour));
            jsonArrayHoursInDay.put(candleHour);
        }
        //System.out.println("middle hours "+(c/jsonArrayDays.length()));
        return jsonArrayDays;
    }

    // выравнивание количество часов в днях, чтобы было одинаковое количество
    static JSONArray alignmentHoursInDay(JSONArray jsonArrayDays){
        for(int i=0; i<jsonArrayDays.length(); i++){
          JSONObject jsonObjectDay = jsonArrayDays.getJSONObject(i);
          JSONArray  jsonArrayHoursInDay = jsonObjectDay.getJSONArray("hours");
          if(jsonArrayHoursInDay.length()==10)continue;
          if(jsonArrayHoursInDay.length()<10){
              int add = 10-jsonArrayHoursInDay.length();
              Integer[] lastCandle = (Integer[]) jsonArrayHoursInDay.get(jsonArrayHoursInDay.length()-1);
              for(int a=0; a<add; a++){
                   jsonArrayHoursInDay.put(lastCandle);
              }

          } else {
              // новый список для укорачивания количества часов
              JSONArray  jsonArrayHoursInDayNEW = new JSONArray();
              int over = (jsonArrayHoursInDay.length()-10)*2;
              // заполняется часами, которые не надо удваивать
              for(int a=0; a<jsonArrayHoursInDay.length()-over; a++){
                  jsonArrayHoursInDayNEW.put(jsonArrayHoursInDay.get(a));
              }
              // удвоенные часы
              //System.out.println("============================= start "+(jsonArrayHoursInDay.length()-over)+" size "+jsonArrayHoursInDay.length());
              for(int a=jsonArrayHoursInDay.length()-over; a<jsonArrayHoursInDay.length(); a+=2){
                  //System.out.println("++++++++++++++++++++++++++++++++++++++++++");
                  Integer[] candle1 = (Integer[]) jsonArrayHoursInDay.get(a);
                  Integer[] candle2 = (Integer[]) jsonArrayHoursInDay.get(a+1);
                  Integer[] candleX2 = new Integer[4];
                  candleX2[0] = candle1[0];
                  candleX2[1] = Math.max(candle1[1],candle2[1]);
                  candleX2[2] = Math.min(candle1[2],candle2[2]);
                  candleX2[3] = candle2[3];
                  jsonArrayHoursInDayNEW.put(candleX2);
                  /*System.out.println("candle1 "+Arrays.toString(candle1));
                  System.out.println("candle2 "+Arrays.toString(candle2));
                  System.out.println("candleX2 "+Arrays.toString(candleX2));*/
              }

              jsonObjectDay.put("hours",jsonArrayHoursInDayNEW);
          }
            //System.out.println("jsonArrayHoursInDay.length() "+jsonObjectDay.getJSONArray("hours").length());
        }
        System.out.println("days "+jsonArrayDays.length());
        return jsonArrayDays;
    }

    public static void main(String[] args) {
        //fixHours();
        //getArrayDays();
        //alignmentHoursInDay(getArrayDays());
        getDayCandle("","",getDayMinCandles("",""));
        int a = 0;


        /*JSONObject min = getDayMinCandles("","");
        for(String key: min.keySet()){
            JSONArray minutesInDay = min.getJSONArray(key);
            if(minutesInDay.length()<180){ a++;
                System.out.println("day "+key+" minutes :"+minutesInDay.length());
            }

        }
        System.out.println("TOTAL "+a);*/

        /*JSONObject ticks = getTickes("20120105","20121101");
        JSONArray dayTick = ticks.getJSONArray("20120105");
        for(int i=0; i< dayTick.length(); i++){
            Integer[] r = (Integer[]) dayTick.get(i);
            System.out.println(Arrays.toString(r));
        }*/
        //20110930
        /*for (String key:min.keySet()){
            a++;
            if(a==2)break;
            System.out.println(key);
            JSONArray minutes = min.getJSONArray(key);
            minutes.forEach(c-> System.out.println(Arrays.toString((Integer[])c)));
        }*/

        //JSONArray minutes = min.getJSONArray("20110930");
        //minutes.forEach(System.out::println);
        //getTickes();
        /*int number = 123610;
        int lastDigit = number % 100;  // Остаток от деления на 10
        System.out.println(lastDigit);*/
        //get5secCandle();
        System.out.println("ok");
    }
}

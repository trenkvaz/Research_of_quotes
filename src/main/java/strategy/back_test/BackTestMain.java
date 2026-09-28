package strategy.back_test;

import org.json.JSONObject;
import strategy.OneTrade;
import strategy.situations.RuleSituation;
import strategy.situations.Situation1;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static strategy.TradeLifeCycleSlicer.sliceTradeLifeCycle;
import static utilities.ReadWrite.*;

public class BackTestMain {
    public static String RI_IMOEX2_ticks__25_06_26_07 = "J:\\static_data_2\\JavaProjects\\Research_of_quotes\\my_trade\\RI.IMOEX2_ticks__25_06-26_07.txt";

    final int TAKE_PROFIT = 900;
    final int STOP_LOSE = 300;
    final int MAX_MOVE = 500; // минимальное движение которое цена должна пройти для сделки
    final int MOVE_LEVEL_SIZE = 500; // размер уровня по которым ходит цена чтобы считать пройденный путь
    final int RANGE_LEVEL = 500; // размерность круглых уровней

    //TODO
    // посмотреть проскальзование и как входит по рынку и по лимиту
    // оценка скорости движения цены может пригодится для логики
    // уровни за пределами круглых сделать такую фиксацию хода цены
    // подсчет касаний уровня
    // посмотреть визуализацию стриков потерь

    // Стоп трейлинг заметно влияет на результат около +8%

    public static String IMOEX_2010_2026_09 = "J:\\static_data_2\\JavaProjects\\Research_of_quotes\\my_trade\\IMOEX.txt";

    JSONObject runTest(JSONObject metaData,int[] stringListTicks,List<int[]> stringListTicks1){
        long s = System.currentTimeMillis();
        //metaData.getString("path_quotes")
        //List<String> stringListTicks = readListStr(IMOEX_2010_2026_09);
        /*serialize((Serializable)stringListTicks,"J:\\static_data_2\\JavaProjects\\Research_of_quotes\\src\\main\\resources\\back_test_trades\\stringListTicks_IMOEX_2010_2026_09");*/
        //System.out.println("time readListStr mills:"+(System.currentTimeMillis()-s));

        /*s = System.currentTimeMillis();
        stringListTicks = deserialize("J:\\static_data_2\\JavaProjects\\Research_of_quotes\\src\\main\\resources\\back_test_trades\\stringListTicks_IMOEX_2010_2026_09");
        System.out.println("time deserialize mills:"+(System.currentTimeMillis()-s));*/
        //s = System.currentTimeMillis();
        RuleSituation ruleSituation = new Situation1(metaData.getJSONObject("situation1"));
        GeneralFilterSituation generalFilterSituation = new GeneralFilterSituation(metaData.getJSONObject("generalFilter"),ruleSituation);
        List<OneTrade> oneTradeList = new ArrayList<>();
        assert stringListTicks != null;
        int size = stringListTicks.length;
        //int size = stringListTicks1.size();
        int end = size-1-3;
        for (int i=0; i<size; i+=3)
           // for (int i=0; i<size; i++)
        {
            /*int[] tick = stringListTicks1.get(i);
            OneTrade oneTrade = generalFilterSituation.getOneTrade(tick[0],tick[1],tick[2],i==end);*/
           //if(stringListTicks[i+2]==136852&&stringListTicks[i]==20130903) System.out.println("timeStr:"+stringListTicks[i+1]);
           OneTrade oneTrade = generalFilterSituation.getOneTrade(stringListTicks[i],stringListTicks[i+1],stringListTicks[i+2],i==end);
           if(oneTrade!=null)oneTradeList.add(oneTrade);
        }

        //sliceTradeLifeCycle(null,stringListTicks,oneTradeList);

        //System.out.println("oneTradeList size:"+oneTradeList.size());
        JSONObject total =  new CountResult().total(oneTradeList,metaData);
        metaData.put("total",total);
        return metaData;
    }



    void runManyTest(){
        List<int[]> stringListTicks = null;
        //stringListTicks = readListInt(IMOEX_2010_2026_09,100);
        List<JSONObject> listTotals = new ArrayList<>();
        int[] arrayTicks = null;
        try {
            /*arrayTicks = processFile(IMOEX_2010_2026_09);
            long s = System.currentTimeMillis();
            writeArrayFast(arrayTicks,"J:\\static_data_2\\JavaProjects\\Research_of_quotes\\src\\main\\resources\\back_test_trades\\size_346524807_array_ints_IMOEX_2010_2026_09");
            System.out.println("time writeArrayFast mills:"+(System.currentTimeMillis()-s));*/
            long s1 = System.currentTimeMillis();
            arrayTicks = readArrayFast("J:\\static_data_2\\JavaProjects\\Research_of_quotes\\src\\main\\resources\\back_test_trades\\size_346524807_array_ints_IMOEX_2010_2026_09",346524807);
            System.out.println("time readArrayFast mills:"+(System.currentTimeMillis()-s1));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        /*for (int i=800; i<2000; i+=100)
            for (int a=200; a<500; a+=100)*/

        /*for (int i=900; i<1000; i+=100)
            for (int a=300; a<400; a+=100)*/

        for (int y=10; y<11; y++){
            String m = String.valueOf(y);
            if(y<10)m="0"+m;
            listTotals = new ArrayList<>();
            for (int i=800; i<3000; i+=100)
                for (int a=200; a<1000; a+=100){
                    //long s = System.currentTimeMillis();
                    JSONObject meta_data = new JSONObject();
                    meta_data.put("path_quotes",IMOEX_2010_2026_09);
                    JSONObject situation = new JSONObject();

                    situation.put("MAX_MOVE",500);
                    situation.put("MOVE_LEVEL_SIZE",500);
                    situation.put("RANGE_LEVEL",500);
                    situation.put("timeStartTrade",73000);
                    situation.put("timeEndTrade",150000);
                    situation.put("timeEndTradeExit",10000); // можно менять меняя время закрытия последней сделки за пределами времени торговли
                    situation.put("isTrailing",false);
                    meta_data.put("situation1",situation);

                    JSONObject generalFilter = new JSONObject();
                    //202605   "2010-2026"
                    generalFilter.put("rangeDates","2019");
                    //generalFilter.put("toInt",100);
                    meta_data.put("generalFilter",generalFilter);
                    situation.put("TAKE_PROFIT",i);
                    situation.put("STOP_LOSE",a);
                    listTotals.add(runTest(meta_data,arrayTicks,stringListTicks ));
                    //System.out.println("time runTest mills:"+(System.currentTimeMillis()-s)+" TAKE_PROFIT:"+i+" STOP_LOSE:"+a);
                }

            listTotals.sort(Comparator.comparingInt((JSONObject obj) -> obj.getJSONObject("total").getInt("RESULT")));


            for (JSONObject total:listTotals){
                JSONObject sit = total.getJSONObject("situation1");
                JSONObject tot = total.getJSONObject("total");
              /*  System.out.println(" year:"+tot.getInt("YEAR")+" RESULT: "+tot.getInt("result - commis")+" totalTrades:"+tot.getInt("totalTrades")+
                        " TAKE_PROFIT:"+sit.getInt("TAKE_PROFIT")+"  STOP_LOSE:"+sit.getInt("STOP_LOSE")+" winLong:"+tot.getInt("winLong")+" winShort:"+tot.getInt("winShort")+"  procent stoptrailing:"+tot.getDouble("procent_stoptrailing")+
                        "  maxStreakLose:"+tot.getInt("maxStreakLose"));*/
                System.out.println(tot+" TAKE_PROFIT:"+sit.getInt("TAKE_PROFIT")+"  STOP_LOSE:"+sit.getInt("STOP_LOSE"));

            }

            System.out.println("=======================================================================");
        }

    }


    void compare(){
        List<int[]> stringListTicks = null;
        stringListTicks = readListInt(IMOEX_2010_2026_09,100);

        int[] arrayTicks = null;
        try {
            arrayTicks = processFile(IMOEX_2010_2026_09);
            long s = System.currentTimeMillis();
            writeArrayFast(arrayTicks,"J:\\static_data_2\\JavaProjects\\Research_of_quotes\\src\\main\\resources\\back_test_trades\\size_346524807_array_ints_IMOEX_2010_2026_09");
            System.out.println("time writeArrayFast mills:"+(System.currentTimeMillis()-s));
            s = System.currentTimeMillis();
            arrayTicks = readArrayFast("J:\\static_data_2\\JavaProjects\\Research_of_quotes\\src\\main\\resources\\back_test_trades\\size_346524807_array_ints_IMOEX_2010_2026_09",346524807);
            System.out.println("time readArrayFast mills:"+(System.currentTimeMillis()-s));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        for (int i=0; i<stringListTicks.size(); i++){
            int[] tickStr = stringListTicks.get(i);
            if(!(tickStr[0]==arrayTicks[i*3]&&tickStr[1]==arrayTicks[i*3+1]&&tickStr[2]==arrayTicks[i*3+2])){
                System.out.println("tickStr:"+Arrays.toString(tickStr));
                System.out.println(" date:"+arrayTicks[i*3]+"  time:"+arrayTicks[i*3+1]+"  price: "+arrayTicks[i*3+2]);
                System.out.println("===========================================================================");
                //break;
            }
        }


    }



    public static void main(String[] args) {
        long s = System.currentTimeMillis();
        //new BackTestMain().compare();
        //new CountResult().totalFromFile();
        new BackTestMain().runManyTest();
        /*int[] arrayTicks = null;
        try {
             arrayTicks = processFile(IMOEX_2010_2026_09);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        for (int i=0; i<300; i+=3) System.out.println(" date:"+arrayTicks[i]+"  time:"+arrayTicks[i+1]+"  price: "+arrayTicks[i+2]);

        List<int[]> stringListTicks = readListInt(IMOEX_2010_2026_09,100);

        for (int i=0; i<100; i++) System.out.println(Arrays.toString(stringListTicks.get(i)));
        System.out.println("time back test mills:"+(System.currentTimeMillis()-s));*/
    }
}

package strategy;

import java.util.ArrayList;
import java.util.List;

import static quote_processing.CheckMyTrading.*;
import static strategy.TestTradeIMOEX.Levels.Result.loseLong;
import static strategy.TradeLifeCycleSlicer.sliceTradeLifeCycle;
import static utilities.ReadWrite.readListStr;

public class TestTradeIMOEX {


    //TODO
    // посмотреть проскальзование и как входит по рынку и по лимиту
    // оценка скорости движения цены может пригодится для логики
    // уровни за пределами круглых сделать такую фиксацию хода цены
    // подсчет касаний уровня
    // посмотреть визуализацию стриков потерь
    static String dok = dok10;
    static void runTicks(){
        Levels levels = new Levels();
        List<String> stringListTicks = readListStr(dok);
        List<TradeLifeCycleSlicer.TradeSession> myTrades = new ArrayList<>();
        String currentDay = "";
        int SIZE = stringListTicks.size();
        String currentMonth = "";
        int maxLose = 0;
        int maxWin = 0;
        int loseStreak = 0;
        int maxLoseStreak = 0;
        int totalRes = 0;
        int medianMonth = 0;
        for (int i=0; i<SIZE; i++){
            String[] data = stringListTicks.get(i).split(",");
            if(i!=SIZE-1)
            if(!checkTimeForTrade(data[1]))continue;
            int price = (int) (Double.parseDouble(data[2])
                    *100
            );
            if(!data[0].substring(0,6).equals(currentMonth)){
                if (!currentMonth.isEmpty()){
                    levels.mainProcess(true,price,data[0],data[1]);//для завершения последней сделки в месяце

                    int result = ((Levels.Result.profit-Levels.Result.lose)-((Levels.Result.loseShortC+Levels.Result.loseLongC)*20));
                    totalRes+=result;
                    medianMonth++;
                    if(result<maxLose)maxLose = result;
                    if(result>maxWin)maxWin= result;
                    System.out.println("DATE: "+data[0].substring(0,6));
                    Levels.total();
                    //myTrades.addAll(Levels.myTrades);
                    //break;

                       if(result>0){
                           if(loseStreak<maxLoseStreak)maxLoseStreak = loseStreak;
                           loseStreak = 0;
                       }
                       else {
                           loseStreak+=result;
                           System.out.println("LOSESTREAK: "+loseStreak+" "+result);
                       }

                }

                currentMonth = data[0].substring(0,6);
                levels = new Levels();
            }

            boolean isStartDay = false;
            if(!data[0].equals(currentDay)){
                //if(!currentDay.isEmpty())myTrades.addAll(Levels.myTrades);
                currentDay = data[0];
                isStartDay = true;
               // System.out.println(currentDay+"  "+data[1]);
            }
            if(i==SIZE-1)isStartDay = true;

            int res = levels.mainProcess(isStartDay,price,data[0],data[1]);


            //if(res==1)break;
//            if(currentDay.equals("20250603")){
//                //System.out.println("=====================");
//                break;
//            }
            if(i==SIZE-1){
                System.out.println("DATE: "+data[0].substring(0,6));
                int result = ((Levels.Result.profit-Levels.Result.lose)-((Levels.Result.loseShortC+Levels.Result.loseLongC)*20));
                totalRes+=result;
                medianMonth++;
                Levels.total();
                //myTrades.addAll(Levels.myTrades);
            }
        }
        //Levels.total();
        //System.out.println("myTrades "+myTrades.size());
        //System.out.println(myTrades.get(0).toString());
        //myTrades.forEach(c-> System.out.println(c.toString()));
        //sliceTradeLifeCycle(stringListTicks, myTrades);
        System.out.println("maxWin "+maxWin+" maxLose "+maxLose+" maxLoseStreak "+maxLoseStreak);
        System.out.println("totalRes "+totalRes+" medianMonth "+(totalRes/medianMonth));
    }


    static class Levels {

        //double startPrice = 0;
        double levelUp = 0;
        double levelDown = 0;
        //double movePrice = 0;
        String trade = "";
        int beforePrice = 0;
        int priceTrade = 0;
        final int TAKE_PROFIT = 900;
        final int STOP_LOSE = 300;
        final int MAX_MOVE = 500;
        final int INDENT_LEVEL = 1;  // отступ уровня чтобы был диапазон на уровне, а не точное касание уровня
        final int MAX_INDENT = 2; // отступ от отступа для подсчета максимального пролета за уровень
        final int MOVE_LEVEL_SIZE = 500; // размер уровня по которым ходит цена чтобы считать пройденный путь
        final int RANGE_LEVEL = 500; // размерность круглых уровней
        //Result result = new Result();

        int one, two, three;
        String dateTrade = "";
        String entryTimeStr = "";
        int closeTrade = 0;
        String beforeTime = "";
        String outPutDateTrade = "";
        int isWaitLimitPrice;
        final int INDENT_LIMIT_TRADE = 100; // отступ на который ставится лимитка если дошло до нужного уровня, если на пробой

        Result result;
        static List<TradeLifeCycleSlicer.TradeSession> myTrades;

        public Levels(){
            result = new Result();
            myTrades = new ArrayList<>();
        }


        public class Result {
            public static int profit;
            public static int lose;
            static int countProfit;
            static int countLose;
            static int countLong;
            static int countShort;

            static int winLong;
            static int loseLong;
            static int winShort;
            static int loseShort;
            static int winLongC;
            static int loseLongC;
            static int winShortC;
            static int loseShortC;
            static int resultForceCloseEndTrade;
            static int countForceCloseEndTrade;
        }

        static void total(){
            //System.out.println(dok);
            System.out.println("profit "+Levels.Result.profit +" lose "+Levels.Result.lose+" long "
                    +Levels.Result.countLong+" short "+Levels.Result.countShort+" result:"+((Levels.Result.profit-Levels.Result.lose)-((Levels.Result.loseShortC+Levels.Result.loseLongC)*20))+" commiss:"+((Levels.Result.loseShortC+Levels.Result.loseLongC)*20));

            System.out.println("winLong "+ Levels.Result.winLong);
            System.out.println("loseLong "+Levels.Result.loseLong);
            System.out.println("winShort "+ Levels.Result.winShort);
            System.out.println("loseShort "+Levels.Result.loseShort);
            System.out.println("winLongC "+ Levels.Result.winLongC);
            System.out.println("loseLongC "+Levels.Result.loseLongC);
            System.out.println("longWinRate:"+((double)Levels.Result.winLongC/(Levels.Result.winLongC+Levels.Result.loseLongC)));
            System.out.println("winShortC "+ Levels.Result.winShortC);
            System.out.println("loseShortC "+Levels.Result.loseShortC);
            System.out.println("shortWinRate:"+((double)Levels.Result.winShortC/(Levels.Result.winShortC+Levels.Result.loseShortC)));
            System.out.println("stop counts: "+(Levels.Result.loseShortC+Levels.Result.loseLongC));
            System.out.println("countForceCloseEndTrade "+Levels.Result.countForceCloseEndTrade+"  resultForceCloseEndTrade: "+Levels.Result.resultForceCloseEndTrade);


            Levels.Result.profit = 0;
            Levels.Result.lose = 0;
            Levels.Result.countProfit = 0;
            Levels.Result.countLose = 0;
            Levels.Result.countLong = 0;
            Levels.Result.countShort = 0;

            Levels.Result.winLong = 0;
            Levels.Result.loseLong = 0;
            Levels.Result.winShort = 0;
            Levels.Result.loseShort = 0;
            Levels.Result.winLongC = 0;
            Levels.Result.loseLongC = 0;
            Levels.Result.winShortC = 0;
            Levels.Result.loseShortC = 0;
            Levels.Result.resultForceCloseEndTrade = 0;
            Levels.Result.countForceCloseEndTrade = 0;
        }


        void refreshLikeStartDay(){
            //startPrice = price;
            //levelDown = (int)((price+0.1 )/ 10)*10;
            //levelUp = levelDown + 10-INDENT_LEVEL;
            levelDown+=INDENT_LEVEL;
            priceTrade = 0;
            trade = "";
            one = 0; two = 0; three = 0;
           // System.out.println();
            dateTrade = "";
            entryTimeStr = "";
            closeTrade = 0;
            outPutDateTrade = "";
        }
        //CLOSE NEXT DAY 20260808  20260809 20260808145959  227128


        /*CLOSE NEXT DAY 20260805  20260806 20260805145959  228660
                230921  20260806073001
                229386  20260807073001
                226948  20260808100001
        CLOSE NEXT DAY 20260808  20260809 20260808145959  227128
        */
        int mainProcess(boolean isStartDay,int price,String date,String time){
            if(isStartDay){
               if(!trade.isEmpty()){
                   closeTrade(price,date,time);
               }

                beforeTime = date+time;;
                beforePrice = price;

                //System.out.println(price+"  "+beforeTime);

                refreshLikeStartDay();
               return 0;
            }
            int[] r = getSizeMove(price);
            //if(r[0]!=0) System.out.println("diff "+r[0]+" price "+price+" level "+r[1]);

            if(trade.isEmpty()){
                //double movePrice = Math.abs(startPrice - price);
                //System.out.println("movePrice "+movePrice+" startPrice "+startPrice+" "+price);
                if((MAX_MOVE==Math.abs(r[0]))&&r[1]!=0
                        //уровень равен круглому
                        &&r[1]%RANGE_LEVEL==0
                ){

                    trade = r[0]>0? "long":"short";

                    //System.out.println(trade+" level "+r[1]+" price "+price+" date:"+date);
                    priceTrade = price;
                    dateTrade = date;
                    if(trade.equals("long"))Result.countLong++;
                    else Result.countShort++;
                    String output2 = date.substring(0, 4)+"-" + date.substring(4, 6)+"-" + date.substring(6, 8);
                    outPutDateTrade = output2+" "+time.substring(0, 2)+":" + time.substring(2, 4)+":" + time.substring(4, 6);
                    //System.out.println(outPutDateTrade);
                    entryTimeStr = date+time;
                }

            } else {
                if(!dateTrade.equals(date)) System.out.println("NEXT DAY "+dateTrade+"  "+date);
                int currentTrade = price-priceTrade;
                boolean isClose = false;
                int result = 0;
                if(trade.equals("long")){
                    // куррентТрейд если положительный и равен или больше тейк-профита
                    if(currentTrade>=TAKE_PROFIT){
                        isClose = true;
                        Result.profit += currentTrade;
                       // System.out.println("TAKE_PROFIT LONG "+price+" date:"+date);
                        Result.winLong += currentTrade;;
                        Result.winLongC++;

                    }
                    // куррентТрейд если отрицательный переводится в положительное и равен или больше стоп-лосса
                    else if(currentTrade*-1>=STOP_LOSE){
                        isClose = true;
                        Result.lose += (currentTrade*-1);
                        //System.out.println("STOP_LOSE LONG "+price+" date:"+date);
                        Result.loseLong += (currentTrade*-1);
                        Result.loseLongC++;
                    }
                    result = currentTrade;
                }

                if(trade.equals("short")){
                    // куррентТрейд если положительный и равен или больше стоп-лосса
                    if(currentTrade>=STOP_LOSE){
                        isClose = true;
                        Result.lose += currentTrade;
                        //System.out.println("STOP_LOSE SHORT "+price+" date:"+date);
                        Result.loseShort += currentTrade;
                        Result.loseShortC++;
                    }
                    // куррентТрейд если отрицательный переводится в положительное и равен или больше тейк-профита
                    else if(currentTrade*-1>=TAKE_PROFIT){
                        isClose = true;
                        Result.profit += (currentTrade*-1);
                        Result.winShort += (currentTrade*-1);
                        Result.winShortC++;
                        //System.out.println("TAKE_PROFIT SHORT "+price+" date:"+date);
                    }
                    result = currentTrade*-1;
                }

                if(isClose){
                    TradeLifeCycleSlicer.TradeSession tradeSession = new TradeLifeCycleSlicer.TradeSession(entryTimeStr,date+time,priceTrade,price,trade,result,outPutDateTrade);
                    //System.out.println(tradeSession.toString());
                    myTrades.add(tradeSession);
                    refreshLikeStartDay();
                }

            }
            beforeTime = date+time;
            beforePrice = price;
            return 0;
        }


        void closeTrade(int price,String date,String time){
            //System.out.println("CLOSE NEXT DAY "+dateTrade+"  "+date+" "+beforeTime+"  "+beforePrice);
            int currentTrade = beforePrice-priceTrade;
            int result = 0;
            if(trade.equals("long")){
                // куррентТрейд если положительный и равен или больше тейк-профита
                if(currentTrade>0){
                    Result.profit += currentTrade;
                    //refreshLikeStartDay(price);
                    // System.out.println("TAKE_PROFIT LONG "+price+" date:"+date);
                    Result.winLong += currentTrade;;
                    Result.winLongC++;

                }
                // куррентТрейд если отрицательный переводится в положительное и равен или больше стоп-лосса
                else {
                    Result.lose += (currentTrade*-1);
                    //refreshLikeStartDay(price);
                    //System.out.println("STOP_LOSE LONG "+price+" date:"+date);
                    Result.loseLong += (currentTrade*-1);
                    Result.loseLongC++;

                }
                result = currentTrade;
            }

            if(trade.equals("short")){
                // куррентТрейд если положительный и равен или больше стоп-лосса
                if(currentTrade>0){
                    Result.lose += currentTrade;
                    //refreshLikeStartDay(price);
                    //System.out.println("STOP_LOSE SHORT "+price+" date:"+date);
                    Result.loseShort += currentTrade;
                    Result.loseShortC++;
                }
                // куррентТрейд если отрицательный переводится в положительное и равен или больше тейк-профита
                else {
                    Result.profit += (currentTrade*-1);
                    //refreshLikeStartDay(price);
                    Result.winShort += (currentTrade*-1);
                    Result.winShortC++;
                    //System.out.println("TAKE_PROFIT SHORT "+price+" date:"+date);
                }
                result = currentTrade*-1;
            }

            Result.resultForceCloseEndTrade+=result+(-20);
            Result.countForceCloseEndTrade++;
            TradeLifeCycleSlicer.TradeSession tradeSession = new TradeLifeCycleSlicer.TradeSession(entryTimeStr,beforeTime,priceTrade,beforePrice,trade,result,outPutDateTrade);
            //System.out.println(tradeSession.toString());

            myTrades.add(tradeSession);
        }



        int[] getSizeMove(int price){

            int[] res = new int[2]; // 0 - diff ,1 - level
            int crossedLevel = 0;
            int prevBlock = price / MOVE_LEVEL_SIZE;
            int currBlock = beforePrice / MOVE_LEVEL_SIZE;
            if (prevBlock != currBlock) {
                // Вычисляем точный уровень, который перешагнули
                // Находим максимальный блок и умножаем на шаг
                crossedLevel = Math.max(prevBlock, currBlock) * MOVE_LEVEL_SIZE;
                res[1] = crossedLevel;
            }

            if(one==0){
                // если первая точка не проставлена идет до кратного MOVE_LEVEL_SIZE И проставляются 1 и 2
               if(crossedLevel!=0){
                   //System.out.println("SET "+crossedLevel);
                   one = crossedLevel; two = crossedLevel;
               }
                // 1 и 2 стоят проверяет ход 3 пока эмуляция через текущую цену
            } else {
                int diffTwoOne = two-one;
                // цена достигла уровня
                if(crossedLevel!=0){
                   //если цена равна 2, то значит вернулась обратно
                    if(crossedLevel==two)res[0] = diffTwoOne;
                    else {
                        // проверка разницы между 3 и 2
                        int diffTreeTwo = price-two;
                        // проверка что цена пошла в ту же сторону что ход между 1 и 2
                        // если да то 2 ставится на 3, а 1 на месте
                        if(diffTwoOne>0&&diffTreeTwo>0||diffTwoOne<0&&diffTreeTwo<0){
                            two = crossedLevel;
                            res[0] = two - one;
                        }  // если не в ту сторону то 2 ставится на 3, а 1 переходит на два
                        else {
                            one = two;
                            two = crossedLevel;
                            res[0] = two - one;
                        }
                    }
                }
                // цена вне уровня считается разница между 2 и 1 плюс разница между 3 и 2
               /* else {
                    return (two - one)+(price - two);
                }*/
            }
            return res;
        }
    }


    public static void checkLevels(int[] series, int step) {
        if (series == null || series.length < 2) return;

        for (int i = 1; i < series.length; i++) {
            int prev = series[i - 1];
            int curr = series[i];

            // Целочисленное деление определяет, в каком "блоке" находится число
            int prevBlock = prev / step;
            int currBlock = curr / step;

            System.out.println("prevBlock "+prevBlock+" currBlock "+currBlock);
            // Если блоки разные — мы пересекли границу кратного числа!
            if (prevBlock != currBlock) {
                // Вычисляем точный уровень, который перешагнули
                // Находим максимальный блок и умножаем на шаг
                int crossedLevel = Math.max(prevBlock, currBlock) * step;

                System.out.printf("Уровень зафиксирован между %d и %d! Пробит уровень: %d%n",
                        prev, curr, crossedLevel);
            }
        }
    }

    static void test(){
        //System.out.println((1000-2000)+(750 - 1000));
        int[] mySeries = {    277120,276990, 276667,276500,276303,277120,276990,};
        int step = 500;

        System.out.println("--- Поиск уровней кратных " + step + " ---");
        checkLevels(mySeries, step);

        //System.out.println(1001%1000);
    }

    static boolean checkTimeForTrade(String time){
        int timeStartTrade = 73000;
        int timeEndTrade = 150000;
        int timeInt = Integer.parseInt(time);
        //System.out.println(timeInt);
        return timeInt>timeStartTrade&&timeInt<timeEndTrade;
    }

    public static void main(String[] args) {
        runTicks();

        //System.out.println(checkTimeForTrade("073001"));

    }
}

package quote_processing;

import org.json.JSONArray;
import org.json.JSONObject;
import utilities.KeyToIndexStructure;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static quote_processing.DeepTickBarSlicer.sliceAndSaveWithIncompleteBar;
import static quote_processing.LeftChartDataPreparer.*;
import static quote_processing.MinuteBarAggregator.*;
import static quote_processing.TickConverter.convertTicks;
import static utilities.ReadWrite.readListStr;
import static utilities.ReadWrite.writeListStr;

public class CheckMyTrading {
    // из Экселя в csv разделитель @ кодировка Юникод УТФ-8
    static String dok1 = "J:\\static_data_2\\JavaProjects\\Research_of_quotes\\my_trade\\Поздняков Алексей Александрович КлФ-ИИС1620644 (01.01.2026 по 31.03.2026).csv";

    static String dok2 = "J:\\static_data_2\\JavaProjects\\Research_of_quotes\\my_trade\\2Поздняков Алексей Александрович КлФ-ИИС1620644 (01.04.2026 по 30.04.2026).csv";

    static String dok3 = "J:\\static_data_2\\JavaProjects\\Research_of_quotes\\my_trade\\Поздняков Алексей Александрович КлФ-ИИС1620644 (01.05.2026 по 15.05.2026).csv";

    static String dok4 = "J:\\static_data_2\\JavaProjects\\Research_of_quotes\\my_trade\\Поздняков Алексей Александрович КлФ-ИИС1620644 (01.05.2026 по 21.07.2026).csv";

    static String dok41 = "J:\\static_data_2\\JavaProjects\\Research_of_quotes\\my_trade\\Поздняков Алексей Александрович КлФ-ИИС1620644 (01.07.2025 по 01.10.2025).csv";

    static String dok42 = "J:\\static_data_2\\JavaProjects\\Research_of_quotes\\my_trade\\Поздняков Алексей Александрович КлФ-ИИС1620644 (01.10.2025 по 01.01.2026).csv";

    static String dok43 = "J:\\static_data_2\\JavaProjects\\Research_of_quotes\\my_trade\\Поздняков Алексей Александрович КлФ-ИИС1620644 (01.01.2026 по 01.04.2026).csv";

    static String dok44 = "J:\\static_data_2\\JavaProjects\\Research_of_quotes\\my_trade\\Поздняков Алексей Александрович КлФ-ИИС1620644 (01.04.2026 по 01.07.2026).csv";

    public static String dok5 ="J:\\static_data_2\\JavaProjects\\Research_of_quotes\\my_trade\\ticks_MIX_2025_07__01_2026_07_31.txt";

    public static String dok6 = "J:\\static_data_2\\JavaProjects\\Research_of_quotes\\my_trade\\RI.IMOEX2_ticks__25_06-26_07.txt";

    public static String dok7 = "J:\\static_data_2\\JavaProjects\\Research_of_quotes\\my_trade\\Поздняков Алексей Александрович КлФ-ИИС1620644 (01.08.2026 по 30.08.2026).csv";

    public static String dok8 = "J:\\static_data_2\\JavaProjects\\Research_of_quotes\\my_trade\\ticks_MIX_25_08.txt";

    public static String dok9 = "J:\\static_data_2\\JavaProjects\\Research_of_quotes\\my_trade\\ticks_RI.IMOEX2_26_08.txt";

    public static String dok10 = "J:\\static_data_2\\JavaProjects\\Research_of_quotes\\my_trade\\IMOEX.txt";
    static List<String[]> readCSV(String dok){
        List<String[]> listRows = new ArrayList<>();
        List<String> stringList = readListStr(dok);
        stringList.forEach(c->{
            String[] arr = c.split("@");
            listRows.add(arr);
        });
        return listRows;
    }

    public static class MyOneTrade{
        long tradeId;
        String date;
        double open;
        double close;
        int amount;
        double commission;
        String typeTrade;
        int buyAsk;
        String name;
        double result;
        String keyDate;
        int possibleMin;
        int possibleMax;
        @Override
        public String toString(){
            return tradeId+" "+date+" "+open+" "+close+" "+typeTrade;
        }
    }

    static List<MyOneTrade> getOneInstrument(List<String[]> trades, String instrument){

        List<double[]> filterList = new ArrayList<>();
        List<MyOneTrade> myOneTradeList = new ArrayList<>();
        int lineCount = 0;
        MyOneTrade myOneTrade;
        for (String[] line : trades){
            lineCount++;
            if(line.length<13)continue;
            //System.out.println("*"+Arrays.toString(line)+"*");
            if(!line[3].equals(instrument)){
                //System.out.println("not equals *"+line[4]+"*"+FUT+"*");
               // System.out.println(Arrays.toString(line));
                continue;
            }
            if(!((line[4].equals("Покупка")||line[4].equals("Продажа"))&&line[2].startsWith("Фьючерсы")))continue;
            //System.out.println(Arrays.toString(line));
            double buyAsk = -1;
            if(line[4].equals("Покупка"))buyAsk = 1;
            //else if(line[5].equals("Продажа"))buyAsk = -1;
            myOneTrade = new MyOneTrade();
            myOneTrade.typeTrade = line[4];
            myOneTrade.buyAsk = (int) buyAsk;
            try {
                String input = line[0];
                String output = input.substring(6, 10) + input.substring(3, 5)+ input.substring(0, 2);
                //System.out.println(output+line[1].replace(":",""));
                myOneTrade.keyDate = output+ line[1].replace(":","");

                double date = Double.parseDouble(output+line[1].replace(":",""));
                String output2 = input.substring(6, 10)+"-" + input.substring(3, 5)+"-" + input.substring(0, 2);
                myOneTrade.date = output2+" "+line[1];
                double amount = Double.parseDouble(line[5].replace(",", "."));
                double price = Double.parseDouble(line[6].replace(",", "."));
                double commission = 0;
                if(!line[9].isEmpty())commission = Double.parseDouble(line[9].replace(",", "."));
               // else if(!line[12].isEmpty())commission = Double.parseDouble(line[12].replace(",", "."));
                filterList.add(new double[]{ buyAsk,amount,price,commission,date });

                myOneTrade.commission = commission;
                myOneTrade.amount = (int) amount;
                myOneTrade.open = price;
                myOneTrade.tradeId = (long) (System.currentTimeMillis()+date);
                myOneTradeList.add(myOneTrade);
            } catch (Exception e){
                System.out.println(lineCount+" error "+ Arrays.toString(line));
                System.out.println(e.getMessage());
                break;
            }

        }

        //filterList.forEach(c-> System.out.println(Arrays.toString(c)));
        //System.out.println("size "+filterList.size());
        return  myOneTradeList;
    }

    static Set<String> getNamesInstruments(List<String[]> trades){
        Set<String> res = new HashSet<>();
        for (String[] line : trades){
            if(line.length<10)continue;
            //System.out.println("*"+line[2]+"*");
            if(!((line[4].equals("Покупка")||line[4].equals("Продажа"))&&line[2].startsWith("Фьючерсы")))continue;

            res.add(line[3]);
        }
        return res;
    }


    static List<MyOneTrade> countInstrument(List<MyOneTrade> trades,JSONObject instrument){
        List<MyOneTrade> myTrades = new ArrayList<>();
        String name = instrument.getString("name");
        int lot = instrument.getInt("lot");
        /*if(instrument.has("opened")){
            double[] opened = (double[]) instrument.get("opened");
            trades.add(opened);}*/
        double commission = trades.size()*0.45; // комиссия Финам 45 копеек за фьючерс
        double win = 0;
        double lose = 0;
        double countWin = 0;
        double countLose = 0;
        double currentType = 0;
        double currentCountPosition = 0;
        //List<Double> currentPositions = new ArrayList<>();
        boolean test = false;
        Queue<MyOneTrade> currentPositions = new LinkedList<>();
        //filterList.add(new double[]{ buyAsk,amount,price,commission,date });
        for(int i=0; i<trades.size(); i++){
            MyOneTrade trade = trades.get(i);
            commission+=trade.commission;

            if(currentType==0){
                currentType = trade.buyAsk;
                for(int p=0; p<trade.amount; p++){
                    currentPositions.add(trade);
                }
                //System.out.println("currentType==0 "+trade[0]+" currentPositions.size() "+currentPositions.size()+" "+Arrays.toString(trade));
                continue;
            }
            // наращение позы
            if(currentType==trade.buyAsk){

                for(int p=0; p<trade.amount; p++){
                    currentPositions.add(trade);
                }
                //System.out.println("currentType==trade[0] "+trade[0]+" currentPositions.size() "+currentPositions.size()+" "+Arrays.toString(trade));
            }

            if(currentType!=trade.buyAsk){
                // считаем разницу между количеством новой позы и количеством текущей

                int addPosition = (int) (trade.amount - currentPositions.size());
                for(int p=0; p<trade.amount; p++){
                    if(currentPositions.isEmpty())break;
                    MyOneTrade openTrade = currentPositions.poll();
                    double pos = openTrade.open;
                    // (текущая поза - закрывающая поза) если закрытие шорт умножаешь на минус 1
                    /*if(!test&&pos==249525){
                        System.out.println("pos "+pos+" trade[2] "+trade[2]+" trade[0] "+trade[0]+" "+((long)openTrade[4]));
                        test = true;
                    }*/

                    double result = ((pos - trade.open)*trade.buyAsk)*lot;
                    //System.out.println(pos);
                    // Дата, цена, тип сделки здесь закрывающая сделка поэтому умножение на -1
                    MyOneTrade myOneTrade = new MyOneTrade();
                    myOneTrade.date = openTrade.date;
                    myOneTrade.typeTrade = openTrade.typeTrade;
                    myOneTrade.open = pos;
                    myOneTrade.close = trade.open;
                    myOneTrade.amount = 1;
                    myOneTrade.name = name;
                    myOneTrade.tradeId = openTrade.tradeId;
                    myOneTrade.result = result;
                    myOneTrade.commission = (openTrade.commission+trade.commission);
                    myOneTrade.keyDate = openTrade.keyDate;
                    myOneTrade.buyAsk = openTrade.buyAsk;
                    //myTrades.add(new double[]{openTrade[4],pos,openTrade[0]});
                    myTrades.add(myOneTrade);
                    if(result>=0){
                        win+=result;
                        countWin++;
                    }
                    else if(result<0){
                        lose+=result;
                        countLose++;
                    }
                }
               /* System.out.println("currentType!=trade[0] "+trade[0]+" currentPositions.size() "+currentPositions.size()+" "+Arrays.toString(trade));*/
                // позы закрыты
                if(currentPositions.isEmpty()){
                    currentType=0;
                }
                // если есть открытие новых после закрытия - переворот

                if(addPosition>0){
                    //System.out.println("addPosition "+addPosition);
                    currentType = trade.buyAsk;
                    for(int p=0; p<addPosition; p++){
                        currentPositions.add(trade);
                    }
                }

            }

        }
        win = Math.round(win);
        lose = Math.round(lose);
        commission = Math.round(commission);

        System.out.println(name+" commission:"+commission+" win:"+win+" lose:"+lose+" result "+(win+lose));
        System.out.println("countWin:"+countWin+" countLose:"+countLose+" countWin/countLose:"+(countWin/countLose));

        //saveMyTrade(myTrades);
       // aggregateTicks(List<String> rawTicks, Set<String> myTrades)
        return myTrades;
    }


    static void saveMyTrade(List<MyOneTrade> myTrades){
        List<String> saveList = new ArrayList<>();
        saveList.add("Trade ID,Символ инструмента,Время открытия,Направление,Цена покупки,Цена продажи,Объём,pnl,комиссия");
        int i=0;
        double res = 0;
        double win = 0;
        double lose = 0;
        double loseCom = 0;
        double winCom = 0;
        for (MyOneTrade myOneTrade:myTrades){
            i++;
            String string = myOneTrade.tradeId+","+myOneTrade.name+","+myOneTrade.date+","+myOneTrade.typeTrade+",";
            if(myOneTrade.buyAsk==1)string+=myOneTrade.open+","+myOneTrade.close+",";
            else string+=myOneTrade.close+","+myOneTrade.open+",";
            string+=myOneTrade.amount+","+myOneTrade.result;
                    //+","+myOneTrade.commission;
            saveList.add(string);
            res+=myOneTrade.result;
            //System.out.println(string);
            //if(i>10)break;
            if(myOneTrade.result>=0){
                win+=myOneTrade.result;
                winCom+=myOneTrade.commission;
            }
            else {
                lose+=myOneTrade.result;
                loseCom+=myOneTrade.commission;
            }

            //if(myOneTrade.result==-1275) System.out.println(myOneTrade.toString());
        }
        System.out.println("RES "+res+" win "+win+" lose "+lose+" winCom "+winCom+" loseCom "+loseCom);
        writeListStr(saveList,"J:\\static_data_2\\JavaProjects\\Research_of_quotes\\my_trade\\mytrades_20260501_20260723.csv");
    }


    static class Tick{
        String dateKey;
        double price;

        public Tick(String stringTick){
            String[] dataTicks = stringTick.split(",");
            dateKey = dataTicks[0]+dataTicks[1];
            price = Double.parseDouble(dataTicks[2]);
        }
    }

    static List<Tick> convertStringTickToTick(List<String> stringListTicks){
        List<Tick> result = new ArrayList<>();
        for (String tick:stringListTicks)result.add(new Tick(tick));
        return result;
    }

    static void checkMyTrades(List<MyOneTrade> myTrades, List<String> stringListTicks){
        Map<String,MyOneTrade> myOneTradeMap = getMyTradeKeyDates(myTrades);
        List<Tick> tickList = convertStringTickToTick(stringListTicks);
        String dublikat = "";
        int c=0;
        double stopWin = 1000;
        double stopLose = -1000;
        int cwin = 0;
        int close = 0;
        //myTrades.forEach(c-> System.out.println(Arrays.toString(c)));
        double result = 0;
        int s =0;
        // Дата, цена, тип сделки
        double checkRes = 0;
        int strickWin = 0;
        int strickLose = 0;
        int strickWinMax = 0;
        int strickLoseMax = 0;
        for (int a=0; a<tickList.size(); a++){
            Tick tick = tickList.get(a);
            String dateKey = tick.dateKey;
            double priceTick = tick.price;
            MyOneTrade myOneTrade = myOneTradeMap.get(dateKey);
            if(myOneTrade==null)continue;
            if(priceTick!=myOneTrade.open)continue;
            if(dublikat.equals(dateKey+priceTick))continue;
            c++;
            dublikat=dateKey+priceTick;

            double res = 0;
            boolean isTachWin = false;
            double max = stopLose;
            double min = stopWin;
            checkRes+=myOneTrade.result;

            for (int i=a+1; i<tickList.size(); i++){
                Tick tick1 = tickList.get(i);
                double priceTick1 = tick1.price;
                //System.out.println(priceTick+"  "+priceTick1+"  "+myOneTrade.buyAsk);
                res = ((priceTick - priceTick1 )*(myOneTrade.buyAsk*-1));
                //res = ticks.list.get(i);
                //if(myOneTrade.date.equals("2026-06-17 10:46:15")) System.out.println(res+" "+max+" "+min);
                if(res>max)max = res;
                if(res<min)min = res;
                //if(s==3) System.out.println(tick+" res "+res+" "+ticks.list.get(i));
                if(res>=stopWin){

                    //System.out.println(res);
                    /*if(!isTachWin) {
                        isTachWin = true;
                        continue;
                    }*/

                    if(res>=stopWin){
                        cwin++;
                        result+=res;
                        //System.out.println("win "+res);
                        strickWin++;
                        if(strickLose>strickLoseMax){
                            //System.out.println("strickLose "+strickLose+" "+strickLoseMax);
                            strickLoseMax=strickLose;
                        }
                        strickLose = 0;
                        //System.out.println("strickWinn "+strickWin+" ");
                        break;
                    }

                }
                /*if(isTachWin){
                    if(res<=100){
                        result+=res;
                        close++;
                        System.out.println(res);
                        break;
                    }
                }*/

                if (res<=stopLose){
                    result+=res;
                    close++;
                    //System.out.println("stop "+res);
                    strickLose++;
                    if(strickWin>strickWinMax){
                        //System.out.println("strickWin "+strickWin+" "+strickWinMax);
                        strickWinMax=strickWin;
                    }
                    strickWin = 0;
                    //System.out.println("strickLose "+strickLose+" ");
                    break;
                }
            }
            myOneTrade.possibleMin = (int) min;
            myOneTrade.possibleMax = (int) max;
           // System.out.println(myOneTrade.date+" "+myOneTrade.typeTrade+"  "+myOneTrade.result+" "+max+" "+min);
        }
        System.out.println("total "+c+"  result "+result+" cwin "+cwin+" close "+close+" strickWinMax "+strickWinMax+" strickLoseMax "+strickLoseMax);
    }

    static List<String[]> mergeTrades(){
        List<String[]> trade = readCSV(dok4);
        List<String[]> trade1 = readCSV(dok41);
        List<String[]> trade2 = readCSV(dok42);
        List<String[]> trade3 = readCSV(dok43);
        List<String[]> trade4 = readCSV(dok44);
        List<String[]> result = new ArrayList<>();

        Set<String> dates = new HashSet<>();

        for (String[] line :trade){
            if(line.length<12)continue;
            //System.out.println("*"+Arrays.toString(line)+"*");
            if(!((line[4].equals("Покупка")||line[4].equals("Продажа"))&&line[2].startsWith("Фьючерсы")))continue;
            String key =  line[0]+line[1];
            if(!dates.contains(key)){
                result.add(line);
                dates.add(key);
            }
        }
        for (String[] line :trade1){
            if(line.length<12)continue;
            //System.out.println("*"+Arrays.toString(line)+"*");
            if(!((line[4].equals("Покупка")||line[4].equals("Продажа"))&&line[2].startsWith("Фьючерсы")))continue;
            String key =  line[0]+line[1];
            if(!dates.contains(key)){
                result.add(line);
                dates.add(key);
            }
        }
        for (String[] line :trade2){
            if(line.length<12)continue;
            //System.out.println("*"+Arrays.toString(line)+"*");
            if(!((line[4].equals("Покупка")||line[4].equals("Продажа"))&&line[2].startsWith("Фьючерсы")))continue;
            String key =  line[0]+line[1];
            if(!dates.contains(key)){
                result.add(line);
                dates.add(key);
            }
        }
        for (String[] line :trade3){
            if(line.length<12)continue;
            //System.out.println("*"+Arrays.toString(line)+"*");
            if(!((line[4].equals("Покупка")||line[4].equals("Продажа"))&&line[2].startsWith("Фьючерсы")))continue;
            String key =  line[0]+line[1];
            if(!dates.contains(key)){
                result.add(line);
                dates.add(key);
            }
        }
        for (String[] line :trade4){
            if(line.length<12)continue;
            //System.out.println("*"+Arrays.toString(line)+"*");
            if(!((line[4].equals("Покупка")||line[4].equals("Продажа"))&&line[2].startsWith("Фьючерсы")))continue;
            String key =  line[0]+line[1];
            if(!dates.contains(key)){
                result.add(line);
                dates.add(key);
            }
        }
        return  result;
    }

    static void countManyInstruments(){
        //List<String[]> trade = mergeTrades();
        List<String[]> trade = readCSV(dok7);
        List<JSONObject> listInstruments = new ArrayList<>();
        //MIX-12.25 MIX-9.25 RTS-9.25 MXI-12.25 MXI-3.26 MIX-6.26 MXI-6.26 MIX-9.26
        //listInstruments.add(new JSONObject().put("name","USDRUBF").put("lot",1000));
        //listInstruments.add(new JSONObject().put("name","MXI-3.26").put("lot",10));
        //listInstruments.add(new JSONObject().put("name","Si-6.26").put("lot",1).put("opened",new double[]{-1.0, 2.0, 76016.0, 0.0}));
        //listInstruments.add(new JSONObject().put("name","MXI-6.26").put("lot",10));
        //listInstruments.add(new JSONObject().put("name","MIX-9.25").put("lot",1));
        //listInstruments.add(new JSONObject().put("name","MIX-12.25").put("lot",1));
        listInstruments.add(new JSONObject().put("name","MIX-9.26").put("lot",1));

        //listInstruments.add(new JSONObject().put("name","MIX-6.26").put("lot",1));
        //listInstruments.add(new JSONObject().put("name","MIX-6.26").put("lot",1));
        List<String> stringListTicks = readListStr(dok8);
        List<String> rawCorrTicks = readListStr(dok9);
        for (JSONObject instr:listInstruments){
            // из текста собирает сделки в виде отдельных направлений покупка или продажа в объект
            List<MyOneTrade> filter = getOneInstrument(trade,instr.getString("name"));
                              // из объектов создает объект конечных сделок с открытием и закрытием, результатом и так далее
            List<MyOneTrade> myOneTradeList = countInstrument(filter,instr);

            checkMyTrades(myOneTradeList,stringListTicks);
                           // создает мапу Дата Сделка, берет список котировок инструмента на их основе создает два списка:
                             // 1 Список Окна срезы по 120 минут котировок с моей сделкой слева, синхронный 2 Список Окна срезы по 120 минут кумулятивная дельна котировок
                          // Возвращает мапу - ключ: дата до минуты, значение: список полные даты до секунды тиков в которых были сделки
                          //И создает правые графики Основной и Дельту
            Map<String,List<String>> mapMinKeyTickKeysTrades = sliceAndSaveWithIncompleteBar(stringListTicks,getMyTradeKeyDates(myOneTradeList));

                        // берет список левого верхнего на экране инструмента который коррелирует с правым это Индекс и переводит в список Минут с тиками внтури
            List<LeftChartDataPreparer.DeepMinuteBar> deepMinuteBarListl = buildLeftMinutes(rawCorrTicks);
                        // из списка полученного выше создает список Часов помещенных в объект ключ: Дата до часов, значение индекс в списке
            KeyToIndexStructure<String, LeftChartDataPreparer.HourBar> hourBarKeyToIndexStructure = buildLeftHours(deepMinuteBarListl);
                         // финальная сборка графиков слева
                        // Левые минуты крутит и находит в Мапе минуту в ней берет список тиков Сделок,
                         // используя как отправную временную точку на их основе создает окна срезы по 120 мин левого верхнего минутного графика
                        // на основе минут и мапы часов создает левый нижний часовой график сохраняет графики в массивы
            processLeftWindows(deepMinuteBarListl, mapMinKeyTickKeysTrades, hourBarKeyToIndexStructure);


        }
    }


    static Map<String,MyOneTrade> getMyTradeKeyDates(List<MyOneTrade> myOneTradeList){
        Map<String,MyOneTrade> res = new HashMap<>();
        myOneTradeList.forEach(c->res.put(c.keyDate,c));
        return res;
    }


    /*static KeyToIndexStructure<String,List<Double>> creatTickMap(){
        List<String> stringListTicks = readListStr(dok5);
        KeyToIndexStructure<String,List<Double>> mapTicks = new KeyToIndexStructure<>();
        int i=0;
        for(String ticks:stringListTicks){
            i++;
            //System.out.println(ticks);
            //if (i==10)break;
            String[] arrTicks = ticks.split(",");
            *//*long t = Long.parseLong(arrTicks[0]+arrTicks[1]);
            if(t==20260617115048l) System.out.println("dddd  "+arrTicks[2]);*//*
            //System.out.println(arrTicks[0]+arrTicks[1]);



            String key = arrTicks[0]+arrTicks[1];



            mapTicks.add(key,Double.parseDouble(arrTicks[2]));
        }
        return mapTicks;
    }*/

    static void creatMinutesFromTicks(){
        List<String> stringListTicks = readListStr(dok5);
        convertTicks(stringListTicks);
    }

    public static void main(String[] args) {
        //[BR-4.26, Si-3.26, USDRUBF, MXI-3.26, MXI-6.26]
        //[MIX-6.26, NG-4.26, Si-6.26, GOLD-6.26, MXI-6.26]
        //List<String[]> res = readFile();
        //List<String[]> trade = readCSV();
        //res.forEach(c-> System.out.println(Arrays.toString(c)));
        //System.out.println(getNamesInstruments(trade));
        //List<double[]> filter = getOneInstrument(trade,"USDRUBF");
        //Si-6.26   filter.add(new double[]{-1.0, 2.0, 76016.0, 0.0});
        //countInstrument(filter);
        countManyInstruments();
        //creatTickMap();
        //creatMinutesFromTicks();
    }

}

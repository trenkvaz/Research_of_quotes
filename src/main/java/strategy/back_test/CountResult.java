package strategy.back_test;

import org.json.JSONObject;
import strategy.OneTrade;

import java.util.ArrayList;
import java.util.List;

import static utilities.ReadWrite.deserialize;

public class CountResult {

    private List<Result> resultDayList = new ArrayList<>();
    private List<Result> resultMonthsList = new ArrayList<>();
    private int currentDate;
    private Result result;
    private Result generalResult = new Result();
    JSONObject meta_data;

    public static class Result {
        int day;
        int dayOfWeek;
        int month;
        int year;
        int amountLong;
        int amountShort;
        int winLong;
        int loseLong;
        int winShort;
        int loseShort;
        int amountWinLong;
        int amountLoseLong;
        int amountWinShort;
        int amountLoseShort;
        int result;
        int amountStopTrailing;
    }


    public JSONObject total(List<OneTrade> oneTradeList,JSONObject meta_data){
        this.meta_data = meta_data;
        setSettings();
        long s = System.currentTimeMillis();
        //serialize((Serializable) oneTradeList,"J:\\static_data_2\\JavaProjects\\Research_of_quotes\\src\\main\\resources\\back_test_trades\\trades1");
        //System.out.println("time saveToFile mills:"+(System.currentTimeMillis()-s));
        //s = System.currentTimeMillis();
        for (int i=0; i<oneTradeList.size(); i++){createDays(oneTradeList.get(i),(i==oneTradeList.size()-1));}
        //System.out.println("time createDays mills:"+(System.currentTimeMillis()-s));
        return displayResult();
    }

    public void totalFromFile(){
        long s = System.currentTimeMillis();
            List<OneTrade> oneTradeList = deserialize("J:\\static_data_2\\JavaProjects\\Research_of_quotes\\src\\main\\resources\\back_test_trades\\trades1");
            System.out.println("time loadFromFile mills:"+(System.currentTimeMillis()-s));
            for (int i=0; i<oneTradeList.size(); i++){createDays(oneTradeList.get(i),(i==oneTradeList.size()-1));}
            displayResult();
    }
//"20260501095932"

    boolean isAllowCountToday;
    int stopLoseDay;

    private void setSettings(){
        stopLoseDay = meta_data.getJSONObject("situation1").getInt("STOP_LOSE")*10*-1;
    }

    private void createDays(OneTrade oneTrade,boolean isStop){
        int day = oneTrade.entryDate;
        if(currentDate!=day){
            // завершение дня
            if(result !=null) {
                resultDayList.add(result);
                setGeneralResult(result);
            }
            //if(!currentDay.isEmpty()){}
            // начало нового дня
            currentDate = day;
            result = new Result();
            result.dayOfWeek = oneTrade.getDayNumber();
            result.day = day;
            result.month = day/100;
            result.year = day/10000;
            isAllowCountToday = true;
        }

        if(isAllowCountToday){
            if(oneTrade.longOrShort==1){
                result.amountLong++;
                if(oneTrade.result>0){
                    result.winLong+=oneTrade.result;
                    result.amountWinLong++;
                }
                else {
                    result.loseLong+=oneTrade.result;
                    result.amountLoseLong++;
                }
            } else {
                result.amountShort++;
                if(oneTrade.result>0){
                    result.winShort+=oneTrade.result;
                    result.amountWinShort++;
                }
                else {
                    result.loseShort+=oneTrade.result;
                    result.amountLoseShort++;
                }
            }
            if(oneTrade.isStopTrailing){
                result.amountStopTrailing++;
            }
            result.result += oneTrade.result;
            countStreakLose(oneTrade.result);

            if(result.result<=stopLoseDay)isAllowCountToday=false;

        }


        if(isStop){
            resultDayList.add(result);
            setGeneralResult(result);
        }
    }

    void createMonths(){
        currentDate = 0;
        result = null;
        for (int i=0; i<resultDayList.size(); i++){
            Result resultDay = resultDayList.get(i);

            if(currentDate!=resultDay.month||i==resultDayList.size()-1){
                // завершение дня
                if(result !=null) resultMonthsList.add(result);
                //if(!currentDay.isEmpty()){}
                // начало нового дня
                currentDate = resultDay.month;
                result = new Result();
                result.month = resultDay.month;
                result.year = resultDay.year;
            }

            assert result != null;

            result.result += resultDay.result;
            result.amountLong += resultDay.amountLong;
            result.amountShort += resultDay.amountShort;
            result.winLong += resultDay.winLong;
            result.loseLong += resultDay.loseLong;
            result.winShort += resultDay.winShort;
            result.loseShort += resultDay.loseShort;
        }
    }

    void setGeneralResult(Result resultDay){
        generalResult.result += resultDay.result;
        generalResult.amountLong += resultDay.amountLong;
        generalResult.amountShort += resultDay.amountShort;
        generalResult.winLong += resultDay.winLong;
        generalResult.loseLong += resultDay.loseLong;
        generalResult.winShort += resultDay.winShort;
        generalResult.loseShort += resultDay.loseShort;

        generalResult.amountWinLong += resultDay.amountWinLong;
        generalResult.amountLoseLong += resultDay.amountLoseLong;
        generalResult.amountWinShort += resultDay.amountWinShort;
        generalResult.amountLoseShort += resultDay.amountLoseShort;
        if(generalResult.year==0)generalResult.year = resultDay.year;
        generalResult.amountStopTrailing +=resultDay.amountStopTrailing;

    }


    JSONObject displayResult(){
        JSONObject total = new JSONObject();
        int totalAmountLose = generalResult.amountLoseLong + generalResult.amountLoseShort;
        int comiss = totalAmountLose*20;
        total.put("YEAR",generalResult.year);
        total.put("totalTrades",(generalResult.amountLong+generalResult.amountShort));
        total.put("RESULT",generalResult.result-comiss);
        //total.put("comiss",comiss);

        //total.put("winLong",generalResult.winLong);
        //total.put("winShort",generalResult.winShort);

        double p = ((double) generalResult.amountStopTrailing /totalAmountLose)*100;
        //System.out.println("generalResult.amountStopTrailing "+generalResult.amountStopTrailing+"  totalAmountLose "+totalAmountLose+" p "+p);
        p = Math.round(p * 10.0) / 10.0;
        total.put("procent_stoptrailing",p);

        total.put("maxStreakLose",maxStreakLose);
        /*total.put("medianWinLong",(generalResult.winLong/ generalResult.amountWinLong));
        total.put("medianWinShort",(generalResult.winShort/ generalResult.amountWinShort));
        total.put("medianLoseLong",(generalResult.loseLong/ generalResult.amountLoseLong));
        total.put("medianLoseShort",(generalResult.loseShort/ generalResult.amountLoseShort));*/
        //total.put("medianWinTrade",((generalResult.winShort+generalResult.winLong)/ (generalResult.amountWinLong+generalResult.amountWinShort)));
        //total.put("medianLoseTrade",((generalResult.loseShort+generalResult.loseLong)/ (generalResult.amountLoseShort+generalResult.amountLoseLong)));
        // соотношение должно быть пропорционально стоп лоссам - стоп лосс/ стоп профит стоп
        total.put("win/lose",((double)(generalResult.amountWinLong+generalResult.amountWinShort)/ (generalResult.amountLoseShort+generalResult.amountLoseLong)));
        // эквиваленто стоп лосс/ стоп профит стоп меньше профит больше
        total.put("medianLoseTrade/medianWinTrade",((double)(generalResult.loseShort+generalResult.loseLong)/ (generalResult.amountLoseShort+generalResult.amountLoseLong))/((double)(generalResult.winShort+generalResult.winLong)/ (generalResult.amountWinLong+generalResult.amountWinShort)));
        return total;
    }

    int maxResult = 0;
    int currentResult = 0;
    int maxStreakLose = 0;
    int currentLose = 0;

    private void countStreakLose(int result){
        currentResult+=result;
        if(currentResult>maxResult){
            maxResult = currentResult;
            if(maxStreakLose<currentLose)maxStreakLose = currentLose;
            currentLose = 0;
        } else if(currentResult<maxResult){
            currentLose = maxResult-currentResult;
            if(maxStreakLose<currentLose)maxStreakLose = currentLose;
        }
        //System.out.println("result:"+result+" currentResult:"+currentResult+" maxResult:"+maxResult+" currentLose:"+currentLose);
    }

    /*public void saveToFile(List<OneTrade> list, String filePath) {
        try (FileOutputStream fileOut = new FileOutputStream(filePath);
             ObjectOutputStream out = new ObjectOutputStream(fileOut)) {

            out.writeObject(list); // Записываем весь список целиком одной командой
            System.out.println("Список успешно сохранен!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<OneTrade> loadFromFile(String filePath) {
        try (FileInputStream fileIn = new FileInputStream(filePath);
             ObjectInputStream in = new ObjectInputStream(fileIn)) {

            // Читаем объект и приводим его к нужному типу списка
            return (List<OneTrade>) in.readObject();

        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>(); // Возвращаем пустой список в случае ошибки
        }
    }*/
}

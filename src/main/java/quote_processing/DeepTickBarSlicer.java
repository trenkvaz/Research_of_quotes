package quote_processing;

import org.json.JSONArray;
import org.json.JSONObject;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static utilities.WriteReadJSON.writeArrayJSON;

public class DeepTickBarSlicer {

    // Класс для хранения сырого тика в памяти внутри минуты
    public static class RawTick {
        String fullDateTimeStr; // Напр: "20260501095932"
        double price;
        int volume;
        String type; // "buy" или "sell"

        RawTick(String fullDateTimeStr, double price, int volume, String type) {
            this.fullDateTimeStr = fullDateTimeStr;
            this.price = price;
            this.volume = volume;
            this.type = type;
        }
    }

    // Структура минутной свечи, которая хранит внутри себя все свои тики
    public static class DeepMinuteBar {
        long timestamp;
        double open = -1;
        double high = Double.MIN_VALUE;
        double low = Double.MAX_VALUE;
        double close = -1;
        int buyVolume = 0;
        int sellVolume = 0;
        String minuteKey;

        // Массив ВСЕХ тиков, которые упали в эту конкретную минуту
        List<RawTick> allTicksInMinute = new ArrayList<>();

        DeepMinuteBar(long timestamp) {
            this.timestamp = timestamp;
        }
    }

    public static Map<String,List<String>> sliceAndSaveWithIncompleteBar(List<String> rawTicks, Map<String, CheckMyTrading.MyOneTrade> map) {
        Map<String, DeepMinuteBar> minutesMap = new TreeMap<>();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
        Set<String> myTrades  = map.keySet();
        String dublicat = "";
        Map<String,List<String>> mapMinKeyTickKeysTrades = new HashMap<>();
        int c = 0;
        // ШАГ 1: Группируем тики по минутам и складываем СЫРЫЕ ТИКИ внутрь каждой минуты
        for (String row : rawTicks) {
            if (row == null || row.trim().isEmpty()) continue;
            String[] parts = row.split(",");
            if (parts.length < 5) continue;

            String dateStr = parts[0];       // 20260501
            String timeStr = parts[1];       // 095932
            double price = Double.parseDouble(parts[2]);
            int volume = Integer.parseInt(parts[3]);
            String type = parts[4].trim().toLowerCase();

            String minuteKey = dateStr + timeStr.substring(0, 4);   // "202605010959"
            String fullDateTimeStr = dateStr + timeStr;             // "20260501095932"

            minutesMap.computeIfAbsent(minuteKey, k -> {
                LocalDateTime ldt = LocalDateTime.parse(minuteKey, timeFormatter);
                return new DeepMinuteBar(ldt.toEpochSecond(ZoneOffset.UTC));
            });

            DeepMinuteBar bar = minutesMap.get(minuteKey);

            // Собираем базовые полные параметры (для стандартной истории)
            if (bar.open == -1) bar.open = price;
            if (price > bar.high) bar.high = price;
            if (price < bar.low) bar.low = price;
            bar.close = price;

            if ("buy".equals(type)) bar.buyVolume += volume;
            else if ("sell".equals(type)) bar.sellVolume += volume;

            // СКЛАДЫВАЕМ ТИК В МАССИВ МИНУТЫ
            bar.allTicksInMinute.add(new RawTick(fullDateTimeStr, price, volume, type));

            bar.minuteKey = minuteKey;
        }

        List<DeepMinuteBar> chronologicalBars = new ArrayList<>(minutesMap.values());
        JSONArray priceWindowsArray = new JSONArray();
        JSONArray deltaWindowsArray = new JSONArray();

        int globalCumulativeDelta = 0;

        // ШАГ 2: Итерируемся по хронологии для создания 120-минутных окон
        for (int i = 0; i < chronologicalBars.size(); i++) {
            DeepMinuteBar currentBar = chronologicalBars.get(i);

            int currentMinuteDelta = currentBar.buyVolume - currentBar.sellVolume;
            globalCumulativeDelta += currentMinuteDelta;

            // Проверяем, есть ли в тиках этой минуты ХОТЯ БЫ ОДНА ваша сделка
            RawTick matchingTradeTick = null;
            for (RawTick tick : currentBar.allTicksInMinute) {
               /* if (myTrades.contains(tick.fullDateTimeStr)) {
                    matchingTradeTick = tick; // Нашли тик вашей сделки
                    break;
                }*/
           // }






            // ЕСЛИ СДЕЛКА НАЙДЕНА — ДЕЛАЕМ ИЗОЛИРОВАННОЕ ОКНО
           // if (matchingTradeTick != null) {

                if(dublicat.equals(tick.fullDateTimeStr+tick.price))continue;
                if (!myTrades.contains(tick.fullDateTimeStr))continue;
                // 3. Если в эту секунду была ваша сделка, сохраняем объект MyTrade
                //if (myTrades.contains(exactSecondKey)) {
                CheckMyTrading.MyOneTrade myOneTrade = map.get(tick.fullDateTimeStr);
                    if(tick.price==myOneTrade.open){
                        System.out.println("myTrades.contains(exactSecondKey) "+tick.fullDateTimeStr+" tick price "+tick.price);
                        c++;
                        dublicat = tick.fullDateTimeStr+tick.price;
                    } else {
                        System.out.println("ERROR "+tick.fullDateTimeStr);
                        continue;
                    }
                matchingTradeTick = tick;


                mapMinKeyTickKeysTrades.computeIfAbsent(currentBar.minuteKey, k -> new ArrayList<>());

                mapMinKeyTickKeysTrades.get(currentBar.minuteKey).add(tick.fullDateTimeStr);




                int startIdx = Math.max(0, i - 119); // Забираем 120 минут истории включая текущую

                JSONArray singlePriceWindow = new JSONArray();
                JSONArray singleDeltaWindow = new JSONArray();

                // Восстанавливаем дельту с начала этого конкретного окна
                int runningDeltaForWindow = globalCumulativeDelta - currentMinuteDelta;

                // 2а. Кладим в окно 119 чистых полных предыдущих минут
                for (int j = startIdx; j < i; j++) {
                    DeepMinuteBar histBar = chronologicalBars.get(j);
                    singlePriceWindow.put(createStandardBarJson(histBar));

                    int hDelta = histBar.buyVolume - histBar.sellVolume;
                    singleDeltaWindow.put(createDeltaBarJson(histBar.timestamp, runningDeltaForWindow, runningDeltaForWindow + hDelta));
                    runningDeltaForWindow += hDelta;
                }

                // 2б. СИМУЛЯЦИЯ НЕПОЛНОЙ 120-Й СВЕЧИ (Фильтруем тики строго до сделки)
                double incOpen = -1;
                double incHigh = Double.MIN_VALUE;
                double incSubLow = Double.MAX_VALUE;
                int incBuyVol = 0;
                int incSellVol = 0;

                String tradeCutoffTime = matchingTradeTick.fullDateTimeStr;

                for (RawTick t : currentBar.allTicksInMinute) {
                    // Сравниваем время тика со временем сделки текстовой сортировкой (работает идеально хронологически)
                    if (t.fullDateTimeStr.compareTo(tradeCutoffTime) <= 0) {
                        if (incOpen == -1) incOpen = t.price;
                        if (t.price > incHigh) incHigh = t.price;
                        if (t.price < incSubLow) incSubLow = t.price;

                        if ("buy".equals(t.type)) incBuyVol += t.volume;
                        else if ("sell".equals(t.type)) incSellVol += t.volume;
                    }
                }

                // Пересчитываем неполную цену (Close равен точной цене сделки)
                JSONObject incompletePriceObj = new JSONObject();
                incompletePriceObj.put("time", currentBar.timestamp);
                incompletePriceObj.put("open", incOpen);
                incompletePriceObj.put("high", incHigh);
                incompletePriceObj.put("low", incSubLow);
                incompletePriceObj.put("close", matchingTradeTick.price);

                // Передаем саму сделку внутрь для отрисовки горизонтального красного уровня на JS стороне
                JSONArray tradesArray = new JSONArray();
                JSONObject tObj = new JSONObject();
                tObj.put("price", matchingTradeTick.price);
                tObj.put("typeTrade", myOneTrade.typeTrade);
                tObj.put("result", myOneTrade.result);
                tObj.put("date", myOneTrade.date);
                tObj.put("possibleMin", myOneTrade.possibleMin);
                tObj.put("possibleMax", myOneTrade.possibleMax);
                tradesArray.put(tObj);
                incompletePriceObj.put("myTrades", tradesArray);

                singlePriceWindow.put(incompletePriceObj);

                // Пересчитываем неполную дельту для этой финальной минуты
                int incompleteMinuteDelta = incBuyVol - incSellVol;
                singleDeltaWindow.put(createDeltaBarJson(currentBar.timestamp, runningDeltaForWindow, runningDeltaForWindow + incompleteMinuteDelta));

                // Сохраняем готовые окна в итоговый массив
                priceWindowsArray.put(singlePriceWindow);
                deltaWindowsArray.put(singleDeltaWindow);
            }
        }

        System.out.println("TOTAL TRADES "+c);

        writeArrayJSON(priceWindowsArray,"J:\\static_data_2\\JavaProjects\\Research_of_quotes\\src\\main\\resources\\jsons_data\\_trade_26_08\\priceSeriesArray_range.json");
        writeArrayJSON(deltaWindowsArray,"J:\\static_data_2\\JavaProjects\\Research_of_quotes\\src\\main\\resources\\jsons_data\\_trade_26_08\\deltaSeriesArray_range.json");
        /*// Записываем финальный результат
        JSONObject rootJson = new JSONObject();
        rootJson.put("priceWindows", priceWindowsArray);
        rootJson.put("deltaWindows", deltaWindowsArray);

        try {
            java.nio.file.Files.writeString(java.nio.file.Paths.get("./data.json"), rootJson.toString());
            System.out.println("[Успех] Окна с неполной 120-й свечой сформированы. Всего: " + priceWindowsArray.length());
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        return mapMinKeyTickKeysTrades;
    }

    private static JSONObject createStandardBarJson(DeepMinuteBar bar) {
        JSONObject obj = new JSONObject();
        obj.put("time", bar.timestamp);
        obj.put("open", bar.open);
        obj.put("high", bar.high);
        obj.put("low", bar.low);
        obj.put("close", bar.close);
        obj.put("myTrades", new JSONArray()); // Для истории массив сделок пустой
        return obj;
    }

    private static JSONObject createDeltaBarJson(long timestamp, int openD, int closeD) {
        JSONObject obj = new JSONObject();
        obj.put("time", timestamp);
        obj.put("open", openD);
        obj.put("high", Math.max(openD, closeD));
        obj.put("low", Math.min(openD, closeD));
        obj.put("close", closeD);
        return obj;
    }
}


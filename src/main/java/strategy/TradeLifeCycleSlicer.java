package strategy;

import org.json.JSONArray;
import org.json.JSONObject;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static utilities.WriteReadJSON.writeArrayJSON;

public class TradeLifeCycleSlicer {

    // Обновленный класс сделки: теперь храним и цену входа, и цену выхода


    public static class MinuteBar {
        public long timestamp;
        public String minuteKey;
        public int open = -1;
        public int high = Integer.MIN_VALUE;
        public int low = Integer.MAX_VALUE;
        public int close = -1;

        MinuteBar(long timestamp, String minuteKey) {
            this.timestamp = timestamp;
            this.minuteKey = minuteKey;
        }
    }

    public static void sliceTradeLifeCycle(List<String> rawPriceRows,int[]arrayTicks, List<OneTrade> myTrades) {
        Map<String, MinuteBar> minutesMap = new TreeMap<>();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

        // 1. Сборка стандартных минут (без вкрапления сделок)
        for (int i=0; i<arrayTicks.length; i+=3)
        //for (String row : rawPriceRows)
        {
            /*if (row == null || row.trim().isEmpty()) continue;
            String[] parts = row.split(",");
            if (parts.length < 5) continue;*/
            String dateStr = String.valueOf(arrayTicks[i]);
            String timeStr = String.valueOf(arrayTicks[i+1]);
            if(timeStr.length()<6)timeStr="0"+timeStr;
            int price = arrayTicks[i+2];
            try {



                String minuteKey = dateStr + timeStr.substring(0, 4);

                minutesMap.computeIfAbsent(minuteKey, k -> {
                    LocalDateTime ldt = LocalDateTime.parse(minuteKey, timeFormatter);
                    return new MinuteBar(ldt.toEpochSecond(ZoneOffset.UTC), minuteKey);
                });

                MinuteBar bar = minutesMap.get(minuteKey);
                if (bar.open == -1) bar.open = price;
                if (price > bar.high) bar.high = price;
                if (price < bar.low) bar.low = price;
                bar.close = price;
            } catch (Exception e) {
                System.out.println("timeStr "+timeStr+" dateStr "+dateStr+" price "+price);
                throw new RuntimeException(e);
            }

        }

        List<MinuteBar> chronologicalBars = new ArrayList<>(minutesMap.values());
        Map<String, Integer> indexMap = new HashMap<>();
        for (int i = 0; i < chronologicalBars.size(); i++) {
            indexMap.put(chronologicalBars.get(i).minuteKey, i);
        }

        // Главный массив окон для итогового JSON
        JSONArray windowsArray = new JSONArray();

        // 2. Нарезка независимых окон под каждую сделку
        for (OneTrade trade : myTrades) {
            String entryMinKey = trade.entryDateTime().substring(0, 12);
            String exitMinKey = trade.exitDateTime().substring(0, 12);

            if (indexMap.containsKey(entryMinKey) && indexMap.containsKey(exitMinKey)) {
                int entryIdx = indexMap.get(entryMinKey);
                int exitIdx = indexMap.get(exitMinKey);

                int startIdx = Math.max(0, entryIdx - 119);

                // А. Массив чистых свечей цены (без вложенных сделок)
                JSONArray priceDataArray = new JSONArray();
                for (int j = startIdx; j <= exitIdx; j++) {
                    MinuteBar bar = chronologicalBars.get(j);
                    JSONObject barObj = new JSONObject();
                    barObj.put("time", bar.timestamp);
                    barObj.put("open", bar.open);
                    barObj.put("high", bar.high);
                    barObj.put("low", bar.low);
                    barObj.put("close", bar.close);
                    priceDataArray.put(barObj);
                }
                MinuteBar entryBar = chronologicalBars.get(entryIdx);
                // Б. Отдельный изолированный объект с описанием сделки
                JSONObject tradeInfoObj = new JSONObject();
                tradeInfoObj.put("entryPrice", trade.entryPrice);
                tradeInfoObj.put("exitPrice", trade.exitPrice);
                tradeInfoObj.put("entryDateTime", trade.entryDateTime());
                tradeInfoObj.put("exitDateTime", trade.exitDateTime());
                tradeInfoObj.put("typeTrade", trade.getLongOrShortStr());
                tradeInfoObj.put("result", trade.result);
                tradeInfoObj.put("outPutDateTrade", trade.getBeautyEntryDateTime());
                tradeInfoObj.put("entryTimestamp", entryBar.timestamp);

                // В. Собираем всё в один единый JSONObject для текущего кадра/окна
                JSONObject windowPayload = new JSONObject();
                windowPayload.put("priceData", priceDataArray);     // Чистые свечи цены отдельно
                windowPayload.put("tradeInfo", tradeInfoObj);       // Описание сделки отдельно

                // (Сюда же при необходимости вы допишете массивы для дельты или левого коррелята)
                // windowPayload.put("deltaData", ...);

                windowsArray.put(windowPayload);
            }
        }



        try {
            writeArrayJSON(windowsArray,"J:\\static_data_2\\JavaProjects\\Research_of_quotes\\src\\main\\resources\\jsons_data\\test_trades_imoex_2019\\priceSeriesArray_range.json");
            System.out.println("[Успех] Сформирована чистая структура окон. Всего окон: " + windowsArray.length());
        } catch (Exception e) {
            e.printStackTrace();
        }
        }
}


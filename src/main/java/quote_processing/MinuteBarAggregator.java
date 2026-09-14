package quote_processing;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static utilities.WriteReadJSON.writeArrayJSON;

public class MinuteBarAggregator {

    // Класс для хранения точных данных о вашей сделке
    public static class MyTrade {
        public final double price;
        public final int second; // Секунда внутри минуты (0-59)
        public final String typeTrade;
        public final double result;
        public final String date;

        public MyTrade(double price, int second, String typeTrade, double result, String date) {
            this.price = price;
            this.second = second;
            this.typeTrade = typeTrade;
            this.result = result;
            this.date = date;
        }

        @Override
        public String toString() {
            return String.format(Locale.US, "{цена: %.2f, сек: %02d}", price, second);
        }
    }

    // Класс структуры минутной свечи
    public static class MinuteBar {
        public long timestamp; // Unix-время начала минуты (в секундах)
        public double open = -1;
        public double high = Double.MIN_VALUE;
        public double low = Double.MAX_VALUE;
        public double close = -1;
        public int buyVolume = 0;
        public int sellVolume = 0;

        // Массив объектов ваших сделок (содержат цену и секунду)
        public List<MyTrade> myTrades = new ArrayList<>();

        MinuteBar(long timestamp) {
            this.timestamp = timestamp;
        }
    }

    /**
     * Конвертирует сырые тики в минутные свечи и привязывает к ним сделки с секундами
     * @param rawTicks Список тиковых строк из файла
     //* @param myTrades Сет временных меток ваших сделок (формат: "20260501095932")
     * @return Список сгруппированных минутных баров
     */
    public static List<MinuteBar> aggregateTicks(List<String> rawTicks,Map<String, CheckMyTrading.MyOneTrade> map) {

        Set<String> myTrades  = map.keySet();
        Map<String, MinuteBar> minutesMap = new TreeMap<>();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
        String dublicat = "";
        int c = 0;
        for (String row : rawTicks) {
            if (row == null || row.trim().isEmpty()) continue;
            String[] parts = row.split(",");
            if (parts.length < 5) continue;

            String dateStr = parts[0];       // "20260501"
            String timeStr = parts[1];       // "095932"
            double price = Double.parseDouble(parts[2]);
            int volume = Integer.parseInt(parts[3]);
            String type = parts[4].trim().toLowerCase();

            // Ключ минуты: "202605010959"
            String minuteKey = dateStr + timeStr.substring(0, 4);

            // Ключ секунды для проверки вашей сделки: "20260501095932"
            String exactSecondKey = dateStr + timeStr;

            // Вытаскиваем секунду как число (из строки "095932" берем последние два символа "32")
            int second = Integer.parseInt(timeStr.substring(4, 6));

            minutesMap.computeIfAbsent(minuteKey, k -> {
                LocalDateTime ldt = LocalDateTime.parse(minuteKey, timeFormatter);
                long epochSeconds = ldt.toEpochSecond(ZoneOffset.UTC);
                return new MinuteBar(epochSeconds);
            });

            MinuteBar bar = minutesMap.get(minuteKey);

            // 1. Агрегируем OHLC цены
            if (bar.open == -1) bar.open = price;
            if (price > bar.high) bar.high = price;
            if (price < bar.low) bar.low = price;
            bar.close = price;

            // 2. Накапливаем объемы
            if ("buy".equals(type)) {
                bar.buyVolume += volume;
            } else if ("sell".equals(type)) {
                bar.sellVolume += volume;
            }

            if(dublicat.equals(exactSecondKey+price))continue;
            // 3. Если в эту секунду была ваша сделка, сохраняем объект MyTrade
            if (myTrades.contains(exactSecondKey)) {

                if(price==map.get(exactSecondKey).open){
                    System.out.println("myTrades.contains(exactSecondKey) "+exactSecondKey+" tick price "+price+" my price "+map.get(exactSecondKey).open);
                    c++;

                    bar.myTrades.add(new MyTrade(price,
                            second,map.get(exactSecondKey).typeTrade,map.get(exactSecondKey).result,map.get(exactSecondKey).date));
                    dublicat = exactSecondKey+ price;
                }

            }
        }
        System.out.println("total trades "+c);
        return new ArrayList<>(minutesMap.values());
    }

    public static void buildJsonOutput(List<MinuteBar> processedBars, Map<String, String> minuteKeysMap) {
        // Создаем главные массивы для графиков
        JSONArray priceSeriesArray = new JSONArray();
        JSONArray deltaSeriesArray = new JSONArray();
        JSONArray markersArray = new JSONArray(); // Маркеры для TradingView
        JSONArray priceWindowsArray = new JSONArray();
        JSONArray deltaWindowsArray = new JSONArray();
        int cumulativeDelta = 0;
        LocalDate lastDate = null;
        DateTimeFormatter dateParser = DateTimeFormatter.ofPattern("yyyyMMdd");

        // Итерируемся по нашим минутным барам
        // (Для демонстрации смены дней используем восстановление ключа минуты из итерации)
        int index = 0;
        for (MinuteBar bar : processedBars) {

            // 1. Формируем JSONObject для свечи ЦЕНЫ
            JSONObject priceObj = new JSONObject();
            priceObj.put("time", bar.timestamp);
            priceObj.put("open", bar.open);
            priceObj.put("high", bar.high);
            priceObj.put("low", bar.low);
            priceObj.put("close", bar.close);

            // Вкладываем НАШИ СДЕЛКИ прямо внутрь объекта этой минутной свечи
            JSONArray tradesInBarArray = new JSONArray();
            for (MyTrade trade : bar.myTrades) {
                JSONObject tradeObj = new JSONObject();
                tradeObj.put("price", trade.price);
                tradeObj.put("second", trade.second);
                tradeObj.put("typeTrade", trade.typeTrade);
                tradeObj.put("result", trade.result);
                tradesInBarArray.put(tradeObj);

                // Попутно создаем маркер-стрелочку для TradingView
                /*JSONObject markerObj = new JSONObject();
                markerObj.put("time", bar.timestamp);
                markerObj.put("position", "belowBar");
                markerObj.put("color", "#2962FF");
                markerObj.put("shape", "arrowUp");
                markerObj.put("text", String.format(Locale.US, "Сделка (%02dс): %.2f", trade.second, trade.price));
                markersArray.put(markerObj);*/
            }
            priceObj.put("myTrades", tradesInBarArray); // Положили массив сделок внутрь свечи
            priceSeriesArray.put(priceObj);             // Положили свечу в общий массив цен

            // 2. Расчет кумулятивной дельты
            int minuteDelta = bar.buyVolume - bar.sellVolume;
            int oldDelta = cumulativeDelta;
            cumulativeDelta += minuteDelta;

            int openDelta = oldDelta;
            int closeDelta = cumulativeDelta;
            int highDelta = Math.max(openDelta, closeDelta);
            int lowDelta = Math.min(openDelta, closeDelta);

            // 3. Формируем JSONObject для свечи ДЕЛЬТЫ
            JSONObject deltaObj = new JSONObject();
            deltaObj.put("time", bar.timestamp);
            deltaObj.put("open", openDelta);
            deltaObj.put("high", highDelta);
            deltaObj.put("low", lowDelta);
            deltaObj.put("close", closeDelta);
            deltaSeriesArray.put(deltaObj);



            if (!bar.myTrades.isEmpty()) {

                // Вычисляем индекс начала окна (максимум 120 минут назад или с самого начала истории)
                int startIdx = Math.max(0,  priceSeriesArray.length() - 120);

                JSONArray singlePriceWindow = new JSONArray();
                JSONArray singleDeltaWindow = new JSONArray();

                // Копируем последние <=120 минут в текущее независимое окно
                for (int j = startIdx; j <  priceSeriesArray.length(); j++) {
                    singlePriceWindow.put( priceSeriesArray.get(j));
                    singleDeltaWindow.put(deltaSeriesArray.get(j));
                }

                // Кладим получившееся окно (массив минут) в главный массив окон
                priceWindowsArray.put(singlePriceWindow);
                deltaWindowsArray.put(singleDeltaWindow);
            }
        }


        // Превращаем JSONArray в готовые строки для передачи в JavaScript/Puppeteer
        /*String priceDataJsonStr = priceSeriesArray.toString();
        String deltaDataJsonStr = deltaSeriesArray.toString();
        String markersJsonStr = markersArray.toString();*/

        writeArrayJSON(priceWindowsArray,"J:\\static_data_2\\JavaProjects\\Research_of_quotes\\src\\main\\resources\\jsons_data\\priceSeriesArray_range.json");
        writeArrayJSON(deltaWindowsArray,"J:\\static_data_2\\JavaProjects\\Research_of_quotes\\src\\main\\resources\\jsons_data\\deltaSeriesArray_range.json");
        //writeArrayJSON(markersArray,"J:\\static_data_2\\JavaProjects\\Research_of_quotes\\src\\main\\resources\\jsons_data\\markersArray.json");
        // Проверяем валидный структурированный вывод в консоли
        System.out.println("МИНУТЫ С ЦЕНАМИ И СДЕЛКАМИ ВНУТРИ:");
        //System.out.println(priceDataJsonStr);
        System.out.println("\nМАРКЕРЫ ДЛЯ СТРЕЛОЧЕК:");
        //System.out.println(markersJsonStr);
    }

    public static void sliceAndSaveData(List<MinuteBar> processedBars) {
        // Списки для хранения всей последовательной истории объектов-минут
        List<MinuteBar> chronologicalBars = new ArrayList<>();

        // Главные массивы массивов (массивы окон) для итогового JSON
        JSONArray priceWindowsArray = new JSONArray();
        JSONArray deltaWindowsArray = new JSONArray();

        int cumulativeDelta = 0;

        // Идем по хронологии всех минутных баров
        for (MinuteBar currentBar : processedBars) {
            // Накапливаем бары в общую историю
            chronologicalBars.add(currentBar);

            // Считаем кумулятивную дельту для текущего шага
            int minuteDelta = currentBar.buyVolume - currentBar.sellVolume;
            cumulativeDelta += minuteDelta;

            // ЕСЛИ В ТЕКУЩЕЙ МИНУТЕ ЕСТЬ СДЕЛКИ — ОБРАБАТЫВАЕМ КАЖДУЮ ОТДЕЛЬНО
            if (!currentBar.myTrades.isEmpty()) {

                // Вычисляем индекс начала истории для окна (120 минут назад)
                int startIdx = Math.max(0, chronologicalBars.size() - 120);

                // Бежим по каждой сделке внутри этой минуты индивидуально!
                for (MyTrade individualTrade : currentBar.myTrades) {

                    JSONArray singlePriceWindow = new JSONArray();
                    JSONArray singleDeltaWindow = new JSONArray();

                    // Переменная для воссоздания исторической кумулятивной дельты внутри этого конкретного окна
                    int runningDeltaForWindow = cumulativeDelta - minuteDelta;

                    // Копируем предысторию (все бары до текущей минуты)
                    for (int j = startIdx; j < chronologicalBars.size() - 1; j++) {
                        MinuteBar histBar = chronologicalBars.get(j);

                        // Добавляем исторический бар цены (без сделок)
                        JSONObject priceObj = createPriceJsonObject(histBar, Collections.emptyList());
                        singlePriceWindow.put(priceObj);

                        // Добавляем исторический бар дельты
                        int hDelta = histBar.buyVolume - histBar.sellVolume;
                        singleDeltaWindow.put(createDeltaJsonObject(histBar, runningDeltaForWindow, runningDeltaForWindow + hDelta));
                        runningDeltaForWindow += hDelta;
                    }

                    // Формируем ОСОБЫЙ последний бар для этого окна (120-я минута)
                    // В него мы кладем ТОЛЬКО ОДНУ эту конкретную сделку
                    JSONObject finalPriceObj = createPriceJsonObject(currentBar, List.of(individualTrade));
                    singlePriceWindow.put(finalPriceObj);

                    // Добавляем последний бар дельты
                    singleDeltaWindow.put(createDeltaJsonObject(currentBar, runningDeltaForWindow, runningDeltaForWindow + minuteDelta));

                    // Кладим получившиеся изолированные 120-минутные массивы в общую структуру
                    priceWindowsArray.put(singlePriceWindow);
                    deltaWindowsArray.put(singleDeltaWindow);
                }
            }
        }

        // Упаковываем массивы окон в один корневой JSON
        /*JSONObject rootJson = new JSONObject();
        rootJson.put("priceWindows", priceWindowsArray);
        rootJson.put("deltaWindows", deltaWindowsArray);

        try {
            java.nio.file.Files.writeString(java.nio.file.Paths.get("./data.json"), rootJson.toString());
            System.out.println("[Успех] Каждая сделка изолирована. Всего сгенерировано окон: " + priceWindowsArray.length());
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        writeArrayJSON(priceWindowsArray,"J:\\static_data_2\\JavaProjects\\Research_of_quotes\\src\\main\\resources\\jsons_data\\priceSeriesArray_range.json");
        writeArrayJSON(deltaWindowsArray,"J:\\static_data_2\\JavaProjects\\Research_of_quotes\\src\\main\\resources\\jsons_data\\deltaSeriesArray_range.json");
    }

    // Хелпер для создания JSONObject цены
    private static JSONObject createPriceJsonObject(MinuteBar bar, List<MyTrade> trades) {
        JSONObject obj = new JSONObject();
        obj.put("time", bar.timestamp);
        obj.put("open", bar.open);
        obj.put("high", bar.high);
        obj.put("low", bar.low);
        obj.put("close", bar.close);

        JSONArray tradesArray = new JSONArray();
        for (MyTrade t : trades) {
            JSONObject tObj = new JSONObject();
            tObj.put("price", t.price);
            tObj.put("second", t.second);
            tObj.put("typeTrade", t.typeTrade);
            tObj.put("result", t.result);
            tObj.put("date", t.date);
            tradesArray.put(tObj);
        }
        obj.put("myTrades", tradesArray);
        return obj;
    }

    // Хелпер для создания JSONObject дельты
    private static JSONObject createDeltaJsonObject(MinuteBar bar, int openD, int closeD) {
        JSONObject obj = new JSONObject();
        obj.put("time", bar.timestamp);
        obj.put("open", openD);
        obj.put("high", Math.max(openD, closeD));
        obj.put("low", Math.min(openD, closeD));
        obj.put("close", closeD);
        return obj;
    }




    public static void main(String[] args) {
        /*List<String> rawTicks = Arrays.asList(
                "20260501,095932,268750,1,Buy,0,",
                "20260501,095945,268800,2,Buy,0,", // Сделка №1 (45-я секунда)
                "20260501,095958,268780,1,Buy,0,", // Сделка №2 (58-я секунда)
                "20260501,100002,268700,5,Sell,0,",
                "20260501,100015,268650,3,Sell,0,"
        );

        Set<String> myTrades = new HashSet<>();
        myTrades.add("20260501095945");
        myTrades.add("20260501095958");

        List<MinuteBar> processedBars = aggregateTicks(rawTicks, myTrades);

        for (MinuteBar bar : processedBars) {
            System.out.println("----------------------------------------");
            System.out.println("Минута (Unix Time): " + bar.timestamp);
            System.out.println(String.format("OHLC цены: O:%.2f | H:%.2f | L:%.2f | C:%.2f", bar.open, bar.high, bar.low, bar.close));
            System.out.println("Мои сделки в этой минуте: " + bar.myTrades);
        }*/
    }
}

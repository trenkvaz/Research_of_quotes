package quote_processing;

import org.json.JSONArray;
import org.json.JSONObject;
import utilities.KeyToIndexStructure;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static utilities.WriteReadJSON.writeArrayJSON;

public class LeftChartDataPreparer {

    // 1. Структура сырого тика для левого графика (без лишних полей)
    public static class RawTick {
        public String fullDateTimeStr; // Напр: "20260501095932"
        public double price;

        public RawTick(String fullDateTimeStr, double price) {
            this.fullDateTimeStr = fullDateTimeStr;
            this.price = price;
        }
    }

    // 2. Структура минутной свечи левого графика, хранящая внутри свои тики
    public static class DeepMinuteBar {
        public long timestamp;         // Unix-время начала минуты (в секундах)
        public String minuteKey;       // Ключ для связи: "202605010959"
        public double open = -1;
        public double high = Double.MIN_VALUE;
        public double low = Double.MAX_VALUE;
        public double close = -1;


        // Список ВСЕХ тиков, которые упали в эту конкретную минуту
        public List<RawTick> allTicks = new ArrayList<>();

        public DeepMinuteBar(long timestamp, String minuteKey) {
            this.timestamp = timestamp;
            this.minuteKey = minuteKey;
        }
    }

    // 3. Структура часовой свечи левого графика
    public static class HourBar {
        public long timestamp;         // Unix-время начала часа (в секундах)
        public double open = -1;
        public double high = Double.MIN_VALUE;
        public double low = Double.MAX_VALUE;
        public double close = -1;
        public String hourKey;
        public HourBar(long timestamp) {
            this.timestamp = timestamp;
        }
    }

    /**
     * ШАГ 1: Создает минутные свечи со вложенными тиками из сырых строк коррелятора
     * @param rawCorrTicks Строки вида "20250602,070012,2770.08,1,Buy,0,"
     */
    public static List<DeepMinuteBar> buildLeftMinutes(List<String> rawCorrTicks) {
        // TreeMap гарантирует автоматическую хронологическую сортировку минут
        Map<String, DeepMinuteBar> minutesMap = new TreeMap<>();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

        for (String row : rawCorrTicks) {
            if (row == null || row.trim().isEmpty()) continue;
            String[] parts = row.split(",");
            if (parts.length < 3) continue;

            String dateStr = parts[0];       // "20250602"
            String timeStr = parts[1];       // "070012"
            double price = Double.parseDouble(parts[2]);

            // Строим ключи времени
            String minuteKey = dateStr + timeStr.substring(0, 4);   // "202506020700"
            String fullDateTimeStr = dateStr + timeStr;             // "20250602070012"

            // Если минута встретилась впервые, инициализируем её
            minutesMap.computeIfAbsent(minuteKey, k -> {
                LocalDateTime ldt = LocalDateTime.parse(minuteKey, timeFormatter);
                long unixSeconds = ldt.toEpochSecond(ZoneOffset.UTC);
                return new DeepMinuteBar(unixSeconds, minuteKey);
            });

            DeepMinuteBar bar = minutesMap.get(minuteKey);

            // Агрегируем стандартные минутные OHLC цены
            if (bar.open == -1) bar.open = price;
            if (price > bar.high) bar.high = price;
            if (price < bar.low) bar.low = price;
            bar.close = price;

            // СОХРАНЯЕМ ТИК ВНУТРЬ МИНУТЫ
            bar.allTicks.add(new RawTick(fullDateTimeStr, price));
        }

        // Превращаем отсортированную мапу в плоский индексированный список
        return new ArrayList<>(minutesMap.values());
    }

    /**
     * ШАГ 2: Создает глобальный массив ЧАСОВ из готового массива МИНУТ левого графика
     * @param leftMinutes Полученный на Шаге 1 список минут DeepMinuteBar
     */
    public static KeyToIndexStructure<String,HourBar> buildLeftHours(List<DeepMinuteBar> leftMinutes) {
        // TreeMap гарантирует автоматическую хронологическую сортировку часов
        Map<Long, HourBar> hoursMap = new TreeMap<>();

        for (DeepMinuteBar minBar : leftMinutes) {
            // МАТЕМАТИЧЕСКОЕ ОКРУГЛЕНИЕ Unix-времени минуты до начала часа
            long hourTimestamp = (minBar.timestamp / 3600) * 3600;

            // Если час встретился впервые, инициализируем его
            hoursMap.computeIfAbsent(hourTimestamp, HourBar::new);
            HourBar hourBar = hoursMap.get(hourTimestamp);

            // Агрегируем часовые OHLC цены на основе минутных баров
            if (hourBar.open == -1) hourBar.open = minBar.open; // Open первой минуты в часе
            if (minBar.high > hourBar.high) hourBar.high = minBar.high;
            if (minBar.low < hourBar.low) hourBar.low = minBar.low;
            hourBar.close = minBar.close; // Последняя минута часа запишет свой Close
            hourBar.hourKey = minBar.minuteKey.substring(0,10);
        }

        List<HourBar> hourBarList = new ArrayList<>(hoursMap.values());
        KeyToIndexStructure<String,HourBar> hourBarKeyToIndexStructure = new KeyToIndexStructure<>();
        hourBarList.forEach(c->{hourBarKeyToIndexStructure.add(c.hourKey,c);});
        // Превращаем в плоский список часов
        return hourBarKeyToIndexStructure;
    }


    public static void processLeftWindows(List<DeepMinuteBar> leftMinutesList, Map<String, List<String>> mapMinKeyTickKeysTrades, KeyToIndexStructure<String, HourBar> leftHoursStructure) {
        JSONArray leftMinCorrWindowsArray = new JSONArray();
        JSONArray leftHourCorrWindowsArray  = new JSONArray();
        // Бежим хронологически по минутам левого графика
        for (int i = 0; i < leftMinutesList.size(); i++) {
            DeepMinuteBar currentMinBar = leftMinutesList.get(i);
            String mKey = currentMinBar.minuteKey; // "202605010959"

            // Проверяем, есть ли сделки в этой минуте по вашей мапе
            if (mapMinKeyTickKeysTrades.containsKey(mKey)) {
                List<String> tradeTicksInMinute = mapMinKeyTickKeysTrades.get(mKey);

                // Поскольку в минуте может быть несколько сделок, каждую отделяем в свое окно!
                for (String exactTradeSecondKey : tradeTicksInMinute) { // "20260501095945"

                    // --- 1. СЛЕВА-ВВЕРХ: ПОИСК БЛИЖАЙШЕГО ТИКА И СБОРКА 120 МИНУТ ---
                    RawTick closestLeftTick = null;
                    for (RawTick lt : currentMinBar.allTicks) {
                        // Сравниваем строки: ищем последний тик, который был ДО или в секунду сделки
                        if (lt.fullDateTimeStr.compareTo(exactTradeSecondKey) <= 0) {
                            closestLeftTick = lt;
                        } else {
                            break; // Тики идут по хронологии, дальше заглядывать не нужно
                        }
                    }

                    // Если тиков до сделки почему-то нет, берем самый первый тик этой минуты
                    if (closestLeftTick == null && !currentMinBar.allTicks.isEmpty()) {
                        closestLeftTick = currentMinBar.allTicks.get(0);
                    }

                    if (closestLeftTick != null) {
                        JSONArray singleMinWindow = new JSONArray();
                        int startMinIdx = Math.max(0, i - 119);

                        // Кладим 119 полных минут ДО текущей минуты
                        for (int j = startMinIdx; j < i; j++) {
                            singleMinWindow.put(createStandardBarJson(leftMinutesList.get(j)));
                        }

                        // Собираем НЕПОЛНУЮ 120-ю минуту на основе отсечки по closestLeftTick
                        JSONObject incompleteLeftMinObj = buildIncompleteMinuteJson(currentMinBar, closestLeftTick.fullDateTimeStr);
                        singleMinWindow.put(incompleteLeftMinObj);

                        // Сохраняем получившееся минутное окно в итоговый массив
                        leftMinCorrWindowsArray.put(singleMinWindow);


                        // --- 2. СЛЕВА-ВНИЗ: ПОИСК ИНДЕКСА ЧАСА И СБОРКА НЕДЕЛИ ИСТОРИИ ---
                        JSONArray singleHourWindow = new JSONArray();

                        // Вычисляем ключ часа: отнимаем последние два числа (минуты) от mKey
                        // "202605010959" -> "2026050109"
                        String hourKey = mKey.substring(0, mKey.length() - 2);

                        // Получаем индекс текущего часа в вашем списке структур
                        int currentHourIdx = leftHoursStructure.getIndex(hourKey);

                        if (currentHourIdx != -1) {
                            // Отматываем ровно одну неделю назад (168 часов) от текущего часа
                            int startHourIdx = Math.max(0, currentHourIdx - 168);

                            // Добавляем в окно всю чистую историческую неделю часов (не включая текущий час)
                            for (int h = startHourIdx; h < currentHourIdx; h++) {
                                HourBar hb = leftHoursStructure.getByEntry(h);
                                singleHourWindow.put(createHourBarJson(hb));
                            }

                            // Строим НЕПОЛНЫЙ последний час на основе накопленных минут этого же часа до момента сделки
                            JSONObject incompleteHourObj = buildIncompleteHourJson(leftMinutesList, i, closestLeftTick.fullDateTimeStr);
                            singleHourWindow.put(incompleteHourObj);

                            // Сохраняем получившееся часовое окно в итоговый массив
                            leftHourCorrWindowsArray.put(singleHourWindow);
                        }
                    }
                }
            }
        }





        writeArrayJSON(leftMinCorrWindowsArray,"J:\\static_data_2\\JavaProjects\\Research_of_quotes\\src\\main\\resources\\jsons_data\\_trade_26_08\\leftMinCorrWindowsArray.json");
        writeArrayJSON(leftHourCorrWindowsArray,"J:\\static_data_2\\JavaProjects\\Research_of_quotes\\src\\main\\resources\\jsons_data\\_trade_26_08\\leftHourCorrWindowsArray.json");




    }

    // Хелпер: Симуляция неполной минуты по строке времени
    private static JSONObject buildIncompleteMinuteJson(DeepMinuteBar bar, String cutoffTime) {
        double o = -1, h = Double.MIN_VALUE, l = Double.MAX_VALUE, c = -1;
        for (RawTick t : bar.allTicks) {
            if (t.fullDateTimeStr.compareTo(cutoffTime) <= 0) {
                if (o == -1) o = t.price;
                if (t.price > h) h = t.price;
                if (t.price < l) l = t.price;
                c = t.price;
            }
        }
        JSONObject obj = new JSONObject();
        obj.put("time", bar.timestamp); obj.put("open", o); obj.put("high", h); obj.put("low", l); obj.put("close", c);
        return obj;
    }

    // Хелпер: Симуляция неполного часа на основе минут текущего часа
    private static JSONObject buildIncompleteHourJson(List<DeepMinuteBar> leftMinutesList, int currentMinIdx, String cutoffTime) {
        DeepMinuteBar currentMinBar = leftMinutesList.get(currentMinIdx);
        long hourTimestamp = (currentMinBar.timestamp / 3600) * 3600;

        double o = -1, h = Double.MIN_VALUE, l = Double.MAX_VALUE, c = -1;

        // Двигаемся по минутам назад, собирая OHLC, пока они принадлежат тому же часу
        int idx = currentMinIdx;
        while (idx >= 0) {
            DeepMinuteBar mBar = leftMinutesList.get(idx);
            long mHourTs = (mBar.timestamp / 3600) * 3600;
            if (mHourTs != hourTimestamp) break; // Вышли за пределы текущего часа

            if (idx == currentMinIdx) {
                // Для финальной минуты часа берем параметры только отфильтрованных тиков до сделки
                for (RawTick t : mBar.allTicks) {
                    if (t.fullDateTimeStr.compareTo(cutoffTime) <= 0) {
                        if (o == -1) o = t.price;
                        if (t.price > h) h = t.price;
                        if (t.price < l) l = t.price;
                        c = t.price;
                    }
                }
            } else {
                // Для предыдущих минут часа берем их полные готовые параметры
                if (mBar.high > h) h = mBar.high;
                if (mBar.low < l) l = mBar.low;
                o = mBar.open; // Сдвигаем open к самой первой минуте часа
                if (c == -1) c = mBar.close;
            }
            idx--;
        }

        JSONObject obj = new JSONObject();
        obj.put("time", hourTimestamp); obj.put("open", o); obj.put("high", h); obj.put("low", l); obj.put("close", c);
        return obj;
    }

    private static JSONObject createStandardBarJson(DeepMinuteBar bar) {
        JSONObject obj = new JSONObject();
        obj.put("time", bar.timestamp); obj.put("open", bar.open); obj.put("high", bar.high); obj.put("low", bar.low); obj.put("close", bar.close);
        return obj;
    }

    private static JSONObject createHourBarJson(HourBar bar) {
        JSONObject obj = new JSONObject();
        obj.put("time", bar.timestamp); obj.put("open", bar.open); obj.put("high", bar.high); obj.put("low", bar.low); obj.put("close", bar.close);
        return obj;
    }

    public static void main(String[] args) {
        // Пример входящих тиков нового (коррелирующего) инструмента
        List<String> rawCorrTicks = Arrays.asList(
                "20260501,095915,2600.50,1,Buy,0,",
                "20260501,095942,2602.00,2,Buy,0,",
                "20260501,100005,2601.20,1,Sell,0,",
                "20260501,100055,2605.80,5,Buy,0,"
        );

        // 1. Собираем минуты со всеми тиками внутри
        List<DeepMinuteBar> leftMinutes = buildLeftMinutes(rawCorrTicks);
        System.out.println("Собрано левых минут: " + leftMinutes.size());
        System.out.println("Тиков внутри первой минуты: " + leftMinutes.get(0).allTicks.size());

        // 2. Из собранных минут делаем чистый глобальный массив часов
        KeyToIndexStructure<String,HourBar> leftHours = buildLeftHours(leftMinutes);
        System.out.println("Собрано левых часов из минут: " + leftHours.list.size());
        System.out.println("Unix-время первого часа: " + leftHours.list.get(0).timestamp);
    }
}


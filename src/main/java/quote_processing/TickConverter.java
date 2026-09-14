package quote_processing;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TickConverter {

    // Класс для промежуточного накопления тиков внутри одной минуты
    private static class MinuteData {
        long timestamp;
        double open = -1;
        double high = Double.MIN_VALUE;
        double low = Double.MAX_VALUE;
        double close = -1;
        int buyVolume = 0;
        int sellVolume = 0;

        MinuteData(long timestamp) {
            this.timestamp = timestamp;
        }
    }

    public static void convertTicks(List<String> rawTicks) {
        // TreeMap гарантирует автоматическую сортировку по дате и времени
        Map<String, MinuteData> minutesMap = new TreeMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

        // Шаг 1: Группируем тики по минутам и собираем базовые данные
        for (String row : rawTicks) {
            if (row == null || row.trim().isEmpty()) continue;
            String[] parts = row.split(",");
            if (parts.length < 5) continue;

            String dateStr = parts[0];       // Напр. 20260501
            String timeStr = parts[1];       // Напр. 095932
            double price = Double.parseDouble(parts[2]);
            int volume = Integer.parseInt(parts[3]);
            String type = parts[4].trim().toLowerCase();

            // Отсекаем секунды, формируем ключ минуты '202605010959'
            String minuteKey = dateStr + timeStr.substring(0, 4);

            minutesMap.computeIfAbsent(minuteKey, k -> {
                LocalDateTime ldt = LocalDateTime.parse(minuteKey, formatter);
                // Переводим в Unix-секунды для Lightweight Charts
                long epochSeconds = ldt.toEpochSecond(ZoneOffset.UTC);
                return new MinuteData(epochSeconds);
            });

            MinuteData bar = minutesMap.get(minuteKey);

            // Заполняем OHLC цены
            if (bar.open == -1) bar.open = price;
            if (price > bar.high) bar.high = price;
            if (price < bar.low) bar.low = price;
            bar.close = price;

            // Накапливаем объемы
            if ("buy".equals(type)) {
                bar.buyVolume += volume;
            } else if ("sell".equals(type)) {
                bar.sellVolume += volume;
            }
        }

        // Итоговые списки для Lightweight Charts
        List<String> priceJsonList = new ArrayList<>();
        List<String> deltaJsonList = new ArrayList<>();

        int cumulativeDelta = 0;
        LocalDate lastDate = null;
        DateTimeFormatter dateParser = DateTimeFormatter.ofPattern("yyyyMMdd");

        // Шаг 2: Формируем массивы для графиков с расчетом кумулятивной дельты
        for (Map.Entry<String, MinuteData> entry : minutesMap.entrySet()) {
            String minuteKey = entry.getKey();
            MinuteData bar = entry.getValue();

            // Извлекаем дату для проверки смены дня (первые 8 символов ключа)
            LocalDate currentDate = LocalDate.parse(minuteKey.substring(0, 8), dateParser);

            // Если начался новый день — сбрасываем кумулятивную дельту в ноль
            if (lastDate != null && !currentDate.isEqual(lastDate)) {
                cumulativeDelta = 0;
            }
            lastDate = currentDate;

            // 1. Формируем JSON строки для свечей цены
            priceJsonList.add(String.format(Locale.US,
                    "{\"time\":%d,\"open\":%.2f,\"high\":%.2f,\"low\":%.2f,\"close\":%.2f}",
                    bar.timestamp, bar.open, bar.high, bar.low, bar.close));

            // 2. Расчет минутной дельты и кумулятивного итога
            int minuteDelta = bar.buyVolume - bar.sellVolume;
            int oldDelta = cumulativeDelta;
            cumulativeDelta += minuteDelta;

            // Чтобы у свечей дельты объема НЕ БЫЛО фитилей (концов):
            // Выравниваем high и low строго по границам тела (open и close)
            int openDelta = oldDelta;
            int closeDelta = cumulativeDelta;
            int highDelta = Math.max(openDelta, closeDelta);
            int lowDelta = Math.min(openDelta, closeDelta);

            // 3. Формируем JSON строки для свечей кумулятивной дельты
            deltaJsonList.add(String.format(Locale.US,
                    "{\"time\":%d,\"open\":%d,\"high\":%d,\"low\":%d,\"close\":%d}",
                    bar.timestamp, openDelta, highDelta, lowDelta, closeDelta));
        }

        // Результат готов к передаче в JavaScript-шаблон графиков:
        //String priceDataJson = "[" + String.join(",", priceJsonList) + "]";
        String deltaDataJson = "[" + String.join(",", deltaJsonList) + "]";

        // Вывод для проверки работы в консоли
        //System.out.println("Массив Свечей Цены: " + priceDataJson);
        //System.out.println("Массив Свечей Дельты: " + deltaDataJson);
        //priceJsonList.forEach(System.out::println);
        deltaJsonList.forEach(System.out::println);
    }


    public static class HourBar {
        public long timestamp;
        public double open = -1;
        public double high = Double.MIN_VALUE;
        public double low = Double.MAX_VALUE;
        public double close = -1;

        HourBar(long timestamp) { this.timestamp = timestamp; }
    }

    /**
     * Группирует текстовые минутные строки в часовые свечи цены
     * @param rawRows Список строк вида: "20250602,070000,2770.08,2770.08,2753.92,2761.72,..."
     */
    public static JSONArray convertRowsToHours(List<String> rawRows) {
        // TreeMap гарантирует автоматическую хронологическую сортировку часов
        Map<Long, HourBar> hoursMap = new TreeMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

        for (String row : rawRows) {
            if (row == null || row.trim().isEmpty()) continue;
            String[] parts = row.split(",");
            if (parts.length < 6) continue;

            String dateStr = parts[0]; // "20250602"
            String timeStr = parts[1]; // "070000" (часы, минуты, секунды)

            double open = Double.parseDouble(parts[2]);
            double high = Double.parseDouble(parts[3]);
            double low = Double.parseDouble(parts[4]);
            double close = Double.parseDouble(parts[5]);

            // ПРОСТОЕ ВЫДЕЛЕНИЕ ЧАСА ИЗ СТРОКИ:
            // Берем первые два символа от времени (например, из "073512" берем "07")
            // и дописываем "0000" (минуты и секунды в ноль). Получаем "070000"
            String hourTimeStr = timeStr.substring(0, 2) + "0000";
            String hourKey = dateStr + hourTimeStr; // "20250602070000"

            // Переводим один раз в Unix-секунды, так как Lightweight Charts принимает только их
            LocalDateTime ldt = LocalDateTime.parse(hourKey, formatter);
            long hourTimestamp = ldt.toEpochSecond(ZoneOffset.UTC);

            // Агрегируем часовую свечу OHLC
            hoursMap.computeIfAbsent(hourTimestamp, HourBar::new);
            HourBar hourBar = hoursMap.get(hourTimestamp);

            if (hourBar.open == -1) hourBar.open = open; // Open самой первой минуты в часе
            if (high > hourBar.high) hourBar.high = high;
            if (low < hourBar.low) hourBar.low = low;
            hourBar.close = close; // Каждая минута перезаписывает Close, останется последняя
        }

        // Упаковываем результат в JSONArray для JavaScript
        JSONArray hourPriceArray = new JSONArray();
        for (HourBar hb : hoursMap.values()) {
            JSONObject hObj = new JSONObject();
            hObj.put("time", hb.timestamp);
            hObj.put("open", hb.open);
            hObj.put("high", hb.high);
            hObj.put("low", hb.low);
            hObj.put("close", hb.close);
            hourPriceArray.put(hObj);
        }

        return hourPriceArray;
    }

    public static void main(String[] args) {
        // Пример входящих минутных строк
        List<String> sampleRows = Arrays.asList(
                "20250602,070000,2770.08,2770.08,2753.92,2761.72,291842.46,0",
                "20250602,070100,2761.72,2765.00,2760.10,2763.50,150200.12,0",
                "20250602,080000,2763.50,2780.00,2762.00,2775.10,420100.50,0" // Строка следующего часа
        );

        JSONArray hourJson = convertRowsToHours(sampleRows);
        System.out.println(hourJson.toString());
    }
}


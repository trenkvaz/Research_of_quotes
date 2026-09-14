package quote_processing;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.*;
import java.util.stream.Collectors;

import static utilities.ReadWrite.readListStr;
import static utilities.ReadWrite.writeListStr;

public class AnyActions {

    static void fixLinesQuotes(){
        List<String> inputList = readListStr("J:\\static_data_2\\PythonProjects\\ts_lab\\MIX_2011_2025_tickes.txt");
        List<String> outputList = new ArrayList<>();
        assert inputList != null;
        outputList.add(inputList.get(0));
        for(int i=1; i<inputList.size(); i++){
            String line = inputList.get(i);
            String outLine = line.substring(0,line.length()-2);
            outputList.add(outLine);
        }
        writeListStr(outputList,"J:\\static_data_2\\PythonProjects\\ts_lab\\MIX_2011_2025_tickes.txt");
    }


    static void addDeltaVolumeQuotes(){
        List<String> inputList = readListStr("J:\\static_data_2\\PythonProjects\\ts_lab\\MIX_2011_2025_tickes.txt");
        List<String> outputList = new ArrayList<>();
        assert inputList != null;
        outputList.add(inputList.get(0));
        for(int i=1; i<inputList.size(); i++){
            String line = inputList.get(i);
            String outLine = "";
            if(line.contains("B")){
                outLine = line.substring(0,line.length()-2);
            } else if(line.contains("Se")){
                 String[] arrLine = line.split(",");
                 arrLine[3] = "-"+arrLine[3];

                outLine = Arrays.stream(arrLine)
                        .limit(4)          // Берем следующие 3 элемента
                        .collect(Collectors.joining(","));


                //for (int a=0; a<4; a++)outLine+=arrLine[a];
               // System.out.println(outLine);
            }

           outputList.add(outLine);
        }
        writeListStr(outputList,"J:\\static_data_2\\PythonProjects\\ts_lab\\MIX_2011_2025_tickes.txt");
    }



    public static void convertForMT5() {
        List<String> inputList = readListStr("J:\\static_data_2\\PythonProjects\\mt5\\MIX_2011_2025_tickers.txt");
        List<String> outputList = new ArrayList<>();

        // 1. Добавляем обязательный заголовок для MetaTrader 5
        outputList.add("Time,Bid,Ask,Last,Volume");

        for (String line : inputList) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }

            // Разделяем строку по запятой
            String[] parts = line.split(",");
            if (parts.length < 5) {
                continue; // Пропускаем некорректные строки
            }

            String rawDate = parts[0].trim();      // "20110930"
            String rawTime = parts[1].trim();      // "100015"
            String rawPrice = parts[2].trim();     // "140005"
            String rawVolume = parts[3].trim();    // "1"
            String direction = parts[4].trim();    // "Buy" или "Sell"

            // 2. Форматируем Дату (из 20110930 в 2011.09.30)
            String formattedDate = rawDate.substring(0, 4) + "." +
                    rawDate.substring(4, 6) + "." +
                    rawDate.substring(6, 8);

            // 3. Форматируем Время (добавляем ведущие нули, если нужно, и двоеточия)
            if (rawTime.length() < 6) {
                rawTime = String.format("%06d", Integer.parseInt(rawTime));
            }
            String formattedTime = rawTime.substring(0, 2) + ":" +
                    rawTime.substring(2, 4) + ":" +
                    rawTime.substring(4, 6);

            String timeField = formattedDate + " " + formattedTime;

            // 4. Логика искусственного спреда для Кумулятивной Дельты
            long price = Long.parseLong(rawPrice);
            long bid = price;
            long ask = price + 1; // Делаем шаг ask на 1 пункт выше
            long last;

            // Если Buy — сделка по Ask (верхняя цена), если Sell — по Bid (нижняя цена)
            if ("buy".equalsIgnoreCase(direction)) {
                last = ask;
            } else {
                last = bid;
            }

            // 5. Собираем готовую строку для MT5
            String mt5Line = String.format("%s,%d,%d,%d,%s", timeField, bid, ask, last, rawVolume);
            outputList.add(mt5Line);
        }
        writeListStr(outputList,"J:\\static_data_2\\PythonProjects\\mt5\\MIX_2011_2025_tickers_mt5.txt");
        //return outputList;
    }

    private static final long DELTA_ZERO_LEVEL = 1000000L;

    static class PriceMinuteBar {
        // Переменные для настоящей ЦЕНЫ фьючерса
        long open = -1;
        long high = Long.MIN_VALUE;
        long low = Long.MAX_VALUE;
        long close = -1;

        long pureMinuteDelta = 0; // Чистая дельта конкретно этой минуты (+ или -)
        String rawDateStr;
    }
    public static void aggregateToWeeklyCumulativeDelta() {
        List<String> rawTicks = readListStr("J:\\static_data_2\\PythonProjects\\mt5\\MIX_2011_2025_tickers.txt");
        Map<String, PriceMinuteBar> barMap = new LinkedHashMap<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        // 1. Сначала собираем стандартные минутные бары ЦЕНЫ и считаем чистую дельту каждой минуты
        for (String line : rawTicks) {
            if (line == null || line.trim().isEmpty()) continue;

            String[] parts = line.split(",");
            if (parts.length < 5) continue;

            String rawDate = parts[0].trim();
            String rawTime = parts[1].trim();
            long price = Long.parseLong(parts[2].trim());   // Это НАСТОЯЩАЯ цена
            long volume = Long.parseLong(parts[3].trim());  // Это объем сделки
            String direction = parts[4].trim();             // Buy или Sell

            String formattedDate = rawDate.substring(0, 4) + "." + rawDate.substring(4, 6) + "." + rawDate.substring(6, 8);
            if (rawTime.length() < 6) rawTime = String.format("%06d", Integer.parseInt(rawTime));
            String formattedMinute = rawTime.substring(0, 2) + ":" + rawTime.substring(2, 4);
            String minuteKey = formattedDate + " " + formattedMinute;

            PriceMinuteBar bar = barMap.computeIfAbsent(minuteKey, k -> new PriceMinuteBar());
            bar.rawDateStr = rawDate;

            // Формируем правильные свечи ЦЕНЫ (140005, 141000 и т.д.)
            if (bar.open == -1) bar.open = price;
            if (price > bar.high) bar.high = price;
            if (price < bar.low) bar.low = price;
            bar.close = price;

            // Считаем чистую дельту минуты: покупки в плюс, продажи в минус
            if ("buy".equalsIgnoreCase(direction)) {
                bar.pureMinuteDelta += volume*10;
            } else {

                bar.pureMinuteDelta -= volume*10;
               // System.out.println(volume+"  "+bar.pureMinuteDelta);
            }
        }

        // 2. Собираем итоговый CSV-файл, где ЦЕНА — это цена, а кумулятивная дельта идет в Volume
        List<String> output = new ArrayList<>();
        output.add("Time,Open,High,Low,Close,Volume");

        long runningDelta = DELTA_ZERO_LEVEL; // Стартуем неделю с 1 000 000
        int currentWeek = -1;
        int currentYear = -1;

        for (Map.Entry<String, PriceMinuteBar> entry : barMap.entrySet()) {
            String timeField = entry.getKey() + ":00";
            PriceMinuteBar b = entry.getValue();

            // Проверка сброса недели
            LocalDate barDate = LocalDate.parse(b.rawDateStr, dateFormatter);
            int weekOfYear = barDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
            int year = barDate.get(IsoFields.WEEK_BASED_YEAR);

            if (currentWeek == -1) {
                currentWeek = weekOfYear;
                currentYear = year;
            } else if (weekOfYear != currentWeek || year != currentYear) {
                runningDelta = DELTA_ZERO_LEVEL; // Сброс кумулятивной дельты в начале недели к 1 000 000
                currentWeek = weekOfYear;
                currentYear = year;
            }

            // РЕАЛЬНОЕ ИЗМЕНЕНИЕ: прибавляем чистую дельту минуты (если она отрицательная, runningDelta УМЕНЬШИТСЯ)
            runningDelta += b.pureMinuteDelta;
            System.out.println(timeField+"  "+runningDelta+"  "+b.pureMinuteDelta);
            // Записываем строчку: НАСТОЯЩИЕ цены фьючерса, а в конце — колеблющаяся дельта
            String barLine = String.format("%s,%d,%d,%d,%d,60,%d",
                    timeField, b.open, b.high, b.low, b.close, runningDelta);
            output.add(barLine);
        }
        writeListStr(output,"J:\\static_data_2\\PythonProjects\\mt5\\MIX_2011_2025_min_delta_mt5.csv");
        //return output;
    }
    public static void main(String[] args) {
       // fixLinesQuotes();
        //addDeltaVolumeQuotes();
        //convertForMT5();
        aggregateToWeeklyCumulativeDelta();
    }
}

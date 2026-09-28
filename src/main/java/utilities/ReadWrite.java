package utilities;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ReadWrite {


    public static List<String> readListStr(String path){
        List<String> result = new ArrayList<>();
        try (Stream<String> stream = Files.lines(Paths.get(path))) {
            stream.forEach(result::add);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }
        return result;
    }

    public static List<int[]> readListInt(String path,int toInt){
        List<int[]> result = new ArrayList<>();
        try (Stream<String> stream = Files.lines(Paths.get(path))) {
            stream.forEach((str)->{
                String[] tickstr = str.split(",");
                int price = 0;
                if(toInt!=0)price =(int) (Double.parseDouble(tickstr[2]) * toInt); else price = Integer.parseInt(tickstr[2]);
                result.add(new int[]{Integer.parseInt(tickstr[0]),Integer.parseInt(tickstr[1]),price});
            });
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }
        return result;
    }

    public static int[] processFile(String filePath) throws IOException {
        System.out.println("Шаг 1: Подсчет количества строк...");
        long startTime = System.currentTimeMillis();
        int lineCount = countLinesFast(filePath);
        System.out.println("Найдено строк: " + lineCount + " за " + (System.currentTimeMillis() - startTime) + " мс");

        if (lineCount == 0) {
            return new int[0];
        }

        // Из каждой строки мы берем ровно 3 числа
        int expectedSize = lineCount * 3;
        System.out.println("Выделение массива int[] размером " + expectedSize + " (~" + (expectedSize * 4 / 1024 / 1024) + " МБ RAM)...");
        int[] array = new int[expectedSize];

        System.out.println("Шаг 2: Побайтовый парсинг данных...");
        startTime = System.currentTimeMillis();
        int totalNumbersParsed = parsePatternFile(filePath, array);
        System.out.println("Успешно распарсено чисел: " + totalNumbersParsed + " за " + (System.currentTimeMillis() - startTime) + " мс");

        // Если в самом конце файла были пустые строки или сбой структуры, подрезаем массив под реальный размер
        if (totalNumbersParsed < expectedSize) {
            int[] trimmedArray = new int[totalNumbersParsed];
            System.arraycopy(array, 0, trimmedArray, 0, totalNumbersParsed);
            return trimmedArray;
        }

        return array;
    }

    private static int countLinesFast(String filePath) throws IOException {
        try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(filePath), 65536)) {
            byte[] buffer = new byte[65536];
            int count = 0;
            int readChars;
            boolean endsWithNewLine = false;

            while ((readChars = is.read(buffer)) != -1) {
                for (int i = 0; i < readChars; ++i) {
                    if (buffer[i] == '\n') {
                        count++;
                        endsWithNewLine = true;
                    } else if (buffer[i] != '\r') {
                        endsWithNewLine = false;
                    }
                }
            }
            // Если файл не пустой и не заканчивался на новую строку, добавляем последнюю строку
            if (count > 0 && !endsWithNewLine) {
                count++;
            } else if (count == 0 && readChars == -1) {
                // Проверка на случай если файл имеет данные, но меньше 64КБ и без \n
                File f = new File(filePath);
                if (f.exists() && f.length() > 0) count = 1;
            }
            return count;
        }
    }


    private static int parsePatternFile(String filePath, int[] array) throws IOException {
        int count = 0;

        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(filePath), 65536)) {
            int c;

            while (true) {
                // -------------------------------------------------------------
                // БЛОК 1: Читаем первое число (дата) до первой запятой
                // -------------------------------------------------------------
                int date = 0;
                boolean hasData = false;

                while ((c = bis.read()) != -1 && c != ',') {
                    if (c >= '0' && c <= '9') {
                        date = date * 10 + (c - '0');
                        hasData = true;
                    }
                }

                if (c == -1 && !hasData) {
                    break; // Конец файла
                }

                // -------------------------------------------------------------
                // БЛОК 2: Читаем второе число (время) до второй запятой
                // -------------------------------------------------------------
                int time = 0;
                boolean hasTimeDigits = false;
                while ((c = bis.read()) != -1 && c != ',') {
                    if (c >= '0' && c <= '9') {
                        time = time * 10 + (c - '0');
                        hasTimeDigits = true;
                    }
                }

                if (hasTimeDigits && time == 0) {
                    time = 240000;
                }
                // -------------------------------------------------------------
                // БЛОК 3: Читаем третье число (цена) в StringBuilder до запятой
                // -------------------------------------------------------------
                StringBuilder priceStr = new StringBuilder(16);
                while ((c = bis.read()) != -1 && c != ',') {
                    // Собираем символы цены как есть (включая точку и минус)
                    if ((c >= '0' && c <= '9') || c == '.' || c == '-') {
                        priceStr.append((char) c);
                    }
                }

                // Переводим в double, умножаем на 100 и округляем до ближайшего целого
                double parsedDouble = Double.parseDouble(priceStr.toString());
                int finalPrice = (int) (parsedDouble * 100);

                // -------------------------------------------------------------
                // СОХРАНЕНИЕ В МАССИВ
                // -------------------------------------------------------------
                array[count++] = date;
                array[count++] = time;
                array[count++] = finalPrice;

                // -------------------------------------------------------------
                // БЛОК 4: Прокрутка остатка строки вхолостую до '\n'
                // -------------------------------------------------------------
                if (c != -1) {
                    while ((c = bis.read()) != -1 && c != '\n') {
                        // Игнорируем Buy, объемы и т.д.
                    }
                }

                if (c == -1) {
                    break;
                }
            }
        }
        return count;
    }




    public static void writeArrayFast(int[] array, String filePath) throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile(filePath, "rw");
             FileChannel channel = raf.getChannel()) {

            // Выделяем прямой буфер на 8 МБ (8 * 1024 * 1024 байт)
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(8 * 1024 * 1024);
            IntBuffer intBuffer = byteBuffer.asIntBuffer();

            for (int i = 0; i < array.length; ) {
                intBuffer.clear();
                // Сколько интов поместится в буфер за этот раз
                int transferSize = Math.min(array.length - i, intBuffer.capacity());

                // Переносим кусок массива в буфер
                intBuffer.put(array, i, transferSize);
                i += transferSize;

                // Настраиваем байтовый буфер для записи в файл (1 int = 4 байта)
                byteBuffer.position(0).limit(transferSize * 4);
                channel.write(byteBuffer);
            }
        }
    }

    // 2. БЫСТРОЕ ЧТЕНИЕ ИЗ ФАЙЛА В МАССИВ
    public static int[] readArrayFast(String filePath, int arrayLength) throws Exception {
        int[] array = new int[arrayLength];

        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r");
             FileChannel channel = raf.getChannel()) {

            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(8 * 1024 * 1024);
            IntBuffer intBuffer = byteBuffer.asIntBuffer();

            for (int i = 0; i < array.length; ) {
                byteBuffer.clear();
                int bytesRead = channel.read(byteBuffer);
                if (bytesRead == -1) break; // Конец файла

                // Переключаем буфер в режим чтения
                byteBuffer.flip();
                int intsRead = bytesRead / 4;

                intBuffer.clear().limit(intsRead);
                intBuffer.get(array, i, intsRead);
                i += intsRead;
            }
        }
        return array;
    }




    public static void writeListStr(List<String> stringList,String path){
        try {
            Files.write(Paths.get(path), stringList);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }


    public static <T extends Serializable> void serialize(T data, String filePath) {
        try (FileOutputStream fileOut = new FileOutputStream(filePath);
             ObjectOutputStream out = new ObjectOutputStream(fileOut)) {

            out.writeObject(data); // Записываем объект (или список) в файл
            System.out.println("Данные успешно сохранены в: " + filePath);

        } catch (Exception e) {
            System.err.println("Ошибка при сериализации: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T deserialize(String filePath) {
        try (FileInputStream fileIn = new FileInputStream(filePath);
             ObjectInputStream in = new ObjectInputStream(fileIn)) {

            // Читаем и автоматически приводим к типу T
            return (T) in.readObject();

        } catch (Exception e) {
            System.err.println("Ошибка при десериализации: " + e.getMessage());
            e.printStackTrace();
            return null; // Если файл не найден или поврежден, возвращаем null
        }
    }

}

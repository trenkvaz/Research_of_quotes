package strategy;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class OneTrade implements Serializable {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    public String entryDateTime; // Напр: "20260501095932"
    public String exitDateTime;  // Напр: "20260501101545"
    public int entryDate;  //20260501
    public int entryTime;  // 101545
    public int entryPrice;  // цена входа
    public int exitPrice;    // цены выхода
    public int exitDate; // 20260501
    public int exitTime;  // 101545
    public int longOrShort; // long = 1 | short -1
    public int result;   //абсолютный результат сделки разница между выходом и входом, если шорт умножить на -1
    //public String beautyEntryDateTime; //читаемая дата открытия позиции "2026-08-07 15:47:22"
    //public int dayOfWeek;
    public boolean isStopTrailing;



    public OneTrade(){}

    public OneTrade(int entryDate, int entryTime,int entryPrice, int exitPrice, int exitDate, int exitTime, int longOrShort) {
        this.entryDate = entryDate;
        this.entryTime = entryTime;
        this.entryPrice = entryPrice;
        this.exitDate = exitDate;
        this.exitTime = exitTime;
        this.exitPrice = exitPrice;
        this.longOrShort = longOrShort;
        result = (exitPrice-entryPrice)*longOrShort;
        //beautyEntryDateTime = getBeautyEntryDateTime(entryDateTime);
        //dayOfWeek = getDayNumber(entryDateTime);


    }


    public String getLongOrShortStr(){
        return (longOrShort==1?"long":"short");
    }

    public String entryDateTime(){
        if(entryDateTime==null){
            String entryTimeStr = String.valueOf(entryTime);
            if(entryTimeStr.length()==5)entryTimeStr = "0"+entryTimeStr;
            //else if(entryTimeStr.length()==1)entryTimeStr = "000000";
            entryDateTime = entryDate+entryTimeStr;
        }
        return entryDateTime;
    }

    public String exitDateTime(){
        if(exitDateTime==null){
            String exitTimeStr = String.valueOf(exitTime);
            if(exitTimeStr.length()==5)exitTimeStr = "0"+exitTimeStr;
            //else if(exitTimeStr.length()==1)exitTimeStr = "000000";
            exitDateTime = exitDate+exitTimeStr;
        }
        return exitDateTime;
    }


    public String getBeautyEntryDateTime() {
        char[] src = entryDateTime().toCharArray();
        char[] dst = new char[19];

        // Копируем Год
        dst[0] = src[0]; dst[1] = src[1]; dst[2] = src[2]; dst[3] = src[3];
        dst[4] = '-';
        // Месяц
        dst[5] = src[4]; dst[6] = src[5];
        dst[7] = '-';
        // День
        dst[8] = src[6]; dst[9] = src[7];
        dst[10] = ' ';
        // Часы
        dst[11] = src[8]; dst[12] = src[9];
        dst[13] = ':';
        // Минуты
        dst[14] = src[10]; dst[15] = src[11];
        dst[16] = ':';
        // Секунды
        dst[17] = src[12]; dst[18] = src[13];

        return new String(dst);
    }



    public int getDayNumber() {
        // Отрезаем время, оставляем только "20260501"
        return LocalDate.parse(entryDateTime().substring(0, 8), DATE_FORMATTER)
                .getDayOfWeek()
                .getValue(); // 1 = Понедельник, ..., 7 = Воскресенье
    }


    public static void main(String[] args) {
        long s = System.nanoTime();
       // formatByChar("20260501095932");
        /*for (int i=0; i<1000; i++)
           getDayNumber(String.valueOf(20260_501_095_932l+((i+1)*10_000_000_000l)));*/
        //System.out.println(getDayNumber(String.valueOf(20260_501_095_932l+((0+1)*10_000_000_000l))));
        System.out.println(((System.nanoTime() - s))/1000);
    }
}
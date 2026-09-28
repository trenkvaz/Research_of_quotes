package strategy.situations;

import org.json.JSONObject;
import strategy.OneTrade;
import strategy.TestTradeIMOEX;

public class Situation1 implements RuleSituation{

    final int TAKE_PROFIT;
    final int STOP_LOSE;
    final int MAX_MOVE;// минимальное движение которое цена должна пройти для сделки
    final int MOVE_LEVEL_SIZE; // размер уровня по которым ходит цена чтобы считать пройденный путь
    final int RANGE_LEVEL;// размерность круглых уровней
    private final int timeStartTrade;// начало торговли пример в 7.30 = 73000;
    private final int timeEndTrade;// завершение торговли пример в 15.00 = 150000;
    private int timeForceCloseTrade; // время принудительного закрытия сделки по умолчаниею пока + 1 час к timeEndTrade
    private boolean isTrailing;


    public Situation1(JSONObject parameters){
        this.TAKE_PROFIT = parameters.getInt("TAKE_PROFIT");
        this.STOP_LOSE = parameters.getInt("STOP_LOSE");
        this.MAX_MOVE = parameters.getInt("MAX_MOVE");
        this.MOVE_LEVEL_SIZE = parameters.getInt("MOVE_LEVEL_SIZE");
        this.RANGE_LEVEL = parameters.getInt("RANGE_LEVEL");
        this.timeStartTrade = parameters.getInt("timeStartTrade");
        this.timeEndTrade = parameters.getInt("timeEndTrade");
        this.isTrailing = parameters.optBoolean("isTrailing",false);
        timeForceCloseTrade = timeEndTrade+parameters.getInt("timeEndTradeExit");
    }

    public Situation1(int TAKE_PROFIT,int STOP_LOSE,int MAX_MOVE,int MOVE_LEVEL_SIZE,int RANGE_LEVEL,int timeStartTrade,int timeEndTrade){
        this.TAKE_PROFIT = TAKE_PROFIT;
        this.STOP_LOSE = STOP_LOSE;
        this.MAX_MOVE = MAX_MOVE;
        this.MOVE_LEVEL_SIZE = MOVE_LEVEL_SIZE;
        this.RANGE_LEVEL = RANGE_LEVEL;
        this.timeStartTrade = timeStartTrade;
        this.timeEndTrade = timeEndTrade;
        timeForceCloseTrade = timeEndTrade; // для теста сравнения пока 0
    }

    int one, two;
    //String entryDateTime = "";
    int entryDate;
    int entryTime;
    int beforeDate;
    int beforeTime;
    //String beautyEntryDateTime = "";
    int beforePrice = 0;
    int entryPrice = 0;
    public int longOrShort = 0;
    @Override
    public OneTrade trade(boolean isStartDay,int price, int date, int time) {
        OneTrade oneTrade = null;
        //int timeInt = Integer.parseInt(time);
        // если время равно принудительному закрытию сделки, пока здесь, ведь нет необходимости крутить котировки дальше
        if(isStartDay||time==timeForceCloseTrade){
            if(longOrShort!=0){
                oneTrade = closeTrade(price,true,date,time);
            }
            beforeDate = date;
            beforeTime = time;
            beforePrice = price;
            // пока котировки переводятся в ситуации внутри дня это нужно, потом будет сложный перевод с учетом прошлых дней
            one = 0; two = 0;
            return oneTrade;
        }
        // здесь перевод в ситуации поэтому должно двигаться всегда пока внутри дня
        int[] sizeMove = getSizeMove(price);

        if(longOrShort!=0){
            //boolean isForceClose = Integer.parseInt(time)==timeForceCloseTrade;
            oneTrade = closeTrade(price,false,date,time);
        }
        else openTrade(price,date,time,sizeMove);

        beforeDate = date;
        beforeTime = time;
        beforePrice = price;

        return oneTrade;
    }

    void openTrade(int price, int date, int time,int[] sizeMove){
        if(!(time>timeStartTrade&&time<timeEndTrade))return;

        if((MAX_MOVE==Math.abs(sizeMove[0]))&&sizeMove[1]!=0
                //уровень равен круглому
                &&sizeMove[1]%RANGE_LEVEL==0
        ){
            longOrShort = sizeMove[0]>0? 1:-1;
            //System.out.println(trade+" level "+sizeMove[1]+" price "+price+" date:"+date);
            entryPrice = price;
            entryDate = date;
            entryTime = time;
        }
    }

    OneTrade closeTrade(int exitPrice,boolean isForceClose,int exitDate, int exitTime){
        //if(!dateTrade.equals(exitDate)) System.out.println("NEXT DAY "+dateTrade+"  "+exitDate);
        int currentTrade = exitPrice-entryPrice;
        if(isForceClose){
            currentTrade = beforePrice - entryPrice;
            exitDate = beforeDate;
            exitTime = beforeTime;
            exitPrice = beforePrice;
        }
        boolean isClose = isForceClose;
        // если не isForceClose то считается по стоп лосам
       /* if(!isForceClose){
            if(currentTrade*longOrShort>=STOP_LOSE){
                isClose = true;
            }
            // куррентТрейд если отрицательный переводится в положительное и равен или больше тейк-профита
            else if(currentTrade*longOrShort>=TAKE_PROFIT){
                isClose = true;
            }
        }*/
        boolean isStopTrailing = false;
        if(!isForceClose){
            if(isTrailing){
                int trailingStops = isTrailingStops(currentTrade);
                if(trailingStops!=0){
                    isClose = true;
                    isStopTrailing = trailingStops==2;
                }
            }
            else {
                if(longOrShort==1){
                    // куррентТрейд если положительный и равен или больше тейк-профита
                    if(currentTrade>=TAKE_PROFIT){
                        isClose = true;
                    }
                    // куррентТрейд если отрицательный переводится в положительное и равен или больше стоп-лосса
                    else if(currentTrade*-1>=STOP_LOSE){
                        isClose = true;
                    }
                }
                if(longOrShort==-1){
                    // куррентТрейд если положительный и равен или больше стоп-лосса
                    if(currentTrade>=STOP_LOSE){
                        isClose = true;
                    }
                    // куррентТрейд если отрицательный переводится в положительное и равен или больше тейк-профита
                    else if(currentTrade*-1>=TAKE_PROFIT){
                        isClose = true;
                    }
                }
            }
        }

        if(isClose){
            OneTrade oneTrade = new OneTrade(entryDate,entryTime,entryPrice,exitPrice,exitDate,exitTime,longOrShort);
            oneTrade.isStopTrailing = isStopTrailing;
            //System.out.println(tradeSession.toString());
            longOrShort = 0;
            one = 0; two = 0;
            isStartTrailing = false;
            return oneTrade;
        }
        return null;
    }

    int[] getSizeMove(int price){
        int[] res = new int[2]; // 0 - diff ,1 - level
        int crossedLevel = 0;
        int prevBlock = price / MOVE_LEVEL_SIZE;
        int currBlock = beforePrice / MOVE_LEVEL_SIZE;
        if (prevBlock != currBlock) {
            // Вычисляем точный уровень, который перешагнули
            // Находим максимальный блок и умножаем на шаг
            crossedLevel = Math.max(prevBlock, currBlock) * MOVE_LEVEL_SIZE;
            res[1] = crossedLevel;
        }
        if(one==0){
            // если первая точка не проставлена идет до кратного MOVE_LEVEL_SIZE И проставляются 1 и 2
            if(crossedLevel!=0){
                //System.out.println("SET "+crossedLevel);
                one = crossedLevel; two = crossedLevel;
            }
            // 1 и 2 стоят проверяет ход 3 пока эмуляция через текущую цену
        } else {
            int diffTwoOne = two-one;
            // цена достигла уровня
            if(crossedLevel!=0){
                //если цена равна 2, то значит вернулась обратно
                if(crossedLevel==two)res[0] = diffTwoOne;
                else {
                    // проверка разницы между 3 и 2
                    int diffTreeTwo = price-two;
                    // проверка что цена пошла в ту же сторону что ход между 1 и 2
                    // если да то 2 ставится на 3, а 1 на месте
                    if(diffTwoOne>0&&diffTreeTwo>0||diffTwoOne<0&&diffTreeTwo<0){
                        two = crossedLevel;
                        res[0] = two - one;
                    }  // если не в ту сторону то 2 ставится на 3, а 1 переходит на два
                    else {
                        one = two;
                        two = crossedLevel;
                        res[0] = two - one;
                    }
                }
            }
            // цена вне уровня считается разница между 2 и 1 плюс разница между 3 и 2
               /* else {
                    return (two - one)+(price - two);
                }*/
        }
        return res;
    }

    boolean isStartTrailing = true;

    int isTrailingStops(int currentTrade){
        int res = 0; // 0 без стопа, 1 обычный стоп, 2 стоп по трейлу
        //boolean isClose = false;

        if(longOrShort==1){

            // куррентТрейд если отрицательный переводится в положительное и равен или больше стоп-лосса
            if(currentTrade*-1>=STOP_LOSE){
                //isClose = true;
                res = 1;
            } else
            // куррентТрейд если положительный и равен или больше тейк-профита
            if(currentTrade>=TAKE_PROFIT){
                //isClose = true;
                 res = 1;
            } else if(currentTrade>=TAKE_PROFIT/4*3){
                   if(!isStartTrailing){
                       isStartTrailing = true;
                   }
            }


            if(isStartTrailing&&res==0){
                if(currentTrade<(TAKE_PROFIT/4*3)-STOP_LOSE){
                    //isClose = true;
                    res = 2;
                }
            }
        }
        if(longOrShort==-1){
            // куррентТрейд если положительный и равен или больше стоп-лосса
            if(currentTrade>=STOP_LOSE){
                //isClose = true;
                res = 1;
            }
            // куррентТрейд если отрицательный переводится в положительное и равен или больше тейк-профита
            else if(currentTrade*-1>=TAKE_PROFIT){
                res = 1;
                //isClose = true;
            } else if(currentTrade*-1>=TAKE_PROFIT/2){
                if(!isStartTrailing){
                    isStartTrailing = true;
                }
            }

            if(isStartTrailing&&res==0){
                if(currentTrade*-1<TAKE_PROFIT/2-STOP_LOSE){
                    //isClose = true;
                    res = 2;
                }
            }
        }
        /*if(res==2)
        System.out.println("longOrShort "+longOrShort+" currentTrade "+currentTrade+" res "+res);*/
        return res;
    }

}

package strategy.situations;

import strategy.OneTrade;

public interface RuleSituation {

    public OneTrade trade(boolean isStartDay,int price,int date,int time);
}

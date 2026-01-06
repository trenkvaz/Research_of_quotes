package getting_quotes;

import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;

import static utilities.WriteReadJSON.readJSON;
import static utilities.WriteReadJSON.writeJSON;

public class FinamAPI {

    //https://api.finam.ru/v1/instruments/MXU5@RTSX/bars?interval.start_time=2025-03-01T00:00:00Z&interval.end_time=2025-03-08T00:00:00Z&timeframe=TIME_FRAME_M5


    public static JSONObject getSessionKey(JSONObject secretKey) throws Exception {
        HttpResponse<String> response = Unirest.post("https://api.finam.ru/v1/sessions")
                .header("Content-Type", "application/json")
                .body(secretKey.toString()).asString();
        return new JSONObject(response.getBody());
    }

    public static JSONObject getBars(JSONObject parameters) throws Exception{
        HttpResponse<String> response = Unirest.get("https://api.finam.ru/v1/instruments/"+parameters.getString("symbol")+"/bars?interval.start_time="+parameters.getString("start_date")+"T00%3A00%3A00Z&interval.end_time="+parameters.getString("ebd_date")+"T00%3A00%3A00Z&timeframe="+parameters.getString("time_frame"))
                .header("Authorization",parameters.getString("token")).asString();
        return new JSONObject(response.getBody());
    }




    public static void main(String[] args) throws Exception {
        //writeJSON(new JSONObject().put("secret",myToken),"my_token");
        //System.out.println(readJSON("my_token"));
        //System.out.println(getSessionKey(readJSON("my_token")));;
        writeJSON(getSessionKey(readJSON("my_token")),"session_token");

        //System.out.println(readJSON("названия инструментов на бирже"));
        System.out.println(readJSON("session_token").getString("token"));
    }
}

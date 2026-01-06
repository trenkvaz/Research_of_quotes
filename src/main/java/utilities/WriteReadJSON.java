package utilities;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;

public class WriteReadJSON {

    static String pathToJSONData = "J:\\dynamic_data\\JavaProjects\\Research_of_quotes\\src\\main\\resources\\jsons_data\\";


    public static void writeJSON(JSONObject settings, String name){
        try {
            OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(pathToJSONData+name+".json"));
            settings.write(writer);
            writer.close();
        } catch (Exception e) {
            System.out.println("writeJSON() "+e);
        }
    }

    public static JSONObject readJSON(String name){
        try {
            return new JSONObject(new JSONTokener(new FileReader(pathToJSONData+name+".json")));
        } catch (Exception e) {
            System.out.println("writeJSON() "+e);
            return new JSONObject();
        }
    }

}

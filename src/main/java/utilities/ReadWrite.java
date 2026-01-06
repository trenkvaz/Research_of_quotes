package utilities;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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

    public static void writeListStr(List<String> stringList,String path){
        try {
            Files.write(Paths.get(path), stringList);
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }


}

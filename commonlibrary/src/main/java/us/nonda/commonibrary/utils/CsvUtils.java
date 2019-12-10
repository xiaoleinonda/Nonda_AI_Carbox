package us.nonda.commonibrary.utils;

import android.util.Log;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.util.List;

public class CsvUtils {

    // 读取 .csv 文件
    private void readCsv(String path) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-8"));  // 防止出现乱码
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());
            Iterable<CSVRecord> csvRecords = csvParser.getRecords();
            for (CSVRecord csvRecord : csvRecords) {
              /*  ApacheBean apacheBean = new ApacheBean();
                apacheBean.setId(Integer.parseInt(csvRecord.get("id")));
                apacheBean.setName(csvRecord.get("name"));
                apacheBean.setAge(Integer.parseInt(csvRecord.get("age")));
                mList.add(apacheBean);*/
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 写入 .csv 文件
    public static void writeCsv(String filePath, List<String> data) {
        try {
            Log.d("情绪", "保存csv" + data.size());
            File file = new File(filePath+System.currentTimeMillis() + ".csv");
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));  // 防止出现乱码
            // 添加头部
            CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("name", "age", "race", "glasses", "gender", "emotionThree", "emotionSeven", "score"));
            // 添加内容
//            csvPrinter
            for (int i = 0; i < data.size(); i++) {
//                csvPrinter.printRecord(
//                        data.get(i).getName(),
//                        data.get(i).getAge(),
//                        data.get(i).getRace(),
//                        data.get(i).getGlasses(),
//                        data.get(i).getGender(),
//                        data.get(i).getEmotionThree(),
//                        data.get(i).getEmotionSeven(),
//                        data.get(i).getScore());
            }
            csvPrinter.printRecord();
            csvPrinter.flush();
        } catch (IOException e) {
            Log.d("情绪", "异常" + e.getMessage());
            e.printStackTrace();
        }
    }
}
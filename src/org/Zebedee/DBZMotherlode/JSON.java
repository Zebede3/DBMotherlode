package org.Zebedee.DBZMotherlode;

import org.dreambot.api.methods.Calculations;
import org.dreambot.api.methods.interactive.Players;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.dreambot.api.methods.MethodProvider.log;

public class JSON {

    final static String botName = Players.localPlayer().getName();

    final static String filePath = botName + ".json";

    static JSONParser parser = new JSONParser();

    static JSONObject obj;



    public static boolean fileExists(){
        File f = new File(filePath);

        return f.exists();
    }


    public static File getFile(){

        File file = new File(filePath);
        if(!file.exists()){
            log("Creating file");
            try {
                if(file.createNewFile()){
                    log("File created!");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            log("Got file:" + file.getAbsolutePath());
        }
        return file;

    }

    public static void writeJson(){

        JSONObject obj = new JSONObject();
        obj.put("name", Players.localPlayer().getName());
        obj.put("miningSpot", generateMiningArea());

        try{
            FileWriter file = new FileWriter(getFile());
            file.write(obj.toJSONString());
            file.flush();
            file.close();
        }catch(IOException e){
            log("Error writing to JSON file: " + e.getMessage());
        }
    }

    /*
    GETTERS
     */

    public static int getMiningArea(){
        log("Getting preferred mining area");
        try{
            JSONParser parser = new JSONParser();
            FileReader file = new FileReader(getFile());

            Object obj = parser.parse(file);

            JSONObject jObj = (JSONObject) obj;
            Long i = (Long) jObj.get("miningSpot");
            log("Got mining spot: " + i);
            return i.intValue();
        }catch(IOException | ParseException e){

            log("Error reading JSON file" + e);
            return -1;
        }
    }

    private static int generateMiningArea(){
        return Calculations.random(1,4);
    }



















}

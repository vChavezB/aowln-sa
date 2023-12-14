package com.github.vchavezb.utilities;

import java.io.*;

/**
 * Created by Thomas Farrenkopf on 22.01.17.
 */
public class FileUtil {

    public static InputStream getInputStream(String filePath){
        InputStream inputStream = null;
        File f = new File(filePath);

        if(f.exists()){
            try {
                inputStream = new FileInputStream(f);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        }else{
            inputStream = FileUtil.class.getClassLoader().getResourceAsStream(filePath);
        }

        return inputStream;
    }

}
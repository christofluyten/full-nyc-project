package com.github.christofluyten.IO;

import java.io.*;

/**
 * Created by christof on 28.04.17.
 */
public class IO {

    public static void writeFile(Object object, String path) throws IOException, ClassNotFoundException {
        System.out.println("start writing " + path);
        File file = new File(path);
        FileOutputStream f = new FileOutputStream(file);
        ObjectOutputStream s = new ObjectOutputStream(f);
        s.writeObject(object);
        f.close();
        s.close();
        System.out.println(path + " added");
    }

    public static Object readFile(String path) throws IOException, ClassNotFoundException {
        System.out.println("start reading " + path);
        File file = new File(path);
        FileInputStream f = new FileInputStream(file);
        ObjectInputStream s = new ObjectInputStream(f);
        Object object = s.readObject();
        s.close();
        f.close();
        System.out.println(path +" read");
        return object;
    }
}

package com.example;

import com.example.api.ElpriserAPI;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;


public class Main {
    public static void main(String[] args) {
        ElpriserAPI elpriserAPI = new ElpriserAPI();

        //Temporära variablar som blir tilldelade vid start
        String date = "2025-09-25";
        ElpriserAPI.Prisklass zone = ElpriserAPI.Prisklass.SE4;

        //Skapar vårat locale och decimal format
        Locale locale = new Locale("sv", "SV");
        String pattern = "###.##";
        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(locale);
        df.applyPattern(pattern);

        //Hämta elpris listan för speciferatt datum och zon
        List<ElpriserAPI.Elpris> list = elpriserAPI.getPriser(date, zone);

        //Hämta priserna från elpris listan
        double[] prisAry = new double[list.size()];
        for(int i =0; i < list.size(); i++){
            prisAry[i] = elprisNumberseperator(list.get(i).toString());
        }

        //Hittar det högsta och lägsta priset och medelvärdet på priserna och skapar en String för dem alla
        double average = 0;
        double highest = 0;
        int highestIndex = 0;
        double lowest = Double.MAX_VALUE;
        int lowestIndex = 0;
        for(int i = 0; i < prisAry.length; i++){
            average += prisAry[i];
            if(prisAry[i] > highest){
                highest = prisAry[i];
                highestIndex = i;
            }
            if(prisAry[i] < lowest){
                lowest = prisAry[i];
                lowestIndex = i;
            }
        }
        average = average / prisAry.length;
        String averageString = df.format(average) + " öre";
        String lowestString = df.format(lowest) + " öre";
        String highestString = df.format(highest) + " öre";




        //Temporära outputs

        System.out.println("Average is " + averageString);
        System.out.println("Lowest price was " + lowestString + ", at hour " + lowestIndex);
        System.out.println("Highest price was " + highestString + ", at hour " + highestIndex);

        int range = 4;
        int window = slidingWindow(prisAry, range);
        int endWindow = window + range;

        System.out.println("Cheapest time period for " + range + " hours is " + window + " to " + endWindow);


    }

    private static double elprisNumberseperator(String string){

        int startIndex = 0;
        boolean startChecker = false;
        int endIndex = 0;

        for (int i = 0; i < string.length(); i++) {
            boolean isNumber = string.charAt(i) >= 48 && string.charAt(i) <= 57;
            if ( !startChecker && (isNumber || string.charAt(i) == 46 )) {
                startIndex = i;
                startChecker = true;
            }
            if (startChecker && (isNumber || string.charAt(i) == 46)) {
                endIndex = i;
            }

            if(startChecker && endIndex < i){
                break;
            }

        }

        return Double.parseDouble(string.substring(startIndex,endIndex))*100;
    }

    private static int slidingWindow(double[] ary, int range){
        int length = ary.length;

        if (length <= range){
            System.out.println("Error: Range too large.");
            return -1;
        }

        double sum = 0;
        for (int i = 0; i < range; i++)
            sum += ary[i];


        int index = 0;
        double currentSum = sum;
        for (int i = range; i < length; i++){
            currentSum += ary[i] - ary[i - range];
            if (currentSum < sum){
                sum = currentSum;
                index = i;
            }
        }
        return index;
    }



}

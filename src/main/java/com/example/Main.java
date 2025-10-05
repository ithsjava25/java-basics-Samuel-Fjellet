package com.example;

import com.example.api.ElpriserAPI;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;


public class Main {

    static String date = "";
    static int range = 0;
    static ElpriserAPI.Prisklass zone;
    static int listSize;
    static TidsPeriod[] prisAry;
    static boolean flagSorted = false;
    static boolean flagDate = false;




    public static void main(String[] args) {
        ElpriserAPI elpriserAPI = new ElpriserAPI();


        //Om något får fel under initialiseringen avbryter programmet.
        if(initializer(args)){
            return;
        }

        //Skapar vårat locale och decimal format
        Locale locale = new Locale("sv", "SE");
        String pattern = "##0.00";
        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(locale);
        df.applyPattern(pattern);

        //Skapar vårat date format
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd");

        //Hämta elpris listan för specification datum och zon
        //Om elpris klassen är tom när vi får den, hämta en från dagen innan
        if (flagDate && !isValidDate(date, "yyyy-MM-dd")){
            System.out.println("ogiltigt datum.");
            return;
        }

        if (!flagDate){
            date = LocalDate.now().toString();
        }

        var list = elpriserAPI.getPriser(date, zone);



        if (range == 2 || range == 4 || range == 8) {
            //Skapar följande datum för att kunna jämföra efter midnatt
            LocalDate extraDate = LocalDate.parse(date, formatter).plusDays(1);
            String extraDateString = extraDate.toString();


            var extraList = elpriserAPI.getPriser(extraDateString, zone);

            prisAry = prisCompiler(list);

            //Så länge den extra dagens data existerar
            if (!extraList.isEmpty()){
                var extraAry = prisCompiler(extraList);
                //Kollar om vi fick mindre data än vi förväntade oss och använder i så fall mindre utrymmer
                int tempRange = range;
                if (extraAry.length < tempRange) {
                    tempRange = extraAry.length;
                }

                //skapar en temporär ary för att kombinera vår prisAry och extraAry i
                var tempAry = new TidsPeriod[prisAry.length + tempRange - 1];

                //Lägger in alla element från prisAry of extraAry in i vår temporära ary
                for(int i = 0; i < prisAry.length + tempRange - 1; i++){
                    if (i < prisAry.length){
                        tempAry[i] = prisAry[i];
                    } else {
                        tempAry[i] = extraAry[i-prisAry.length];
                    }
                }
                prisAry = tempAry;

                listSize = list.size() + extraAry.length;


            } else {
                listSize = list.size();
            }



            int endWindow = slidingWindow(prisAry, range);

            if(endWindow == -1){
                return;
            }

            int startWindow = endWindow - range+1;
            double tempAverage = prisAry[startWindow].pris;

            for(int i = startWindow+1; i <= endWindow; i++){
                tempAverage += prisAry[i].pris;
            }

            tempAverage = tempAverage/range;
            String tempAveragestring = df.format(tempAverage);

            System.out.println("Billigaste tids period för " + range + " timmar är: från kl " + prisAry[startWindow].tid + " till kl " + prisAry[endWindow].tid);
            System.out.println("Påbörja laddning klockan " + prisAry[startWindow].tid + " med ett Medelpris för fönster: " + tempAveragestring + " öre");

        } else {
            listSize = list.size();
            prisAry = prisCompiler(list);
        }


        //Hittar det högsta och lägsta priset och medelvärdet på priserna och skapar en String för dem alla
        double average = 0;
        double highest = 0;
        String highestPeriod = "";
        double lowest = Double.MAX_VALUE;
        String lowestPeriod = "";


        for (TidsPeriod tidsPeriod : prisAry) {
            average += tidsPeriod.pris;
            if (tidsPeriod.pris > highest) {
                highest = tidsPeriod.pris;
                highestPeriod = tidsPeriod.period;
            }
            if (tidsPeriod.pris < lowest) {
                lowest = tidsPeriod.pris;
                lowestPeriod = tidsPeriod.period;
            }
        }
        average = average / prisAry.length;
        String averageString = df.format(average) + " öre";
        String lowestString = df.format(lowest) + " öre";
        String highestString = df.format(highest) + " öre";

        System.out.println("Medelpris är " + averageString);
        System.out.println("Lägsta pris är " + lowestString + ", vid tiderna " + lowestPeriod);
        System.out.println("Högsta pris är " + highestString + ", vid tiderna " + highestPeriod);


        if (flagSorted){
            var listSorted = bubbleSorter(prisCompiler(list));

            List<String> sortedStringarray = new ArrayList<>();
            for (TidsPeriod tidsPeriod : listSorted) {
                String sortedString = tidsPeriod.period + " " + tidsPeriod.pris + " öre";
                sortedStringarray.add(sortedString);
            }

            System.out.println(sortedStringarray);

        }


    }

    //Hämta priserna från elpris listan och läger dem i en array av klasen TidsPeriod


    public static TidsPeriod[] bubbleSorter(TidsPeriod[] list){
        TidsPeriod temp;
        boolean swapped;
        for(int i = 0; i < list.length - 1; i++){
            swapped = false;
            for(int j = 0; j < list.length - i - 1; j++){
                if(list[j].pris > list[j + 1].pris){

                    temp = list[j];
                    list[j] = list[j + 1];
                    list[j + 1] = temp;
                    swapped = true;
                }
                if(!swapped)
                    break;
            }

        }

        return list;
    }

    public static TidsPeriod[] prisCompiler(List<ElpriserAPI.Elpris> list){
        var prisAry = new TidsPeriod[24];

        //För varje timme, går igenom list och lägger ihop medelvärdet för varje tillgänglig timme i prisAry.
        for(int i = 0; i < 24; i++){

            double hourPris = 0;
            int hourAverage = 0;

            for (ElpriserAPI.Elpris currentElement : list) {
                int currentHour = currentElement.timeStart().getHour();

                if (currentHour == i) {
                    hourPris += currentElement.sekPerKWh();
                    hourAverage += 1;
                }
            }

            prisAry[i] = new TidsPeriod(hourPris/hourAverage, i);

        }

        int index = 0;
        while (index < prisAry.length){
            if(Double.isNaN(prisAry[index].pris)){

                prisAry = removeElement(prisAry, index);
            }
            else
                index += 1;
        }

        return prisAry;
    }

    private static boolean initializer(String[] args){

        //Om input är tom ska programmet returnera hjälp medellandet
        if(args.length==0){
            helpPrinter();
            return true;
        }

        //Skapar flaggor för parametrar vi kollar efter
        boolean flagZon = false;
        boolean flagRange = false;
        String zon = "";

        //Hämtar/skapar variabler från parametrar
        for(String arg : args){
            if(arg.equals("--zone")){
                flagZon = true;
            }
            else if(flagZon){
                zon  = arg;
                flagZon = false;
            }
            else if(arg.equals("--date")){
                flagDate = true;
            }
            else if(flagDate && Objects.equals(date, "")) {
                date = arg;
            }
            else if(arg.equals("--charging")) {
                flagRange = true;
            }
            else if(flagRange) {
                if (arg.equals("2h") || arg.equals("4h") || arg.equals("8h")) {
                    range = Integer.parseInt(arg.substring(0, 1));
                }
                flagRange = false;

            }
            else if(arg.equals("--sorted"))
                flagSorted = true;
            else if(arg.equals("--help")) {
                helpPrinter();
                return true;
            }
        }

        if(zon.isBlank()){
            System.out.println("Saknar zone");
            return true;
        }

        //Får det korrekta zone formatet till elpris APIn
        switch(zon){
            case "SE1":
                zone = ElpriserAPI.Prisklass.SE1;
                break;
            case "SE2":
                zone = ElpriserAPI.Prisklass.SE2;
                break;
            case "SE3":
                zone = ElpriserAPI.Prisklass.SE3;
                break;
            case "SE4":
                zone = ElpriserAPI.Prisklass.SE4;
                break;
            default:
                System.out.println("ogiltig zon");
                return true;

        }
        return false;
    }

    private static int slidingWindow(TidsPeriod[] ary, int range) {
        int length = ary.length;

        if (length <= range){
            System.out.println("Error: För lång laddnings tid.");
            return -1;
        }

        double sum = 0;
        for (int i = 0; i < range; i++)
            sum += ary[i].pris;


        int index = 0;
        double currentSum = sum;
        for (int i = range; i < length; i++){
            currentSum += ary[i].pris - ary[i-range].pris;
            if (currentSum < sum){


                sum = currentSum;
                index = i;
            }
        }
        return index;
    }

    private static void helpPrinter(){
        System.out.println("------------------Script usage----------------------------------------------------------------");
        System.out.println("Input options:         | Example: --zone SE3 --date 2025/05/28 -- charging 8h -- sorted");
        System.out.println("--zone SE1|SE2|SE3|SE4 | Required. Price zone to check.");
        System.out.println("--date yyyy/MM/dd      | Optional. Date for the check, uses today's date if nothing was input.");
        System.out.println("--charging 2h|4h|8h    | Optional. Finds the cheapest time period with given hour period.");
        System.out.println("--sorted               | Optional. Sorts prices in descending order.");
    }

    public static boolean isValidDate(String dateStr, String format) {
        // Define the date format
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);

        try {
            // Parse the date string
            LocalDate.parse(dateStr, formatter);
            return true; // If parsing succeeds, the date is valid
        } catch (DateTimeParseException e) {
            return false; // If parsing fails, the date is invalid
        }
    }

    public static TidsPeriod[] removeElement(TidsPeriod[] arr, int index){

        if (arr == null || index < 0 || index >= arr.length){
            return arr;
        }

        TidsPeriod[] newArr = new TidsPeriod[arr.length - 1];

        for (int i = 0, k = 0; i < arr.length; i++){
            if (i == index){
                continue;
            }
            newArr[k++] = arr[i];
        }
        return newArr;
    }


}



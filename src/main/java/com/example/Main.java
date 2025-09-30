package com.example;

import com.example.api.ElpriserAPI;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;


public class Main {
    public static void main(String[] args) {
        ElpriserAPI elpriserAPI = new ElpriserAPI();

        String date = LocalDate.now().toString();

        if(args.length==0){
            helpPrinter();
            return;
        }

        //Skapar flaggor för parametrar
        boolean flagZon = false;
        boolean flagDate = false;
        boolean flagRange = false;
        boolean flagSorted = false;
        String zon = "";
        int range = 0;

        if(args.length == 0){
            helpPrinter();
            return;
        }

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
            else if(flagDate) {
                date = arg;
                flagDate = false;
            }
            else if(arg.equals("--charging")) {
                flagRange = true;
            }
            else if(flagRange) {
                range = Integer.parseInt(arg.substring(0,1));
                flagRange = false;
            }
            else if(arg.equals("--sorted"))
                flagSorted = true;
            else if(arg.equals("--help")) {
                helpPrinter();
                return;
            }
        }

        if(zon.isBlank()){
            System.out.println("Saknar zone");
            return;
        }

        //Får det korrekta zone formatet till elpris APIn
        ElpriserAPI.Prisklass zone = ElpriserAPI.Prisklass.SE1;
        switch(zon){
            case "SE1":
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
                return;

        }


        //Skapar vårat locale och decimal format
        Locale locale = new Locale("sv", "SE");
        String pattern = "###.##";
        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(locale);
        df.applyPattern(pattern);

        //Hämta elpris listan för specification datum och zon
        //Om elpris klassen är tom när vi får den, hämta en från dagen innan
        var list = elpriserAPI.getPriser(date, zone);
        if (list.isEmpty()) {
            System.out.println("Ogiltigt datum. Använder dagens priser.");
            list = elpriserAPI.getPriser(LocalDate.now().toString(), zone);
        }

        //Hämta priserna från elpris listan och läger dem i en array av klasen TidsPeriod
        var prisAry = new TidsPeriod[list.size()];
        for(int i =0; i < list.size(); i++){
            var temp = list.get(i);
            prisAry[i] = new TidsPeriod(temp.sekPerKWh(), temp.timeStart());
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

        if(range == 2 || range == 4 || range == 8){

            System.out.println(date);

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("yyyy-MM-dd");

            LocalDate extraDate = LocalDate.parse(date, formatter).plusDays(1);
            String extraDateString = extraDate.toString();
            var extraList = elpriserAPI.getPriser(extraDateString, zone);
            var extraAry = new TidsPeriod[range-1];
            for(int i =0; i < range-1; i++){
                var temp = extraList.get(i);
                extraAry[i] = new TidsPeriod(temp.sekPerKWh(), temp.timeStart());
            }

            int window = slidingWindow(prisAry, range, extraAry);
            int endWindow = window + range;
            double tempAverage = prisAry[window].pris;
            for(int i = window+1; i < endWindow; i++){
                if(i < prisAry.length) {
                    tempAverage += prisAry[i].pris;
                }else if(i >= prisAry.length){
                    tempAverage += extraAry[i- prisAry.length].pris;
                }
            }
            tempAverage = tempAverage/range;
            String tempAveragestring = df.format(tempAverage);

            System.out.println("Biligaste tids period för " + range + " timmar är: från kl " + window + " till kl " + endWindow);
            System.out.println("Påbörja laddning klockan " + window + " med ett medelvärda på " + tempAveragestring);
        }

    }


    private static int slidingWindow(TidsPeriod[] list, int range,  TidsPeriod[] extraList) {

        int tempLength = list.length + extraList.length;
        TidsPeriod[] ary = new TidsPeriod[tempLength];
        for (int i = 0; i < tempLength; i++) {
            if (i < list.length) {
                ary[i] = list[i];
            }else if (i < list.length + extraList.length) {
                ary[i] = extraList[i-list.length];
            }
        }

        int length = ary.length;

        if (length <= range){
            System.out.println("Error: Range too large.");
            return -1;
        }

        double sum = 0;
        for (int i = 0; i < range; i++)
            sum += ary[i].pris;


        int index = 0;
        double currentSum = sum;
        for (int i = range; i < length; i++){
            currentSum += ary[i].pris - ary[i - range].pris;
            if (currentSum < sum){
                sum = currentSum;
                index = i;
            }
        }
        return index;
    }

    private static void helpPrinter(){
        System.out.println("------------------Script usage---------------------------------------------------------");
        System.out.println("Input options:         | Exempel: --zone SE3 --date 2025/05/28 -- charging 8h -- sorted");
        System.out.println("--zone SE1|SE2|SE3|SE4 | Required. Pris zon för kollen");
        System.out.println("--date yyyy/MM/dd      | Optional. Datum för kollen, väljer dagens om inget datum är givet.");
        System.out.println("--charging 2h|4h|8h    | Optional. Hitta den billigaste tids perioden med given tim period.");
        System.out.println("--sorted               | Optional. Sorterar Priserna Från dyrast till billigast");
    }

    
}

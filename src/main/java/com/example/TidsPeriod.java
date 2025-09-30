package com.example;

import java.time.ZonedDateTime;

public class TidsPeriod {

    double pris;
    String period;
    String tid;

    public TidsPeriod(double pris, ZonedDateTime startTid){
        this.pris = pris*100;
        this.period = switch (startTid.getHour()) {
            case 0 -> "00-01";
            case 1 -> "01-00";
            case 2 -> "02-03";
            case 3 -> "03-04";
            case 4 -> "04-05";
            case 5 -> "05-06";
            case 6 -> "06-07";
            case 7 -> "07-08";
            case 8 -> "08-09";
            case 9 -> "09-10";
            case 10 -> "10-11";
            case 11 -> "11-12";
            case 12 -> "12-13";
            case 13 -> "13-14";
            case 14 -> "14-15";
            case 15 -> "15-16";
            case 16 -> "16-17";
            case 17 -> "17-18";
            case 18 -> "18-19";
            case 19 -> "19-20";
            case 20 -> "20-21";
            case 21 -> "21-22";
            case 22 -> "22-23";
            case 23 -> "23-24";
            default -> "";

        };

        this.tid = switch (startTid.getHour()) {
            case 0 -> "00:00";
            case 1 -> "01:00";
            case 2 -> "02:00";
            case 3 -> "03:00";
            case 4 -> "04:00";
            case 5 -> "05:00";
            case 6 -> "06:00";
            case 7 -> "07:00";
            case 8 -> "08:00";
            case 9 -> "09:00";
            case 10 -> "10:00";
            case 11 -> "11:00";
            case 12 -> "12:00";
            case 13 -> "13:00";
            case 14 -> "14:00";
            case 15 -> "15:00";
            case 16 -> "16:00";
            case 17 -> "17:00";
            case 18 -> "18:00";
            case 19 -> "19:00";
            case 20 -> "20:00";
            case 21 -> "21:00";
            case 22 -> "22:00";
            case 23 -> "23:00";
            default -> "";
        };


    }




}

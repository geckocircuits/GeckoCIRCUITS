/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations AG
 *
 *  GeckoCIRCUITS is free software: you can redistribute it and/or modify it under 
 *  the terms of the GNU General Public License as published by the Free Software 
 *  Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  GeckoCIRCUITS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  GeckoCIRCUITS.  If not, see <http://www.gnu.org/licenses/>.
 */

package ch.technokrat.gecko.geckocircuits.circuit.circuitcomponents;

import java.util.ArrayList;

/**
 *
 * @author anstupar
 */
public class CapacitanceCharacteristic {

    private ArrayList<Double> y_val_C;
    private ArrayList<Double> x_val_V;
    private boolean isLogYScale;

    public CapacitanceCharacteristic(double[] nonliny, double[] nonlinx, boolean logscaleY) {
        y_val_C = new ArrayList<Double>();
        x_val_V = new ArrayList<Double>();
        isLogYScale = logscaleY;
        assert nonliny.length == nonlinx.length;
        for (int i = 0; i < nonliny.length; i++) {
            y_val_C.add(new Double(nonliny[i]));
        }
        for (int i = 0; i < nonlinx.length; i++) {
            x_val_V.add(new Double(nonlinx[i]));
        }

        //System.out.println(x_val_V.toString());
        //System.out.println(y_val_C.toString());

        /*double[] testval = {10.0, 25.1, 33.7, 58.9, 105.6, 273.54, 352.798, 426.9, 482.8723};
         for (int i = 0; i < testval.length; i++)
         System.out.println("" + testval[i] + " " + getCapacitanceAtV(testval[i]));*/
    }

    public CapacitanceCharacteristic() {
        isLogYScale = true;
        y_val_C = new ArrayList<Double>();
        x_val_V = new ArrayList<Double>();
    }

    public void setLogYScale(boolean logscaleY) {
        isLogYScale = logscaleY;
    }

    public double[] exportY() {
        double nonliny[] = new double[y_val_C.size()];

        for (int i = 0; i < nonliny.length; i++) {
            nonliny[i] = y_val_C.get(i).doubleValue();
        }

        return nonliny;
    }

    public double[] exportX() {
        double nonlinx[] = new double[x_val_V.size()];

        for (int i = 0; i < nonlinx.length; i++) {
            nonlinx[i] = x_val_V.get(i).doubleValue();
        }

        return nonlinx;
    }

    public void insertVC_point(double V, double C) {
        if (V > x_val_V.get(x_val_V.size() - 1).doubleValue()) {
            //add at end of list if V value is larger than last V value (since list is sorted ascending)
            x_val_V.add(new Double(V));
            y_val_C.add(new Double(C));
        } else if (characteristicContainsVpoint(V) >= 0) {
            //already exists point with this x-value, replace it with new point
            int valuetoreplace_index = characteristicContainsVpoint(V);
            x_val_V.set(valuetoreplace_index, new Double(V));
            y_val_C.set(valuetoreplace_index, new Double(C));
        } else {
            //new point that comes somewhere in the middle of the existing list; find place, insert
            for (Double point : x_val_V) {
                if (point.doubleValue() > V) {
                    int insert_index = x_val_V.indexOf(point);
                    x_val_V.add(insert_index, new Double(V));
                    y_val_C.add(insert_index, new Double(C));
                    break;
                }
            }
        }

    }

    private int characteristicContainsVpoint(double V) {
        //checks if there is already a capacitance defined for a particular V point, returns index if it does, otherwise returns negative value
        int existing_index = -1;
        for (Double point : x_val_V) {
            if (point.doubleValue() == V) {
                existing_index = x_val_V.indexOf(point);
                break;
            }
        }
        return existing_index;
    }

    public double getCapacitanceAtV(double V) {
        //function that evaluates the non-linear characteristic to return a capacitance at a specific voltage
        int i = 0;
        int vBound = x_val_V.size();
        //go through stored x values (voltages) to find one closest to required value
        while ((i < vBound) && (x_val_V.get(i).doubleValue() < V)) {
            i++;
        }

        //if desired value outside of or on the bounds of entered range, return values at the bounds
        if (i == 0) {
            return x_val_V.get(0).doubleValue();
        }
        if (i >= vBound) {
            return x_val_V.get(x_val_V.size() - 1).doubleValue();
        }

        //otherwise interpolate
        double x1 = x_val_V.get(i - 1);
        double x2 = x_val_V.get(i);


        //if using logarithmic scale on Y axis (for C)
        if (isLogYScale) {
            double lg10_y1 = Math.log10(y_val_C.get(i - 1));
            double lg10_y2 = Math.log10(y_val_C.get(i));
            double lg10_y = lg10_y1 + (lg10_y2 - lg10_y1) * (V - x1) / (x2 - x1);
            return Math.pow(10, lg10_y);
        } else //otherwise simple linear interpolation
        {
            double y1 = y_val_C.get(i - 1);
            double y2 = y_val_C.get(i);
            double y = y1 + ((y2 - y1) / (x2 - x1)) * (V - x1);
            return y;
        }

    }
}

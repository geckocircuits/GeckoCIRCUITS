/*  This file is part of GeckoCIRCUITS. Copyright (C) ETH Zurich, Gecko-Simulations GmbH
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
package gecko.geckocircuits.control;

import gecko.geckocircuits.allg.UserParameter;
import gecko.geckocircuits.circuit.circuitcomponents.TextInfoType;
import gecko.i18n.resources.I18nKeys;

public final class Cispr16Settings {
    
    private static final double DEFAULT_MAX_FREQ = 2000000;
    private static final double DEFAULT_MIN_FREQ = 9000;
        
    
    public final UserParameter<Boolean> _showName;
    public final UserParameter<Boolean> _peak;
    public final UserParameter<Boolean> _qpeak;
    public final UserParameter<Boolean> _average;
    public final UserParameter<Boolean> _useBlackman;
    public final UserParameter<Boolean> _showRMSValues;    
    
    public final UserParameter<Double> _maxFreq;    
    public final UserParameter<Double> _minFreq;    
    public final UserParameter<Double> _qpInteval;
    public final UserParameter<Double> _filterThreshold;
    
    public final UserParameter<Boolean> _automaticQPSelection;
    
    public Cispr16Settings(final ReglerCISPR16 regler) {
         _showName = UserParameter.Builder.
            <Boolean>start("showName", true).
            longName(I18nKeys.DISPLAY_COMPONENT_NAME_IN_CIRCUIT_SHEET).
            shortName("showName").            
            showInTextInfo(TextInfoType.SHOW_NEVER).            
            arrayIndex(regler, -1).
            build();
         
         _peak = UserParameter.Builder.
            <Boolean>start("calcPeak", true).
            longName(I18nKeys.CONSIDER_PEAK_CALCULATION).
            shortName("calcPeak").            
            showInTextInfo(TextInfoType.SHOW_NEVER).            
            arrayIndex(regler, -1).
            build();
         
         _qpeak = UserParameter.Builder.
            <Boolean>start("calcQPeak", true).
            longName(I18nKeys.CONSIDER_QUASI_PEAK_CALCULATION).
            shortName("quasiPeak").            
            showInTextInfo(TextInfoType.SHOW_NEVER).            
            arrayIndex(regler, -1).
            build();
         
         _filterThreshold = UserParameter.Builder.
            <Double>start("filterThreshold", 0.5).
            longName(I18nKeys.FILTER_THRESHOLD).
            shortName("filterThreshold").            
            showInTextInfo(TextInfoType.SHOW_NEVER).            
            arrayIndex(regler, -1).
            build();
         
         _average = UserParameter.Builder.
            <Boolean>start("calcAverage", false).
            longName(I18nKeys.CONSIDER_AVERAGE_CALCULATION).
            shortName("average").            
            showInTextInfo(TextInfoType.SHOW_NEVER).            
            arrayIndex(regler, -1).
            build();                  
         
         _useBlackman = UserParameter.Builder.
            <Boolean>start("useBlackman", true).
            longName(I18nKeys.USE_BLACKMAN_WINDOW).
            shortName("useBlackman").            
            showInTextInfo(TextInfoType.SHOW_NEVER).            
            arrayIndex(regler, -1).
            build();
         
         _showRMSValues = UserParameter.Builder.
            <Boolean>start("showRMS", true).
            longName(I18nKeys.SHOW_OUTPUTS_AS_RMS_VALUES).
            shortName("showRMS").            
            showInTextInfo(TextInfoType.SHOW_NEVER).            
            arrayIndex(regler, -1).
            build();    
         
         _maxFreq = UserParameter.Builder.
            <Double>start("maxFreq", DEFAULT_MAX_FREQ).
            longName(I18nKeys.MAXIMUM_FREQUENCY_IN_CALCULATION).
            shortName("maximumFrequency").            
            unit("Hz").
            showInTextInfo(TextInfoType.SHOW_NEVER).            
            arrayIndex(regler, -1).
            build();    
         
         _minFreq = UserParameter.Builder.
            <Double>start("minFreq", DEFAULT_MIN_FREQ).
            longName(I18nKeys.MINIMUM_FREQUENCY_IN_CALCULATION).
            shortName("minimumFrequency").            
            unit("Hz").
            showInTextInfo(TextInfoType.SHOW_NEVER).            
            arrayIndex(regler, -1).
            build();   
         
         _automaticQPSelection = UserParameter.Builder.
            <Boolean>start("automaticQPSelection", true).
            longName(I18nKeys.AUTO_QP_SELECTION).
            shortName("autoQPSelection").            
            showInTextInfo(TextInfoType.SHOW_NEVER).            
            arrayIndex(regler, -1).
            build();
         
         _qpInteval = UserParameter.Builder.
            <Double>start("QPinterval", 9000.0).
            longName(I18nKeys.QP_INTERVAL).
            shortName("QPinterval").            
            unit("Hz").
            showInTextInfo(TextInfoType.SHOW_NEVER).            
            arrayIndex(regler, -1).
            build(); 
    }        
    
}

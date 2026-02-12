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
package gecko.geckocircuits.newscope;

import gecko.geckocircuits.datacontainer.AbstractDataContainer;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author andy
 */
abstract class AbstractCurvePainter {
  AbstractDataContainer _ramData;
  final Axis _xAxis;
  final AbstractCurve _curve;
  final DataLoader _dataLoader = new DataLoader(this);
  ConcurrentLinkedQueue<Point2D.Float> _allPoints = new ConcurrentLinkedQueue<Point2D.Float>();

    abstract void paintComponent(final Graphics2D g2d);
    
    /*
     * load the full data range up to the given pixel value
     */
    abstract void reLoadData(final int minimumPixel, final int maximumPixel);
    
    /*
     * load only part of the data, from startPixel to stopPixel
     */
    abstract void loadDataRange(final int startPixel, final int stopPixel);
    
                
  
    abstract List<Axis> getSensitiveAxis();
  
  public AbstractCurvePainter(final AbstractCurve curve){
    _curve = curve;
    _xAxis = curve._xAxis;

  }

  
  public AbstractDataContainer getRamData(){
    return _ramData;
  }

  
  public void setRamData(final AbstractDataContainer newData){
    _ramData = newData;
  }

  
  public Axis getXAxis(){
    return _xAxis;
  }

  public void loadRequiredData(final AbstractDataContainer newData, final boolean forceLoad){
    if(newData == null){
      return;
    }
    _dataLoader.loadRequiredData(newData, forceLoad);
  }

  GeckoSymbol getPointSymbol(){
    if(_curve.isSymbolEnabled()){
      return _curve.getSymbol();
    }else{
      return null;
    }
  }

  void addValueToAllPoints(final Object value, final float xPixel){
    if(value instanceof HiLoData){
      final HiLoData hiLoData = (HiLoData)value;
      _allPoints.add(new Point2D.Float(xPixel, hiLoData._yLo));
      _allPoints.add(new Point2D.Float(xPixel, hiLoData._yHi));
    }else if(value instanceof Float){
      _allPoints.add(new Point2D.Float(xPixel, (Float)value));
    }else if(value instanceof Double){
      _allPoints.add(new Point2D.Float(xPixel, ((Double)value).floatValue()));
    }else{
      assert false;
    }
  }
}

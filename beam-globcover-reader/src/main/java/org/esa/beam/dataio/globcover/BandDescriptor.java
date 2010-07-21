/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.dataio.globcover;

/**
 * @author Marco Peters
* @since BEAM 4.7
*/
class BandDescriptor {
    private String name;
    private Integer dataType;
    private int width;
    private int height;
    private String description;
    private double scaleFactor;
    private double offsetValue;
    private String unit;
    private boolean fillValueUsed;
    private double fillValue;

    public void setName(String name) {
        this.name = name;
    }

    public void setDataType(Integer dataType) {
        this.dataType = dataType;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setScaleFactor(double scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    public void setOffsetValue(double offsetValue) {
        this.offsetValue = offsetValue;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public void setFillValueUsed(boolean fillValueUsed) {
        this.fillValueUsed = fillValueUsed;
    }

    public void setFillValue(double fillValue) {
        this.fillValue = fillValue;
    }

    public String getName() {
        return name;
    }

    public Integer getDataType() {
        return dataType;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getDescription() {
        return description;
    }

    public double getScaleFactor() {
        return scaleFactor;
    }

    public double getOffsetValue() {
        return offsetValue;
    }

    public String getUnit() {
        return unit;
    }

    public boolean isFillValueUsed() {
        return fillValueUsed;
    }

    public double getFillValue() {
        return fillValue;
    }

}

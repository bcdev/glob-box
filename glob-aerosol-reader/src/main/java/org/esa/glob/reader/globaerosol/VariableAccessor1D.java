/*
 * $Id: $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.glob.reader.globaerosol;

import java.io.IOException;

import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Variable;


public class VariableAccessor1D {

    private final Variable variable;
    private final int indexDim;
    private final int rank;

    public VariableAccessor1D(Variable variable, String dimemsionName) {
        this.variable = variable;
        this.indexDim = variable.findDimensionIndex(dimemsionName);
        this.rank = variable.getRank();
    }
    
    public Array read(int offset, int length) throws IOException, InvalidRangeException {
        return variable.read(getSection(offset, length)).reduce();
    }
    
    private Section getSection(int offset, int length) throws InvalidRangeException {
        int[] origin = new int[rank];
        int[] size = new int[rank];
        for (int i = 0; i < rank; i++) {
            if (i == indexDim) {
                origin[i] = offset;
                size[i] = length;
            } else {
                origin[i] = 0;
                size[i] = 1;
            }
        }
        return new Section(origin, size);
    }
}

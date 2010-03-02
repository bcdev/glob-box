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
package org.esa.beam.dataio.arcbin;

import java.io.File;
import java.io.IOException;


public class Main {


    public static void main(String[] args) throws IOException {
        File dir = new File(args[0]);
        if (!dir.exists()) {
            System.out.println("dir does not exist!!");
        }
        ProjectionReader projectionReader = new ProjectionReader(new File(dir, "prj.adf"));
        System.exit(0);
    }


}

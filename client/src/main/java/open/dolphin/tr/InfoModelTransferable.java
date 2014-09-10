/*
 * InfoModelTransferable.java
 * Copyright (C) 2002 Dolphin Project. All rights reserved.
 * Copyright (C) 2004 Digital Globe, Inc. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *	
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *	
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package open.dolphin.tr;

import java.awt.datatransfer.*;
import java.io.IOException;
import open.dolphin.infomodel.RegisteredDiagnosisModel;

     
/**
 * Transferable class of the IInfoModel.
 * (スタンプ箱から病名)
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author masuda, Masuda Naika
 */ 
public final class InfoModelTransferable extends DolphinTransferable {

    /** Data Flavor of this class */
    public static DataFlavor infoModelFlavor = 
            new DataFlavor(RegisteredDiagnosisModel.class, "RegisteredDiagnosis");

    public static final DataFlavor[] flavors = {InfoModelTransferable.infoModelFlavor, DataFlavor.stringFlavor};
    
    // 複数対応 masuda
    private final RegisteredDiagnosisModel[] infoModels;

    /** Creates new InfoModelTransferable */
    public InfoModelTransferable(RegisteredDiagnosisModel[] infoModels) {
        this.infoModels = infoModels;
    }

    @Override
    public synchronized DataFlavor[] getTransferDataFlavors() {
    	return flavors;
    }
     
    @Override
    public boolean isDataFlavorSupported( DataFlavor flavor ) {
        for (DataFlavor df : flavors) {
            if (df.equals(flavor)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized Object getTransferData(DataFlavor flavor)
	    throws UnsupportedFlavorException, IOException {

        if (infoModelFlavor.equals(flavor)) {
            return infoModels;
        } else if (DataFlavor.stringFlavor.equals(flavor)) {
            return getDiagnosisText();
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }

    @Override
    public String toString() {
        return "InfoModel Transferable";
    }
    
    // 病名のテキストを作る
    private String getDiagnosisText() {

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (RegisteredDiagnosisModel rd : infoModels) {
            if (!first) {
                sb.append("\n");
            } else {
                first = false;
            }
            sb.append("＃");
            sb.append(rd.getDiagnosis());
            sb.append("(").append(rd.getStartDate()).append(")");
        }
        return sb.toString();
    }
}

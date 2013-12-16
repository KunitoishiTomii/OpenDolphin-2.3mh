/*
 * TextStamp.java
 * Copyright (C) 2002 Dolphin Project. All rights reserved.
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
package open.dolphin.infomodel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import javax.persistence.Transient;

/**
 * TextStampModel
 * 
 * @author Kazushi Minagawa
 * @author modified by masuda, Masuda Naika
 */
public class TextStampModel implements IStampModel {

    private String text;
    
    /**
     * Creates a new instance of TextStamp
     */
    public TextStampModel() {
    }

    public String getText() {
        return text;
    }

    public void setText(String val) {
        text = val;
    }
    
    // スタンプ箱のエディタで使用
    @Transient
    @JsonIgnore
    private String stampId;

    @Transient
    @JsonIgnore
    private String stampName;
    
    public void setStampId(String stampId) {
        this.stampId = stampId;
    }
    public String getStampId() {
        return stampId;
    }
    
    public void setStampName(String stampName) {
        this.stampName = stampName;
    }
    public String getStampName() {
        return stampName;
    }

    @Override
    public String toString() {
        return getText();
    }
}

/* $Id: SourceFileAttrInfo.java,v 1.11 2003/12/06 22:15:38 eric Exp $
 *
 * ProGuard -- obfuscation and shrinking package for Java class files.
 *
 * Copyright (c) 1999      Mark Welsh (markw@retrologic.com)
 * Copyright (c) 2002-2003 Eric Lafortune (eric@graphics.cornell.edu)
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package proguard.classfile;

import proguard.classfile.visitor.*;

import java.io.*;

/**
 * Representation of an attribute.
 *
 * @author Mark Welsh
 * @author Eric Lafortune
 */
public class SourceFileAttrInfo extends AttrInfo
{
    private static final int CONSTANT_FIELD_SIZE = 2;


    public int u2sourceFileIndex;


    protected SourceFileAttrInfo()
    {
    }


    // Implementations for AttrInfo.

    protected int getAttrInfoLength()
    {
        return CONSTANT_FIELD_SIZE;
    }

    protected void readInfo(DataInput din, ClassFile cf) throws IOException
    {
        u2sourceFileIndex = din.readUnsignedShort();
    }

    protected void writeInfo(DataOutput dout) throws IOException
    {
        dout.writeShort(u2sourceFileIndex);
    }

    public void accept(ClassFile classFile, AttrInfoVisitor attrInfoVisitor)
    {
        attrInfoVisitor.visitSourceFileAttrInfo(classFile, this);
    }
}

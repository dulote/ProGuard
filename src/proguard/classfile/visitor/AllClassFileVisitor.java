/* $Id: AllClassFileVisitor.java,v 1.8 2003/08/22 17:11:15 eric Exp $
 *
 * ProGuard -- obfuscation and shrinking package for Java class files.
 *
 * Copyright (c) 2002-2003 Eric Lafortune (eric@graphics.cornell.edu)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package proguard.classfile.visitor;

import proguard.classfile.*;


/**
 * This ClassPoolVisitor lets a given ClassFileVisitor visit all ClassFile
 * objects of the class pools it visits.
 *
 * @author Eric Lafortune
 */
public class AllClassFileVisitor implements ClassPoolVisitor
{
    private ClassFileVisitor classFileVisitor;


    public AllClassFileVisitor(ClassFileVisitor classFileVisitor)
    {
        this.classFileVisitor = classFileVisitor;
    }


    public void visitClassPool(ClassPool classPool)
    {
        classPool.classFilesAccept(classFileVisitor);
    }
}

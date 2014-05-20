/*******************************************************************************
 * PSHDL is a library and (trans-)compiler for PSHDL input. It generates
 *     output suitable for implementation or simulation of it.
 *
 *     Copyright (C) 2013 Karsten Becker (feedback (at) pshdl (dot) org)
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *     This License does not grant permission to use the trade names, trademarks,
 *     service marks, or product names of the Licensor, except as required for
 *     reasonable and customary use in describing the origin of the Work.
 *
 * Contributors:
 *     Karsten Becker - initial API and implementation
 ******************************************************************************/
package org.pshdl.generator.vhdl.libraries;

import java.util.List;

import de.upb.hni.vmagic.builtin.NumericStd;
import de.upb.hni.vmagic.builtin.Standard;
import de.upb.hni.vmagic.builtin.StdLogic1164;
import de.upb.hni.vmagic.declaration.FunctionDeclaration;
import de.upb.hni.vmagic.declaration.PackageDeclarativeItem;
import de.upb.hni.vmagic.libraryunit.PackageDeclaration;
import de.upb.hni.vmagic.libraryunit.UseClause;
import de.upb.hni.vmagic.object.Constant;
import de.upb.hni.vmagic.type.SubtypeIndication;

public class VHDLTypesLibrary {
	public static final UseClause USE_CLAUSE = new UseClause("work.Types.ALL");
	public static final PackageDeclaration PACKAGE;
	public static final FunctionDeclaration TERNARY_INTEGER;
	public static final FunctionDeclaration TERNARY_SL;
	public static final FunctionDeclaration TERNARY_SLV;
	public static final FunctionDeclaration TERNARY_UNSIGNED;
	public static final FunctionDeclaration TERNARY_SIGNED;
	public static final FunctionDeclaration LOG2CEIL;
	public static final FunctionDeclaration LOG2FLOOR;
	static {
		PACKAGE = new PackageDeclaration("pshdl.Types");
		final List<PackageDeclarativeItem> declarations = PACKAGE.getDeclarations();
		TERNARY_INTEGER = createTernaryOp(declarations, Standard.INTEGER);
		TERNARY_SL = createTernaryOp(declarations, StdLogic1164.STD_LOGIC);
		TERNARY_SLV = createTernaryOp(declarations, StdLogic1164.STD_LOGIC_VECTOR);
		TERNARY_UNSIGNED = createTernaryOp(declarations, NumericStd.UNSIGNED);
		TERNARY_SIGNED = createTernaryOp(declarations, NumericStd.SIGNED);
		LOG2CEIL = new FunctionDeclaration("log2ceil", Standard.INTEGER, new Constant("num", Standard.INTEGER));
		LOG2FLOOR = new FunctionDeclaration("log2floor", Standard.INTEGER, new Constant("num", Standard.INTEGER));
	}

	private static FunctionDeclaration createTernaryOp(List<PackageDeclarativeItem> declarations, SubtypeIndication type) {
		final FunctionDeclaration fd = new FunctionDeclaration("ternaryOp", type, new Constant("condition", Standard.BOOLEAN), new Constant("thenValue", type), new Constant(
				"elseValue", type));
		declarations.add(fd);
		return fd;
	}
}

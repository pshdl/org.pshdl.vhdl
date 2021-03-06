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

import org.pshdl.model.HDLPrimitive;
import org.pshdl.model.HDLPrimitive.HDLPrimitiveType;
import org.pshdl.model.HDLShiftOp.HDLShiftOpType;

import de.upb.hni.vmagic.AssociationElement;
import de.upb.hni.vmagic.builtin.Standard;
import de.upb.hni.vmagic.declaration.Function;
import de.upb.hni.vmagic.declaration.FunctionDeclaration;
import de.upb.hni.vmagic.declaration.PackageDeclarativeItem;
import de.upb.hni.vmagic.expression.Expression;
import de.upb.hni.vmagic.expression.FunctionCall;
import de.upb.hni.vmagic.libraryunit.PackageDeclaration;
import de.upb.hni.vmagic.libraryunit.UseClause;
import de.upb.hni.vmagic.object.Constant;
import de.upb.hni.vmagic.type.SubtypeIndication;

public class VHDLShiftLibrary {
	public static final UseClause USE_CLAUSE = new UseClause("work.ShiftOps.ALL");
	public static final PackageDeclaration PACKAGE;
	static {
		PACKAGE = new PackageDeclaration("pshdl.ShiftOps");
		final List<PackageDeclarativeItem> declarations = PACKAGE.getDeclarations();
		final HDLPrimitiveType[] values = HDLPrimitiveType.values();
		for (final HDLShiftOpType op : HDLShiftOpType.values()) {
			for (final HDLPrimitiveType left : values) {
				final String name = getFunctionName(op, left);
				if (HDLPrimitive.isAny(left)) {
					continue;
				}
				final SubtypeIndication lt = VHDLCastsLibrary.getType(left);
				if (lt != null) {
					final FunctionDeclaration fd = new FunctionDeclaration(name, VHDLCastsLibrary.getType(left), new Constant("arg", VHDLCastsLibrary.getType(left)),
							new Constant("s", Standard.NATURAL));
					declarations.add(fd);
				}
			}
		}
	}

	private static String getFunctionName(HDLShiftOpType op, HDLPrimitiveType left) {
		final String rightName = left.name().charAt(0) + left.name().substring(1).toLowerCase();
		final String name = op.name().toLowerCase() + rightName;
		return name;
	}

	public static Expression shift(Expression vhdlExpr, Expression amount, HDLPrimitiveType type, HDLShiftOpType op) {
		final String name = getFunctionName(op, type);
		final Function resolve = PACKAGE.getScope().resolve(name, Function.class);
		final FunctionCall call = new FunctionCall(resolve);
		call.getParameters().add(new AssociationElement(vhdlExpr));
		call.getParameters().add(new AssociationElement(amount));
		return call;
	}
}

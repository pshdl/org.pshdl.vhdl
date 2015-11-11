/*******************************************************************************
 * PSHDL is a library and (trans-)compiler for PSHDL input. It generates
 *     output suitable for implementation or simulation of it.
 *
 *     Copyright (C) 2014 Karsten Becker (feedback (at) pshdl (dot) org)
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
package org.pshdl.generator.vhdl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;

import org.pshdl.generator.vhdl.libraries.VHDLTypesLibrary;
import org.pshdl.model.HDLAssignment;
import org.pshdl.model.HDLEnumRef;
import org.pshdl.model.HDLExpression;
import org.pshdl.model.HDLFunctionCall;
import org.pshdl.model.HDLLiteral;
import org.pshdl.model.HDLManip;
import org.pshdl.model.HDLManip.HDLManipType;
import org.pshdl.model.HDLPrimitive;
import org.pshdl.model.HDLRange;
import org.pshdl.model.HDLVariableRef;
import org.pshdl.model.IHDLObject;
import org.pshdl.model.evaluation.ConstantEvaluate;
import org.pshdl.model.evaluation.HDLEvaluationContext;
import org.pshdl.model.types.builtIn.HDLBuiltInFunctions.BuiltInFunctions;
import org.pshdl.model.types.builtIn.TestbenchFunctions.SimulationFunctions;
import org.pshdl.model.utils.HDLCore;
import org.pshdl.model.utils.HDLQualifiedName;

import com.google.common.base.Enums;
import com.google.common.base.Optional;

import de.upb.hni.vmagic.AssociationElement;
import de.upb.hni.vmagic.Range.Direction;
import de.upb.hni.vmagic.builtin.Standard;
import de.upb.hni.vmagic.declaration.FunctionDeclaration;
import de.upb.hni.vmagic.expression.Aggregate;
import de.upb.hni.vmagic.expression.Expression;
import de.upb.hni.vmagic.expression.FunctionCall;
import de.upb.hni.vmagic.literal.CharacterLiteral;
import de.upb.hni.vmagic.literal.PhysicalLiteral;
import de.upb.hni.vmagic.output.VhdlOutput;
import de.upb.hni.vmagic.statement.AssertionStatement;
import de.upb.hni.vmagic.statement.WaitStatement;
import de.upb.hni.vmagic.type.UnresolvedType;

public class VHDLFunctions implements IVHDLCodeFunctionProvider {

	public VHDLFunctions() {
	}

	private static Collection<IVHDLCodeFunctionProvider> codeProvider;

	@Override
	public Expression toVHDLExpression(HDLFunctionCall function) {
		final HDLQualifiedName refName = function.getFunctionRefName();
		final Optional<BuiltInFunctions> e = Enums.getIfPresent(BuiltInFunctions.class, refName.getLastSegment());
		if (e.isPresent()) {
			switch (e.get()) {
			case max:
			case min:
			case abs:
				final FunctionDeclaration fd = new FunctionDeclaration(function.getFunctionRefName().getLastSegment(), UnresolvedType.NO_NAME);
				final FunctionCall res = new FunctionCall(fd);
				addArguments(function, res);
				return res;
			case highZ:
				if (function.getParams().size() == 0)
					return new CharacterLiteral('Z');

				final Aggregate aggregate = new Aggregate();
				final HDLRange range = new HDLRange().setFrom(HDLLiteral.get(1)).setTo(function.getParams().get(0));
				aggregate.createAssociation(new CharacterLiteral('Z'), VHDLExpressionExtension.INST.toVHDL(range, Direction.TO));
				return aggregate;
			case assertThat:
				return null;
			case log2ceil: {
				final FunctionCall fc = new FunctionCall(VHDLTypesLibrary.LOG2CEIL);
				addArguments(function, fc);
				return fc;
			}
			case log2floor: {
				final FunctionCall fc = new FunctionCall(VHDLTypesLibrary.LOG2FLOOR);
				addArguments(function, fc);
				return fc;
			}
			}

		}
		return null;
	}

	public void addArguments(HDLFunctionCall function, FunctionCall fc) {
		for (final HDLExpression exp : function.getParams()) {
			fc.getParameters().add(new AssociationElement(VHDLExpressionExtension.vhdlOf(exp)));
		}
	}

	@Override
	public VHDLContext toVHDLStatement(HDLFunctionCall function, int pid, HDLEvaluationContext context) {
		final HDLQualifiedName refName = function.getFunctionRefName();
		final Optional<SimulationFunctions> e = Enums.getIfPresent(SimulationFunctions.class, refName.getLastSegment());
		if (e.isPresent()) {
			final SimulationFunctions func = e.get();
			switch (func) {
			case wait: {
				final VHDLContext res = new VHDLContext();
				res.setNoSensitivity(pid);
				final WaitStatement ws = new WaitStatement();
				res.addUnclockedStatement(pid, ws, function);
				return res;
			}
			case waitFor: {
				final VHDLContext res = new VHDLContext();
				res.setNoSensitivity(pid);
				final HDLEnumRef ref = (HDLEnumRef) function.getParams().get(1);
				final BigInteger hdlExpression = ConstantEvaluate.valueOfForced(function.getParams().get(0), context, "VHDL");
				final Expression hdlLiteral = VHDLExpressionExtension.vhdlOf(HDLLiteral.get(hdlExpression));
				final WaitStatement ws = new WaitStatement(new PhysicalLiteral(VhdlOutput.toVhdlString(hdlLiteral), ref.getVarRefName().getLastSegment()));
				res.addUnclockedStatement(pid, ws, function);
				return res;
			}
			case pulse: {
				final ArrayList<HDLExpression> params = function.getParams();
				final HDLVariableRef ref = getVarRef(params.get(0));
				final VHDLContext res = new VHDLContext();
				res.setNoSensitivity(pid);
				final IHDLObject container = function.getContainer();
				HDLAssignment ass = setValue(ref, 0, container);
				res.addUnclockedStatement(pid, VHDLStatementExtension.vhdlOf(ass, pid).getStatement(), ass);
				HDLFunctionCall wait = new HDLFunctionCall().setFunction(SimulationFunctions.waitFor.getName()).addParams(params.get(1)).addParams(params.get(2))
						.copyDeepFrozen(container);
				res.addUnclockedStatement(pid, VHDLStatementExtension.vhdlOf(wait, pid).getStatement(), wait);
				ass = setValue(ref, 1, container);
				res.addUnclockedStatement(pid, VHDLStatementExtension.vhdlOf(ass, pid).getStatement(), ass);
				wait = new HDLFunctionCall().setFunction(SimulationFunctions.waitFor.getName()).addParams(params.get(1)).addParams(params.get(2)).copyDeepFrozen(container);
				res.addUnclockedStatement(pid, VHDLStatementExtension.vhdlOf(wait, pid).getStatement(), wait);
				return res;
			}
			case waitUntil: {
				final VHDLContext res = new VHDLContext();
				res.setNoSensitivity(pid);
				final WaitStatement ws = new WaitStatement(VHDLExpressionExtension.vhdlOf(function.getParams().get(0)), null);
				res.addUnclockedStatement(pid, ws, function);
				return res;
			}
			}
		}
		final Optional<BuiltInFunctions> b = Enums.getIfPresent(BuiltInFunctions.class, refName.getLastSegment());
		if (b.isPresent()) {
			if (b.get() == BuiltInFunctions.assertThat) {
				final ArrayList<HDLExpression> params = function.getParams();
				final Expression condition = VHDLExpressionExtension.vhdlOf(params.get(0));
				final Expression report = VHDLExpressionExtension.vhdlOf(params.get(2));
				final String varRef = ((HDLEnumRef) params.get(1)).getVarRefName().getLastSegment();
				Expression severity = null;
				switch (varRef) {
				case "FATAL":
					severity = Standard.SEVERITY_LEVEL_FAILURE;
					break;
				case "ERROR":
					severity = Standard.SEVERITY_LEVEL_ERROR;
					break;
				case "WARNING":
					severity = Standard.SEVERITY_LEVEL_WARNING;
					break;
				case "INFO":
					severity = Standard.SEVERITY_LEVEL_NOTE;
					break;
				}
				final VHDLContext res = new VHDLContext();
				res.addUnclockedStatement(pid, new AssertionStatement(condition, report, severity), function);
				return res;
			}
		}
		return null;
	}

	private static HDLAssignment setValue(HDLVariableRef ref, int value, IHDLObject container) {
		final HDLManip val = new HDLManip().setCastTo(HDLPrimitive.getBit()).setType(HDLManipType.CAST).setTarget(HDLLiteral.get(value));
		return new HDLAssignment().setLeft(ref).setRight(val).copyDeepFrozen(container);
	}

	private static HDLVariableRef getVarRef(HDLExpression hdlExpression) {
		if (hdlExpression instanceof HDLVariableRef) {
			final HDLVariableRef ref = (HDLVariableRef) hdlExpression;
			return ref;
		}
		if (hdlExpression instanceof HDLManip) {
			final HDLManip manip = (HDLManip) hdlExpression;
			return getVarRef(manip.getTarget());
		}
		return null;
	}

	public static VHDLContext toOutputStatement(HDLFunctionCall call, int pid, HDLEvaluationContext context) {
		for (final IVHDLCodeFunctionProvider provider : getCodeProvider()) {
			final VHDLContext vhdlContext = provider.toVHDLStatement(call, pid, context);
			if (vhdlContext != null)
				return vhdlContext;
		}
		return null;
	}

	private static Collection<IVHDLCodeFunctionProvider> getCodeProvider() {
		if (codeProvider == null) {
			codeProvider = HDLCore.getAllImplementations(IVHDLCodeFunctionProvider.class);
		}
		return codeProvider;
	}

	public static Expression toOutputExpression(HDLFunctionCall call) {
		for (final IVHDLCodeFunctionProvider provider : getCodeProvider()) {
			final Expression vhdlContext = provider.toVHDLExpression(call);
			if (vhdlContext != null)
				return vhdlContext;
		}
		return null;
	}

}

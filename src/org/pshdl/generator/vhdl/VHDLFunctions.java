package org.pshdl.generator.vhdl;

import java.math.*;
import java.util.*;

import org.pshdl.model.*;
import org.pshdl.model.HDLManip.HDLManipType;
import org.pshdl.model.evaluation.*;
import org.pshdl.model.types.builtIn.HDLBuiltInFunctions.BuiltInFunctions;
import org.pshdl.model.types.builtIn.TestbenchFunctions.SimulationFunctions;
import org.pshdl.model.utils.*;

import com.google.common.base.*;

import de.upb.hni.vmagic.*;
import de.upb.hni.vmagic.Range.Direction;
import de.upb.hni.vmagic.declaration.*;
import de.upb.hni.vmagic.expression.*;
import de.upb.hni.vmagic.literal.*;
import de.upb.hni.vmagic.statement.*;
import de.upb.hni.vmagic.type.*;

public class VHDLFunctions implements IVHDLCodeFunctionProvider {

	public VHDLFunctions() {
	}

	private static Collection<IVHDLCodeFunctionProvider> codeProvider = HDLCore.getAllImplementations(IVHDLCodeFunctionProvider.class);

	@Override
	public Expression toVHDLExpression(HDLFunctionCall function) {
		final HDLQualifiedName refName = function.getNameRefName();
		final Optional<BuiltInFunctions> e = Enums.getIfPresent(BuiltInFunctions.class, refName.getLastSegment());
		if (e.isPresent()) {
			switch (e.get()) {
			case max:
			case min:
			case abs:
				final FunctionDeclaration fd = new FunctionDeclaration(function.getNameRefName().getLastSegment(), UnresolvedType.NO_NAME);
				final FunctionCall res = new FunctionCall(fd);
				for (final HDLExpression exp : function.getParams()) {
					res.getParameters().add(new AssociationElement(VHDLExpressionExtension.vhdlOf(exp)));
				}
				return res;
			case highZ:
				if (function.getParams().size() == 0)
					return new CharacterLiteral('Z');

				final Aggregate aggregate = new Aggregate();
				final HDLRange range = new HDLRange().setFrom(HDLLiteral.get(1)).setTo(function.getParams().get(0));
				aggregate.createAssociation(new CharacterLiteral('Z'), VHDLExpressionExtension.INST.toVHDL(range, Direction.TO));
				return aggregate;
			}

		}
		return null;
	}

	@Override
	public VHDLContext toVHDLStatement(HDLFunctionCall function, int pid) {
		final HDLQualifiedName refName = function.getNameRefName();
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
				final Optional<BigInteger> hdlExpression = ConstantEvaluate.valueOf(function.getParams().get(0));
				if (!hdlExpression.isPresent())
					throw new IllegalArgumentException(function.getParams().get(0) + " is not constant");
				final WaitStatement ws = new WaitStatement(new PhysicalLiteral(hdlExpression.get().toString(), ref.getVarRefName().getLastSegment()));
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
				HDLFunctionCall wait = new HDLFunctionCall().setName(SimulationFunctions.waitFor.getName()).addParams(params.get(1)).addParams(params.get(2))
						.copyDeepFrozen(container);
				res.addUnclockedStatement(pid, VHDLStatementExtension.vhdlOf(wait, pid).getStatement(), wait);
				ass = setValue(ref, 1, container);
				res.addUnclockedStatement(pid, VHDLStatementExtension.vhdlOf(ass, pid).getStatement(), ass);
				wait = new HDLFunctionCall().setName(SimulationFunctions.waitFor.getName()).addParams(params.get(1)).addParams(params.get(2)).copyDeepFrozen(container);
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

	public static VHDLContext toOutputStatement(HDLFunctionCall call, int pid) {
		for (final IVHDLCodeFunctionProvider provider : codeProvider) {
			final VHDLContext vhdlContext = provider.toVHDLStatement(call, pid);
			if (vhdlContext != null)
				return vhdlContext;
		}
		return null;
	}

	public static Expression toOutputExpression(HDLFunctionCall call) {
		for (final IVHDLCodeFunctionProvider provider : codeProvider) {
			final Expression vhdlContext = provider.toVHDLExpression(call);
			if (vhdlContext != null)
				return vhdlContext;
		}
		return null;
	}

}

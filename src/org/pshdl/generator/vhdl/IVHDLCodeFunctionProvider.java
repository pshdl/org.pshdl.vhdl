package org.pshdl.generator.vhdl;

import org.pshdl.model.*;

import de.upb.hni.vmagic.expression.*;

public interface IVHDLCodeFunctionProvider {
	public VHDLContext toVHDLStatement(HDLFunctionCall call, int i);

	public Expression<?> toVHDLExpression(HDLFunctionCall call);
}

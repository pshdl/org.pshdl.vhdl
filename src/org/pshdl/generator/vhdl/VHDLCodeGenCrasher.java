package org.pshdl.generator.vhdl;

import org.pshdl.model.HDLPackage;
import org.pshdl.model.utils.TestcaseReducer.CrashValidator.CrashValidatorRunnable;

public class VHDLCodeGenCrasher implements CrashValidatorRunnable {

	@Override
	public void run(String src, HDLPackage pkg) throws Throwable {
		final PStoVHDLCompiler compiler = new PStoVHDLCompiler();
		compiler.doCompile(src, pkg);
	}

}
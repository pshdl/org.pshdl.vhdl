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
package org.pshdl.generator.vhdl;

import static org.pshdl.model.extensions.FullNameExtension.fullNameOf;
import static org.pshdl.model.validation.Problem.ProblemSeverity.ERROR;
import static org.pshdl.model.validation.Problem.ProblemSeverity.WARNING;

import java.util.Map;
import java.util.Set;

import org.pshdl.model.HDLEnum;
import org.pshdl.model.HDLInterface;
import org.pshdl.model.HDLPackage;
import org.pshdl.model.HDLUnit;
import org.pshdl.model.HDLVariable;
import org.pshdl.model.evaluation.HDLEvaluationContext;
import org.pshdl.model.utils.HDLQualifiedName;
import org.pshdl.model.utils.services.IHDLValidator;
import org.pshdl.model.validation.HDLValidator.HDLAdvise;
import org.pshdl.model.validation.Problem;
import org.pshdl.model.validation.Problem.ProblemSeverity;

public class VHDLOutputValidator implements IHDLValidator {

	public static enum VHDLErrorCode implements IErrorCode {
		KEYWORD_NAME(WARNING), KEYWORD_TYPE(ERROR), PARSE_ERROR(ERROR);

		public final ProblemSeverity severity;

		VHDLErrorCode(ProblemSeverity severity) {
			this.severity = severity;
		}

		@Override
		public ProblemSeverity getSeverity() {
			return severity;
		}

	}

	@Override
	public Class<?> getErrorClass() {
		return VHDLErrorCode.class;
	}

	@Override
	public HDLAdvise advise(Problem problem) {
		final IErrorCode code = problem.code;
		if (code instanceof VHDLErrorCode) {
			final VHDLErrorCode vCode = (VHDLErrorCode) code;
			switch (vCode) {
			case KEYWORD_NAME:
				return new HDLAdvise(problem, "The used name is a keyword or an extended identifier in VHDL",
						"Keywords will be escaped using the extended identifier convention, this may look strange, but is fully working",
						"Don't use a VHDL keyword or don't end with an underscore as identifier");
			case KEYWORD_TYPE:
				return new HDLAdvise(problem, "The used type name is a keyword in VHDL", "Keywords are not supported for type names", "Don't use a VHDL keyword as identifier");

			}
		}
		return null;
	}

	@Override
	public boolean check(HDLPackage unit, Set<Problem> problems, Map<HDLQualifiedName, HDLEvaluationContext> context) {
		checkNames(unit, problems);
		return true;
	}

	private void checkNames(HDLPackage unit, Set<Problem> problems) {
		final HDLVariable[] vars = unit.getAllObjectsOf(HDLVariable.class, true);
		for (final HDLVariable hdlVariable : vars) {
			final String varName = hdlVariable.getName();
			if (!VHDLUtils.getVHDLName(varName).equals(varName)) {
				problems.add(new Problem(VHDLErrorCode.KEYWORD_NAME, hdlVariable));
			}
		}
		final HDLUnit[] units = unit.getAllObjectsOf(HDLUnit.class, true);
		for (final HDLUnit hdlUnit : units) {
			final String name = fullNameOf(hdlUnit).toString('_');
			if (VHDLUtils.isKeyword(name)) {
				problems.add(new Problem(VHDLErrorCode.KEYWORD_TYPE, hdlUnit));
			}
		}
		final HDLInterface[] ifs = unit.getAllObjectsOf(HDLInterface.class, true);
		for (final HDLInterface hdlUnit : ifs) {
			final String name = fullNameOf(hdlUnit).toString('_');
			if (VHDLUtils.isKeyword(name)) {
				problems.add(new Problem(VHDLErrorCode.KEYWORD_TYPE, hdlUnit));
			}
		}
		final HDLEnum[] enums = unit.getAllObjectsOf(HDLEnum.class, true);
		for (final HDLEnum hdlUnit : enums) {
			final String name = fullNameOf(hdlUnit).toString('_');
			if (VHDLUtils.isKeyword(name)) {
				problems.add(new Problem(VHDLErrorCode.KEYWORD_TYPE, hdlUnit));
			}
		}
	}

	@Override
	public String getName() {
		return "VHDL Validator";
	}

}

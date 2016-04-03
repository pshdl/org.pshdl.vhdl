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

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.antlr.runtime.RecognitionException;
import org.pshdl.generator.vhdl.VHDLOutputValidator.VHDLErrorCode;
import org.pshdl.model.HDLAnnotation;
import org.pshdl.model.HDLArithOp;
import org.pshdl.model.HDLArithOp.HDLArithOpType;
import org.pshdl.model.HDLExpression;
import org.pshdl.model.HDLInterface;
import org.pshdl.model.HDLLiteral;
import org.pshdl.model.HDLObject.GenericMeta;
import org.pshdl.model.HDLPrimitive;
import org.pshdl.model.HDLPrimitive.HDLPrimitiveType;
import org.pshdl.model.HDLUnresolvedFragment;
import org.pshdl.model.HDLVariable;
import org.pshdl.model.HDLVariableDeclaration;
import org.pshdl.model.HDLVariableDeclaration.HDLDirection;
import org.pshdl.model.evaluation.ConstantEvaluate;
import org.pshdl.model.parser.SourceInfo;
import org.pshdl.model.types.builtIn.HDLBuiltInAnnotationProvider.HDLBuiltInAnnotations;
import org.pshdl.model.utils.HDLLibrary;
import org.pshdl.model.utils.HDLProblemException;
import org.pshdl.model.utils.HDLQualifiedName;
import org.pshdl.model.utils.MetaAccess;
import org.pshdl.model.validation.Problem;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import de.upb.hni.vmagic.DiscreteRange;
import de.upb.hni.vmagic.LibraryDeclarativeRegion;
import de.upb.hni.vmagic.Range;
import de.upb.hni.vmagic.Range.Direction;
import de.upb.hni.vmagic.RangeProvider;
import de.upb.hni.vmagic.RootDeclarativeRegion;
import de.upb.hni.vmagic.VhdlFile;
import de.upb.hni.vmagic.builtin.NumericStd;
import de.upb.hni.vmagic.builtin.Standard;
import de.upb.hni.vmagic.builtin.StdLogic1164;
import de.upb.hni.vmagic.declaration.PackageDeclarativeItem;
import de.upb.hni.vmagic.expression.BinaryExpression;
import de.upb.hni.vmagic.expression.Expression;
import de.upb.hni.vmagic.expression.ExpressionKind;
import de.upb.hni.vmagic.expression.Parentheses;
import de.upb.hni.vmagic.libraryunit.Entity;
import de.upb.hni.vmagic.libraryunit.LibraryUnit;
import de.upb.hni.vmagic.libraryunit.PackageDeclaration;
import de.upb.hni.vmagic.literal.BinaryLiteral;
import de.upb.hni.vmagic.literal.CharacterLiteral;
import de.upb.hni.vmagic.literal.DecimalLiteral;
import de.upb.hni.vmagic.literal.HexLiteral;
import de.upb.hni.vmagic.literal.StringLiteral;
import de.upb.hni.vmagic.object.Constant;
import de.upb.hni.vmagic.object.Signal;
import de.upb.hni.vmagic.object.VhdlObjectProvider;
import de.upb.hni.vmagic.output.VhdlOutput;
import de.upb.hni.vmagic.parser.ParseError;
import de.upb.hni.vmagic.parser.VhdlParserException;
import de.upb.hni.vmagic.parser.VhdlParserExceptionThrower;
import de.upb.hni.vmagic.parser.VhdlParserSettings;
import de.upb.hni.vmagic.parser.annotation.PositionInformation;
import de.upb.hni.vmagic.parser.annotation.SourcePosition;
import de.upb.hni.vmagic.type.ConstrainedArray;
import de.upb.hni.vmagic.type.EnumerationType;
import de.upb.hni.vmagic.type.IndexSubtypeIndication;
import de.upb.hni.vmagic.type.RangeSubtypeIndication;
import de.upb.hni.vmagic.type.SubtypeIndication;
import de.upb.hni.vmagic.type.Type;
import de.upb.hni.vmagic.type.UnconstrainedArray;

@SuppressWarnings("rawtypes")
public class VHDLImporter {
	private static class Scopes {
		public final RootDeclarativeRegion rootScope;
		public final LibraryDeclarativeRegion workScope;

		public Scopes(RootDeclarativeRegion rootScope, LibraryDeclarativeRegion workScope) {
			super();
			this.rootScope = rootScope;
			this.workScope = workScope;
		}
	}

	@SuppressWarnings("unchecked")
	public static List<HDLInterface> importFile(HDLQualifiedName pkg, InputStream is, HDLLibrary lib, String src) throws IOException, HDLProblemException {
		final Scopes scopes = getScopes(lib);
		final List<HDLInterface> res = Lists.newLinkedList();
		final VhdlParserSettings vhdlParserSettings = new VhdlParserSettings();
		vhdlParserSettings.setPrintErrors(false);
		VhdlFile file;
		try {
			file = VhdlParserExceptionThrower.parseStream(is, vhdlParserSettings, scopes.rootScope, scopes.workScope);
			final List<ParseError> parseErrors = VhdlParserExceptionThrower.getParseErrors(file);
			if (!parseErrors.isEmpty()) {
				final List<Problem> problems = Lists.newArrayList();
				for (final ParseError parseError : parseErrors) {
					final PositionInformation pos = parseError.getPosition();
					final SourcePosition begin = pos.getBegin();
					final int length = pos.getEnd().getIndex() - begin.getIndex();
					problems.add(new Problem(VHDLErrorCode.PARSE_ERROR, parseError.getMessage(), begin.getLine(), begin.getColumn(), length, begin.getIndex()));
				}
				throw new HDLProblemException(problems.toArray(new Problem[problems.size()]));
			}
		} catch (final RecognitionException e) {
			throw new HDLProblemException(new Problem(VHDLErrorCode.PARSE_ERROR, e.getMessage(), e.line, e.charPositionInLine, e.token.getText().length(), -1));
		}
		final List<LibraryUnit> list = file.getElements();
		for (final LibraryUnit unit : list) {
			if (unit instanceof Entity) {
				final Entity entity = (Entity) unit;
				final String id = entity.getIdentifier();
				HDLInterface vInterface = new HDLInterface().setName(pkg.append(id).toString());
				final List<String> comments = Lists.newArrayList();
				final List<VhdlObjectProvider<Constant>> param = entity.getGeneric();
				for (final VhdlObjectProvider port : param) {
					final List<Constant> signals = port.getVhdlObjects();
					for (final Constant signal : signals) {
						final HDLDirection direction = HDLDirection.valueOf(signal.getMode().getUpperCase());
						final HDLQualifiedName qfn = pkg.append(id).append(signal.getIdentifier());
						final Optional<HDLVariableDeclaration> var = getVariable(signal.getDefaultValue(), signal.getType(), direction, qfn, null, new ArrayList<HDLExpression>(),
								scopes);
						if (var.isPresent()) {
							vInterface = vInterface.addPorts(var.get().setDirection(HDLDirection.PARAMETER));
						} else {
							comments.add("WARNING: Failed to properly import signal:" + VhdlOutput.toVhdlString(signal));
						}
					}
				}
				final List<VhdlObjectProvider<Signal>> ports = entity.getPort();
				for (final VhdlObjectProvider port : ports) {
					final List<Signal> signals = port.getVhdlObjects();
					for (final Signal signal : signals) {
						HDLDirection direction;
						try {
							direction = HDLDirection.valueOf(signal.getMode().getUpperCase());
							final HDLQualifiedName qfn = pkg.append(id).append(signal.getIdentifier());
							final Optional<HDLVariableDeclaration> var = getVariable(null, signal.getType(), direction, qfn, null, new ArrayList<HDLExpression>(), scopes);
							if (var.isPresent()) {
								vInterface = vInterface.addPorts(var.get());
							} else {
								comments.add("WARNING: Failed to properly import:" + VhdlOutput.toVhdlString(signal));
							}
						} catch (final Exception e) {
							comments.add("WARNING: Failed to properly import:" + VhdlOutput.toVhdlString(signal) + " " + e.getMessage());
						}
					}
				}
				vInterface = vInterface.copyDeepFrozen(null);
				vInterface.addMeta(SourceInfo.COMMENT, comments);
				res.add(vInterface);
				lib.addInterface(vInterface, src);
			}
			getScopes(lib).workScope.getFiles().add(file);
		}
		return res;
	}

	private final static MetaAccess<Scopes> SCOPES = new GenericMeta<>("SCOPES", true);

	private static Scopes getScopes(HDLLibrary lib) {
		Scopes scopes = lib.getMeta(SCOPES);
		if (scopes != null)
			return scopes;
		final RootDeclarativeRegion rootScope = new RootDeclarativeRegion();
		final LibraryDeclarativeRegion workScope = new LibraryDeclarativeRegion("work");
		rootScope.getLibraries().add(workScope);
		scopes = new Scopes(rootScope, workScope);
		lib.addMeta(SCOPES, scopes);
		return scopes;
	}

	public static Optional<HDLVariableDeclaration> getVariable(Expression defaultValue, SubtypeIndication left, HDLDirection direction, HDLQualifiedName qfn, HDLExpression width,
			ArrayList<HDLExpression> dimensions, Scopes scopes) {
		if (left instanceof IndexSubtypeIndication) {
			final IndexSubtypeIndication isi = (IndexSubtypeIndication) left;
			final Range dr = (Range) isi.getRanges().get(0);
			final Optional<? extends HDLExpression> convertRange = convertRange(dr);
			if (!convertRange.isPresent())
				return Optional.absent();
			return getVariable(defaultValue, isi.getBaseType(), direction, qfn, convertRange.get(), dimensions, scopes);
		}
		if (StdLogic1164.STD_LOGIC.equals(left) || StdLogic1164.STD_ULOGIC.equals(left) || Standard.BIT.equals(left))
			return createVar(defaultValue, direction, HDLPrimitiveType.BIT, qfn, width, dimensions);
		final boolean isBitVector = Standard.BIT_VECTOR.equals(left);
		if (StdLogic1164.STD_LOGIC_VECTOR.equals(left) || StdLogic1164.STD_ULOGIC.equals(left) || isBitVector)
			return createVar(defaultValue, direction, HDLPrimitiveType.BITVECTOR, qfn, width, dimensions);
		if (NumericStd.SIGNED.equals(left))
			return createVar(defaultValue, direction, HDLPrimitiveType.INT, qfn, width, dimensions);
		if (NumericStd.UNSIGNED.equals(left))
			return createVar(defaultValue, direction, HDLPrimitiveType.UINT, qfn, width, dimensions);
		if (Standard.INTEGER.equals(left))
			return createVar(defaultValue, direction, HDLPrimitiveType.INTEGER, qfn, width, dimensions);
		if (Standard.NATURAL.equals(left))
			return createVar(defaultValue, direction, HDLPrimitiveType.NATURAL, qfn, width, dimensions);
		if (left instanceof RangeSubtypeIndication) {
			final RangeSubtypeIndication it = (RangeSubtypeIndication) left;
			final Optional<HDLVariableDeclaration> var = getVariable(defaultValue, it.getBaseType(), direction, qfn, width, dimensions, scopes);
			if (var.isPresent()) {
				HDLVariableDeclaration subVar = var.get();
				final RangeProvider rangeProvider = it.getRange();
				if (rangeProvider instanceof Range) {
					final Range range = (Range) rangeProvider;
					final Optional<? extends HDLExpression> from = getExpression(range.getFrom(), false);
					final Optional<? extends HDLExpression> to = getExpression(range.getTo(), false);
					if (from.isPresent() && to.isPresent())
						if (range.getDirection() == Direction.DOWNTO) {
							subVar = subVar.addAnnotations(HDLBuiltInAnnotations.range.create(to.get() + ";" + from.get()));
						} else {
							subVar = subVar.addAnnotations(HDLBuiltInAnnotations.range.create(from.get() + ";" + to.get()));
						}
				}
				return Optional.of(subVar);
			}
		}
		if (Standard.STRING.equals(left))
			return createVar(defaultValue, direction, HDLPrimitiveType.STRING, qfn, width, dimensions);
		if (Standard.BOOLEAN.equals(left))
			return createVar(defaultValue, direction, HDLPrimitiveType.BOOL, qfn, width, dimensions);
		if (left instanceof ConstrainedArray) {
			final ConstrainedArray ca = (ConstrainedArray) left;
			final List<DiscreteRange> ranges = ca.getIndexRanges();
			scopes.workScope.getScope().resolve(ca.getIdentifier());
			for (final DiscreteRange discreteRange : ranges) {
				final Optional<? extends HDLExpression> convertRange = convertRange((Range) discreteRange);
				if (!convertRange.isPresent())
					return Optional.absent();
				dimensions.add(convertRange.get());
			}
			final Optional<HDLVariableDeclaration> var = getVariable(defaultValue, ca.getElementType(), direction, qfn, null, dimensions, scopes);
			if (var.isPresent())
				return Optional
						.of(var.get().addAnnotations(new HDLAnnotation().setName(HDLBuiltInAnnotations.VHDLType.toString()).setValue(getFullName(ca.getIdentifier(), scopes))));
		}
		if (left instanceof UnconstrainedArray) {
			final UnconstrainedArray ca = (UnconstrainedArray) left;
			scopes.workScope.getScope().resolve(ca.getIdentifier());
			dimensions.add(HDLLiteral.get(-20));
			final Optional<HDLVariableDeclaration> var = getVariable(defaultValue, ca.getElementType(), direction, qfn, null, dimensions, scopes);
			if (var.isPresent())
				return Optional
						.of(var.get().addAnnotations(new HDLAnnotation().setName(HDLBuiltInAnnotations.VHDLType.toString()).setValue(getFullName(ca.getIdentifier(), scopes))));
		}
		if (left instanceof EnumerationType) {
			System.out.println("VHDLImporter.getVariable()" + ((EnumerationType) left).getIdentifier());
		}
		return Optional.absent();
	}

	private static String getFullName(String identifier, Scopes scopes) {
		String pkg = null;
		for (final Object lib : scopes.rootScope.getLibraries()) {
			for (final Object file : ((LibraryDeclarativeRegion) lib).getFiles()) {
				for (final Object libraryUnit : ((VhdlFile) file).getElements())
					if (libraryUnit instanceof PackageDeclaration) {
						final PackageDeclaration pd = (PackageDeclaration) libraryUnit;
						pkg = pd.getIdentifier();
						final List<PackageDeclarativeItem> declarations = pd.getDeclarations();
						for (final PackageDeclarativeItem pdi : declarations)
							if (pdi instanceof Type) {
								final Type t = (Type) pdi;
								if (t.getIdentifier().equalsIgnoreCase(identifier))
									return "work." + pkg + "." + identifier;
							}
					}
			}
		}
		return null;
	}

	private static Optional<? extends HDLExpression> convertRange(Range dr) {
		final Optional<? extends HDLExpression> from = getExpression(dr.getFrom(), false);
		if (!from.isPresent())
			return Optional.absent();
		final Optional<? extends HDLExpression> to = getExpression(dr.getTo(), false);
		if (!to.isPresent())
			return Optional.absent();
		HDLExpression width;
		if (dr.getDirection() == Direction.DOWNTO) {
			width = subThenPlus1(from.get(), to.get());
		} else {
			width = subThenPlus1(to.get(), from.get());
		}
		return Optional.of(width);
	}

	private static Optional<HDLVariableDeclaration> createVar(Expression defaultValue, HDLDirection direction, HDLPrimitiveType pt, HDLQualifiedName name, HDLExpression width,
			ArrayList<HDLExpression> dimensions) {
		final HDLPrimitive p = new HDLPrimitive().setType(pt).setWidth(width);
		HDLExpression hDefault = null;
		final HDLVariable variable = new HDLVariable().setName(name.getLastSegment()).setDimensions(dimensions);
		if (defaultValue != null) {
			final Optional<? extends HDLExpression> optional = getExpression(defaultValue, pt == HDLPrimitiveType.STRING);
			if (optional.isPresent()) {
				hDefault = optional.get();
			} else {
				variable.addMeta(SourceInfo.COMMENT, Arrays.asList("Failed to convert default value of:" + VhdlOutput.toVhdlString(defaultValue)));
			}
		}
		return Optional.of(new HDLVariableDeclaration().setDirection(direction).setType(p).addVariables(variable.setDefaultValue(hDefault)));
	}

	private static HDLExpression subThenPlus1(HDLExpression from, HDLExpression to) {
		final HDLArithOp left = HDLArithOp.subtract(from, to);
		final HDLArithOp op = HDLArithOp.add(left, 1).copyDeepFrozen(null);
		final Optional<BigInteger> constant = ConstantEvaluate.valueOf(op);
		if (constant.isPresent())
			return HDLLiteral.get(constant.get());
		return op;
	}

	@SuppressWarnings("incomplete-switch")
	private static Optional<? extends HDLExpression> getExpression(Expression from, boolean canBeString) {
		// TODO Support references to Generics
		if ((from instanceof HexLiteral) || (from instanceof BinaryLiteral) || (from instanceof CharacterLiteral)) {
			final String hex = from.toString();
			final String hexValue = hex.substring(2, hex.length() - 1);
			if (hexValue.length() != 0)
				return Optional.of(HDLLiteral.get(new BigInteger(hexValue, 16)));
			return Optional.of(HDLLiteral.get(0));
		}
		if (from instanceof DecimalLiteral) {
			final DecimalLiteral dl = (DecimalLiteral) from;
			return Optional.of(HDLLiteral.get(new BigInteger(dl.getValue())));
		}
		if (from instanceof StringLiteral) {
			final StringLiteral sl = (StringLiteral) from;
			return Optional.of(new HDLLiteral().setStr(canBeString).setVal(sl.getString()));
		}
		if (from instanceof BinaryExpression) {
			final BinaryExpression bin = (BinaryExpression) from;
			final Optional<? extends HDLExpression> left = getExpression(bin.getLeft(), false);
			if (!left.isPresent())
				return Optional.absent();
			final Optional<? extends HDLExpression> right = getExpression(bin.getRight(), false);
			if (!right.isPresent())
				return Optional.absent();
			final ExpressionKind kind = bin.getExpressionKind();
			switch (kind) {
			case PLUS:
				return Optional.of(new HDLArithOp().setLeft(left.get()).setType(HDLArithOpType.PLUS).setRight(right.get()));
			case MINUS:
				return Optional.of(new HDLArithOp().setLeft(left.get()).setType(HDLArithOpType.MINUS).setRight(right.get()));
			case MULTIPLY:
				return Optional.of(new HDLArithOp().setLeft(left.get()).setType(HDLArithOpType.MUL).setRight(right.get()));
			case DIVIDE:
				return Optional.of(new HDLArithOp().setLeft(left.get()).setType(HDLArithOpType.DIV).setRight(right.get()));
			case MOD:
				return Optional.of(new HDLArithOp().setLeft(left.get()).setType(HDLArithOpType.MOD).setRight(right.get()));
			case POW:
				return Optional.of(new HDLArithOp().setLeft(left.get()).setType(HDLArithOpType.POW).setRight(right.get()));
			}
		}
		if (from instanceof Constant) {
			final Constant c = (Constant) from;
			return getExpression(c.getDefaultValue(), true);
		}
		if (Standard.BOOLEAN_FALSE.equals(from))
			return Optional.of(HDLLiteral.getFalse());
		if (Standard.BOOLEAN_TRUE.equals(from))
			return Optional.of(HDLLiteral.getTrue());
		if (from instanceof Parentheses) {
			final Parentheses p = (Parentheses) from;
			return getExpression(p.getExpression(), canBeString);
		}
		if (from instanceof Signal) {
			final Signal signal = (Signal) from;
			return Optional.of(new HDLUnresolvedFragment().setIsStatement(false).setFrag(signal.getIdentifier()));
		}
		return Optional.absent();
	}

	public static void main(String[] args) throws IOException {
		final String targetPackage = args[0];
		final HDLLibrary lib = new HDLLibrary();
		for (int i = 1; i < args.length; i++) {
			final String string = args[i];
			final File file = new File(string);
			if (file.isDirectory()) {
				final File[] files = file.listFiles(new FilenameFilter() {

					@Override
					public boolean accept(File arg0, String arg1) {
						return arg1.endsWith("vhd");
					}
				});
				if (files != null) {
					for (final File f : files) {
						try {
							importFile(f, lib, targetPackage);
						} catch (final Exception e) {
							e.printStackTrace();
						}
					}
				}
			} else {
				try {
					importFile(file, lib, targetPackage);
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private static void importFile(File f, HDLLibrary lib, String targetPackage) throws IOException, VhdlParserException {
		try (final FileInputStream fis = new FileInputStream(f)) {
			final List<HDLInterface> hifs = importFile(new HDLQualifiedName(targetPackage), fis, lib, f.getAbsolutePath());
			for (final HDLInterface hdi : hifs) {
				System.out.println(hdi);
			}
		}
	}
}
